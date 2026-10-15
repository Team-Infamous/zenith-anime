package com.example.data.util

/**
 * Turns raw AniList relation metadata and sequel titles into consumer-friendly wording.
 *
 * Users should read "Season 2" or "Part 3" next to a related entry. They should never be shown the
 * raw AniList relation types ("ADAPTATION", "SEQUEL", "SIDE_STORY") and never the word "Adaptation".
 *
 * Pure Kotlin, no Android or third-party dependencies, so it is directly unit testable.
 */
object SeasonLabeler {

    /** Raw AniList relation types that must never reach the user interface verbatim. */
    val HIDDEN_RELATION_LABELS: Set<String> = setOf(
        "ADAPTATION",
        "SEQUEL",
        "PREQUEL",
        "PARENT",
        "SIDE_STORY",
        "SIDE STORY",
        "SPIN_OFF",
        "SPIN OFF",
        "ALTERNATIVE",
        "SUMMARY",
        "CHARACTER",
        "SOURCE",
        "COMPILATION",
        "CONTAINS",
        "OTHER"
    )

    private const val FINAL_SEASON = 99

    private val RELATION_LABELS: Map<String, String> = mapOf(
        "ADAPTATION" to "Same series",
        "SEQUEL" to "Next season",
        "PREQUEL" to "Previous season",
        "PARENT" to "Main story",
        "SIDE_STORY" to "Side story",
        "SIDE STORY" to "Side story",
        "SPIN_OFF" to "Spin-off",
        "SPIN OFF" to "Spin-off",
        "ALTERNATIVE" to "Alternate version",
        "SUMMARY" to "Recap",
        "CHARACTER" to "Character",
        "SOURCE" to "Source material",
        "COMPILATION" to "Compilation",
        "CONTAINS" to "Contains",
        "OTHER" to "Related"
    )

    private val FINAL_SEASON_REGEX = Regex("\\bfinal\\s+(?:season|part|cour)\\b")
    private val NTH_SEASON_REGEX = Regex("\\b(\\d{1,2})(?:st|nd|rd|th)\\s+season\\b")
    private val NTH_COUR_REGEX = Regex("\\b(\\d{1,2})(?:st|nd|rd|th)\\s+cour\\b")
    private val SEASON_NUMBER_REGEX = Regex("\\bseason\\s+(\\d{1,2})\\b")
    private val SEASON_ROMAN_REGEX = Regex("\\bseason\\s+([ivxlcdm]{1,5})\\b")
    private val PART_REGEX = Regex("\\bpart\\s+([0-9]{1,2}|[ivxlcdm]{1,5})\\b")
    private val COUR_NUMBER_REGEX = Regex("\\bcour\\s+(\\d{1,2})\\b")
    private val TOKEN_SPLIT_REGEX = Regex("[^a-z0-9]+")

    /**
     * Turns a raw relation type into consumer wording. The result never contains the word
     * "Adaptation", whatever the input.
     */
    fun friendlyRelationLabel(relationType: String): String {
        val normalised = relationType.trim().uppercase().replace("-", "_")
        val mapped = RELATION_LABELS[normalised]
            ?: RELATION_LABELS[normalised.replace("_", " ")]
            ?: titleCase(relationType)
        return if (mapped.contains("adaptation", ignoreCase = true)) "Same series" else mapped
    }

