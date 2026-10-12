package com.example.util

object ArabicSearchNormalizer {

    // Arabic diacritics Unicode range (Tashkeel)
    private val TASHKEEL_REGEX = Regex("[\u064B-\u065F\u0670\u06D6-\u06ED]")
    // Arabic Tatweel (Kashida)
    private const val TATWEEL = '\u0640'

    /**
     * Normalizes Arabic text for flexible, fuzzy searching:
     * 1. Strips all diacritics (tashkeel).
     * 2. Strips tatweel (kashida).
     * 3. Unifies Alif forms (أ, إ, آ, ٱ -> ا).
     * 4. Unifies Yaa and Alif Maqsura (ي, ى -> ي).
     * 5. Unifies Taa Marbuta and Haa (ة -> ه).
     * 6. Collapses and trims whitespaces.
     */
    fun normalize(text: String?): String {
        if (text.isNullOrBlank()) return ""

        var result = text
            // Strip Tashkeel
            .replace(TASHKEEL_REGEX, "")
            // Strip Tatweel
            .replace(TATWEEL.toString(), "")
            // Unify Alif
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            // Unify Yaa and Alif Maqsura
            .replace('ى', 'ي')
            // Unify Taa Marbuta to Haa
            .replace('ة', 'ه')
            // Normalize spaces
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()

        return result
    }

    /**
     * Calculates a matching score between a search query and a doctor name.
     * Higher score = better match:
     * - Exact match: 100
     * - Exact normalized match: 90
     * - Name starts with query: 80
     * - All query words match in order: 70
     * - All query words present in name: 60
     * - Partial substring match: 50
     * - No match: 0
     */
    fun calculateMatchScore(query: String, targetName: String): Int {
        val normQuery = normalize(query)
        val normTarget = normalize(targetName)

        if (normQuery.isEmpty() || normTarget.isEmpty()) return 0
        if (query.trim() == targetName.trim()) return 100
        if (normQuery == normTarget) return 95
        if (normTarget.startsWith(normQuery)) return 85

        val queryWords = normQuery.split(" ").filter { it.isNotBlank() }
        val targetWords = normTarget.split(" ").filter { it.isNotBlank() }

        if (queryWords.isEmpty()) return 0

        // Check if all query words are in target
        val allWordsPresent = queryWords.all { qWord ->
            targetWords.any { tWord -> tWord.contains(qWord) }
        }

        if (allWordsPresent) {
            // Check if exact full words match
            val exactWordsMatch = queryWords.all { qWord ->
                targetWords.contains(qWord)
            }
            if (exactWordsMatch) return 75
            return 65
        }

        if (normTarget.contains(normQuery)) return 55

        // If query has multiple words and at least one matches
        val someWordsPresent = queryWords.any { qWord ->
            qWord.length >= 3 && targetWords.any { tWord -> tWord.contains(qWord) }
        }
        if (someWordsPresent) return 40

        return 0
    }

    /**
     * Checks if target matches query based on normalized search rules.
     */
    fun matches(query: String, target: String?): Boolean {
        if (target.isNullOrBlank()) return false
        val normQuery = normalize(query)
        if (normQuery.isBlank()) return true
        val normTarget = normalize(target)
        return normTarget.contains(normQuery)
    }
}
