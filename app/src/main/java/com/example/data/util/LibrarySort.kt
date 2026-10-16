package com.example.data.util

import com.example.data.model.Anime

/**
 * The sort keys offered for My List and every other anime grid, plus the single place that knows how
 * to apply them.
 *
 * The saved lists used to render in whatever order Room happened to return, so items appeared to
 * shuffle between visits. One shared comparator means every grid is deterministic, and a title
 * tie-break keeps equal values (same rating, same year) stable instead of jumping around.
 */
object LibrarySort {
    const val RECENTLY_ADDED = "Recently Added"
    const val TITLE_ASC = "Title A-Z"
    const val TITLE_DESC = "Title Z-A"
    const val RATING = "Highest Rated"
    const val YEAR = "Newest Year"
    const val EPISODES = "Most Episodes"

    val ALL = listOf(RECENTLY_ADDED, TITLE_ASC, TITLE_DESC, RATING, YEAR, EPISODES)

    fun apply(anime: List<Anime>, order: String, titlePreference: String = "English"): List<Anime> = when (order) {
        TITLE_ASC -> anime.sortedWith(byTitle(titlePreference))
        TITLE_DESC -> anime.sortedWith(byTitle(titlePreference).reversed())
        RATING -> anime.sortedWith(
            compareByDescending<Anime> { it.rating }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayTitle(titlePreference) }
        )
        YEAR -> anime.sortedWith(
            compareByDescending<Anime> { it.year }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayTitle(titlePreference) }
        )
        EPISODES -> anime.sortedWith(
            compareByDescending<Anime> { it.availableEpisodes }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayTitle(titlePreference) }
        )
        else -> anime.sortedWith(
            compareByDescending<Anime> { it.lastUpdated }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayTitle(titlePreference) }
        )
    }

    private fun byTitle(titlePreference: String): Comparator<Anime> =
        compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayTitle(titlePreference) }
}
