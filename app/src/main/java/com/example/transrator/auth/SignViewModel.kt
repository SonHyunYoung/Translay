package com.example.transrator.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.transrator.data.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
private val NAME_REGEX = Regex("^[a-zA-Z0-9가-힣_.]+$")
private val PW_REGEX = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")

data class SignUpUiState(
    val email: String = "",
    val name: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val emailChecked: Boolean = false,
    val emailMessage: String? = null,
    val emailOk: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
) {
    val nameError: String? get() = when {
        name.isEmpty() -> null
        name.length !in 2..15 -> "이름은 2~15자로 입력해주세요."
        !NAME_REGEX.matches(name) -> "공백과 _ . 를 제외한 특수기호는 사용할 수 없어요."
        else -> null
    }
    val passwordError: String? get() =
        if (password.isNotEmpty() && !PW_REGEX.matches(password))
            "8자 이상, 대·소문자, 숫자, 특수문자(@$!%*?&)를 포함해주세요." else null
    val confirmError: String? get() =
        if (passwordConfirm.isNotEmpty() && password != passwordConfirm)
            "비밀번호가 일치하지 않아요." else null
    val canSubmit: Boolean get() =
        emailChecked && name.isNotEmpty() && password.isNotEmpty() && passwordConfirm.isNotEmpty() &&
                nameError == null && passwordError == null && confirmError == null && !isLoading
}

class SignUpViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepository(TokenManager(app))
    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state

    fun onEmail(v: String) {
        // 이메일이 바뀌면 중복체크 결과 무효화
        _state.value = _state.value.copy(email = v.trim(), emailChecked = false, emailMessage = null, error = null)
    }
    fun onName(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun onPassword(v: String) { _state.value = _state.value.copy(password = v, error = null) }
    fun onConfirm(v: String) { _state.value = _state.value.copy(passwordConfirm = v, error = null) }

    fun checkEmail() {
        val email = _state.value.email
        if (!EMAIL_REGEX.matches(email)) {
            _state.value = _state.value.copy(emailMessage = "올바른 이메일 형식이 아니에요.", emailOk = false); return
        }
        viewModelScope.launch {
            repo.checkEmail(email)
                .onSuccess { ok ->
                    _state.value = _state.value.copy(
                        emailChecked = ok, emailOk = ok,
                        emailMessage = if (ok) "사용 가능한 이메일이에요." else "이미 가입된 이메일이에요."
                    )
                }
                .onFailure { _state.value = _state.value.copy(emailMessage = it.message, emailOk = false) }
        }
    }

    fun signUp() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.value = s.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repo.signUp(s.email, s.name, s.password)
                .onSuccess { _state.value = _state.value.copy(isLoading = false, success = true) }
                .onFailure { _state.value = _state.value.copy(isLoading = false, error = it.message) }
        }
    }
}