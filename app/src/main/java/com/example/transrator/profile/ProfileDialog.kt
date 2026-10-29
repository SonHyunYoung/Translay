package com.example.transrator.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.transrator.auth.Logo
import com.example.transrator.auth.accentColor
import com.example.transrator.ui.theme.BrandBlue
import com.example.transrator.ui.theme.BrandLight
import com.example.transrator.ui.theme.Pretendard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileDialog(s: ProfileUiState, vm: ProfileViewModel) {
    val isEdit = s.editing != null

    Dialog(onDismissRequest = { if (!s.saving) vm.closeDialog() }) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (isEdit) "프로필 편집" else "프로필 추가",
                    color = accentColor(), fontFamily = Pretendard,
                    fontWeight = FontWeight.Black, fontSize = 30.sp
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))

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
                    shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandLight,
                        focusedLabelColor = BrandLight,
                        cursorColor = BrandLight
                    )
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()

                Box(Modifier.fillMaxWidth().height(20.dp)) {
                    s.dialogError?.let {
                        Text(
                            it, color = MaterialTheme.colorScheme.error, fontFamily = Pretendard,
                            fontSize = 12.sp, maxLines = 1,
                            modifier = Modifier.padding(start = 4.dp, top = 3.dp)
                        )
                    }
                }

                Text(
                    "원문 언어", fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 20.sp,   // 13 → 17
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ja" to "일본어", "zh" to "중국어", "en" to "영어").forEach { (code, label) ->
                        val selected = s.langInput == code
                        FilterChip(
                            selected = selected,
                            onClick = { vm.onLang(code) },
                            modifier = Modifier.weight(1f).height(40.dp),            // 높이 추가
                            label = {
                                Text(
                                    label, fontFamily = Pretendard, fontSize = 16.sp,   // 13 → 16
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandBlue,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = selected,
                                selectedBorderColor = BrandBlue
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (isEdit) {
                        Button(
                            onClick = vm::askDelete, enabled = !s.saving,
                            modifier = Modifier.height(45.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD90000), contentColor = Color.White
                            )
                        ) { Text("삭제", fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                    }
                    Spacer(Modifier.weight(1f))   // 남는 공간 → 취소/저장을 오른쪽으로
                    Button(
                        onClick = { if (!s.saving) vm.closeDialog() },
                        modifier = Modifier.height(45.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White)
                    ) { Text("취소", fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { vm.save() },
                        modifier = Modifier.height(45.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White)
                    ) { Text("저장", fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                }
            }
        }
    }

    if (s.confirmDelete) {
        AlertDialog(
            onDismissRequest = vm::cancelDelete,
            title = { Text("프로필 삭제", fontFamily = Pretendard, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "정말 이 프로필을 삭제하시겠습니까?\n등록한 캐릭터, 호칭, 단어도 함께 삭제되며 되돌릴 수 없어요.",
                    fontFamily = Pretendard
                )
            },
            confirmButton = {
                TextButton(onClick = vm::delete) {
                    Text("삭제", color = Color(0xFFD90000), fontFamily = Pretendard)
                }
            },
            dismissButton = { TextButton(onClick = vm::cancelDelete) { Text("취소", fontFamily = Pretendard) } }
        )
    }
}
