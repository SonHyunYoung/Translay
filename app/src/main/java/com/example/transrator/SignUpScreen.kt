package com.example.transrator

import android.R
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transrator.ui.theme.BrandBlue
import com.example.transrator.ui.theme.Pretendard

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onSignedUp: () -> Unit,
    vm: SignUpViewModel = viewModel()
) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(s.success) {
        if (s.success) {
            Toast.makeText(context, "회원가입에 성공했습니다. 로그인 페이지로 이동합니다.", Toast.LENGTH_SHORT).show()
            onSignedUp()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, "뒤로",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("회원가입", fontFamily = Pretendard, fontWeight = FontWeight.Black, fontSize = 32.sp, color = accentColor())
        Spacer(Modifier.height(6.dp))
        Text(
            "회원가입을 통해 Translay의 실시간 번역을 경험해보세요!", fontFamily = Pretendard, fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        FormField(
            s.name, vm::onName, label = "이름",
            placeholder = "2~15자로 입력해주세요",
            error = s.nameError
        )
        Spacer(Modifier.height(4.dp))
        FormField(
            s.email, vm::onEmail, label = "이메일", placeholder = "example@email.com",
            keyboard = KeyboardType.Email,
            error = if (s.emailMessage != null && !s.emailOk) s.emailMessage else null,
            success = if (s.emailOk) s.emailMessage else null,
            trailing = {
                Button(
                    onClick = vm::checkEmail,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlue, contentColor = Color.White
                    )
                ) {
                    Text(
                        "중복확인", fontFamily = Pretendard, fontWeight = FontWeight.Bold,
                        fontSize = 13.sp, textAlign = TextAlign.Center
                    )
                }
            }
        )
        FormField(
            s.password, vm::onPassword, label = "비밀번호",
            labelHint = "8자 이상, 대·소문자·숫자·특수문자 포함",
            placeholder = "비밀번호를 입력해주세요",
            isPassword = true,
            error = s.passwordError,
            success = if (s.password.isNotEmpty() && s.passwordError == null) "사용 가능한 비밀번호예요." else null
        )
        Spacer(Modifier.height(4.dp))
        FormField(
            s.passwordConfirm, vm::onConfirm, label = "비밀번호 확인",
            placeholder = "비밀번호를 한 번 더 입력해주세요",
            isPassword = true,
            error = s.confirmError,
            success = if (s.passwordConfirm.isNotEmpty() && s.confirmError == null) "비밀번호가 일치해요." else null
        )
        Spacer(Modifier.height(4.dp))
        Hint(s.error)

        BrandButton("회원가입", enabled = s.canSubmit, loading = s.isLoading, onClick = vm::signUp)

        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("이미 계정이 있으신가요? ", fontFamily = Pretendard, fontSize = 13.sp)
            Text(
                "로그인 페이지로", fontFamily = Pretendard, fontWeight = FontWeight.Bold,
                fontSize = 13.sp, color = accentColor(),
                modifier = Modifier.clickable(onClick = onBack).padding(vertical = 6.dp)
            )
        }
    }
}