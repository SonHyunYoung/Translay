package com.example.transrator

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query

interface TranslayApi {
    @POST("api/translate")
    suspend fun translate(
        @Header("Authorization") token: String,
        @Body request: TranslateRequest
    ): TranslateResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/auth/signUp")
    suspend fun signUp(@Body request: SignUpRequest): MessageResponse

    // 200 = 사용 가능, 409 = 중복 (HttpException으로 옴)
    @GET("api/auth/EmailCheck")
    suspend fun emailCheck(@Query("email") email: String): MessageResponse
}

