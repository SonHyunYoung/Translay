package com.example.transrator.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.transrator.data.local.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private val PROFILE_NAME_REGEX = Regex("^[a-zA-Z0-9가-힣_.\\s]+$")
const val MAX_PROFILES = 4

data class ProfileUiState(
    val profiles: List<Profile> = emptyList(),
    val loading: Boolean = false,
    val message: String? = null,        // 목록 단계 오류/안내
    val dialogOpen: Boolean = false,
    val editing: Profile? = null,       // null이면 생성 모달
    val nameInput: String = "",
    val langInput: String = "ja",       // 원문 언어 (ja / zh / en)
    val dialogError: String? = null,
    val confirmDelete: Boolean = false,
    val saving: Boolean = false
) {
    val canAdd get() = profiles.size < MAX_PROFILES
}

class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val tokens = TokenManager(app)
    private val repo = ProfileRepository(tokens)
    private val _s = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _s

    init { load() }

    fun load() {
        _s.value = _s.value.copy(loading = true, message = null)
        viewModelScope.launch {
            repo.list()
                .onSuccess { _s.value = _s.value.copy(loading = false, profiles = it) }
                .onFailure { _s.value = _s.value.copy(loading = false, message = it.message) }
        }
    }

    fun select(p: Profile) = tokens.saveProfileId(p.profile_id)

    fun openCreate() {
        _s.value = _s.value.copy(
            dialogOpen = true, editing = null, nameInput = "", langInput = "ja", dialogError = null
        )
    }

    fun openEdit(p: Profile) {
        _s.value = _s.value.copy(
            dialogOpen = true, editing = p, nameInput = p.name,
            langInput = p.sourcelanguage ?: "ja", dialogError = null
        )
    }

    fun closeDialog() {
        _s.value = _s.value.copy(dialogOpen = false, confirmDelete = false, dialogError = null)
    }

    fun onName(v: String) { _s.value = _s.value.copy(nameInput = v, dialogError = null) }
    fun onLang(v: String) { _s.value = _s.value.copy(langInput = v) }

    private fun validate(name: String): String? = when {
        name.length !in 2..25 -> "프로필 이름은 2~25자로 입력해주세요."
        !PROFILE_NAME_REGEX.matches(name) -> "_ . 를 제외한 특수기호는 사용할 수 없어요."
        else -> null
    }

    fun save() {
        val s = _s.value
        val name = s.nameInput.trim()
        validate(name)?.let { _s.value = s.copy(dialogError = it); return }

        _s.value = s.copy(saving = true)
        viewModelScope.launch {
            val e = s.editing
            val r = if (e == null) repo.create(name, s.langInput)
            else repo.update(e.profile_id, name, s.langInput)
            r.onSuccess {
                closeDialog()
                _s.value = _s.value.copy(saving = false)
                load()
            }.onFailure {
                _s.value = _s.value.copy(saving = false, dialogError = it.message)
            }
        }
    }

    fun askDelete() { _s.value = _s.value.copy(confirmDelete = true) }
    fun cancelDelete() { _s.value = _s.value.copy(confirmDelete = false) }

    fun delete() {
        val p = _s.value.editing ?: return
        _s.value = _s.value.copy(saving = true)
        viewModelScope.launch {
            repo.delete(p.profile_id)
                .onSuccess {
                    if (tokens.profileId() == p.profile_id) tokens.saveProfileId(-1)
                    closeDialog()
                    _s.value = _s.value.copy(saving = false)
                    load()
                }
                .onFailure {
                    _s.value = _s.value.copy(saving = false, confirmDelete = false, dialogError = it.message)
                }
        }
    }
}