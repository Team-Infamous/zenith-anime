package com.example.data.provider

import com.example.data.model.Anime
import com.example.data.model.Episode
import java.net.URLEncoder

class AniKotoProvider : StreamingProvider {
    override val id: String = "anikoto"
    override val displayName: String = "AniKoto (Fast)"
    override val domain: String = "anikototv.to"
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
        val lang = if (language.lowercase() == "dub") "dub" else "sub"
        val slug = anime.title
            .lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")

        val autoPlayParam = if (autoplay) "1" else "0"
        val autoSkipParam = if (autoSkip) "1" else "0"
        return "https://anikototv.to/watch/$slug?ep=${episode.number}&audio=$lang&autoplay=$autoPlayParam&autoskip=$autoSkipParam&start=$startAtSec"
    }
}
