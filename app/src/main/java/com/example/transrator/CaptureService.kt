package com.example.transrator

//MLkit 일본어 OCR 사용
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button

class CaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private lateinit var windowManager: WindowManager
    private var captureButton: android.view.View? = null





    // VirtualDisplay와 ImageReader를 멤버로 (한 번만 생성)
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

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
                    android.util.Log.d("CAPTURE", "MediaProjection 중지됨")
                }
            }, null)

            // VirtualDisplay를 여기서 딱 한 번 생성
            setupVirtualDisplay()

            showCaptureButton()
        }
        return START_NOT_STICKY
    }

    // VirtualDisplay 한 번만 생성
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
        // 캡처 버튼 + 중지 버튼을 담을 레이아웃
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
        }

        // 캡처 버튼
        val captureBtn = Button(this).apply {
            text = "캡처"
            setOnClickListener { captureScreen() }
        }

        // 중지 버튼
        val stopBtn = Button(this).apply {
            text = "중지"
            setOnClickListener {
                stopSelf()  // 서비스 종료 → onDestroy에서 정리됨
            }
        }

        layout.addView(captureBtn)
        layout.addView(stopBtn)
        captureButton = layout  // 멤버 변수에 레이아웃 저장

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

    // 이제 여기선 이미 만들어진 imageReader에서 이미지만 꺼냄
    private fun captureScreen() {
        android.util.Log.d("CAPTURE", "captureScreen 시작")

        val image = imageReader?.acquireLatestImage()
        if (image != null) {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth

            val bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            android.util.Log.d("CAPTURE", "캡처 성공! 크기: ${bitmap.width} x ${bitmap.height}")
            CaptureResult.bitmap = bitmap

            image.close()

            //  OCR 실행
            runOcr(bitmap)
        }
    }

    // OCR 함수 추가
    private fun runOcr(bitmap: Bitmap) {
        val recognizer = TextRecognition.getClient(
            JapaneseTextRecognizerOptions.Builder().build()
        )

        val inputImage = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                android.util.Log.d("OCR", "===== 인식 결과 =====")
                android.util.Log.d("OCR", visionText.text)
                android.util.Log.d("OCR", "====================")

                CaptureResult.ocrText = visionText.text

                // ↓ 여기 추가
                if (visionText.text.isNotEmpty()) {
                    requestTranslate(visionText.text)
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("OCR", "OCR 실패: $e")
            }
    }

    private fun requestTranslate(text: String) {
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MSwiZW1haWwiOiJ0ZXN0QHRlc3QuY29tIiwiaWF0IjoxNzg5OTI4NzIwLCJleHAiOjE3OTI1MjA3MjB9.hYEI_XRAaWGa5akCSdpRCZmeV4VQlyIiP15fc-cSqqU"  // 테스트용
        val profileId = 1  // 테스트용 프로필 ID

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.translate(
                    token = token,
                    request = TranslateRequest(
                        profileId = profileId,
                        text = text
                    )
                )

                android.util.Log.d("TRANSLATE", "번역 결과: ${response.translatedText}")
                CaptureResult.translatedText = response.translatedText

            } catch (e: Exception) {
                android.util.Log.e("TRANSLATE", "번역 실패: $e")
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
        captureButton?.let { windowManager.removeView(it) }
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

object CaptureResult {
    var bitmap: Bitmap? = null
    var ocrText: String? = null

    var translatedText: String? = null
}