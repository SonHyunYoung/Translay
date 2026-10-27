package com.example.transrator.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transrator.auth.Logo
import com.example.transrator.auth.accentColor
import com.example.transrator.ui.theme.BrandBlue
import com.example.transrator.ui.theme.Pretendard

private val AvatarPalette = listOf(
    Color(0xFFE57373), Color(0xFF64B5F6), Color(0xFF81C784), Color(0xFFFFB74D),
    Color(0xFFBA68C8), Color(0xFF4DB6AC), Color(0xFFF06292), Color(0xFF7986CB)
)

internal fun avatarColor(index: Int): Color =
    AvatarPalette[index.coerceAtLeast(0) % AvatarPalette.size]

@Composable
internal fun Avatar(
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
internal fun Initial(name: String, size: Int) {
    Text(
        name.trim().firstOrNull()?.uppercase() ?: "",
        color = Color.White, fontFamily = Pretendard,
        fontWeight = FontWeight.Bold, fontSize = size.sp
    )
}

@Composable
internal fun ProfileItem(
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
internal fun ProfileGrid(
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
internal fun BlueButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White)
    ) { Text(text, fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
}

/** 로고+라인은 전체 폭, 제목/부제는 24dp 안쪽 왼쪽 정렬 */
@Composable
internal fun ProfileHeader(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(0.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Logo(48) }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(Modifier.fillMaxWidth())
        Spacer(Modifier.height(32.dp))
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(
                title, color = accentColor(), fontFamily = Pretendard,
                fontWeight = FontWeight.Black, fontSize = 32.sp
            )
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    subtitle, fontFamily = Pretendard, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}