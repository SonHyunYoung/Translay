package com.example.transrator.glossary

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transrator.ui.theme.BrandBlue
import com.example.transrator.ui.theme.Pretendard

@Composable
internal fun CharacterDetailScreen(s: GlossaryUiState, vm: GlossaryViewModel) {
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(30.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("원문", fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 22.sp,   // 18 → 22
                modifier = Modifier.width(68.dp))                                                    // 56 → 68
            OutlinedTextField(
                value = s.editSource, onValueChange = vm::onEditSource, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 22.sp          // 20 → 22
                ),
                shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("번역", fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 22.sp,   // 18 → 22
                modifier = Modifier.width(68.dp))                                                    // 56 → 68
            OutlinedTextField(
                value = s.editTarget, onValueChange = vm::onEditTarget, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Pretendard, fontSize = 20.sp),
                shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(30.dp))
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("호칭", fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 22.sp)   // 18 → 22
            Spacer(Modifier.weight(1f))
            SmallBlueButton("호칭 추가") { vm.openDialog("term") }
        }
        HorizontalDivider()
        s.message?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontFamily = Pretendard, fontSize = 13.sp,
                modifier = Modifier.padding(16.dp))
        }

        if (s.terms.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("등록된 호칭이 없습니다.", fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 25.sp)
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(s.terms, key = { it.term_id }) { t ->
                    ItemRow(
                        title = t.target, sub = t.source, onClick = null,
                        onDelete = { vm.askDelete(DeleteTarget(DeleteKind.TERM, t.term_id, t.source)) }
                    )
                }
            }
        }

        Button(
            onClick = { vm.saveDetail { Toast.makeText(context, "저장되었어요.", Toast.LENGTH_SHORT).show() } },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White)
        ) { Text("변경 내용 저장", fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
    }
}