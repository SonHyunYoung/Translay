package com.example.transrator.auth

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transrator.ui.theme.BrandBlue
import com.example.transrator.ui.theme.BrandLight
import com.example.transrator.ui.theme.Pretendard

private val BrandBlueLight = Color(0xFF6C9BFF)

@Composable
internal fun accentColor(): Color = BrandBlue

@Composable
internal fun Logo(size: Int = 44) = Text(
    "Translay", color = BrandBlue, fontFamily = Pretendard,
    fontWeight = FontWeight.Black, fontSize = size.sp
)

@Composable
internal fun BrandButton(
    text: String,
    enabled: Boolean = true,
    loading: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick, enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandBlue, contentColor = Color.White,
            disabledContainerColor = BrandBlue.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.8f)
        )
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
        else Text(text, fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 25.sp)
    }
}

/** 오류는 빨간색, 성공 안내는 파란색. 자리(20dp)는 항상 확보 */
@Composable
internal fun Hint(error: String?, success: String? = null) {
    Box(Modifier.fillMaxWidth().height(20.dp)) {
        when {
            error != null -> Text(
                error, fontFamily = Pretendard, fontSize = 12.sp, maxLines = 1,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp, top = 3.dp)
            )
            success != null -> Text(
                success, fontFamily = Pretendard, fontSize = 12.sp, maxLines = 1,
                color = accentColor(),
                modifier = Modifier.padding(start = 4.dp, top = 3.dp)
            )
        }
    }
}

/** 라벨 + 입력칸 + 안내 문구 한 세트 */
@Composable
internal fun FormField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    labelHint: String? = null,          // 라벨 옆에 붙는 작은 규칙 안내
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
    error: String? = null,              // 값이 있으면 빨간 테두리 + 아래 빨간 문구
    success: String? = null,            //값이 있으면 파란 문구
    trailing: (@Composable () -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth()) {
        if (label != null) Row(
            Modifier.padding(start = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 20.sp)
            if (labelHint != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    labelHint, fontFamily = Pretendard, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(verticalAlignment = Alignment.Top) {
            OutlinedTextField(
                value = value, onValueChange = onChange,
                modifier = Modifier.weight(1f),
                singleLine = true, isError = error != null,
                shape = RoundedCornerShape(12.dp),
                placeholder = placeholder?.let {
                    { Text(it, fontFamily = Pretendard, fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) }
                },
                leadingIcon = leadingIcon?.let { { Icon(it, null, tint = accentColor()) } },
                trailingIcon = if (isPassword) {
                    {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                if (visible) "비밀번호 숨기기" else "비밀번호 보기",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else null,
                visualTransformation =
                    if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else keyboard
                ),
                textStyle = TextStyle(
                    fontFamily = Pretendard, fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandLight,
                    focusedLabelColor = BrandLight,
                    cursorColor = BrandLight
                )
            )
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
        Hint(error, success)
    }
}