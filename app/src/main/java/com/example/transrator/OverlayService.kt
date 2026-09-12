package com.example.transrator  // ← 본인 패키지명으로

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()

        // WindowManager 가져오기 (오버레이를 화면에 붙이는 시스템 서비스)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // 빨간 네모 뷰 만들기
        overlayView = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(128, 255, 0, 0))  // 반투명 빨강
        }

        // 오버레이 창 설정
        val params = WindowManager.LayoutParams(
            600,   // 너비 (px)
            300,   // 높이 (px)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,  // 오버레이 타입
            // 터치를 통과시키는 플래그 (게임 조작 유지)
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER  // 화면 중앙에 배치

        // 화면에 붙이기
        windowManager.addView(overlayView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 서비스 끝나면 오버레이 제거
        overlayView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}