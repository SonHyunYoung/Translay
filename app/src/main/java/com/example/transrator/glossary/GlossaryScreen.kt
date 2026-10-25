package com.example.transrator.glossary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transrator.auth.accentColor
import com.example.transrator.ui.theme.BrandBlue
import com.example.transrator.ui.theme.Pretendard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun SmallBlueButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White)
    ) { Text(text, fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
}

@Composable
fun GlossaryTab(vm: GlossaryViewModel = viewModel()) {
    val s by vm.state.collectAsState()

    if (s.detail != null) {
        BackHandler { vm.closeDetail() }
        CharacterDetailScreen(s, vm)
    } else {
        GlossaryList(s, vm)
    }

    when (s.dialog) {
        "main" -> RegisterDialog(
            title = "고유 명사 등록", allowTerms = s.category == 0,
            error = s.dialogError, onDismiss = vm::closeDialog,
            onSave = { src, tgt, terms -> vm.register(src, tgt, terms) }
        )
        "term" -> RegisterDialog(
            title = "호칭 등록", allowTerms = false,
            error = s.dialogError, onDismiss = vm::closeDialog,
            onSave = { src, tgt, _ -> vm.registerTerm(src, tgt) }
        )
    }

    s.deleteTarget?.let { t ->
        AlertDialog(
            onDismissRequest = vm::cancelDelete,
            title = { Text("삭제", fontFamily = Pretendard, fontWeight = FontWeight.Bold) },
            text = { Text("'${t.label}'을(를) 삭제하시겠습니까?", fontFamily = Pretendard) },
            confirmButton = {
                TextButton(onClick = vm::confirmDelete) {
                    Text("삭제", color = Color(0xFFD90000), fontFamily = Pretendard)
                }
            },
            dismissButton = { TextButton(onClick = vm::cancelDelete) { Text("취소", fontFamily = Pretendard) } }
        )
    }
}

@Composable
private fun GlossaryList(s: GlossaryUiState, vm: GlossaryViewModel) {
    var sortMenu by remember { mutableStateOf(false) }
    val isChar = s.category == 0
    val empty = if (isChar) s.characters.isEmpty() else s.words.isEmpty()

    Column(Modifier.fillMaxSize()) {
        // 1. 등록 버튼
        Button(
            onClick = { vm.openDialog("main") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 36.dp).height(64.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White)
        ) {
            Text("고유명사 등록", fontFamily = Pretendard, fontWeight = FontWeight.Black, fontSize = 26.sp)
        }
        HorizontalDivider()

        // 2. 분류 칩(왼쪽) + 정렬 드롭다운(오른쪽)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("캐릭터", "고유명사").forEachIndexed { i, label ->
                    FilterChip(
                        selected = s.category == i, onClick = { vm.setCategory(i) },
                        modifier = Modifier.height(44.dp),
                        label = { Text(label, fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandBlue, selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Box {
                OutlinedButton(
                    onClick = { sortMenu = true }, shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) { Text("∨ " + if (s.sortByName) "가나다순" else "등록순", fontFamily = Pretendard, fontSize = 13.sp) }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    DropdownMenuItem(text = { Text("등록순", fontFamily = Pretendard) },
                        onClick = { sortMenu = false; vm.setSort(false) })
                    DropdownMenuItem(text = { Text("가나다순", fontFamily = Pretendard) },
                        onClick = { sortMenu = false; vm.setSort(true) })
                }
            }
        }
        HorizontalDivider()

        s.message?.let {
            Text(
                it, color = MaterialTheme.colorScheme.error, fontFamily = Pretendard, fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
            )
        }

        // 3. 목록
        if (empty && !s.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "등록된 고유명사가 없습니다.",
                    fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                if (isChar) {
                    items(s.characters, key = { it.character_id }) { c ->
                        ItemRow(
                            title = c.target_name, sub = c.source_name,
                            onClick = { vm.openDetail(c) },
                            onDelete = { vm.askDelete(DeleteTarget(DeleteKind.CHARACTER, c.character_id, c.source_name)) }
                        )
                    }
                } else {
                    items(s.words, key = { it.word_id }) { w ->
                        ItemRow(
                            title = w.target, sub = w.source, onClick = null,
                            onDelete = { vm.askDelete(DeleteTarget(DeleteKind.WORD, w.word_id, w.source)) }
                        )
                    }
                }
            }
        }
    }
}

/** 번역명(target)은 크게, 원문(source)은 작게 */
@Composable
internal fun ItemRow(title: String, sub: String, onClick: (() -> Unit)?, onDelete: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth()
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    sub, fontFamily = Pretendard, fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "✕", color = Color.Gray, fontSize = 24.sp,
                modifier = Modifier.clickable(onClick = onDelete).padding(12.dp)
            )
        }
        HorizontalDivider()
    }
}

