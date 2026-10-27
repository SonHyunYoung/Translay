package com.example.transrator.glossary

import com.example.transrator.auth.MessageResponse
import com.example.transrator.data.local.TokenManager
import com.example.transrator.data.remote.RetrofitClient
import com.google.gson.Gson
import retrofit2.HttpException

class GlossaryRepository(private val tokens: TokenManager) {
    private val api = RetrofitClient.api
    private fun auth() = "Bearer ${tokens.accessToken()}"
    private fun pid() = tokens.profileId()

    suspend fun characters(sort: String?) = call { api.getCharacters(auth(), pid(), sort).character }
    suspend fun words(sort: String?) = call { api.getWords(auth(), pid(), sort).word }
    suspend fun terms(cid: Int) = call { api.getTerms(auth(), pid(), cid).terms }

    suspend fun addCharacter(src: String, tgt: String, terms: List<TermRequest>) =
        call { api.createCharacter(auth(), pid(), CharacterRequest(src, tgt, terms.ifEmpty { null })); Unit }
    suspend fun updateCharacter(cid: Int, src: String, tgt: String) =
        call { api.updateCharacter(auth(), pid(), cid, CharacterRequest(src, tgt)); Unit }
    suspend fun deleteCharacter(cid: Int) = call { api.deleteCharacter(auth(), pid(), cid); Unit }

    suspend fun addTerm(cid: Int, src: String, tgt: String) =
        call { api.createTerm(auth(), pid(), cid, TermRequest(src, tgt)); Unit }
    suspend fun deleteTerm(cid: Int, tid: Int) = call { api.deleteTerm(auth(), pid(), cid, tid); Unit }

    suspend fun addWord(src: String, tgt: String) = call { api.createWord(auth(), pid(), WordRequest(src, tgt)); Unit }
    suspend fun deleteWord(wid: Int) = call { api.deleteWord(auth(), pid(), wid); Unit }

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

    suspend fun updateWord(wid: Int, src: String, tgt: String) =
        call { api.updateWord(auth(), pid(), wid, WordRequest(src, tgt)); Unit }
}