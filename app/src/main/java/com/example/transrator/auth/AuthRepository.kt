package com.example.transrator.auth

import com.example.transrator.data.remote.RetrofitClient
import com.example.transrator.data.TokenManager
import com.google.gson.Gson
import retrofit2.HttpException

class AuthRepository(private val tokens: TokenManager) {
    private val api = RetrofitClient.api

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val res = api.login(LoginRequest(email, password))
        tokens.save(res.accessToken, res.refreshToken)
    }.mapError()

    suspend fun signUp(email: String, name: String, password: String): Result<Unit> = runCatching {
        api.signUp(SignUpRequest(email, name, password)); Unit
    }.mapError()

    /** true = 사용 가능 */
    suspend fun checkEmail(email: String): Result<Boolean> = try {
        api.emailCheck(email); Result.success(true)
    } catch (e: HttpException) {
        if (e.code() == 409) Result.success(false)
        else Result.failure(Exception(e.serverMessage()))
    } catch (e: Exception) {
        Result.failure(Exception("서버에 연결할 수 없습니다."))
    }

    private fun <T> Result<T>.mapError(): Result<T> = fold(
        onSuccess = { this },
        onFailure = { e ->
            Result.failure(Exception(
                if (e is HttpException) e.serverMessage() else "서버에 연결할 수 없습니다."
            ))
        }
    )

    private fun HttpException.serverMessage(): String = try {
        Gson().fromJson(response()?.errorBody()?.string(), MessageResponse::class.java)
            ?.message ?: "요청에 실패했습니다."
    } catch (e: Exception) { "요청에 실패했습니다." }

    /** true = 로그인 유지 가능(토큰 갱신 성공), false = 다시 로그인 필요, null = 서버 연결 실패 */
    suspend fun tryRefresh(): Boolean? {
        val refresh = tokens.refreshToken() ?: return false
        return try {
            val res = api.refresh(RefreshRequest(refresh))
            tokens.save(res.accessToken, res.refreshToken ?: refresh)
            true
        } catch (e: HttpException) {
            if (e.code() == 401) { tokens.clear(); false } else null
        } catch (e: Exception) {
            null
        }
    }
}
