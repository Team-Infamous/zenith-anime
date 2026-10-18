package com.example.data.resolver

import com.example.data.model.Anime
import com.example.data.provider.ProviderManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Local deterministic tests for resolver decoding, URL normalization, provider cycling,
 * and anime model title/episode calculation rules.
 */
class StreamResolverLocalTest {

    private val allAnimeResolver = AllAnimeResolver()

    @Test
    fun allAnimeDecodesXorHexUtf8AndNormalizesClockUrls() {
        val rawPath = "/apivtwo/clock?id=abc123"
        val encodedHex = rawPath.toByteArray(Charsets.UTF_8)
            .joinToString("") { b -> "%02x".format((b.toInt() and 0xFF) xor 0x38) }
        val decoded = allAnimeResolver.decodeIfNeeded("--$encodedHex")
        assertEquals("https://api.allanime.day/apivtwo/clock.json?id=abc123", decoded)
    }

    @Test
    fun providerManagerCyclesThroughAllFallbackProviders() {
        val manager = ProviderManager()
        val providers = manager.getAvailableProviders()
        var current = providers.first().id
        val visited = mutableSetOf(current)
        repeat(providers.size - 1) {
            current = manager.getFallbackProvider(current).id
            visited += current
        }
        assertEquals(providers.size, visited.size)
        assertEquals(providers.first().id, manager.getFallbackProvider(providers.last().id).id)
        assertNotEquals(providers[0].id, manager.getFallbackProvider(providers[0].id).id)
    }

    @Test
    fun animeDisplayTitleAndAvailableEpisodesFollowSpec() {
        val show = Anime(
            id = "10",
            title = "Default Title",
            englishTitle = "English Name",
            romajiTitle = "Romaji Name",
            nativeTitle = "Native Name",
            coverImage = "",
            bannerImage = "",
            synopsis = "",
            status = "RELEASING",
            totalEpisodes = 24,
            nextAiringEpisode = 8
        )
        assertEquals("Native Name", show.displayTitle("Native"))
        assertEquals("Romaji Name", show.displayTitle("Romaji"))
        assertEquals("Romaji Name", show.displayTitle("Japanese"))
        assertEquals("English Name", show.displayTitle("English"))
        assertEquals(7, show.availableEpisodes)

        val upcoming = show.copy(status = "NOT_YET_RELEASED", nextAiringEpisode = 1)
        assertEquals(0, upcoming.availableEpisodes)

        val airingWithoutNext = show.copy(nextAiringEpisode = null, totalEpisodes = 12)
        assertEquals(12, airingWithoutNext.availableEpisodes)
    }
}
