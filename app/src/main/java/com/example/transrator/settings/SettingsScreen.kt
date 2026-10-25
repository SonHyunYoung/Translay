package com.example.transrator.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transrator.auth.accentColor
import com.example.transrator.data.local.SettingsManager
import com.example.transrator.ui.theme.BrandBlue
import com.example.transrator.ui.theme.Pretendard

private val FONT_OPTIONS = listOf(
    "default" to "시스템 기본",
    "sans" to "고딕(산세리프)",
    "serif" to "명조(세리프)",
    "mono" to "고정폭",
    "pretendard" to "Pretendard"
)

private val SIZE_OPTIONS = listOf(24, 28, 32, 36, 40, 44, 48, 56, 64)
private val BG_COLORS = listOf(0xFFFFFFFF, 0xFF000000)
private val TEXT_COLORS = listOf(0xFF000000, 0xFFFFFFFF)

fun fontFamilyFor(key: String): FontFamily = when (key) {
    "sans" -> FontFamily.SansSerif
    "serif" -> FontFamily.Serif
    "mono" -> FontFamily.Monospace
    "pretendard" -> Pretendard
    else -> FontFamily.Default
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text, fontFamily = Pretendard, fontWeight = FontWeight.Black, fontSize = 24.sp,
        modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 12.dp)
    )
}

@Composable
private fun ColorRow(title: String, colors: List<Long>, selected: Int, onPick: (Int) -> Unit) {
    Text(
        title, fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 16.sp,
        modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 8.dp)
    )
    Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        colors.forEach { c ->
            val argb = c.toInt()
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color(argb))
                    .border(
                        if (selected == argb) 3.dp else 1.dp,
                        if (selected == argb) accentColor() else Color.Gray, CircleShape
                    )
                    .clickable { onPick(argb) }
            )
        }
    }
}

@Composable
private fun CenterDropdown(
    label: String, modifier: Modifier = Modifier,
    items: List<Pair<String, () -> Unit>>, itemFont: (Int) -> FontFamily = { Pretendard }
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { open = true }, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                label, fontFamily = Pretendard, fontSize = 17.sp,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
            )
            Text("∨", fontSize = 16.sp)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEachIndexed { i, (text, action) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text, fontFamily = itemFont(i), fontSize = 17.sp,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                        )
                    },
                    onClick = { open = false; action() }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val manager = remember { SettingsManager(context) }
    val initial = remember { manager.load() }

    var fontKey by remember { mutableStateOf(initial.fontKey) }
    var bold by remember { mutableStateOf(initial.bold) }
    var sizePx by remember { mutableIntStateOf(initial.fontSize) }
    var bgColor by remember { mutableIntStateOf(initial.bgColor) }
    var bgAlpha by remember { mutableFloatStateOf(initial.bgAlpha.toFloat()) }
    var textColor by remember { mutableIntStateOf(initial.textColor) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // 미리보기
        Box(
            Modifier.fillMaxWidth().padding(20.dp).height(130.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .background(Color(bgColor).copy(alpha = bgAlpha / 100f), RoundedCornerShape(10.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val previewSp = with(density) { sizePx.toDp().value }.sp
            Text(
                "이 문장은 번역 오버레이에 표시되는 글씨 미리보기입니다.",
                color = Color(textColor),
                fontFamily = fontFamilyFor(fontKey),
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                fontSize = previewSp, lineHeight = previewSp * 1.4f,
                textAlign = TextAlign.Center
            )
        }
        HorizontalDivider()

        // ---- 폰트 설정 ----
        SectionTitle("폰트 설정")
        CenterDropdown(
            label = FONT_OPTIONS.first { it.first == fontKey }.second,
            modifier = Modifier.padding(horizontal = 24.dp),
            items = FONT_OPTIONS.map { (key, name) -> name to { fontKey = key } },
            itemFont = { fontFamilyFor(FONT_OPTIONS[it].first) }
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { bold = !bold }, modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(8.dp)
            ) { Text(if (bold) "굵게 ✓" else "굵게", fontFamily = Pretendard, fontSize = 17.sp) }

            CenterDropdown(
                label = "${sizePx}px", modifier = Modifier.weight(1f),
                items = SIZE_OPTIONS.map { px -> "${px}px" to { sizePx = px } }
            )
        }
        ColorRow("글자 색", TEXT_COLORS, textColor) { textColor = it }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()

        // ---- 오버레이 설정 ----
        SectionTitle("오버레이 설정")
        ColorRow("배경 색", BG_COLORS, bgColor) {
            bgColor = it
            textColor = if (it == 0xFFFFFFFF.toInt()) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        Text(
            "배경 투명도 ${bgAlpha.toInt()}%", fontFamily = Pretendard, fontWeight = FontWeight.Bold,
            fontSize = 16.sp, modifier = Modifier.padding(start = 24.dp, top = 14.dp)
        )
        Slider(
            value = bgAlpha, onValueChange = { bgAlpha = it }, valueRange = 10f..100f,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))

        // ---- 버튼 ----
        Button(
            onClick = { Toast.makeText(context, "번역 영역 설정은 준비 중이에요.", Toast.LENGTH_SHORT).show() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White)
        ) { Text("번역 영역 설정", fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 17.sp) }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                manager.save(
                    initial.copy(
                        fontKey = fontKey, bold = bold, fontSize = sizePx,
                        bgColor = bgColor, bgAlpha = bgAlpha.toInt(), textColor = textColor
                    )
                )
                Toast.makeText(context, "설정이 저장되었어요.", Toast.LENGTH_SHORT).show()
                onDone()
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White)
        ) { Text("설정 완료", fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
        Spacer(Modifier.height(24.dp))
    }
}