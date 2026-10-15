package com.example.data.resolver

import android.util.Log
import com.example.data.model.Anime
import com.example.data.model.Episode

/** A selectable entry in the player's server picker. */
data class ServerOption(
    val id: String,
    val displayName: String,
    val description: String,
    val isRemote: Boolean
)

/**
 * Owns the ordered resolver list and implements automatic failover: the preferred server is tried
 * first, then the remaining servers until one returns a playable stream. Every failure is reported
 * so the UI can explain what happened instead of pretending playback succeeded.
 */
class StreamResolverRegistry {

    private val resolvers: List<AnimeStreamResolver> = listOf(
        AnikotoResolver(),
        AllAnimeResolver()
    )

    fun allResolvers(): List<AnimeStreamResolver> = resolvers

    fun resolverFor(serverId: String): AnimeStreamResolver? {
        val normalized = normalizeResolverId(serverId)
        return resolvers.firstOrNull { it.id.equals(normalized, ignoreCase = true) }
    }

    /**
     * Maps legacy/alias setting keys ("zoro", "aniwatch", "hianime", "megacloud", "animekai") to
     * the live resolver IDs so direct resolution always triggers for direct-stream server choices.
     */
    fun normalizeResolverId(serverId: String): String = when (serverId.trim().lowercase()) {
        "mega", "megacloud", "zoro", "aniwatch", "hianime", "anikoto" -> "mega"
        "allanime", "animekai" -> "allanime"
        else -> serverId.trim().lowercase()
    }

    /** True only when the user explicitly chose an embed-only server. */
    fun isExplicitEmbedServer(serverId: String): Boolean {
        val lower = serverId.trim().lowercase()
        if (lower == EMBED_SERVER_ID) return true
        // If it maps to one of our direct resolvers, it is NOT an embed-only server!
        if (resolverFor(lower) != null) return false
        return lower in setOf("aniembed", "viduki", "anineko", "animegg", "s1", "s2", "s3", "s4", "s5", "s6")
    }

    fun serverOptions(): List<ServerOption> {
        val remote = resolvers.map {
            ServerOption(
                id = it.id,
                displayName = if (it.id == "mega") "Mega (Recommended)" else it.displayName,
                description = if (it.id == "mega") "High-speed direct HLS • downloads supported" else "Direct stream • downloads supported",
                isRemote = true
            )
        }
        val others = listOf(
            ServerOption(
                id = "aniembed",
                displayName = "AniEmbed (aniembed.se)",
                description = "Fast embed • AniList synced • Sub & Dub",
                isRemote = false
            ),
            ServerOption(
                id = "viduki",
                displayName = "Viduki (viduki.net)",
                description = "Multi-stream embed • Minimal ads",
                isRemote = false
            ),
            ServerOption(
                id = EMBED_SERVER_ID,
                displayName = "Built-in player (embeds)",
                description = "Fallback player • auto server cycle",
                isRemote = false
            )
        )
        return remote + others
    }

    /**
     * Resolves an episode, honouring the preferred server then falling back to the others.
     * [maxAttempts] bounds how many resolvers are tried so failures stay fast.
     */
    suspend fun resolve(
        anime: Anime,
        episode: Episode,
        language: String,
        preferredServerId: String,
        autoFailover: Boolean = true,
        maxAttempts: Int = 3
    ): ResolveResult {
        val preferred = resolverFor(preferredServerId)
        val ordered = buildList {
            if (preferred != null) add(preferred)
            if (autoFailover || preferred == null) {
                addAll(resolvers.filter { it.id != preferred?.id })
            }
        }.distinctBy { it.id }.take(maxAttempts.coerceAtLeast(1))

        val attempts = mutableListOf<ResolveAttempt>()
        for (resolver in ordered) {
            val output = try {
                resolver.resolve(anime, episode, language)
            } catch (t: Throwable) {
                Log.w(TAG, "${resolver.id} threw ${t.javaClass.simpleName}: ${t.message}")
                AnimeStreamResolver.Output(emptyList(), error = t.javaClass.simpleName)
            }
            attempts += ResolveAttempt(
                resolverId = resolver.id,
                resolverName = resolver.displayName,
                sources = output.sources,
                error = output.error,
                availableAudioLanguages = output.availableAudioLanguages,
                dubUnavailable = output.dubUnavailable
            )
            if (output.sources.isNotEmpty()) break
        }

        return ResolveResult(
            attempts = attempts,
            queryTitle = anime.title,
            requestedLanguage = language
        )
    }

    companion object {
        /** The WebView embed player, kept as the always-available fallback. */
        const val EMBED_SERVER_ID = "embed"

        /** Picks the best source for offline download: requested quality first, then progressive. */
        fun pickForDownload(sources: List<StreamSource>, preferredQuality: String): StreamSource? {
            if (sources.isEmpty()) return null
            val wanted = Regex("(\\d{3,4})").find(preferredQuality)?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (wanted != null) {
                sources.firstOrNull { source ->
                    Regex("(\\d{3,4})").find(source.qualityLabel)?.groupValues?.getOrNull(1)
                        ?.toIntOrNull() == wanted
                }?.let { return it }
            }
            return sources.firstOrNull { it.isProgressive } ?: sources.first()
        }

        private const val TAG = "ZenithResolver"
    }
}
