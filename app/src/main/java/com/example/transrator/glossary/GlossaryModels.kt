package com.example.transrator.glossary

data class Term(val term_id: Int, val source: String, val target: String)
data class GlossaryCharacter(val character_id: Int, val source_name: String, val target_name: String)
data class Word(val word_id: Int, val source: String, val target: String)

data class CharacterListResponse(val message: String?, val character: List<GlossaryCharacter> = emptyList())
data class WordListResponse(val message: String?, val word: List<Word> = emptyList())
data class TermListResponse(val message: String?, val terms: List<Term> = emptyList())

data class TermRequest(val source: String, val target: String)
data class CharacterRequest(val source: String, val target: String, val terms: List<TermRequest>? = null)
data class WordRequest(val source: String, val target: String)