package com.example.transrator

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
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

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // 번역 텍스트를 보여줄 뷰
        val textView = TextView(this).apply {
            setBackgroundColor(Color.argb(200, 0, 0, 0))   // 반투명 검정
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(24, 16, 24, 16)
            text = ""   // 초기엔 빈 텍스트
        }
        overlayView = textView

        // 하단 고정, 내용 크기만큼만 차지
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,   // 가로는 꽉
            WindowManager.LayoutParams.WRAP_CONTENT,   // 높이는 내용만큼
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,   // 게임 터치 막지 않음
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM   // 화면 하단에 배치

        windowManager.addView(overlayView, params)

        // 번역 결과가 바뀔 때마다 텍스트 갱신
        collectJob = serviceScope.launch {
            CaptureResult.translatedText.collectLatest { translated ->
                overlayView?.text = translated
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        collectJob?.cancel()
        serviceScope.cancel()
        overlayView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}