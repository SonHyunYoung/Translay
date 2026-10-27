package com.example.transrator.glossary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.transrator.data.local.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class DeleteKind { CHARACTER, WORD, TERM }
data class DeleteTarget(val kind: DeleteKind, val id: Int, val label: String)

data class GlossaryUiState(
    val category: Int = 0,                 // 0 캐릭터, 1 고유명사(단어)
    val sortByName: Boolean = false,       // false 등록순, true 가나다순
    val characters: List<GlossaryCharacter> = emptyList(),
    val words: List<Word> = emptyList(),
    val loading: Boolean = false,
    val message: String? = null,
    val dialog: String? = null,            // null 닫힘, "main" 등록, "term" 호칭 등록
    val dialogError: String? = null,
    val deleteTarget: DeleteTarget? = null,
    val detail: GlossaryCharacter? = null,
    val terms: List<Term> = emptyList(),
    val editSource: String = "",
    val editTarget: String = ""
)

class GlossaryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GlossaryRepository(TokenManager(app))
    private val _s = MutableStateFlow(GlossaryUiState())
    val state: StateFlow<GlossaryUiState> = _s

    init { load() }

    private val sort get() = if (_s.value.sortByName) "name" else null
    private fun fail(e: Throwable) { _s.value = _s.value.copy(loading = false, message = e.message) }

    fun load() {
        _s.value = _s.value.copy(loading = true, message = null)
        viewModelScope.launch {
            repo.characters(sort).onSuccess { _s.value = _s.value.copy(characters = it) }.onFailure(::fail)
            repo.words(sort).onSuccess { _s.value = _s.value.copy(words = it, loading = false) }.onFailure(::fail)
        }
    }

    fun setCategory(c: Int) { _s.value = _s.value.copy(category = c) }
    fun setSort(byName: Boolean) { _s.value = _s.value.copy(sortByName = byName); load() }

    fun openDialog(kind: String) { _s.value = _s.value.copy(dialog = kind, dialogError = null) }
    fun closeDialog() { _s.value = _s.value.copy(dialog = null, dialogError = null) }

    /** 캐릭터/단어 등록 (캐릭터는 호칭 동시 등록 가능) */
    fun register(src: String, tgt: String, terms: List<TermRequest>) {
        if (src.isBlank() || tgt.isBlank()) { _s.value = _s.value.copy(dialogError = "원어와 번역을 모두 입력해주세요."); return }
        viewModelScope.launch {
            val r = if (_s.value.category == 0)
                repo.addCharacter(src.trim(), tgt.trim(), terms.filter { it.source.isNotBlank() && it.target.isNotBlank() })
            else repo.addWord(src.trim(), tgt.trim())
            r.onSuccess { closeDialog(); load() }
                .onFailure { _s.value = _s.value.copy(dialogError = it.message) }
        }
    }

    fun registerTerm(src: String, tgt: String) {
        val c = _s.value.detail ?: return
        if (src.isBlank() || tgt.isBlank()) { _s.value = _s.value.copy(dialogError = "원어와 번역을 모두 입력해주세요."); return }
        viewModelScope.launch {
            repo.addTerm(c.character_id, src.trim(), tgt.trim())
                .onSuccess { closeDialog(); loadTerms(c.character_id) }
                .onFailure { _s.value = _s.value.copy(dialogError = it.message) }
        }
    }

    fun askDelete(t: DeleteTarget) { _s.value = _s.value.copy(deleteTarget = t) }
    fun cancelDelete() { _s.value = _s.value.copy(deleteTarget = null) }

    fun confirmDelete() {
        val t = _s.value.deleteTarget ?: return
        _s.value = _s.value.copy(deleteTarget = null)
        viewModelScope.launch {
            val r = when (t.kind) {
                DeleteKind.CHARACTER -> repo.deleteCharacter(t.id)
                DeleteKind.WORD -> repo.deleteWord(t.id)
                DeleteKind.TERM -> repo.deleteTerm(_s.value.detail!!.character_id, t.id)
            }
            r.onSuccess { if (t.kind == DeleteKind.TERM) loadTerms(_s.value.detail!!.character_id) else load() }
                .onFailure { _s.value = _s.value.copy(message = it.message) }
        }
    }

    // ---- 상세 ----
    fun openDetail(c: GlossaryCharacter) {
        _s.value = _s.value.copy(detail = c, editSource = c.source_name, editTarget = c.target_name, terms = emptyList(), message = null)
        loadTerms(c.character_id)
    }
    fun closeDetail() { _s.value = _s.value.copy(detail = null, message = null); load() }
    fun onEditSource(v: String) { _s.value = _s.value.copy(editSource = v) }
    fun onEditTarget(v: String) { _s.value = _s.value.copy(editTarget = v) }

    private fun loadTerms(cid: Int) {
        viewModelScope.launch {
            repo.terms(cid).onSuccess { _s.value = _s.value.copy(terms = it) }.onFailure(::fail)
        }
    }

    fun saveDetail(onDone: () -> Unit) {
        val s = _s.value
        val c = s.detail ?: return
        val src = s.editSource.trim()
        val tgt = s.editTarget.trim()

        if (src.isBlank() || tgt.isBlank()) {
            _s.value = s.copy(message = "이름과 번역을 입력해주세요."); return
        }
        // 변경 없음: 서버 호출 없이 종료
        if (src == c.source_name && tgt == c.target_name) {
            closeDetail(); return
        }
        viewModelScope.launch {
            repo.updateCharacter(c.character_id, src, tgt)
                .onSuccess { onDone(); closeDetail() }
                .onFailure(::fail)
        }
    }
}