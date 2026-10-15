package com.example.data.provider

import com.example.data.model.Anime
import com.example.data.model.Episode

class AnimeGGProvider : StreamingProvider {
    override val id: String = "animegg"
    override val displayName: String = "AnimeGG"
    override val domain: String = "animegg.org"
    override val capabilities: ProviderCapabilities = ProviderCapabilities(
        supportsSub = true,
        supportsDub = true,
        supportsAutoNext = true,
        supportsAutoSkipIntro = false,
        supportsProgressEvents = true,
        supportsStartAt = false,
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
        val slug = anime.title
            .lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")

        val suffix = if (language.lowercase() == "dub") "-dub" else ""
        return "https://www.animegg.org/embed/$slug$suffix-episode-${episode.number}"
    }
}
