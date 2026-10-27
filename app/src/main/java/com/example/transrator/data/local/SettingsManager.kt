package com.example.transrator.data.local

import android.content.Context
import android.graphics.RectF

data class TranslateSettings(
    val fontKey: String = "default",
    val bold: Boolean = true,
    val fontSize: Int = 36,                     // px 단위
    val bgColor: Int = 0xFFFFFFFF.toInt(),      // 흰색
    val bgAlpha: Int = 90,
    val textColor: Int = 0xFF000000.toInt(),    // 검정
    val togglePos: String = "TOP_RIGHT",
    val nameRegion: RectF? = null,
    val dialogRegion: RectF? = null
)

enum class TogglePos(val label: String, val x: Float, val y: Float) {
    TOP_LEFT("위 왼쪽", 0.1f, 0.08f),
    TOP_RIGHT("위 오른쪽", 0.9f, 0.08f),
    BOTTOM_LEFT("아래 왼쪽", 0.1f, 0.92f),
    BOTTOM_RIGHT("아래 오른쪽", 0.9f, 0.92f);

    companion object {
        fun from(key: String) = entries.firstOrNull { it.name == key } ?: TOP_RIGHT
    }
}

class SettingsManager(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load() = TranslateSettings(
        fontKey = p.getString("fontKey", "default") ?: "default",
        bold = p.getBoolean("bold", true),
        fontSize = p.getInt("fontSize", 36),
        bgColor = p.getInt("bgColor", 0xFFFFFFFF.toInt()),
        bgAlpha = p.getInt("bgAlpha", 90),
        textColor = p.getInt("textColor", 0xFF000000.toInt()),
        togglePos = p.getString("togglePos", "TOP_RIGHT") ?: "TOP_RIGHT",
        nameRegion = region("name"),
        dialogRegion = region("dialog")
    )

    fun save(s: TranslateSettings) {
        p.edit()
            .putString("fontKey", s.fontKey).putBoolean("bold", s.bold).putInt("fontSize", s.fontSize)
            .putInt("bgColor", s.bgColor).putInt("bgAlpha", s.bgAlpha).putInt("textColor", s.textColor)
            .putString("togglePos", s.togglePos)
            .apply()
        saveRegion("name", s.nameRegion)
        saveRegion("dialog", s.dialogRegion)
    }

    /** null이면 저장된 영역을 지움 */
    private fun saveRegion(k: String, r: RectF?) {
        val e = p.edit()
        if (r == null) {
            e.remove("${k}_l").remove("${k}_t").remove("${k}_r").remove("${k}_b")
        } else {
            e.putFloat("${k}_l", r.left).putFloat("${k}_t", r.top)
                .putFloat("${k}_r", r.right).putFloat("${k}_b", r.bottom)
        }
        e.apply()
    }

    private fun region(k: String): RectF? =
        if (p.contains("${k}_l")) RectF(
            p.getFloat("${k}_l", 0f), p.getFloat("${k}_t", 0f),
            p.getFloat("${k}_r", 1f), p.getFloat("${k}_b", 1f)
        ) else null
}