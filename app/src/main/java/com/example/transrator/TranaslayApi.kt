package com.example.transrator

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface TranslayApi {
    @POST("translate")
    suspend fun translate(
        @Header("Authorization") token: String,
        @Body request: TranslateRequest
    ): TranslateResponse
}