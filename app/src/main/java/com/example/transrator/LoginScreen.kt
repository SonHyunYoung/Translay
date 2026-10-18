package com.example.transrator

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
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
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Logo()
        Spacer(Modifier.height(24.dp))
        Field(s.email, vm::onEmail, placeholder = "email", keyboard = KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        Field(s.password, vm::onPassword, placeholder = "password", isPassword = true)
        Hint(s.error)
        Spacer(Modifier.height(12.dp))
        BrandButton("로그인", loading = s.isLoading, onClick = vm::login)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "회원가입", fontFamily = Pretendard, fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onSignUp).padding(8.dp)
            )
            Text("|", fontSize = 13.sp)
            Text(
                "비밀번호 찾기", fontFamily = Pretendard, fontSize = 13.sp,
                modifier = Modifier.clickable {
                    Toast.makeText(context, "준비 중인 기능이에요.", Toast.LENGTH_SHORT).show()
                }.padding(8.dp)
            )
        }
    }
}