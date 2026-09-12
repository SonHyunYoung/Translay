package com.example.transrator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.transrator.ui.theme.TransratorTheme

class CaptureActivity : ComponentActivity() {

    private lateinit var projectionManager: MediaProjectionManager
    private val capturedBitmap = mutableStateOf<Bitmap?>(null)
    private val ocrText = mutableStateOf<String?>(null)

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(this, CaptureService::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
            }
            startForegroundService(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        setContent {
            TransratorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    CaptureScreen(
                        modifier = Modifier.padding(padding),
                        bitmap = capturedBitmap.value,
                        ocrText = ocrText.value,
                        onCaptureClick = {
                            projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                        },
                        onRefreshClick = {
                            capturedBitmap.value = CaptureResult.bitmap
                            ocrText.value = CaptureResult.ocrText
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CaptureScreen(
    modifier: Modifier = Modifier,
    bitmap: Bitmap?,
    ocrText: String?,
    onCaptureClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onCaptureClick) {
            Text("화면 캡처")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRefreshClick) {
            Text("결과 불러오기")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // OCR 결과 텍스트
        if (ocrText != null) {
            Text("OCR 결과:")
            Text(ocrText)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 캡처 이미지
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "캡처 이미지",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}