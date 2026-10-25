package com.example.transrator.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.transrator.data.local.TokenManager
import com.example.transrator.data.remote.RetrofitClient
import com.example.transrator.profile.Profile
import com.example.transrator.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MainUiState(val profileName: String = "", val profileIndex: Int = 0)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val tokens = TokenManager(app)
    private val repo = ProfileRepository(tokens)
    private val _s = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _s

    init { load() }

    private fun load() {
        viewModelScope.launch {
            repo.list().onSuccess { list: List<Profile> ->
                val id = tokens.profileId()
                val idx = list.indexOfFirst { it.profile_id == id }
                if (idx >= 0) _s.value = MainUiState(list[idx].name, idx)
            }
        }
    }

    /** 서버 refresh token 폐기(실패해도 로컬은 지움) 후 완료 콜백 */
    fun logout(done: () -> Unit) {
        viewModelScope.launch {
            try { RetrofitClient.api.logout("Bearer ${tokens.accessToken()}") } catch (_: Exception) {}
            tokens.clear()
            done()
        }
    }
}