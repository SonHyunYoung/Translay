package com.example.transrator

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity

/**
 * 오버레이('다른 앱 위에 표시') 권한이 없으면 어느 화면에서든
 * 닫을 수 없는 안내 다이얼로그를 띄우는 공통 Activity.
 * AuthActivity, MainActivity 등 모든 Activity가 ComponentActivity 대신 이것을 상속한다.
 */
open class PermissionGuardActivity : ComponentActivity() {

    private var guardDialog: AlertDialog? = null

    override fun onResume() {
        super.onResume()
        ensureOverlayPermission()
    }

    override fun onPause() {
        super.onPause()
        dismissGuard()   // 설정 화면으로 나가거나 화면이 가려질 때 창 누수 방지
    }

    override fun onDestroy() {
        dismissGuard()
        super.onDestroy()
    }

    /** 권한이 있으면 true. 없으면 강제 다이얼로그를 띄우고 false. */
    protected fun ensureOverlayPermission(): Boolean {
        if (Settings.canDrawOverlays(this)) {
            dismissGuard()
            return true
        }
        if (guardDialog?.isShowing == true || isFinishing || isDestroyed) return false

        guardDialog = AlertDialog.Builder(this)
            .setTitle("권한 안내")
            .setMessage(
                "앱의 사용을 위해서 '다른 앱 위에 표시' 권한이 필요합니다.\n\n" +
                        "권한을 허용하지 않으면 앱을 사용할 수 없습니다."
            )
            .setCancelable(false)   // 뒤로가기로 닫기 불가
            .setPositiveButton("설정으로 이동", null)   // 아래에서 직접 처리(자동 닫힘 방지)
            .setNegativeButton("앱 종료", null)
            .create()
            .also { d ->
                d.setCanceledOnTouchOutside(false)   // 바깥 터치로 닫기 불가
                d.setOnShowListener {
                    d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                        )
                    }
                    d.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                        finishAffinity()
                    }
                }
                d.show()
            }
        return false
    }

    private fun dismissGuard() {
        guardDialog?.dismiss()
        guardDialog = null
    }
}