package com.example.transrator.capture

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import com.example.transrator.data.local.SettingsManager

/** 캡처 서비스 없이 영역 지정만 하는 서비스. 시작 버튼을 눌러야 오버레이가 뜬다 */
class RegionPickerService : Service() {

    private lateinit var windowManager: WindowManager
    private var startPanel: View? = null
    private var pickerView: View? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (startPanel == null && pickerView == null) showStartPanel()
        return START_NOT_STICKY
    }

    /** 게임 위에 뜨는 작은 시작/닫기 버튼 */
    private fun showStartPanel() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        val startBtn = Button(this).apply {
            text = "영역 지정 시작"
            setOnClickListener { removePanel(); showPicker() }
        }
        val closeBtn = Button(this).apply {
            text = "닫기"
            setOnClickListener { stopSelf() }
        }
        layout.addView(startBtn)
        layout.addView(closeBtn)

        val margin = (24 * resources.displayMetrics.density).toInt()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = margin
            y = margin
        }

        try {
            windowManager.addView(layout, params)
            startPanel = layout
        } catch (e: Exception) {
            Log.e("REGION", "시작 버튼 표시 실패: $e")
            stopSelf()
        }
    }

    private fun removePanel() {
        startPanel?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        startPanel = null
    }

    private fun showPicker() {
        val settings = SettingsManager(this)
        val current = settings.load()

        val view = RegionPickerView(
            this,
            current.dialogRegion,
            current.nameRegion,
            onSave = { dialog, name ->
                Log.d("REGION", "저장: dialog=$dialog, name=$name")
                settings.save(settings.load().copy(dialogRegion = dialog, nameRegion = name))
                stopSelf()
            },
            onCancel = {
                Log.d("REGION", "취소")
                stopSelf()
            }
        )
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        // 카메라 구멍(컷아웃) 영역까지 덮어서 오버레이가 실제 화면 전체와 일치하게 함
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        try {
            windowManager.addView(view, params)
            pickerView = view
        } catch (e: Exception) {
            Log.e("REGION", "오버레이 표시 실패: $e")
            stopSelf()
        }
    }

    override fun onDestroy() {
        removePanel()
        pickerView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        pickerView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}