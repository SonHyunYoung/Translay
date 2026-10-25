package com.example.transrator.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.example.transrator.auth.Logo
import com.example.transrator.auth.accentColor
import com.example.transrator.glossary.GlossaryTab
import com.example.transrator.profile.Avatar
import com.example.transrator.profile.Initial
import com.example.transrator.ui.theme.BrandBlue
import com.example.transrator.ui.theme.Pretendard

private data class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab("메인", Icons.Outlined.Home, Icons.Filled.Home),
    Tab("고유명사", Icons.Outlined.MenuBook, Icons.Filled.MenuBook),
    Tab("번역 설정", Icons.Outlined.Settings, Icons.Filled.Settings)
)

@Composable
fun MainScreen(
    profileName: String,
    avatarColor: Color,
    running: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onChangeProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val soon = { Toast.makeText(context, "준비 중인 기능이에요.", Toast.LENGTH_SHORT).show() }

    Scaffold(
        topBar = {
            Column {
                Spacer(Modifier.height(20.dp))
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
            }
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(if (tab == i) t.selectedIcon else t.icon, t.label) },
                        label = { Text(t.label, fontFamily = Pretendard, fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = accentColor(),
                            indicatorColor = BrandBlue
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> HomeTab(running, onStart, onStop)
                1 -> GlossaryTab()          // import com.example.transrator.glossary.GlossaryTab
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("준비 중인 화면이에요.", fontFamily = Pretendard)
                }
            }
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

@Composable
private fun HomeTab(running: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
    val stopColor = Color(0xFFD90000)

    Column(
        Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))   // 버튼을 아래로 내리는 여백

        // 1. 번역 시작/중지
        Button(
            onClick = if (running) onStop else onStart,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (running) stopColor else BrandBlue,
                contentColor = Color.White
            )
        ) {
            Text(
                if (running) "번역 중지" else "번역 시작",
                fontFamily = Pretendard, fontWeight = FontWeight.Black, fontSize = 26.sp
            )
        }

        Spacer(Modifier.height(36.dp))
        HorizontalDivider()          // 버튼 | 사용 방법 구분선
        Spacer(Modifier.height(24.dp))

        // 2. 사용 방법
        Column(Modifier.fillMaxWidth()) {
            Text(
                "사용 방법", fontFamily = Pretendard,
                fontWeight = FontWeight.Black, fontSize = 26.sp
            )
            Spacer(Modifier.height(18.dp))
            listOf(
                "번역 시작 버튼을 누르고 화면 캡처를 허용해주세요.",
                "게임에 접속해 번역할 스토리를 열어주세요.",
                "화면 오른쪽 위의 '시작' 버튼을 눌러 번역을 시작하세요."
            ).forEachIndexed { i, step ->
                Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.Top) {   // 8dp → 14dp
                    Box(
                        Modifier.size(32.dp).background(BrandBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${i + 1}", color = Color.White, fontFamily = Pretendard,
                            fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        step, fontFamily = Pretendard, fontSize = 18.sp, lineHeight = 27.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // 3. 면책 문구 (맨 아래)
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text(
            "본 앱은 AI 번역 결과를 사용합니다. AI 번역은 정확하지 않을 수 있습니다.\n" +
                    "\n본 앱은 비주얼 노벨류의 게임에서의 사용을 상정하고 만들어져 있습니다.",
            fontFamily = Pretendard, fontSize = 13.sp, lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
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