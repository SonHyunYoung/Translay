package com.example.transrator.account

data class UserInfo(val email: String? = null, val name: String? = null)
data class MyPageResponse(val message: String?, val user: UserInfo? = null)
data class NameRequest(val name: String)
data class PasswordRequest(val currentPassword: String, val newPassword: String)