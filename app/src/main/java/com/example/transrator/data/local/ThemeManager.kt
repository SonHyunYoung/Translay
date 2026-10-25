package com.example.transrator.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

/** "light" / "dark" / "system" */
object ThemeManager {
    val mode = MutableStateFlow("system")

    fun init(context: Context) {
        mode.value = context.applicationContext
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("themeMode", "system") ?: "system"
    }

    fun set(context: Context, value: String) {
        context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit().putString("themeMode", value).apply()
        mode.value = value
    }
}