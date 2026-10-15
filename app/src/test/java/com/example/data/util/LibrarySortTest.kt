package com.example.data.util

import com.example.data.model.Anime
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * My List used to render whatever order Room returned, so saved grids reshuffled themselves between
 * visits. These tests pin down every offered order, including the title tie-break that keeps equal
 * ratings, years and episode counts from flipping around.
 */
class LibrarySortTest {

    private fun anime(
        id: String,
        title: String,
        rating: Double = 8.0,
        year: Int = 2020,
        episodes: Int = 12,
        addedAt: Long = 0L
    ) = Anime(
        id = id,
        title = title,
        coverImage = "",
        bannerImage = "",
        synopsis = "",
        rating = rating,
        year = year,
        totalEpisodes = episodes,
        lastUpdated = addedAt
    )

    private val naruto = anime("1", "naruto", rating = 9.0, year = 2002, episodes = 220, addedAt = 300L)
    private val bleach = anime("2", "Bleach", rating = 8.0, year = 2004, episodes = 366, addedAt = 100L)
    private val akira = anime("3", "AKIRA", rating = 8.5, year = 1988, episodes = 1, addedAt = 200L)

    private val unsorted = listOf(naruto, bleach, akira)

    private fun titles(order: String) = LibrarySort.apply(unsorted, order).map { it.title }

    @Test
    fun recentlyAddedIsTheDefaultAndPutsTheNewestFirst() {
        assertEquals(listOf("naruto", "AKIRA", "Bleach"), titles(LibrarySort.RECENTLY_ADDED))
    }

    @Test
    fun unknownOrderFallsBackToRecentlyAddedInsteadOfThrowing() {
        assertEquals(titles(LibrarySort.RECENTLY_ADDED), titles("Something Removed In A Future Build"))
    }

    @Test
    fun titleOrderingIgnoresCase() {
        // "AKIRA" only sorts first because the comparison is case-insensitive.
        assertEquals(listOf("AKIRA", "Bleach", "naruto"), titles(LibrarySort.TITLE_ASC))
    }

    @Test
    fun descendingTitleIsTheExactReverseOfAscending() {
        assertEquals(titles(LibrarySort.TITLE_ASC).reversed(), titles(LibrarySort.TITLE_DESC))
    }

    @Test
    fun ratingPutsTheHighestFirst() {
        assertEquals(listOf("naruto", "AKIRA", "Bleach"), titles(LibrarySort.RATING))
    }

    @Test
    fun yearPutsTheNewestFirst() {
        assertEquals(listOf("Bleach", "naruto", "AKIRA"), titles(LibrarySort.YEAR))
    }

    @Test
    fun episodesPutsTheLongestRunningFirst() {
        assertEquals(listOf("Bleach", "naruto", "AKIRA"), titles(LibrarySort.EPISODES))
    }

    @Test
    fun equalValuesBreakOnTitleSoTheOrderStaysStable() {
        val first = anime("a", "Zeta", rating = 8.0, year = 2010, episodes = 12)
        val second = anime("b", "Alpha", rating = 8.0, year = 2010, episodes = 12)
        val third = anime("c", "Mid", rating = 8.0, year = 2010, episodes = 12)

        listOf(LibrarySort.RATING, LibrarySort.YEAR, LibrarySort.EPISODES).forEach { order ->
            assertEquals(
                "$order should tie-break on title",
                listOf("Alpha", "Mid", "Zeta"),
                LibrarySort.apply(listOf(first, second, third), order).map { it.title }
            )
        }
    }

    @Test
    fun everyOfferedOrderKeepsEveryItem() {
        LibrarySort.ALL.forEach { order ->
            val sorted = LibrarySort.apply(unsorted, order)
            assertEquals("$order changed the number of items", unsorted.size, sorted.size)
            assertEquals("$order dropped or duplicated an item", unsorted.map { it.id }.toSet(), sorted.map { it.id }.toSet())
        }
    }

    @Test
    fun emptyInputIsHandledForEveryOrder() {
        LibrarySort.ALL.forEach { order ->
            assertEquals(emptyList<Anime>(), LibrarySort.apply(emptyList(), order))
        }
    }
}
