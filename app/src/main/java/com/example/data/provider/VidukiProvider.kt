package com.example.data.provider

import com.example.data.model.Anime
import com.example.data.model.Episode

/**
 * Provider for Viduki (https://www.viduki.net/#api)
 * Streaming API embed player with multiple server fallbacks and custom accent theming.
 */
class VidukiProvider : StreamingProvider {
    override val id: String = "viduki"
    override val displayName: String = "Server Viduki (viduki.net)"
    override val domain: String = "viduki.net"
    override val capabilities: ProviderCapabilities = ProviderCapabilities(
        supportsSub = true,
        supportsDub = false,
        supportsAutoNext = true,
        supportsAutoSkipIntro = false,
        supportsProgressEvents = true,
        supportsStartAt = false,
        requiresEmbed = true
    )

    override fun supportsAnime(anime: Anime): Boolean = anime.id.isNotEmpty()

    override fun getEmbedUrl(
        anime: Anime,
        episode: Episode,
        language: String,
        autoplay: Boolean,
        autoSkip: Boolean,
        startAtSec: Long
    ): String {
        val targetId = if (anime.malId.isNotEmpty() && anime.malId != "0") anime.malId else anime.id
        val isMovie = anime.type.equals("Movie", ignoreCase = true)
        return if (isMovie) {
            "https://viduki.net/1/movie/$targetId?color=00dc82"
        } else {
            "https://viduki.net/1/tv/$targetId/1/${episode.number}?color=00dc82"
        }
    }
}
