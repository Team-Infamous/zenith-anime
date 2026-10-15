package com.example.data.provider

import com.example.data.model.Anime
import com.example.data.model.Episode

class ProviderManager {
    private val providers = listOf(
        AniKotoProvider(),
        AniNekoProvider(),
        MegaPlayBuzzProvider(),
        AnimeGGProvider(),
        TryEmbedProvider(),
        MegaPlayProvider(),
        VidSrcProvider(),
        AnimeFlixProvider(),
        StreamWishProvider(),
        AniEmbedProvider(),
        VidukiProvider()
    )

    fun getAvailableProviders(): List<StreamingProvider> = providers

    fun getProvider(id: String): StreamingProvider {
        return providers.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: providers.first()
    }

    /**
     * Picks the best embed provider for the requested [language] ("sub" or "dub").
     * When the user switches to Dub, providers that do not support dub (such as Viduki) are
     * replaced by a dub-capable provider (such as AniEmbed or MegaPlayBuzz) so the embed actually
     * plays dubbed audio.
     */
    fun getProviderForLanguage(id: String, language: String): StreamingProvider {
        val wantDub = language.equals("dub", ignoreCase = true)
        val matched = providers.firstOrNull { it.id.equals(id, ignoreCase = true) }
        if (matched != null && (!wantDub || matched.capabilities.supportsDub)) {
            return matched
        }
        if (wantDub) {
            return providers.firstOrNull { it.id == "aniembed" && it.capabilities.supportsDub }
                ?: providers.firstOrNull { it.capabilities.supportsDub }
                ?: providers.first()
        }
        return matched ?: providers.first()
    }

    fun getFallbackProvider(currentId: String): StreamingProvider {
        return providers.firstOrNull { it.id != currentId } ?: providers.first()
    }
}
