package com.example.transrator.capture

import android.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import com.example.transrator.data.local.AppDatabase
import com.example.transrator.data.remote.RetrofitClient
import com.example.transrator.data.local.TokenManager
import com.example.transrator.data.remote.TranslateRequest
import com.example.transrator.data.local.TranslationCacheEntity

class CaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private lateinit var windowManager: WindowManager
    private var captureButton: View? = null

    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    @Volatile private var ocrPaused = false   // 번역 표시 중 OCR 정지 여부

    private val db by lazy { AppDatabase.getInstance(applicationContext) }
    private val tokenManager by lazy { TokenManager(applicationContext) }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var captureJob: Job? = null

    private val recognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }

    private val detector = ChangeDetector(stableRequired = 2)

    // 번역 응답 겹침 방지 (최신 요청만 반영)
    @Volatile private var requestId = 0
    // 탭이 들어올 때마다 증가: 탭 이전 화면에서 시작된 OCR 결과를 버리기 위함
    @Volatile private var generation = 0

    companion object {
        // 이름표 제외, 본문 텍스트만 크롭 (나중에 유저 지정 영역으로 교체)
        val DIALOG_REGION = RectF(0f, 0.54f, 1f, 0.62f)
        const val CAPTURE_INTERVAL_MS = 500L
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi

        // 유저 탭 감지 → 이전 대사 결과 무효화 + OCR 재개
        serviceScope.launch {
            CaptureResult.resumeSignal.collectLatest {
                if (it > 0) {
                    requestId++
                    generation++
                    detector.reset()
                    ocrPaused = false
                    Log.d("CAPTURE", "유저 탭 감지 → 진행 중 결과 무효화, OCR 재개")
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>("data")

        if (data != null) {
            val projectionManager =
                getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)

            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d("CAPTURE", "MediaProjection 중지됨")
                }
            }, null)

            setupVirtualDisplay()
            showCaptureButton()
        }
        return START_NOT_STICKY
    }

    private fun setupVirtualDisplay() {
        CaptureResult.running.value = true

        imageReader = ImageReader.newInstance(
            screenWidth, screenHeight, PixelFormat.RGBA_8888, 2
        )

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null, null
        )
    }

    private fun showCaptureButton() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val toggleBtn = Button(this).apply {
            text = "시작"
            setOnClickListener {
                if (captureJob?.isActive == true) {
                    stopCaptureLoop()
                    text = "시작"
                } else {
                    startCaptureLoop()
                    text = "정지"
                }
            }
        }

        val stopBtn = Button(this).apply {
            text = "종료"
            setOnClickListener { stopSelf() }
        }

        layout.addView(toggleBtn)
        layout.addView(stopBtn)
        captureButton = layout

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END

        windowManager.addView(captureButton, params)
    }

    // ---------- 캡처 루프 ----------
    private fun startCaptureLoop() {
        if (captureJob?.isActive == true) return
        Log.d("CAPTURE", "캡처 루프 시작")

        captureJob = serviceScope.launch {
            while (isActive) {
                captureAndProcess()
                delay(CAPTURE_INTERVAL_MS)
            }
        }
    }

    private fun stopCaptureLoop() {
        Log.d("CAPTURE", "캡처 루프 정지")
        captureJob?.cancel()
        captureJob = null
        ocrPaused = false

        // 세션 종료 시 캐시 정리
        val profileId = tokenManager.profileId()
        serviceScope.launch(Dispatchers.IO) {
            db.translationCacheDao().clearByProfile(profileId)
            Log.d("CACHE", "캐시 정리됨 (세션 종료)")
        }
    }

    // 한 프레임: 캡처 → 크롭 → OCR
    private fun captureAndProcess() {
        if (ocrPaused) return   // 번역 표시 중이면 캡처/OCR 생략

        val cropped = captureCropped() ?: return
        runOcr(cropped)
    }

    private fun captureCropped(): Bitmap? {
        val image = imageReader?.acquireLatestImage() ?: return null

        try {
            val plane = image.planes[0]
            val pixelStride = plane.pixelStride
            val rowPadding = plane.rowStride - pixelStride * screenWidth

            val full = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            full.copyPixelsFromBuffer(plane.buffer)

            // 크롭은 패딩을 뺀 screenWidth 기준
            val x = (screenWidth * DIALOG_REGION.left).toInt()
            val y = (screenHeight * DIALOG_REGION.top).toInt()
            val w = (screenWidth * (DIALOG_REGION.right - DIALOG_REGION.left)).toInt()
            val h = (screenHeight * (DIALOG_REGION.bottom - DIALOG_REGION.top)).toInt()

            val cropped = Bitmap.createBitmap(full, x, y, w, h)
            full.recycle()
            return cropped
        } finally {
            image.close()
        }
    }

    private fun runOcr(bitmap: Bitmap) {
        val myGeneration = generation
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                // 탭 이전 화면에서 시작된 OCR 결과면 버림
                if (myGeneration != generation) return@addOnSuccessListener

                val text = visionText.text
                Log.d("OCR", "인식: ${text.replace("\n", " / ")}")
                CaptureResult.ocrText = text

                val toTranslate = detector.onOcr(text)
                if (toTranslate != null) {
                    Log.d("DETECT", "완성 감지: $toTranslate")
                    ocrPaused = true
                    handleConfirmedText(toTranslate)
                }
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "OCR 실패: $e")
            }
            .addOnCompleteListener {
                bitmap.recycle()
            }
    }

    private fun handleConfirmedText(text: String) {
        val profileId = tokenManager.profileId()
        if (profileId == -1) {
            Log.e("CACHE", "선택된 프로필이 없습니다.")
            ocrPaused = false
            return
        }
        val normalized = ChangeDetector.normalize(text)

        serviceScope.launch(Dispatchers.IO) {
            val cached = db.translationCacheDao().findBySourceText(profileId, normalized)

            if (cached != null) {
                Log.d("CACHE", "캐시 히트 → 즉시 표시: ${cached.translatedText}")
                CaptureResult.translatedText.value = cached.translatedText
            } else {
                Log.d("CACHE", "캐시 미스 → 서버 요청")
                requestTranslate(text, profileId, normalized)
            }
        }
    }

    private fun requestTranslate(text: String, profileId: Int, normalized: String) {
        val token = "Bearer ${tokenManager.accessToken()}"
        val myRequestId = ++requestId

        serviceScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.translate(
                    token = token,
                    request = TranslateRequest(profileId = profileId, text = text)
                )

                val translated = response.translatedText ?: ""

                // 번역 자체는 유효하므로 캐시에는 항상 저장
                if (translated.isNotBlank()) {
                    db.translationCacheDao().insert(
                        TranslationCacheEntity(
                            profileId = profileId,
                            sourceText = normalized,
                            translatedText = translated
                        )
                    )
                }

                // 탭으로 넘어갔거나 더 최신 요청이 있으면 표시하지 않음
                if (myRequestId != requestId) {
                    Log.d("TRANSLATE", "오래된 요청 결과 표시 생략: $translated")
                    return@launch
                }

                Log.d("TRANSLATE", "번역 결과: $translated")
                CaptureResult.translatedText.value = translated

            } catch (e: Exception) {
                Log.e("TRANSLATE", "번역 실패: $e")
                if (myRequestId == requestId) ocrPaused = false
            }
        }
    }

    private fun startForegroundNotification() {
        val channelId = "capture_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "화면 캡처",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("화면 캡처 중")
            .setContentText("게임 화면을 캡처하고 있습니다")
            .setSmallIcon(R.drawable.ic_menu_camera)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        captureButton?.let { windowManager.removeView(it) }
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        recognizer.close()
        CaptureResult.running.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

object CaptureResult {
    var bitmap: Bitmap? = null
    var ocrText: String? = null

    val running = MutableStateFlow(false)
    val translatedText = MutableStateFlow("")

    // OverlayService → CaptureService로 "탭했다" 알리는 신호
    val resumeSignal = MutableStateFlow(0)

    // 설정 화면 → CaptureService로 "영역 지정 화면 띄워라" 알리는 신호
    val pickRegionSignal = MutableStateFlow(0)
}