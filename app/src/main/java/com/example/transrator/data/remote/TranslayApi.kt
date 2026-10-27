package com.example.transrator.data.remote

import com.example.transrator.account.MyPageResponse
import com.example.transrator.account.NameRequest
import com.example.transrator.account.PasswordRequest
import com.example.transrator.auth.LoginRequest
import com.example.transrator.auth.LoginResponse
import com.example.transrator.auth.MessageResponse
import com.example.transrator.auth.RefreshRequest
import com.example.transrator.auth.RefreshResponse
import com.example.transrator.auth.SignUpRequest
import com.example.transrator.glossary.CharacterListResponse
import com.example.transrator.glossary.CharacterRequest
import com.example.transrator.glossary.TermListResponse
import com.example.transrator.glossary.TermRequest
import com.example.transrator.glossary.WordListResponse
import com.example.transrator.glossary.WordRequest
import com.example.transrator.profile.ProfileListResponse
import com.example.transrator.profile.ProfileRequest
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

    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") token: String): MessageResponse

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): RefreshResponse

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

    @GET("api/character/{pid}")
    suspend fun getCharacters(@Header("Authorization") t: String, @Path("pid") pid: Int, @Query("sort") sort: String?): CharacterListResponse
    @POST("api/character/{pid}")
    suspend fun createCharacter(@Header("Authorization") t: String, @Path("pid") pid: Int, @Body b: CharacterRequest): MessageResponse
    @PATCH("api/character/{pid}/{cid}")
    suspend fun updateCharacter(@Header("Authorization") t: String, @Path("pid") pid: Int, @Path("cid") cid: Int, @Body b: CharacterRequest): MessageResponse
    @DELETE("api/character/{pid}/{cid}")
    suspend fun deleteCharacter(@Header("Authorization") t: String, @Path("pid") pid: Int, @Path("cid") cid: Int): MessageResponse

    @GET("api/character/{pid}/{cid}/term")
    suspend fun getTerms(@Header("Authorization") t: String, @Path("pid") pid: Int, @Path("cid") cid: Int): TermListResponse
    @POST("api/character/{pid}/{cid}/term")
    suspend fun createTerm(@Header("Authorization") t: String, @Path("pid") pid: Int, @Path("cid") cid: Int, @Body b: TermRequest): MessageResponse
    @DELETE("api/character/{pid}/{cid}/terms/{tid}")
    suspend fun deleteTerm(@Header("Authorization") t: String, @Path("pid") pid: Int, @Path("cid") cid: Int, @Path("tid") tid: Int): MessageResponse

    @GET("api/word/{pid}")
    suspend fun getWords(@Header("Authorization") t: String, @Path("pid") pid: Int, @Query("sort") sort: String?): WordListResponse
    @POST("api/word/{pid}")
    suspend fun createWord(@Header("Authorization") t: String, @Path("pid") pid: Int, @Body b: WordRequest): MessageResponse
    @PATCH("api/word/{pid}/{wid}")
    suspend fun updateWord(@Header("Authorization") t: String, @Path("pid") pid: Int, @Path("wid") wid: Int, @Body b: WordRequest): MessageResponse
    @DELETE("api/word/{pid}/{wid}")
    suspend fun deleteWord(@Header("Authorization") t: String, @Path("pid") pid: Int, @Path("wid") wid: Int): MessageResponse

    @GET("api/user/mypage")
    suspend fun myPage(@Header("Authorization") t: String): MyPageResponse

    @PATCH("api/user/mypage/name")
    suspend fun updateName(@Header("Authorization") t: String, @Body b: NameRequest): MessageResponse

    @PATCH("api/user/mypage/password")
    suspend fun updatePassword(@Header("Authorization") t: String, @Body b: PasswordRequest): MessageResponse

}

