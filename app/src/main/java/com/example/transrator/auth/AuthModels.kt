package com.example.transrator

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val message: String?, val accessToken: String, val refreshToken: String)
data class SignUpRequest(val email: String, val name: String, val password: String)
data class MessageResponse(val message: String?)