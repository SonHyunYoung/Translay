package com.example.transrator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.transrator.ui.theme.TransratorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TransratorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // 오버레이 권한 상태 (있으면 true)
    var hasPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    // 다이얼로그 표시 여부
    var showDialog by remember { mutableStateOf(false) }

    // ----- 앱 켤 때 & 설정에서 돌아올 때 권한 확인 -----
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 권한 상태 갱신
                hasPermission = Settings.canDrawOverlays(context)
                // 권한 없으면 다이얼로그 띄우기
                showDialog = !hasPermission
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ----- 메인 화면 -----
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (hasPermission) "오버레이 권한: 허용됨" else "오버레이 권한: 필요함"
        )
    }

    // ----- 권한 안내 다이얼로그 -----
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("권한 안내") },
            text = {
                Text(
                    "앱의 사용을 위해서 '다른 앱 위에 표시' 권한이 필요합니다.\n\n" +
                            "권한이 없으면 앱 사용이 불가합니다."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    // 설정 화면으로 이동
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }) {
                    Text("설정으로 이동")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("나중에")
                }
            }
        )
    }
}