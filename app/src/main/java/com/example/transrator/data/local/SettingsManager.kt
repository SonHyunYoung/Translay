package com.example.transrator.data.local

import android.content.Context
import android.graphics.RectF

data class TranslateSettings(
    val fontKey: String = "default",
    val bold: Boolean = true,
    val fontSize: Int = 36,
    val bgColor: Int = 0xFFFFFFFF.toInt(),
    val bgAlpha: Int = 90,
    val textColor: Int = 0xFF000000.toInt(),
    val togglePos: String = "TOP_RIGHT",
    val nameRegion: RectF? = null,      // 현재 프로필의 이름 영역
    val dialogRegion: RectF? = null     // 현재 프로필의 대사 영역
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
    private val appContext = context.applicationContext
    private val p = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val tokens = TokenManager(appContext)

    /** 영역은 프로필별로 저장: p{프로필ID}_{name|dialog}_{l|t|r|b} */
    private fun rk(pid: Int, region: String, part: String) = "p${pid}_${region}_$part"

    fun load(): TranslateSettings {
        val pid = tokens.profileId()
        return TranslateSettings(
            fontKey = p.getString("fontKey", "default") ?: "default",
            bold = p.getBoolean("bold", true),
            fontSize = p.getInt("fontSize", 36),
            bgColor = p.getInt("bgColor", 0xFFFFFFFF.toInt()),
            bgAlpha = p.getInt("bgAlpha", 90),
            textColor = p.getInt("textColor", 0xFF000000.toInt()),
            togglePos = p.getString("togglePos", "TOP_RIGHT") ?: "TOP_RIGHT",
            nameRegion = region(pid, "name"),
            dialogRegion = region(pid, "dialog")
        )
    }

    fun save(s: TranslateSettings) {
        val pid = tokens.profileId()
        val e = p.edit()
            .putString("fontKey", s.fontKey).putBoolean("bold", s.bold).putInt("fontSize", s.fontSize)
            .putInt("bgColor", s.bgColor).putInt("bgAlpha", s.bgAlpha).putInt("textColor", s.textColor)
            .putString("togglePos", s.togglePos)
        putRegion(e, pid, "name", s.nameRegion)
        putRegion(e, pid, "dialog", s.dialogRegion)
        e.apply()
    }

    /** null이면 저장된 영역을 지움 */
    private fun putRegion(e: android.content.SharedPreferences.Editor, pid: Int, name: String, r: RectF?) {
        if (r == null) {
            e.remove(rk(pid, name, "l")).remove(rk(pid, name, "t"))
                .remove(rk(pid, name, "r")).remove(rk(pid, name, "b"))
        } else {
            e.putFloat(rk(pid, name, "l"), r.left).putFloat(rk(pid, name, "t"), r.top)
                .putFloat(rk(pid, name, "r"), r.right).putFloat(rk(pid, name, "b"), r.bottom)
        }
    }

    private fun region(pid: Int, name: String): RectF? =
        if (p.contains(rk(pid, name, "l"))) RectF(
            p.getFloat(rk(pid, name, "l"), 0f), p.getFloat(rk(pid, name, "t"), 0f),
            p.getFloat(rk(pid, name, "r"), 1f), p.getFloat(rk(pid, name, "b"), 1f)
        ) else null
}