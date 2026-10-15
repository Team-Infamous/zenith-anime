package com.example.data.provider

import com.example.data.model.Anime
import com.example.data.model.Episode

class StreamWishProvider : StreamingProvider {
    override val id: String = "S5"
    override val displayName: String = "Server S5 (FastCloud CDN)"
    override val domain: String = "megacloud.tv"
    override val capabilities: ProviderCapabilities = ProviderCapabilities(
        supportsSub = true,
        supportsDub = true,
        supportsAutoNext = true,
        supportsAutoSkipIntro = true,
        supportsProgressEvents = true,
        supportsStartAt = true
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
        return "https://megacloud.tv/embed-2/e-1/$targetId?ep=${episode.number}&lang=$lang&autoPlay=${if (autoplay) 1 else 0}"
    }
}
