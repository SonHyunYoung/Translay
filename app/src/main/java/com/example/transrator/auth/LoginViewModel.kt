package com.example.transrator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepository(TokenManager(app))
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun onEmail(v: String) { _state.value = _state.value.copy(email = v.trim(), error = null) }
    fun onPassword(v: String) { _state.value = _state.value.copy(password = v, error = null) }

    fun login() {
        val s = _state.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(error = "이메일과 비밀번호를 입력해주세요."); return
        }
        _state.value = s.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repo.login(s.email, s.password)
                .onSuccess { _state.value = _state.value.copy(isLoading = false, success = true) }
                .onFailure { _state.value = _state.value.copy(isLoading = false, error = it.message) }
        }
    }
}