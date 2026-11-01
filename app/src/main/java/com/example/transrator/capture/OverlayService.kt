package com.example.transrator.capture

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.IBinder
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.example.transrator.data.local.SettingsManager
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

    private var hiddenByUser = false
    private var lastShown = ""

    override fun onCreate() {
        super.onCreate()
        Log.d("OVERLAY", "서비스 시작")

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val st = SettingsManager(this).load()

        val textView = object : TextView(this) {
            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_OUTSIDE) hideOverlay()
                return false
            }
        }.apply {
            val alpha = (st.bgAlpha * 255 / 100).coerceIn(0, 255)
            setBackgroundColor(
                Color.argb(alpha, Color.red(st.bgColor), Color.green(st.bgColor), Color.blue(st.bgColor))
            )
            setTextColor(st.textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, st.fontSize.toFloat())
            val base = when (st.fontKey) {
                "sans" -> Typeface.SANS_SERIF
                "serif" -> Typeface.SERIF
                "mono" -> Typeface.MONOSPACE
                else -> Typeface.DEFAULT
            }
            typeface = Typeface.create(base, if (st.bold) Typeface.BOLD else Typeface.NORMAL)
            setPadding(24, 16, 24, 16)
            text = ""
            visibility = View.GONE
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
        params.gravity = Gravity.BOTTOM

        windowManager.addView(overlayView, params)

        collectJob = serviceScope.launch {
            CaptureResult.translatedText.collectLatest { translated ->
                Log.d("OVERLAY", "수신: $translated (hidden=$hiddenByUser)")
                if (translated.isBlank()) return@collectLatest

                if (translated != lastShown) {
                    lastShown = translated
                    hiddenByUser = false
                }
                if (!hiddenByUser) showOverlay(translated)
            }
        }
    }

    private fun showOverlay(text: String) {
        Log.d("OVERLAY", "표시: $text")
        val view = overlayView ?: return

        // "【이름】\n번역" 형태면 이름 줄만 굵게
        val spannable = SpannableString(text)
        if (text.startsWith("【")) {
            val end = text.indexOf('\n').let { if (it < 0) text.length else it }
            spannable.setSpan(
                StyleSpan(Typeface.BOLD), 0, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        view.text = spannable
        view.visibility = View.VISIBLE
    }

    private fun hideOverlay() {
        hiddenByUser = true
        overlayView?.visibility = View.GONE
        CaptureResult.resumeSignal.value = CaptureResult.resumeSignal.value + 1
    }

    override fun onDestroy() {
        super.onDestroy()
        collectJob?.cancel()
        serviceScope.cancel()
        overlayView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}