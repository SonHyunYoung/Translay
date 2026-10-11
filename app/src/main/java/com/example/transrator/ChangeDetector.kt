package com.example.transrator

class ChangeDetector(
    private val stableRequired: Int = 2   // 연속 몇 회 같아야 완성으로 볼지
) {
    private var candidate = ""        // 안정화 대기 중인 텍스트
    private var stableCount = 0
    private var lastConfirmed = ""    // 마지막으로 확정(번역 요청)한 텍스트

    /**
     * OCR 원문을 받아서, 번역해야 할 때만 원문을 돌려준다.
     * 번역이 필요 없으면 null.
     */
    fun onOcr(raw: String): String? {
        val text = normalize(raw)

        // 대사창이 비었음 → 다음 대사를 위해 초기화
        if (text.isEmpty()) {
            reset()
            return null
        }

        // 이미 번역 요청한 문장이면 생략
        if (text == lastConfirmed) return null

        // 안정화 판정
        if (text == candidate) {
            stableCount++
        } else {
            candidate = text        // 아직 바뀌는 중 (타이핑)
            stableCount = 1
        }

        if (stableCount >= stableRequired) {
            lastConfirmed = text
            stableCount = 0
            return raw              // 번역은 정규화 전 원문으로
        }
        return null
    }

    private fun reset() {
        candidate = ""
        stableCount = 0
        lastConfirmed = ""
    }

    companion object {
        // 일본어는 공백이 의미 없으므로 모두 제거해 비교 안정성을 높임
        private val WS = Regex("\\s+")
        fun normalize(text: String): String = text.replace(WS, "").trim()
    }
}