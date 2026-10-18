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

private val AvatarPalette = listOf(
    Color(0xFFE57373), Color(0xFF64B5F6), Color(0xFF81C784), Color(0xFFFFB74D),
    Color(0xFFBA68C8), Color(0xFF4DB6AC), Color(0xFFF06292), Color(0xFF7986CB)
)
private fun avatarColor(index: Int) = AvatarPalette[index % AvatarPalette.size]

@Composable
private fun Avatar(
    size: Int,
    color: Color = Color(0xFFD9D9D9),
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center, content = content
    )
}

@Composable
private fun Initial(name: String, size: Int) {
    Text(
        name.trim().firstOrNull()?.uppercase() ?: "",
        color = Color.White, fontFamily = Pretendard,
        fontWeight = FontWeight.Bold, fontSize = size.sp
    )
}

@Composable
private fun ProfileItem(
    label: String,
    onClick: () -> Unit,
    color: Color = Color(0xFFD9D9D9),
    content: @Composable BoxScope.() -> Unit = {}
) {
    Column(
        Modifier.width(120.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Avatar(96, color, content)
        Spacer(Modifier.height(8.dp))
        Text(label, fontFamily = Pretendard, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ProfileGrid(
    s: ProfileUiState,
    showAdd: Boolean,
    onProfile: (Profile) -> Unit,
    onAdd: () -> Unit
) {
    val items: List<Profile?> = s.profiles + if (showAdd && s.canAdd) listOf(null) else emptyList()
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { p ->
                    if (p != null) {
                        val color = avatarColor(s.profiles.indexOf(p))
                        ProfileItem(p.name, { onProfile(p) }, color) { Initial(p.name, 36) }
                    } else {
                        ProfileItem("프로필 추가", onAdd) {
                            Icon(Icons.Filled.Add, "추가", Modifier.size(52.dp), tint = Color.DarkGray)
                        }
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

/** 로고+라인은 전체 폭, 제목/부제는 24dp 안쪽 왼쪽 정렬 */
@Composable
private fun Header(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Logo(48) }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(Modifier.fillMaxWidth())
        Spacer(Modifier.height(32.dp))
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(title, color = accentColor(), fontFamily = Pretendard,
                fontWeight = FontWeight.Black, fontSize = 32.sp)
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, fontFamily = Pretendard, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
    LaunchedEffect(Unit) { vm.load() }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Header("프로필 선택", "접속할 프로필을 선택해주세요")
        if (s.loading) CircularProgressIndicator()
        s.message?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontFamily = Pretendard, fontSize = 13.sp)
        }
        ProfileGrid(s, showAdd = true, onProfile = { vm.select(it); onSelected() }, onAdd = { vm.openCreate() })
        Spacer(Modifier.height(32.dp))
        BlueButton("프로필 편집", onEdit,
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp))
    }
    if (s.dialogOpen) ProfileDialog(s, vm)
}

// ---------------- 프로필 편집 ----------------
@Composable
fun ProfileEditScreen(onDone: () -> Unit, vm: ProfileViewModel = viewModel()) {
    val s by vm.state.collectAsState()

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Header("프로필 편집", "편집/삭제할 프로필을 선택해주세요")
        if (s.loading) CircularProgressIndicator()
        s.message?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontFamily = Pretendard, fontSize = 13.sp)
        }
        ProfileGrid(s, showAdd = false, onProfile = { vm.openEdit(it) }, onAdd = {})
        Spacer(Modifier.height(32.dp))
        BlueButton("설정 완료", onDone,
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp))
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
                val dialogColor = avatarColor(
                    if (isEdit) s.profiles.indexOfFirst { it.profile_id == s.editing!!.profile_id }
                    else s.profiles.size
                )
                Avatar(110, dialogColor) { Initial(s.nameInput, 48) }
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