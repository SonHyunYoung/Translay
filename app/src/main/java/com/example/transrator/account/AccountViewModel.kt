package com.example.transrator.account

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.transrator.data.local.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private val NAME_REGEX = Regex("^[a-zA-Z0-9가-힣_.]+$")
private val PW_REGEX = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")

data class AccountUiState(
    val email: String = "",
    val originalName: String = "",
    val name: String = "",
    val currentPw: String = "",
    val newPw: String = "",
    val confirmPw: String = "",
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null
) {
    val nameError: String? get() = when {
        name.isEmpty() -> "이름을 입력해주세요."
        name.length !in 2..15 -> "이름은 2~15자로 입력해주세요."
        !NAME_REGEX.matches(name) -> "공백과 _ . 를 제외한 특수기호는 사용할 수 없어요."
        else -> null
    }
    val pwTouched get() = currentPw.isNotEmpty() || newPw.isNotEmpty() || confirmPw.isNotEmpty()
    val newPwError: String? get() =
        if (newPw.isNotEmpty() && !PW_REGEX.matches(newPw)) "8자 이상, 대·소문자·숫자·특수문자를 포함해주세요." else null
    val confirmError: String? get() =
        if (confirmPw.isNotEmpty() && newPw != confirmPw) "비밀번호가 일치하지 않아요." else null
}

class AccountViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AccountRepository(TokenManager(app))
    private val _s = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _s

    init {
        _s.value = _s.value.copy(loading = true)
        viewModelScope.launch {
            repo.load()
                .onSuccess {
                    _s.value = _s.value.copy(
                        loading = false, email = it.email ?: "",
                        originalName = it.name ?: "", name = it.name ?: ""
                    )
                }
                .onFailure { _s.value = _s.value.copy(loading = false, error = it.message) }
        }
    }

    fun onName(v: String) { _s.value = _s.value.copy(name = v, error = null) }
    fun onCurrent(v: String) { _s.value = _s.value.copy(currentPw = v, error = null) }
    fun onNew(v: String) { _s.value = _s.value.copy(newPw = v, error = null) }
    fun onConfirm(v: String) { _s.value = _s.value.copy(confirmPw = v, error = null) }

    /** 저장. 변경할 게 없으면 바로 onDone */
    fun save(onDone: () -> Unit) {
        val s = _s.value
        val nameChanged = s.name != s.originalName
        if (nameChanged && s.nameError != null) { _s.value = s.copy(error = s.nameError); return }
        if (s.pwTouched) {
            when {
                s.currentPw.isEmpty() -> { _s.value = s.copy(error = "현재 비밀번호를 입력해주세요."); return }
                s.newPw.isEmpty() -> { _s.value = s.copy(error = "변경할 비밀번호를 입력해주세요."); return }
                s.newPwError != null -> { _s.value = s.copy(error = s.newPwError); return }
                s.confirmError != null || s.confirmPw.isEmpty() -> {
                    _s.value = s.copy(error = "비밀번호 확인이 일치하지 않아요."); return
                }
            }
        }
        if (!nameChanged && !s.pwTouched) { onDone(); return }

        _s.value = s.copy(saving = true, error = null)
        viewModelScope.launch {
            if (nameChanged) {
                val r = repo.updateName(s.name.trim())
                if (r.isFailure) {
                    _s.value = _s.value.copy(saving = false, error = r.exceptionOrNull()?.message); return@launch
                }
            }
            if (s.pwTouched) {
                val r = repo.updatePassword(s.currentPw, s.newPw)
                if (r.isFailure) {
                    _s.value = _s.value.copy(saving = false, error = r.exceptionOrNull()?.message); return@launch
                }
            }
            _s.value = _s.value.copy(saving = false, originalName = s.name.trim(), currentPw = "", newPw = "", confirmPw = "")
            onDone()
        }
    }
}