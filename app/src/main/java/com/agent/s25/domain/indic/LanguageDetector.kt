package com.agent.s25.domain.indic

enum class DetectedLanguage(val code: String, val bcp47: String) {
    ENGLISH("en", "en-US"),
    GUJARATI("gu", "gu-IN"),
    HINDI("hi", "hi-IN")
}

object LanguageDetector {
    private val GUJARATI_LATIN_KEYWORDS = listOf("kem cho", "su karo", "yaad apavje", "pahochine", "maru", "baki che", "aavse", "mane", "tame", "karo")
    private val HINDI_LATIN_KEYWORDS = listOf("kya haal", "batao", "karo", "kitna", "hai", "mujhe", "pahunch kar", "mera", "aap")

    /**
     * Defaults strictly to ENGLISH unless Gujarati/Hindi native script or phonetics are detected.
     */
    fun detect(text: String): DetectedLanguage {
        val lower = text.lowercase().trim()
        if (lower.isEmpty()) return DetectedLanguage.ENGLISH

        // 1. Check Native Scripts
        var gujaratiChars = 0
        var devanagariChars = 0

        for (ch in text) {
            val code = ch.code
            if (code in 0x0A80..0x0AFF) gujaratiChars++
            else if (code in 0x0900..0x097F) devanagariChars++
        }

        if (gujaratiChars > 0 && gujaratiChars >= devanagariChars) return DetectedLanguage.GUJARATI
        if (devanagariChars > 0) return DetectedLanguage.HINDI

        // 2. Check Romanized Gujlish / Hinglish Keywords
        for (kw in GUJARATI_LATIN_KEYWORDS) {
            if (lower.contains(kw)) return DetectedLanguage.GUJARATI
        }

        for (kw in HINDI_LATIN_KEYWORDS) {
            if (lower.contains(kw)) return DetectedLanguage.HINDI
        }

        // Default primary language: English
        return DetectedLanguage.ENGLISH
    }
}
