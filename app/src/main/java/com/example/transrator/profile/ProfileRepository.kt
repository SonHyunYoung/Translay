package com.example.transrator.profile

import com.example.transrator.data.remote.RetrofitClient
import com.example.transrator.data.local.TokenManager
import com.example.transrator.auth.MessageResponse
import com.google.gson.Gson
import retrofit2.HttpException

class ProfileRepository(private val tokens: TokenManager) {
    private val api = RetrofitClient.api
    private fun auth() = "Bearer ${tokens.accessToken()}"

    suspend fun list() = call { api.getProfiles(auth()).profiles }
    suspend fun create(name: String) = call { api.createProfile(auth(), ProfileRequest(name, "ja")); Unit }
    suspend fun update(id: Int, name: String, lang: String) =
        call { api.updateProfile(auth(), id, ProfileRequest(name, lang)); Unit }
    suspend fun delete(id: Int) = call { api.deleteProfile(auth(), id); Unit }

    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: HttpException) {
        val msg = try {
            Gson().fromJson(e.response()?.errorBody()?.string(), MessageResponse::class.java)?.message
        } catch (x: Exception) { null }
        Result.failure(Exception(msg ?: "요청에 실패했습니다."))
    } catch (e: Exception) {
        Result.failure(Exception("서버에 연결할 수 없습니다."))
    }

    suspend fun create(name: String, lang: String) =
        call { api.createProfile(auth(), ProfileRequest(name, lang)); Unit }

}