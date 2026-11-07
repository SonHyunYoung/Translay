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
import kotlinx.coroutines.flow.drop

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
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
import com.example.transrator.data.local.SettingsManager
import com.example.transrator.data.local.TogglePos
import com.example.transrator.data.local.TokenManager
import com.example.transrator.data.local.TranslationCacheEntity
import com.example.transrator.data.remote.RetrofitClient
import com.example.transrator.data.remote.TranslateRequest
import java.io.File
import java.io.FileOutputStream

class CaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private lateinit var windowManager: WindowManager
    private var captureButton: View? = null

    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    @Volatile private var ocrPaused = false

    // 마지막 프레임의 이름 영역 크롭 (대사 확정 시 한 번만 OCR)
    @Volatile private var lastNameBitmap: Bitmap? = null

    private val db by lazy { AppDatabase.getInstance(applicationContext) }
    private val tokenManager by lazy { TokenManager(applicationContext) }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var captureJob: Job? = null

    private val recognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }

    private val detector = ChangeDetector(stableRequired = 2)

    @Volatile private var requestId = 0
    @Volatile private var generation = 0

    companion object {
        val DIALOG_REGION = RectF(0f, 0.54f, 1f, 0.62f)
        const val CAPTURE_INTERVAL_MS = 500L
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        screenDensity = metrics.densityDpi
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        serviceScope.launch {
            CaptureResult.resumeSignal.drop(1).collectLatest {
                requestId++
                generation++
                detector.reset()
                ocrPaused = false
                CaptureResult.translating.value = false
                Log.d("CAPTURE", "유저 탭 감지 → 진행 중 결과 무효화, OCR 재개")
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        try {
            syncDisplaySize()
            relocateToggle()
        } catch (e: Exception) {
            Log.e("CAPTURE", "회전 처리 실패: $e")
        }
    }

    @Synchronized
    private fun syncDisplaySize() {
        val m = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(m)
        if (m.widthPixels == screenWidth && m.heightPixels == screenHeight) return

        screenWidth = m.widthPixels
        screenHeight = m.heightPixels
        try {
            val vd = virtualDisplay ?: return
            imageReader?.close()
            imageReader = ImageReader.newInstance(
                screenWidth, screenHeight, PixelFormat.RGBA_8888, 2
            )
            vd.resize(screenWidth, screenHeight, screenDensity)
            vd.surface = imageReader?.surface
            Log.d("CAPTURE", "화면 크기 변경 감지: ${screenWidth}x${screenHeight}")
        } catch (e: Exception) {
            Log.e("CAPTURE", "크기 동기화 실패: $e")
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
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        applyTogglePosition(params)

        windowManager.addView(captureButton, params)
    }

    private fun applyTogglePosition(params: WindowManager.LayoutParams) {
        val layout = captureButton ?: return
        val pos = TogglePos.from(SettingsManager(this).load().togglePos)

        layout.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val w = layout.measuredWidth
        val h = layout.measuredHeight
        val margin = (24 * resources.displayMetrics.density).toInt()

        params.gravity = Gravity.START or Gravity.TOP
        params.x = ((screenWidth * pos.x).toInt() - w / 2)
            .coerceIn(0, (screenWidth - w).coerceAtLeast(0))
        params.y = if (pos.name.startsWith("TOP")) margin
        else (screenHeight - h - margin).coerceAtLeast(0)
    }

    private fun relocateToggle() {
        val v = captureButton ?: return
        val params = v.layoutParams as? WindowManager.LayoutParams ?: return
        applyTogglePosition(params)
        windowManager.updateViewLayout(v, params)
    }

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
        CaptureResult.translating.value = false

        val profileId = tokenManager.profileId()
        serviceScope.launch(Dispatchers.IO) {
            db.translationCacheDao().clearByProfile(profileId)
            Log.d("CACHE", "캐시 정리됨 (세션 종료)")
        }
    }

    private fun captureAndProcess() {
        if (ocrPaused) return

        syncDisplaySize()
        val cropped = captureCropped() ?: return
        runOcr(cropped)
    }

    private fun captureCropped(): Bitmap? {
        val image = imageReader?.acquireLatestImage() ?: return null

        try {
            val w0 = image.width
            val h0 = image.height
            val plane = image.planes[0]
            val rowPadding = plane.rowStride - plane.pixelStride * w0

            val full = Bitmap.createBitmap(
                w0 + rowPadding / plane.pixelStride, h0, Bitmap.Config.ARGB_8888
            )
            full.copyPixelsFromBuffer(plane.buffer)

            val settings = SettingsManager(this).load()

            val region = settings.dialogRegion ?: DIALOG_REGION

            val x = (w0 * region.left).toInt().coerceIn(0, w0 - 1)
            val y = (h0 * region.top).toInt().coerceIn(0, h0 - 1)
            val w = (w0 * (region.right - region.left)).toInt().coerceIn(1, w0 - x)
            val h = (h0 * (region.bottom - region.top)).toInt().coerceIn(1, h0 - y)

            val cropped = Bitmap.createBitmap(full, x, y, w, h)

            val old = lastNameBitmap
            lastNameBitmap = settings.nameRegion?.let { r ->
                val nx = (w0 * r.left).toInt().coerceIn(0, w0 - 1)
                val ny = (h0 * r.top).toInt().coerceIn(0, h0 - 1)
                val nw = (w0 * (r.right - r.left)).toInt().coerceIn(1, w0 - nx)
                val nh = (h0 * (r.bottom - r.top)).toInt().coerceIn(1, h0 - ny)
                Bitmap.createBitmap(full, nx, ny, nw, nh)
            }
            old?.recycle()

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
                if (myGeneration != generation) return@addOnSuccessListener

                val text = visionText.text
                Log.d("OCR", "인식: ${text.replace("\n", " / ")}")
                CaptureResult.ocrText = text

                val toTranslate = detector.onOcr(text)
                if (toTranslate != null) {
                    Log.d("DETECT", "완성 감지: $toTranslate")
                    ocrPaused = true
                    recognizeSpeaker { speaker ->
                        Log.d("SPEAKER", "이름: $speaker")
                        handleConfirmedText(toTranslate, speaker)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "OCR 실패: $e")
            }
            .addOnCompleteListener {
                bitmap.recycle()
            }
    }

    /** 이름 영역을 OCR (영역 미지정/실패 시 null). 전처리 없이 원본 크롭을 읽는다 */
    private fun recognizeSpeaker(done: (String?) -> Unit) {
        val bmp = lastNameBitmap
        if (bmp == null || bmp.isRecycled) {
            Log.d("SPEAKER", "이름 영역 없음 (미지정이거나 아직 캡처 전)")
            done(null); return
        }

        val input = try {
            bmp.copy(Bitmap.Config.ARGB_8888, false)
        } catch (e: Exception) {
            Log.e("SPEAKER", "이름 이미지 준비 실패: $e")
            null
        }
        if (input == null) { done(null); return }

        // 디버그: OCR에 실제로 들어가는 이름 이미지 (확인 후 이 블록을 지우세요)
        try {
            FileOutputStream(File(getExternalFilesDir(null), "debug_name.png")).use {
                input.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } catch (e: Exception) {
            Log.e("SPEAKER", "디버그 저장 실패: $e")
        }
        Log.d("SPEAKER", "이름 입력 크기: ${input.width}x${input.height}")

        recognizer.process(InputImage.fromBitmap(input, 0))
            .addOnSuccessListener { r ->
                // 줄이 여럿이면(후리가나 + 이름) 글씨가 가장 큰 줄을 이름으로 사용
                val lines = r.textBlocks.flatMap { it.lines }
                val best = lines.maxByOrNull { it.boundingBox?.height() ?: 0 }
                val raw = best?.text ?: r.text
                Log.d("SPEAKER", "줄 ${lines.size}개 → 선택: '$raw'")
                done(raw.replace(Regex("\\s+"), "").ifBlank { null })
            }
            .addOnFailureListener { e ->
                Log.e("SPEAKER", "이름 OCR 실패: $e")
                done(null)
            }
            .addOnCompleteListener { input.recycle() }
    }

    private fun handleConfirmedText(text: String, speaker: String?) {
        val profileId = tokenManager.profileId()
        if (profileId == -1) {
            Log.e("CACHE", "선택된 프로필이 없습니다.")
            ocrPaused = false
            return
        }
        val key = ChangeDetector.normalize(text) + "#" + (speaker ?: "")

        serviceScope.launch(Dispatchers.IO) {
            val cached = db.translationCacheDao().findBySourceText(profileId, key)

            if (cached != null) {
                Log.d("CACHE", "캐시 히트 → 즉시 표시: ${cached.translatedText}")
                CaptureResult.translatedText.value = cached.translatedText
            } else {
                Log.d("CACHE", "캐시 미스 → 서버 요청")
                requestTranslate(text, profileId, key, speaker)
            }
        }
    }

    private fun requestTranslate(text: String, profileId: Int, cacheKey: String, speaker: String?) {
        val token = "Bearer ${tokenManager.accessToken()}"
        val myRequestId = ++requestId
        CaptureResult.translating.value = true

        serviceScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.translate(
                    token = token,
                    request = TranslateRequest(
                        profileId = profileId,
                        text = text,
                        speaker = speaker
                    )
                )

                val translated = response.translatedText ?: ""
                val name = response.speakerName
                val shown = if (name.isNullOrBlank()) translated else "【$name】\n$translated"

                if (translated.isNotBlank()) {
                    db.translationCacheDao().insert(
                        TranslationCacheEntity(
                            profileId = profileId,
                            sourceText = cacheKey,
                            translatedText = shown
                        )
                    )
                }

                if (myRequestId != requestId) {
                    Log.d("TRANSLATE", "오래된 요청 결과 표시 생략: $shown")
                    return@launch
                }

                Log.d("TRANSLATE", "번역 결과: $shown")
                CaptureResult.translatedText.value = shown

            } catch (e: Exception) {
                Log.e("TRANSLATE", "번역 실패: $e")
                if (myRequestId == requestId) ocrPaused = false
            } finally {
                // 최신 요청일 때만 "번역 중" 해제 (이전 요청이 늦게 끝나도 새 요청 표시 유지)
                if (myRequestId == requestId) CaptureResult.translating.value = false
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
        lastNameBitmap?.recycle()
        lastNameBitmap = null
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        recognizer.close()
        CaptureResult.running.value = false
        CaptureResult.translating.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

object CaptureResult {
    var bitmap: Bitmap? = null
    var ocrText: String? = null

    val running = MutableStateFlow(false)
    val translatedText = MutableStateFlow("")

    val resumeSignal = MutableStateFlow(0)

    // 서버에 번역 요청 중인지 (캐시 히트에서는 켜지 않음)
    val translating = MutableStateFlow(false)

    val pickRegionSignal = MutableStateFlow(0)
}