package com.example.transrator

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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
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
import android.view.WindowManager
import android.widget.Button

class CaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private lateinit var windowManager: WindowManager
    private var captureButton: android.view.View? = null

    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    // 코루틴 스코프 (서비스 종료 시 cancel)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var captureJob: Job? = null

    // OCR 클라이언트는 한 번만 생성
    private val recognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }

    // 변화 감지기 (연속 2회 같으면 완성으로 판정)
    private val detector = ChangeDetector(stableRequired = 2)

    // 번역 요청 겹침 방지용 (가장 최신 요청의 결과만 반영)
    private var requestId = 0

    companion object {
        // 임시: 전체 화면으로 테스트. 나중에 대사창 영역으로 좁힐 것
        // 이름표 제외, 본문 텍스트만 크롭
        val DIALOG_REGION = RectF(0f, 0.54f, 1f, 0.62f)   // 예시, 실제 이미지 보고 조정
        const val CAPTURE_INTERVAL_MS = 500L
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>("data")

        if (data != null) {
            val projectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
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
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
        }

        // 시작/정지 토글 버튼
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
    }

    // 한 프레임: 캡처 → 크롭 → OCR
    private fun captureAndProcess() {
        val cropped = captureCropped() ?: return
        saveBitmapForDebug(cropped, "debug_crop.png")
        runOcr(cropped)
    }

    // 최신 이미지를 꺼내 대사 영역만 크롭한 비트맵 반환
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

            Log.d("CAPTURE_SIZE", "full: ${full.width} x ${full.height}, screenWidth=$screenWidth, screenHeight=$screenHeight")
            // 디버그: 전체 화면도 저장
            saveBitmapForDebug(full, "debug_full.png")

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

    private fun saveBitmapForDebug(bitmap: Bitmap, fileName: String = "debug_crop.png") {
        try {
            val file = java.io.File(getExternalFilesDir(null), fileName)
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d("DEBUG_CROP", "저장됨: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("DEBUG_CROP", "저장 실패: $e")
        }
    }

    private fun runOcr(bitmap: Bitmap) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val text = visionText.text
                Log.d("OCR", "인식: ${text.replace("\n", " / ")}")
                CaptureResult.ocrText = text

                // 변화 감지: 완성된 문장일 때만 번역 요청
                val toTranslate = detector.onOcr(text)
                if (toTranslate != null) {
                    Log.d("DETECT", "완성 감지 → 번역 요청: $toTranslate")
                    requestTranslate(toTranslate)
                }
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "OCR 실패: $e")
            }
            .addOnCompleteListener {
                bitmap.recycle()   // 성공/실패 상관없이 OCR이 끝난 뒤 반환
            }
    }

    private fun requestTranslate(text: String) {
        val token = "Bearer jwttoken"   // TODO: 로컬에서만 채우고 커밋 전 비우기
        val profileId = 3  // 테스트용 프로필 ID

        val myRequestId = ++requestId
        val startTime = System.currentTimeMillis()   // 추가

        serviceScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.translate(
                    token = token,
                    request = TranslateRequest(
                        profileId = profileId,
                        text = text
                    )
                )

                val elapsed = System.currentTimeMillis() - startTime   // 추가

                if (myRequestId != requestId) {
                    Log.d("TRANSLATE", "오래된 요청 결과 무시 (${elapsed}ms): ${response.translatedText}")
                    return@launch
                }

                Log.d("TRANSLATE", "번역 결과 (${elapsed}ms): ${response.translatedText}")
                CaptureResult.translatedText = response.translatedText

            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - startTime   // 추가
                Log.e("TRANSLATE", "번역 실패 (${elapsed}ms): $e")
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
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
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
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

object CaptureResult {
    var bitmap: Bitmap? = null
    var ocrText: String? = null
    var translatedText: String? = null
}