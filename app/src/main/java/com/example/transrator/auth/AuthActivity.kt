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
import com.example.transrator.profile.ProfileEditScreen
import com.example.transrator.profile.ProfileSelectScreen
import com.example.transrator.data.TokenManager
import com.example.transrator.ui.theme.TransratorTheme

class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TransratorTheme {
                Surface(Modifier.fillMaxSize().systemBarsPadding()) {
                    val nav = rememberNavController()
                    NavHost(nav, startDestination = "login") {
                        composable("login") {
                            LoginScreen(
                                onLoggedIn = {
                                    nav.navigate("profiles") {
                                        popUpTo("login") {
                                            inclusive = true
                                        }
                                    }
                                },
                                onSignUp = { nav.navigate("signup") }
                            )
                        }
                        composable("profiles") {
                            BackHandler {
                                TokenManager(this@AuthActivity).clear()   // 저장된 토큰/프로필 정보 삭제
                                nav.navigate("login") { popUpTo("profiles") { inclusive = true } }
                            }
                            ProfileSelectScreen(
                                onSelected = {
                                    startActivity(
                                        Intent(
                                            this@AuthActivity,
                                            MainActivity::class.java
                                        )
                                    )
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