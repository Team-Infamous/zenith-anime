package com.example.data.provider

import com.example.data.model.Anime
import com.example.data.model.Episode

class MegaPlayProvider : StreamingProvider {
    override val id: String = "S3"
    override val displayName: String = "Server S3 (VidSrc Ultra)"
    override val domain: String = "vidsrc.pm"
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
        return "https://vidsrc.pm/embed/anime/$targetId/${episode.number}"
    }
}
