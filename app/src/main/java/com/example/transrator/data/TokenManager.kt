package com.example.transrator.data

import android.content.Context

class TokenManager(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("auth", Context.MODE_PRIVATE)

    fun save(access: String, refresh: String) {
        prefs.edit().putString("access", access).putString("refresh", refresh).apply()
    }
    fun accessToken(): String? = prefs.getString("access", null)
    fun refreshToken(): String? = prefs.getString("refresh", null)
    fun clear() = prefs.edit().clear().apply()

    fun saveProfileId(id: Int) = prefs.edit().putInt("profileId", id).apply()
    fun profileId(): Int = prefs.getInt("profileId", -1)
}