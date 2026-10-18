package com.example.transrator

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transrator.ui.theme.Pretendard

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onSignUp: () -> Unit,
    vm: LoginViewModel = viewModel()
) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(s.success) { if (s.success) onLoggedIn() }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.weight(1.4f).fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Logo(60)
                Spacer(Modifier.height(60.dp))

            }
        }

        // 가운데: 입력 박스 + 버튼
        FormField(
            s.email, vm::onEmail, placeholder = "이메일",
            leadingIcon = Icons.Outlined.Email, keyboard = KeyboardType.Email
        )
        Spacer(Modifier.height(4.dp))
        FormField(
            s.password, vm::onPassword, placeholder = "비밀번호",
            leadingIcon = Icons.Outlined.Lock, isPassword = true, error = s.error
        )
        Spacer(Modifier.height(12.dp))
        BrandButton("로그인", loading = s.isLoading, onClick = vm::login)
        Spacer(Modifier.height(10.dp))

        // 로그인 버튼 밑: 회원가입 | 비밀번호 찾기
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "회원가입", fontFamily = Pretendard, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable(onClick = onSignUp).padding(8.dp)
            )
            Text(
                "|", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "비밀번호 찾기", fontFamily = Pretendard, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable {
                    Toast.makeText(context, "준비 중인 기능이에요.", Toast.LENGTH_SHORT).show()
                }.padding(8.dp)
            )
        }

        // 아래쪽 빈 공간
        Spacer(Modifier.weight(1f))
    }
}