package com.example.data.provider

import com.example.data.model.Anime
import com.example.data.model.Episode

class AniNekoProvider : StreamingProvider {
    override val id: String = "anineko"
    override val displayName: String = "AniNeko (HD)"
    override val domain: String = "anineko.to"
    override val capabilities: ProviderCapabilities = ProviderCapabilities(
        supportsSub = true,
        supportsDub = true,
        supportsAutoNext = true,
        supportsAutoSkipIntro = true,
        supportsProgressEvents = true,
        supportsStartAt = true,
        requiresEmbed = true
    )

    override fun supportsAnime(anime: Anime): Boolean = true

    override fun getEmbedUrl(
        anime: Anime,
        episode: Episode,
        language: String,
        autoplay: Boolean,
        autoSkip: Boolean,
        startAtSec: Long
    ): String {
        val isDub = language.lowercase() == "dub"
        val slug = anime.title
            .lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")

        val subParam = if (isDub) "0" else "1"
        return "https://anineko.to/embed/$slug/${episode.number}?sub=$subParam&autoplay=${if (autoplay) 1 else 0}"
    }
}