    /**
     * Detects the season/part number encoded in a title.
     *
     * "Attack on Titan Season 2" -> 2, "Bleach: Thousand-Year Blood War Part 3" -> 3,
     * "Mushoku Tensei II" -> 2, "Overlord IV" -> 4, "... 3rd Season" -> 3,
     * "... Final Season" -> 99, "Hunter x Hunter" -> 0 (no false positives from stray letters).
     */
    fun seasonNumberFromTitle(title: String): Int {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return 0
        val lower = trimmed.lowercase()

        if (FINAL_SEASON_REGEX.containsMatchIn(lower)) return FINAL_SEASON

        NTH_SEASON_REGEX.find(lower)?.let { match ->
            val number = match.groupValues[1].toIntOrNull() ?: 0
            if (number > 0) return number
        }

        SEASON_NUMBER_REGEX.find(lower)?.let { match ->
            val number = match.groupValues[1].toIntOrNull() ?: 0
            if (number > 0) return number
        }

        SEASON_ROMAN_REGEX.find(lower)?.let { match ->
            val number = romanToInt(match.groupValues[1])
            if (number > 0) return number
        }

        PART_REGEX.find(lower)?.let { match ->
            val raw = match.groupValues[1]
            val number = raw.toIntOrNull() ?: romanToInt(raw)
            if (number > 0) return number
        }

        NTH_COUR_REGEX.find(lower)?.let { match ->
            val number = match.groupValues[1].toIntOrNull() ?: 0
            if (number > 0) return number
        }

        COUR_NUMBER_REGEX.find(lower)?.let { match ->
            val number = match.groupValues[1].toIntOrNull() ?: 0
            if (number > 0) return number
        }

        // Trailing roman numerals such as "Overlord IV" or "Mushoku Tensei II". Only multi-character
        // tokens qualify so that ordinary words ("Titan", "Family", the pronoun "I") never match.
        for (token in lower.split(TOKEN_SPLIT_REGEX)) {
            if (token.length < 2) continue
            val number = romanToInt(token)
            if (number in 2..12) return number
        }

        return 0
    }

    /**
     * Label shown next to a related entry, honouring the user's "Season" or "Part" wording style.
     */
    fun seasonLabel(title: String, relationType: String, format: String, style: String): String {
        val upperFormat = format.trim().uppercase()
        if (upperFormat == "SPECIAL" || upperFormat == "OVA" || upperFormat == "ONA" || upperFormat == "MUSIC") {
            return "Specials"
        }

        val usePartWording = style.trim().equals("Part", ignoreCase = true)
        val number = seasonNumberFromTitle(title)
        val label = when {
            number == FINAL_SEASON -> if (usePartWording) "Final Part" else "Final Season"
            number > 0 -> if (usePartWording) "Part $number" else "Season $number"
            else -> friendlyRelationLabel(relationType)
        }
        return if (label.contains("adaptation", ignoreCase = true)) "Same series" else label
    }

    /**
     * 1-based rank of [title] inside the franchise, ordered by detected season number.
     * Returns 0 when the season number cannot be determined.
     */
    fun seasonIndexInFranchise(title: String, franchiseTitles: List<String>): Int {
        if (franchiseTitles.isEmpty()) return 0
        val mine = seasonNumberFromTitle(title)
        if (mine <= 0) return 0
        val earlier = franchiseTitles.count { candidate ->
            val other = seasonNumberFromTitle(candidate)
            // A title without a season marker is the first entry of the franchise, so it always
            // sorts before anything that does carry a number.
            if (other <= 0) true else other < mine
        }
        return earlier + 1
    }

    private fun titleCase(raw: String): String {
        val cleaned = raw.trim().replace("_", " ").replace("-", " ").trim()
        if (cleaned.isEmpty()) return "Related"
        return cleaned.split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercaseChar() }
            }
    }

    /** Strict roman numeral parsing: returns 0 when the token is not a valid roman numeral 1..3999. */
    private fun romanToInt(raw: String): Int {
        val token = raw.trim().lowercase()
        if (token.isEmpty()) return 0
        if (!token.all { it in "ivxlcdm" }) return 0

        val values = mapOf(
            'i' to 1, 'v' to 5, 'x' to 10, 'l' to 50, 'c' to 100, 'd' to 500, 'm' to 1000
        )
        var total = 0
        var previous = 0
        for (index in token.indices.reversed()) {
            val value = values[token[index]] ?: return 0
            total += if (value < previous) -value else value
            previous = maxOf(previous, value)
        }
        // Reject non-canonical sequences such as "iiii" or "viv".
        return if (toRoman(total) == token) total else 0
    }

    private fun toRoman(number: Int): String {
        if (number <= 0) return ""
        val table = listOf(
            1000 to "m", 900 to "cm", 500 to "d", 400 to "cd",
            100 to "c", 90 to "xc", 50 to "l", 40 to "xl",
            10 to "x", 9 to "ix", 5 to "v", 4 to "iv", 1 to "i"
        )
        var remaining = number
        val builder = StringBuilder()
        for ((value, numeral) in table) {
            while (remaining >= value) {
                builder.append(numeral)
                remaining -= value
            }
        }
        return builder.toString()
    }
}
