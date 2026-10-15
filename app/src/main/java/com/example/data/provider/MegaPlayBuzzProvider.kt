package com.example.data.provider

import com.example.data.model.Anime
import com.example.data.model.Episode

class MegaPlayBuzzProvider : StreamingProvider {
    override val id: String = "S6"
    override val displayName: String = "MegaPlay (Main)"
    override val domain: String = "megaplay.buzz"
    override val capabilities: ProviderCapabilities = ProviderCapabilities(
        supportsSub = true,
        supportsDub = true,
        supportsAutoNext = true,
        supportsAutoSkipIntro = false,
        supportsProgressEvents = true,
        supportsStartAt = false,
        requiresEmbed = true
    )

    override fun supportsAnime(anime: Anime): Boolean =
        anime.malId.isNotEmpty() && anime.malId != "0"

    override fun getEmbedUrl(
        anime: Anime,
        episode: Episode,
        language: String,
        autoplay: Boolean,
        autoSkip: Boolean,
        startAtSec: Long
    ): String {
        val lang = if (language.lowercase() == "dub") "dub" else "sub"
        val malId = anime.malId
        return if (malId.isNotEmpty() && malId != "0") {
            "https://megaplay.buzz/stream/mal/$malId/${episode.number}/$lang"
        } else {
            "https://megaplay.buzz/stream/ani/${anime.id}/${episode.number}/$lang"
        }
    }
}
