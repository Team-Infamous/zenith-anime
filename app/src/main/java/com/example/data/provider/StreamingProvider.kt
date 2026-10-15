package com.example.data.provider

import com.example.data.model.Anime
import com.example.data.model.Episode

data class ProviderCapabilities(
    val supportsSub: Boolean = true,
    val supportsDub: Boolean = true,
    val supportsAutoNext: Boolean = true,
    val supportsAutoSkipIntro: Boolean = true,
    val supportsProgressEvents: Boolean = true,
    val supportsStartAt: Boolean = true,
    val requiresEmbed: Boolean = false
)

interface StreamingProvider {
    val id: String
    val displayName: String
    val domain: String
    val capabilities: ProviderCapabilities

    fun supportsAnime(anime: Anime): Boolean
    fun getEmbedUrl(
        anime: Anime,
        episode: Episode,
        language: String,
        autoplay: Boolean,
        autoSkip: Boolean,
        startAtSec: Long
    ): String
}
