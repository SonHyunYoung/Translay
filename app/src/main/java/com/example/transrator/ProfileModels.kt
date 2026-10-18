package com.example.transrator

data class Profile(
    val profile_id: Int,
    val name: String,
    val sourcelanguage: String? = "ja"
)
data class ProfileListResponse(val message: String?, val profiles: List<Profile> = emptyList())
data class ProfileRequest(val name: String, val sourcelanguage: String? = null)