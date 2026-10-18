package com.example.transrator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transrator.ui.theme.BrandBlue
import com.example.transrator.ui.theme.Pretendard

@Composable
private fun Avatar(size: Int, content: @Composable BoxScope.() -> Unit = {}) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Color(0xFFD9D9D9)),
        contentAlignment = Alignment.Center, content = content
    )
}

@Composable
private fun ProfileItem(label: String, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit = {}) {
    Column(
        Modifier.width(88.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Avatar(72, content)
        Spacer(Modifier.height(6.dp))
        Text(label, fontFamily = Pretendard, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ProfileGrid(
    s: ProfileUiState,
    onProfile: (Profile) -> Unit,
    onAdd: () -> Unit
) {
    // 최대 4개 + 추가 버튼 → 2열 그리드로 배치
    val items: List<Profile?> = s.profiles + if (s.canAdd) listOf(null) else emptyList()
    Column(verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { p ->
                    if (p != null) ProfileItem(p.name, { onProfile(p) })
                    else ProfileItem("프로필 추가", onAdd) {
                        Icon(Icons.Filled.Add, "추가", Modifier.size(40.dp), tint = Color.DarkGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun BlueButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White)
    ) { Text(text, fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
}

@Composable
private fun Header(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Logo(36)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(40.dp))
        Text(title, color = accentColor(), fontFamily = Pretendard, fontWeight = FontWeight.Black, fontSize = 34.sp)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = accentColor(), fontFamily = Pretendard, fontWeight = FontWeight.Bold,
                fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(32.dp))
    }
}

// ---------------- 프로필 선택 ----------------
@Composable
fun ProfileSelectScreen(
    onSelected: () -> Unit,
    onEdit: () -> Unit,
    vm: ProfileViewModel = viewModel()
) {
    val s by vm.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Header("프로필 선택")
        if (s.loading) CircularProgressIndicator()
        s.message?.let { Text(it, color = MaterialTheme.colorScheme.error, fontFamily = Pretendard, fontSize = 13.sp) }
        ProfileGrid(s, onProfile = { vm.select(it); onSelected() }, onAdd = { vm.openCreate() })
        Spacer(Modifier.height(28.dp))
        BlueButton("프로필 편집", onEdit)
    }
    if (s.dialogOpen) ProfileDialog(s, vm)
}

// ---------------- 프로필 편집 ----------------
@Composable
fun ProfileEditScreen(onDone: () -> Unit, vm: ProfileViewModel = viewModel()) {
    val s by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Header("프로필 편집", "편집/삭제할 프로필을 선택해주세요")
        if (s.loading) CircularProgressIndicator()
        s.message?.let { Text(it, color = MaterialTheme.colorScheme.error, fontFamily = Pretendard, fontSize = 13.sp) }
        ProfileGrid(s.copy(profiles = s.profiles), onProfile = { vm.openEdit(it) }, onAdd = { vm.openCreate() })
        Spacer(Modifier.height(28.dp))
        BlueButton("설정 완료", onDone, Modifier.fillMaxWidth().height(48.dp))
    }
    if (s.dialogOpen) ProfileDialog(s, vm)
}

// ---------------- 모달 (생성/편집 공용) ----------------
@Composable
private fun ProfileDialog(s: ProfileUiState, vm: ProfileViewModel) {
    val isEdit = s.editing != null

    Dialog(onDismissRequest = { if (!s.saving) vm.closeDialog() }) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Logo(28)
                Spacer(Modifier.height(12.dp))
                Text(
                    if (isEdit) "프로필 편집" else "프로필 추가",
                    fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Avatar(110)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = s.nameInput, onValueChange = vm::onName,
                    singleLine = true, isError = s.dialogError != null,
                    placeholder = { Text("프로필 이름", fontFamily = Pretendard) },
                    shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()
                )
                Box(Modifier.fillMaxWidth().height(20.dp)) {
                    s.dialogError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontFamily = Pretendard,
                            fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(start = 4.dp, top = 3.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (isEdit) {
                        Button(
                            onClick = vm::askDelete, enabled = !s.saving,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD90000), contentColor = Color.White)
                        ) { Text("삭제", fontFamily = Pretendard, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.weight(1f))
                    BlueButton("취소", { if (!s.saving) vm.closeDialog() })
                    Spacer(Modifier.width(8.dp))
                    BlueButton("저장", { vm.save() })
                }
            }
        }
    }

    if (s.confirmDelete) {
        AlertDialog(
            onDismissRequest = vm::cancelDelete,
            title = { Text("프로필 삭제", fontFamily = Pretendard, fontWeight = FontWeight.Bold) },
            text = {
                Text("정말 이 프로필을 삭제하시겠습니까?\n등록한 캐릭터, 호칭, 단어도 함께 삭제되며 되돌릴 수 없어요.",
                    fontFamily = Pretendard)
            },
            confirmButton = {
                TextButton(onClick = vm::delete) { Text("삭제", color = Color(0xFFD90000), fontFamily = Pretendard) }
            },
            dismissButton = { TextButton(onClick = vm::cancelDelete) { Text("취소", fontFamily = Pretendard) } }
        )
    }
}