/** 고유명사/호칭 공용 등록 모달 */
@Composable
private fun RegisterDialog(
    title: String, allowTerms: Boolean, error: String?,
    onDismiss: () -> Unit, onSave: (String, String, List<TermRequest>) -> Unit
) {
    var src by remember { mutableStateOf("") }
    var tgt by remember { mutableStateOf("") }
    val terms = remember { mutableStateListOf<TermRequest>() }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val dashColor = accentColor()

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    title, color = accentColor(), fontFamily = Pretendard, fontWeight = FontWeight.Black,
                    fontSize = 22.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(Modifier.padding(vertical = 12.dp))

                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 380.dp)
                        .verticalScroll(scroll)
                ) {
                    LabeledInput("원어", src) { src = it }
                    Spacer(Modifier.height(8.dp))
                    LabeledInput("번역", tgt) { tgt = it }

                    if (allowTerms) {
                        terms.forEachIndexed { i, t ->
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "호칭 ${i + 1}", fontFamily = Pretendard, fontWeight = FontWeight.Black,
                                    fontSize = 18.sp, modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "✕", color = Color.Gray, fontSize = 20.sp,
                                    modifier = Modifier.clickable { terms.removeAt(i) }.padding(4.dp)
                                )
                            }
                            LabeledInput("원어", t.source) { terms[i] = t.copy(source = it) }
                            Spacer(Modifier.height(8.dp))
                            LabeledInput("번역", t.target) { terms[i] = t.copy(target = it) }
                        }

                        // 점선 테두리 호칭 추가 버튼
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 0.dp, end = 0.dp, top = 14.dp, bottom = 6.dp)
                                .height(52.dp)
                                .drawBehind {
                                    drawRoundRect(
                                        color = dashColor,
                                        cornerRadius = CornerRadius(10.dp.toPx()),
                                        style = Stroke(
                                            width = 2.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                                        )
                                    )
                                }
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    terms.add(TermRequest("", ""))
                                    scope.launch {
                                        delay(50)
                                        scroll.animateScrollTo(scroll.maxValue)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.Add, contentDescription = null,
                                    tint = dashColor, modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    " 호칭 추가", color = dashColor, fontFamily = Pretendard,
                                    fontWeight = FontWeight.Black, fontSize = 18.sp
                                )
                            }
                        }
                    }
                }

                Box(Modifier.fillMaxWidth().height(20.dp)) {
                    error?.let {
                        Text(
                            it, color = MaterialTheme.colorScheme.error, fontFamily = Pretendard,
                            fontSize = 12.sp, maxLines = 1
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    SmallBlueButton("취소", onClick = onDismiss)
                    Spacer(Modifier.width(8.dp))
                    SmallBlueButton("저장") { onSave(src, tgt, terms.toList()) }
                }
            }
        }
    }
}

@Composable
private fun LabeledInput(label: String, value: String, onChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label, fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            modifier = Modifier.width(48.dp)
        )
        OutlinedTextField(
            value = value, onValueChange = onChange, singleLine = true,
            shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)
        )
    }
}