package com.example.data.resolver

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Minimal, dependency-free HLS playlist reader.
 *
 * A master playlist lists variants (`#EXT-X-STREAM-INF` followed by the variant URI); a media
 * playlist lists segments. Resolvers use [variants] to expose per-quality download choices and
 * [segments] to estimate size and to download without Media3 when needed.
 */
object HlsParser {

    data class Variant(
        val url: String,
        val resolution: String,
        val bandwidth: Int,
        val codecs: String = ""
    ) {
        /** Numeric height for sorting, e.g. "1080p" -> 1080 or "1920x1080" -> 1080. */
        val height: Int
            get() = Regex("(?:x|\\b)(\\d{3,4})p?\\b").find(resolution)?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: Regex("(\\d{3,4})").find(resolution)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0

        val label: String get() = if (height > 0) "${height}p" else "Auto"
    }

    fun isMaster(playlist: String): Boolean = playlist.contains("#EXT-X-STREAM-INF")

    /** Resolves a possibly relative URI against the playlist URL. */
    fun resolveUri(baseUrl: String, uri: String): String {
        val trimmed = uri.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        return try {
            val parsed = baseUrl.toHttpUrlOrNull() ?: return trimmed
            parsed.resolve(trimmed)?.toString() ?: trimmed
        } catch (_: Throwable) {
            trimmed
        }
    }

    fun variants(playlistUrl: String, playlist: String): List<Variant> {
        val lines = playlist.lines()
        val out = mutableListOf<Variant>()
        for (index in lines.indices) {
            val line = lines[index].trim()
            if (!line.startsWith("#EXT-X-STREAM-INF")) continue
            val uriLine = lines.drop(index + 1).firstOrNull { it.isNotBlank() && !it.trim().startsWith("#") }
            if (uriLine == null) continue
            val resolution = Regex("RESOLUTION=\\d+x(\\d+)").find(line)?.groupValues?.getOrNull(1)
            val bandwidth = Regex("BANDWIDTH=(\\d+)").find(line)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            val codecs = Regex("CODECS=\"([^\"]+)\"").find(line)?.groupValues?.getOrNull(1).orEmpty()
            out += Variant(
                url = resolveUri(playlistUrl, uriLine),
                resolution = resolution?.let { "${it}p" } ?: "",
                bandwidth = bandwidth,
                codecs = codecs
            )
        }
        return out.sortedWith(compareByDescending<Variant> { it.height }.thenByDescending { it.bandwidth })
    }

    fun segments(playlistUrl: String, playlist: String): List<String> {
        return playlist.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { resolveUri(playlistUrl, it) }
    }

    /**
     * Parses `#EXT-X-MEDIA:TYPE=AUDIO` rendition tags from an HLS master playlist so multi-audio
     * streams (e.g. Japanese + English Dub in one master manifest) can be discovered and selected.
     */
    fun audioTracks(playlistUrl: String, playlist: String): List<AudioTrackOption> {
        val out = mutableListOf<AudioTrackOption>()
        for ((idx, rawLine) in playlist.lines().withIndex()) {
            val line = rawLine.trim()
            if (!line.startsWith("#EXT-X-MEDIA", ignoreCase = true)) continue
            val type = attrValue(line, "TYPE") ?: continue
            if (!type.equals("AUDIO", ignoreCase = true)) continue

            val name = attrValue(line, "NAME") ?: "Audio ${out.size + 1}"
            val lang = attrValue(line, "LANGUAGE") ?: name
            val groupId = attrValue(line, "GROUP-ID") ?: "audio"
            val defaultAttr = attrValue(line, "DEFAULT")
            val uriAttr = attrValue(line, "URI")?.takeIf { it.isNotBlank() }?.let { resolveUri(playlistUrl, it) }

            val combinedLower = "$name $lang".lowercase()
            val isDub = combinedLower.contains("eng") ||
                combinedLower.contains("dub") ||
                combinedLower.contains("en-") ||
                lang.equals("en", ignoreCase = true)

            out += AudioTrackOption(
                id = "$groupId|$lang|$idx",
                label = name,
                language = lang,
                uri = uriAttr,
                isDub = isDub,
                isDefault = defaultAttr.equals("YES", ignoreCase = true)
            )
        }
        return out
    }

    private fun attrValue(line: String, key: String): String? {
        Regex("$key=\"([^\"]*)\"", RegexOption.IGNORE_CASE).find(line)?.groupValues?.getOrNull(1)?.let { return it }
        return Regex("$key=([^,\\s]+)", RegexOption.IGNORE_CASE).find(line)?.groupValues?.getOrNull(1)
    }

    /** Picks the variant closest to (and not above) the preferred height; falls back to best. */
    fun pick(variants: List<Variant>, preferredLabel: String): Variant? {
        if (variants.isEmpty()) return null
        val preferred = Regex("(\\d{3,4})").find(preferredLabel)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (preferred == null || preferredLabel.equals("Auto", true)) return variants.first()
        return variants.firstOrNull { it.height in 1..preferred } ?: variants.last()
    }
}
