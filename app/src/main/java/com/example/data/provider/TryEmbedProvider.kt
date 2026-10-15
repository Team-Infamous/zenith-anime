package com.example.data.provider

import com.example.data.model.Anime
import com.example.data.model.Episode

class TryEmbedProvider : StreamingProvider {
    override val id: String = "S2"
    override val displayName: String = "Server S2 (TryEmbed)"
    override val domain: String = "tryembed.us.cc"
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
        val langPath = if (language.lowercase() == "dub") "dub" else "sub"
        val queryParams = mutableListOf<String>()
        if (autoplay) queryParams.add("autoplay=true")
        if (autoSkip) queryParams.add("autoSkip=true")
        queryParams.add("autoNext=false")
        queryParams.add("skin=silicon")

        val query = if (queryParams.isNotEmpty()) "?" + queryParams.joinToString("&") else ""
        return "https://tryembed.us.cc/embed/anime/$targetId/${episode.number}/$langPath$query"
    }
}
