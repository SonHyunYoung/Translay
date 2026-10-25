package com.example.transrator.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.transrator.MainActivity
import com.example.transrator.data.local.TokenManager
import com.example.transrator.profile.ProfileEditScreen
import com.example.transrator.profile.ProfileSelectScreen
import com.example.transrator.ui.theme.TransratorTheme

class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val explicitStart = intent.getStringExtra("start")
        val tokens = TokenManager(this)

        setContent {
            TransratorTheme {
                Surface(Modifier.fillMaxSize()) {
                    var startRoute by remember {
                        mutableStateOf<String?>(
                            when {
                                explicitStart != null -> explicitStart
                                tokens.refreshToken() == null -> "login"
                                else -> null
                            }
                        )
                    }

                    LaunchedEffect(Unit) {
                        if (startRoute == null) {
                            val ok = AuthRepository(tokens).tryRefresh()
                            startRoute = if (ok == true) "profiles" else "login"
                        }
                    }

                    val route = startRoute
                    if (route == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Logo(48)
                        }
                    } else {
                        val nav = rememberNavController()
                        NavHost(nav, startDestination = route) {
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
                                    if (explicitStart == "profiles") {
                                        startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                                    }
                                    finish()
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
}