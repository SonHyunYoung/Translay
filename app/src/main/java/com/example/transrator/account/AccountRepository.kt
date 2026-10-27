package com.example.transrator.account

import com.example.transrator.auth.MessageResponse
import com.example.transrator.data.local.TokenManager
import com.example.transrator.data.remote.RetrofitClient
import com.google.gson.Gson
import retrofit2.HttpException

class AccountRepository(private val tokens: TokenManager) {
    private val api = RetrofitClient.api
    private fun auth() = "Bearer ${tokens.accessToken()}"

    suspend fun load() = call {
        api.myPage(auth()).user ?: UserInfo()
    }
    suspend fun updateName(name: String) = call { api.updateName(auth(), NameRequest(name)); Unit }
    suspend fun updatePassword(cur: String, new: String) =
        call { api.updatePassword(auth(), PasswordRequest(cur, new)); Unit }

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

    suspend fun deleteAccount(password: String) =
        call { api.deleteAccount(auth(), DeleteAccountRequest(password)); Unit }
}