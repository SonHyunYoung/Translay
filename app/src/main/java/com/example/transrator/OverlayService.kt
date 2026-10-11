package com.example.transrator

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: TextView? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var collectJob: Job? = null

    // 유저가 탭해서 숨긴 상태인지
    private var hiddenByUser = false
    // 마지막으로 표시했던 번역 (새 번역이 오면 숨김 해제 판단용)
    private var lastShown = ""

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // ACTION_OUTSIDE를 받기 위해 onTouchEvent를 오버라이드한 TextView
        val textView = object : TextView(this) {
            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    hideOverlay()
                }
                return false   // 이벤트를 소비하지 않음 → 게임으로 그대로 전달됨
            }
        }.apply {
            setBackgroundColor(Color.argb(200, 0, 0, 0))   // 반투명 검정
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(24, 16, 24, 16)
            text = ""
            visibility = View.GONE   // 처음엔 숨김
        }
        overlayView = textView

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM   // 대사창 자리에 맞춰 배치 (나중에 영역 지정과 연동)

        windowManager.addView(overlayView, params)

        // 번역 결과가 바뀔 때마다 처리
        collectJob = serviceScope.launch {
            CaptureResult.translatedText.collectLatest { translated ->
                if (translated.isBlank()) return@collectLatest

                // 새로운 번역이면 숨김 해제하고 다시 표시
                if (translated != lastShown) {
                    lastShown = translated
                    hiddenByUser = false
                }

                if (!hiddenByUser) {
                    showOverlay(translated)
                }
            }
        }
    }

    private fun showOverlay(text: String) {
        overlayView?.text = text
        overlayView?.visibility = View.VISIBLE
    }

    private fun hideOverlay() {
        hiddenByUser = true
        overlayView?.visibility = View.GONE
        CaptureResult.resumeSignal.value = CaptureResult.resumeSignal.value + 1   // OCR 재개 신호
    }

    override fun onDestroy() {
        super.onDestroy()
        collectJob?.cancel()
        serviceScope.cancel()
        overlayView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}