package com.example.transrator

import android.widget.Toast
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
private fun LabeledRow(label: String, content: @Composable ColumnScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            label, fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            textAlign = TextAlign.Center, modifier = Modifier.width(72.dp).padding(top = 16.dp)
        )
        Column(Modifier.weight(1f), content = content)
    }
}

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
            Toast.makeText(context, "회원가입이 완료되었어요. 로그인해주세요.", Toast.LENGTH_SHORT).show()
            onSignedUp()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = Color.Gray)
            }
            Logo(40)
        }
        Spacer(Modifier.height(20.dp))

        LabeledRow("이메일") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Field(s.email, vm::onEmail, Modifier.weight(1f), keyboard = KeyboardType.Email)
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = vm::checkEmail, shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White)
                ) {
                    Text(
                        "중복\n체크", fontFamily = Pretendard, fontWeight = FontWeight.Bold,
                        fontSize = 12.sp, textAlign = TextAlign.Center
                    )
                }
            }
            Hint(s.emailMessage, ok = s.emailOk)
        }
        Spacer(Modifier.height(10.dp))
        LabeledRow("이름") {
            Field(s.name, vm::onName, error = s.nameError)
            Hint(s.nameError ?: "2~15자, 공백과 _ . 외 특수기호 불가", ok = s.nameError == null)
        }
        Spacer(Modifier.height(10.dp))
        LabeledRow("비밀번호") {
            Field(s.password, vm::onPassword, isPassword = true, error = s.passwordError)
            Hint(s.passwordError ?: "8자 이상, 대·소문자·숫자·특수문자 포함", ok = s.passwordError == null)
        }
        Spacer(Modifier.height(10.dp))
        LabeledRow("비밀번호\n확인") {
            Field(s.passwordConfirm, vm::onConfirm, isPassword = true, error = s.confirmError)
            Hint(s.confirmError)
        }
        Spacer(Modifier.height(20.dp))
        Hint(s.error)
        BrandButton("회원가입", enabled = s.canSubmit, loading = s.isLoading, onClick = vm::signUp)
    }
}