package com.example.transrator

data class TranslateRequest(
    val profileId: Int,
    val text: String,
    val context: List<ContextItem> = emptyList()
)

data class ContextItem(
    val source_text: String,
    val translated_text: String
)

data class TranslateResponse(
    val message: String,
    val originalText: String?,
    val translatedText: String?
)