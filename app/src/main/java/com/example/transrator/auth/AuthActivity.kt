package com.example.transrator.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.transrator.MainActivity
import com.example.transrator.data.TokenManager
import com.example.transrator.profile.ProfileEditScreen
import com.example.transrator.profile.ProfileSelectScreen
import com.example.transrator.ui.theme.TransratorTheme

class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // "profiles"면 프로필 선택 화면으로 바로 진입 (메인에서 넘어온 경우)
        val startRoute = intent.getStringExtra("start") ?: "login"

        setContent {
            TransratorTheme {
                Surface(Modifier.fillMaxSize().systemBarsPadding()) {
                    val nav = rememberNavController()
                    NavHost(nav, startDestination = startRoute) {
                        composable("login") {
                            LoginScreen(
                                onLoggedIn = {
                                    nav.navigate("profiles") { popUpTo("login") { inclusive = true } }
                                },
                                onSignUp = { nav.navigate("signup") }
                            )
                        }
                        composable("profiles") {
                            BackHandler {
                                if (startRoute == "profiles") {
                                    // 메인에서 "프로필 변경"으로 온 경우: 메인으로 복귀
                                    startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                                    finish()
                                } else {
                                    // 로그인 직후 진입: 로그인 화면으로 (토큰 삭제)
                                    TokenManager(this@AuthActivity).clear()
                                    nav.navigate("login") { popUpTo("profiles") { inclusive = true } }
                                }
                            }
                            ProfileSelectScreen(
                                onSelected = {
                                    startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                                    finish()
                                },
                                onEdit = { nav.navigate("profileEdit") }
                            )
                        }
                        composable("profileEdit") {
                            ProfileEditScreen(onDone = { nav.popBackStack() })
                        }
                        composable("signup") {
                            SignUpScreen(
                                onBack = { nav.popBackStack() },
                                onSignedUp = { nav.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}