package com.example.data.provider

import com.example.data.model.Anime
import com.example.data.model.Episode

class AnimeFlixProvider : StreamingProvider {
    override val id: String = "S4"
    override val displayName: String = "Server S4 (Kaido Stream)"
    override val domain: String = "kaido.to"
    override val capabilities: ProviderCapabilities = ProviderCapabilities(
        supportsSub = true,
        supportsDub = true,
        supportsAutoNext = true,
        supportsAutoSkipIntro = false,
        supportsProgressEvents = true,
        supportsStartAt = false
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
        val lang = if (language.lowercase() == "dub") "dub" else "sub"
        return "https://kaido.to/embed/$targetId?ep=${episode.number}&type=$lang"
    }
}
