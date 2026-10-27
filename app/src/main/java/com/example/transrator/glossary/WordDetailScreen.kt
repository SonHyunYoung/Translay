package com.example.transrator.glossary

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transrator.ui.theme.BrandBlue
import com.example.transrator.ui.theme.Pretendard

@Composable
internal fun WordDetailScreen(s: GlossaryUiState, vm: GlossaryViewModel) {
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(top = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("원문", fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                modifier = Modifier.width(68.dp))
            OutlinedTextField(
                value = s.editSource, onValueChange = vm::onEditSource, singleLine = true,
                textStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 22.sp),
                shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("번역", fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                modifier = Modifier.width(68.dp))
            OutlinedTextField(
                value = s.editTarget, onValueChange = vm::onEditTarget, singleLine = true,
                textStyle = TextStyle(fontFamily = Pretendard, fontSize = 20.sp),
                shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)
            )
        }
        s.message?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontFamily = Pretendard, fontSize = 13.sp,
                modifier = Modifier.padding(16.dp))
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = { vm.saveWordDetail { Toast.makeText(context, "저장되었어요.", Toast.LENGTH_SHORT).show() } },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White)
        ) { Text("변경 내용 저장", fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
    }
}