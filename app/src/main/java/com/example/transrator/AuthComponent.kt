package com.example.transrator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transrator.ui.theme.BrandBlue
import com.example.transrator.ui.theme.Pretendard

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
    onClick: () -> Unit
) {
    Button(
        onClick = onClick, enabled = enabled && !loading,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandBlue, contentColor = Color.White,
            disabledContainerColor = BrandBlue.copy(alpha = 0.4f),
            disabledContentColor = Color.White
        )
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
        else Text(text, fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
internal fun Field(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isPassword: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
    error: String? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = modifier.fillMaxWidth(),
        singleLine = true, shape = RoundedCornerShape(8.dp), isError = error != null,
        placeholder = placeholder?.let { { Text(it, fontFamily = Pretendard, color = Color.Gray) } },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboard),
        textStyle = TextStyle(fontFamily = Pretendard, fontSize = 16.sp)
    )
}

@Composable
internal fun Hint(text: String?, ok: Boolean = false) {
    if (text != null) Text(
        text, fontFamily = Pretendard, fontSize = 12.sp,
        color = if (ok) BrandBlue else MaterialTheme.colorScheme.error,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp)
    )
}