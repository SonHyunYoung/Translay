package com.example.transrator

class ChangeDetector(
    private val stableRequired: Int = 2,
    private val similarityThreshold: Int = 2   // 이 글자 수 이하 차이는 같은 문장으로 간주
) {
    private var candidate = ""
    private var stableCount = 0
    private var lastConfirmed = ""

    fun onOcr(raw: String): String? {
        val text = normalize(raw)

        if (text.isEmpty()) {
            reset()
            return null
        }

        if (isSimilar(text, lastConfirmed)) return null   // 이미 번역한 것과 거의 같으면 생략

        if (isSimilar(text, candidate)) {
            stableCount++
        } else {
            candidate = text
            stableCount = 1
        }

        if (stableCount >= stableRequired) {
            lastConfirmed = text
            stableCount = 0
            return raw
        }
        return null
    }

    private fun isSimilar(a: String, b: String): Boolean {
        if (a.isEmpty() || b.isEmpty()) return a == b
        // 길이가 많이 다르면 바로 다른 문장으로 판단 (성능 최적화)
        if (kotlin.math.abs(a.length - b.length) > similarityThreshold) return false
        return levenshtein(a, b) <= similarityThreshold
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
                else minOf(dp[i - 1][j - 1], dp[i - 1][j], dp[i][j - 1]) + 1
            }
        }
        return dp[a.length][b.length]
    }

    fun reset() {
        candidate = ""; stableCount = 0; lastConfirmed = ""
    }

    companion object {
        private val WS = Regex("\\s+")
        private val ELLIPSIS = Regex("[.。．]{2,}|…+")

        fun normalize(text: String): String {
            return text
                .replace(ELLIPSIS, "…")
                .replace(WS, "")
                .trim()
        }
    }
}