package com.example.transrator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.transrator.auth.AuthActivity
import com.example.transrator.capture.CaptureResult
import com.example.transrator.capture.CaptureService
import com.example.transrator.capture.OverlayService
import com.example.transrator.data.local.ThemeManager
import com.example.transrator.data.local.TokenManager
import com.example.transrator.main.MainScreen
import com.example.transrator.main.MainViewModel
import com.example.transrator.profile.avatarColor
import com.example.transrator.ui.theme.TransratorTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private var lastBackPressed = 0L

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            startForegroundService(Intent(this, CaptureService::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
            })
            startService(Intent(this, OverlayService::class.java))
            Toast.makeText(
                this, "번역을 시작합니다. 게임 화면에서 '시작'을 눌러주세요.", Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.init(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 뒤로가기 두 번 누르면 종료
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val now = SystemClock.elapsedRealtime()
                if (now - lastBackPressed < 2000) {
                    stopTranslate()
                    finish()
                } else {
                    lastBackPressed = now
                    Toast.makeText(
                        this@MainActivity, "한 번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        setContent {
            TransratorTheme {
                Surface(Modifier.fillMaxSize()) {
                    val s by vm.state.collectAsState()
                    val running by CaptureResult.running.collectAsState()
                    MainScreen(
                        profileName = s.profileName,
                        avatarColor = avatarColor(s.profileIndex),
                        running = running,
                        onStart = ::startTranslate,
                        onStop = ::stopTranslate,
                        onChangeProfile = { goToProfiles() },
                        onLogout = {
                            stopTranslate()
                            vm.logout {
                                startActivity(Intent(this, AuthActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                            }
                        },
                        onDeleted = {
                            stopTranslate()
                            TokenManager(this).clear()
                            startActivity(Intent(this, AuthActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                        },
                    )
                }
            }
        }
    }

    private fun goToProfiles() {
        stopTranslate()   // 번역 중 프로필이 바뀌는 것을 방지
        startActivity(Intent(this, AuthActivity::class.java).apply {
            putExtra("start", "profiles")
        })
        finish()
    }

    private fun startTranslate() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "'다른 앱 위에 표시' 권한이 필요해요.", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }
        val pm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(pm.createScreenCaptureIntent())
    }

    private fun stopTranslate() {
        stopService(Intent(this, CaptureService::class.java))
        stopService(Intent(this, OverlayService::class.java))
    }
}