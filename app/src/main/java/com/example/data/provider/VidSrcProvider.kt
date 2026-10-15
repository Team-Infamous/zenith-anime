package com.example.data.provider

import com.example.data.model.Anime
import com.example.data.model.Episode

class VidSrcProvider : StreamingProvider {
    override val id: String = "S1"
    override val displayName: String = "Server S1 (VidLink HD)"
    override val domain: String = "vidlink.pro"
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
        val queryParams = mutableListOf<String>()
        if (autoplay) queryParams.add("autoplay=1")
        if (autoSkip) queryParams.add("autoskip=1")
        queryParams.add("fallback=true")
        val query = if (queryParams.isNotEmpty()) "?" + queryParams.joinToString("&") else ""
        return "https://vidlink.pro/anime/$targetId/${episode.number}/$lang$query"
    }
}
