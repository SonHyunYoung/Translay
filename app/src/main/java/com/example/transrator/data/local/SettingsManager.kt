package com.example.transrator.data.local

import android.content.Context
import android.graphics.RectF

data class TranslateSettings(
    val fontKey: String = "default",
    val bold: Boolean = true,
    val fontSize: Int = 36,
    val bgColor: Int = 0xFFFFFFFF.toInt(),     // 흰색
    val bgAlpha: Int = 90,
    val textColor: Int = 0xFF000000.toInt(),   // 검정
    val nameRegion: RectF? = null,
    val dialogRegion: RectF? = null
)

class SettingsManager(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load() = TranslateSettings(
        fontKey = p.getString("fontKey", "default") ?: "default",
        bold = p.getBoolean("bold", true),
        fontSize = p.getInt("fontSize", 36),
        bgColor = p.getInt("bgColor", 0xFFFFFFFF.toInt()),
        bgAlpha = p.getInt("bgAlpha", 90),
        textColor = p.getInt("textColor", 0xFF000000.toInt()),
        nameRegion = region("name"),
        dialogRegion = region("dialog")
    )

    fun save(s: TranslateSettings) {
        p.edit()
            .putString("fontKey", s.fontKey).putBoolean("bold", s.bold).putInt("fontSize", s.fontSize)
            .putInt("bgColor", s.bgColor).putInt("bgAlpha", s.bgAlpha).putInt("textColor", s.textColor)
            .apply()
        s.nameRegion?.let { saveRegion("name", it) }
        s.dialogRegion?.let { saveRegion("dialog", it) }
    }

    private fun saveRegion(k: String, r: RectF) {
        p.edit().putFloat("${k}_l", r.left).putFloat("${k}_t", r.top)
            .putFloat("${k}_r", r.right).putFloat("${k}_b", r.bottom).apply()
    }

    private fun region(k: String): RectF? =
        if (p.contains("${k}_l")) RectF(
            p.getFloat("${k}_l", 0f), p.getFloat("${k}_t", 0f),
            p.getFloat("${k}_r", 1f), p.getFloat("${k}_b", 1f)
        ) else null
}