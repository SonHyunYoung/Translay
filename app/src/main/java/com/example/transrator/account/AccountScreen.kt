package com.example.transrator.account

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transrator.auth.FormField
import com.example.transrator.auth.accentColor
import com.example.transrator.data.local.ThemeManager
import com.example.transrator.ui.theme.BrandBlue
import com.example.transrator.ui.theme.Pretendard

@Composable
private fun Section(text: String) {
    Text(
        text, fontFamily = Pretendard, fontWeight = FontWeight.Black, fontSize = 24.sp,
        modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 10.dp)
    )
}

@Composable
private fun ThemeCard(label: String, color: Color, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().height(72.dp)
                .background(color, RoundedCornerShape(8.dp))
                .border(
                    if (selected) 3.dp else 1.dp,
                    if (selected) accentColor() else Color.Gray,
                    RoundedCornerShape(8.dp)
                )
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label, fontFamily = Pretendard,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold, fontSize = 14.sp
        )
    }
}

@Composable
fun AccountScreen(onDone: () -> Unit, onDeleted: () -> Unit, vm: AccountViewModel = viewModel()) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    val themeMode by ThemeManager.mode.collectAsState()

    var showDelete by remember { mutableStateOf(false) }
    var deletePw by remember { mutableStateOf("") }
    var deleteError by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Section("계정")
        Column(Modifier.padding(horizontal = 24.dp)) {
            // 이메일은 읽기 전용
            OutlinedTextField(
                value = s.email, onValueChange = {}, readOnly = true, enabled = false,
                singleLine = true, label = { Text("이메일", fontFamily = Pretendard) },
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            FormField(
                s.name, vm::onName, label = "이름", placeholder = "닉네임",
                error = if (s.name != s.originalName) s.nameError else null
            )
            FormField(s.currentPw, vm::onCurrent, label = "현재 비밀번호", placeholder = "현재 비밀번호를 입력해주세요.", isPassword = true)
            FormField(s.newPw, vm::onNew, label = "변경 비밀번호", placeholder = "변경하고 싶은 비밀번호를 입력해주세요.", isPassword = true, error = s.newPwError)
            FormField(s.confirmPw, vm::onConfirm, label = "비밀번호 확인", placeholder = "변경 비밀번호를 한번 더 입력해주세요.", isPassword = true, error = s.confirmError)
            if (s.error != null) {
                Text(s.error!!, color = MaterialTheme.colorScheme.error, fontFamily = Pretendard, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()

        Section("테마")
        Row(
            Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemeCard("라이트", Color(0xFFFFFFFF), themeMode == "light", { ThemeManager.set(context, "light") }, Modifier.weight(1f))
            ThemeCard("다크", Color(0xFF1C1B1F), themeMode == "dark", { ThemeManager.set(context, "dark") }, Modifier.weight(1f))
            ThemeCard("시스템 설정", Color(0xFFBDBDBD), themeMode == "system", { ThemeManager.set(context, "system") }, Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        // 회원 탈퇴 (설정 변경 버튼 위)
        Button(
            onClick = { deletePw = ""; deleteError = null; showDelete = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD90000), contentColor = Color.White
            )
        ) {
            Text("회원 탈퇴", fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                vm.save {
                    Toast.makeText(context, "설정이 변경되었어요.", Toast.LENGTH_SHORT).show()
                    onDone()
                }
            },
            enabled = !s.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White)
        ) {
            if (s.saving) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("설정 변경", fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showDelete) {
        Dialog(onDismissRequest = { showDelete = false }) {
            Surface(shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "회원탈퇴", color = accentColor(), fontFamily = Pretendard,
                        fontWeight = FontWeight.Black, fontSize = 22.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))

                    Text(
                        "회원 탈퇴를 하면 프로필, 고유명사 등 모든 데이터가 삭제되며 되돌릴 수 없어요.",
                        fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    )

                    FormField(
                        deletePw, { deletePw = it; deleteError = null },
                        placeholder = "비밀번호 입력", isPassword = true, error = deleteError
                    )

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            vm.deleteAccount(deletePw, { showDelete = false; onDeleted() }, { deleteError = it })
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD90000), contentColor = Color.White
                        )
                    ) {
                        Text("회원 탈퇴", fontFamily = Pretendard, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}