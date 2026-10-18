package com.example.transrator

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                                    // TODO: 프로필 선택 화면으로 이동 (지금은 MainActivity)
                                    startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                                    finish()
                                },
                                onSignUp = { nav.navigate("signup") }
                            )
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