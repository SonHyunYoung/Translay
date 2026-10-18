package com.example.transrator

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import retrofit2.http.Query
import retrofit2.http.Path

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

    @GET("api/profile")
    suspend fun getProfiles(@Header("Authorization") token: String): ProfileListResponse

    @POST("api/profile")
    suspend fun createProfile(@Header("Authorization") token: String, @Body request: ProfileRequest): MessageResponse

    @PATCH("api/profile/{id}")
    suspend fun updateProfile(@Header("Authorization") token: String, @Path("id") id: Int, @Body request: ProfileRequest): MessageResponse

    @DELETE("api/profile/{id}")
    suspend fun deleteProfile(@Header("Authorization") token: String, @Path("id") id: Int): MessageResponse
}

