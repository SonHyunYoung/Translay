package com.example.transrator.main

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.transrator.auth.Logo
import com.example.transrator.profile.Avatar
import com.example.transrator.profile.Initial
import com.example.transrator.ui.theme.BrandBlue
import com.example.transrator.ui.theme.Pretendard

@Composable
private fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White)
    ) { Text(text, fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 22.sp) }
}

@Composable
private fun PopupItem(text: String, color: Color = Color.Unspecified, onClick: () -> Unit) {
    Text(
        text, fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 18.sp,
        color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp)
    )
}

@Composable
fun MainScreen(
    profileName: String,
    avatarColor: Color,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onChangeProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val soon = { Toast.makeText(context, "준비 중인 기능이에요.", Toast.LENGTH_SHORT).show() }

    Column(Modifier.fillMaxSize()) {
        // 상단바: 로고(왼쪽) + 프로필 아바타(오른쪽)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Logo(30)
            Spacer(Modifier.weight(1f))
            Box {
                Box(Modifier.clickable { menuOpen = true }) {
                    Avatar(44, avatarColor) { Initial(profileName, 20) }
                }
                if (menuOpen) {
                    Popup(
                        alignment = Alignment.TopEnd,
                        onDismissRequest = { menuOpen = false },
                        properties = PopupProperties(focusable = true)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp), shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.width(200.dp)
                        ) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Avatar(32, avatarColor) { Initial(profileName, 14) }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        profileName, fontFamily = Pretendard,
                                        fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1
                                    )
                                }
                                HorizontalDivider()
                                PopupItem("계정 설정") { menuOpen = false; soon() }
                                PopupItem("프로필 변경") { menuOpen = false; onChangeProfile() }
                                PopupItem("로그아웃", Color(0xFFD90000)) {
                                    menuOpen = false; confirmLogout = true
                                }
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(Modifier.fillMaxWidth())

        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
        ) {
            MenuButton("고유명사 등록", soon)
            MenuButton("고유명사 목록", soon)
            MenuButton("번역 설정", soon)
            MenuButton("번역 시작", onStart)
            MenuButton("번역 중지", onStop)
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("로그아웃", fontFamily = Pretendard, fontWeight = FontWeight.Bold) },
            text = { Text("로그아웃 하시겠습니까?", fontFamily = Pretendard) },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) {
                    Text("로그아웃", color = Color(0xFFD90000), fontFamily = Pretendard)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("취소", fontFamily = Pretendard) }
            }
        )
    }
}