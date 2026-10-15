package com.example.data.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonLabelerTest {

    // ---------------------------------------------------------------- friendlyRelationLabel

    @Test
    fun sequelAndPrequelBecomeSeasonWording() {
        assertEquals("Next season", SeasonLabeler.friendlyRelationLabel("SEQUEL"))
        assertEquals("Previous season", SeasonLabeler.friendlyRelationLabel("PREQUEL"))
        assertEquals("Side story", SeasonLabeler.friendlyRelationLabel("SIDE_STORY"))
        assertEquals("Spin-off", SeasonLabeler.friendlyRelationLabel("SPIN_OFF"))
        assertEquals("Recap", SeasonLabeler.friendlyRelationLabel("SUMMARY"))
        assertEquals("Main story", SeasonLabeler.friendlyRelationLabel("PARENT"))
    }

    @Test
    fun lowercaseAndSpacedRelationsAreNormalised() {
        assertEquals("Next season", SeasonLabeler.friendlyRelationLabel("sequel"))
        assertEquals("Side story", SeasonLabeler.friendlyRelationLabel("side story"))
        assertEquals("Alternate version", SeasonLabeler.friendlyRelationLabel("ALTERNATIVE"))
    }

    @Test
    fun adaptationIsNeverShownAsAdaptation() {
        assertEquals("Same series", SeasonLabeler.friendlyRelationLabel("ADAPTATION"))
        assertEquals("Same series", SeasonLabeler.friendlyRelationLabel("adaptation"))
    }

    @Test
    fun unknownRelationTypesAreTitleCasedWithoutUnderscores() {
        assertEquals("Weird Link", SeasonLabeler.friendlyRelationLabel("WEIRD_LINK"))
        assertEquals("Related", SeasonLabeler.friendlyRelationLabel(""))
    }

    @Test
    fun noHiddenRelationLabelEverLeaksTheWordAdaptation() {
        assertTrue(SeasonLabeler.HIDDEN_RELATION_LABELS.contains("ADAPTATION"))
        SeasonLabeler.HIDDEN_RELATION_LABELS.forEach { raw ->
            val label = SeasonLabeler.friendlyRelationLabel(raw)
            assertFalse("leaked for $raw", label.contains("adaptation", ignoreCase = true))
        }
    }

    // ---------------------------------------------------------------- seasonNumberFromTitle

    @Test
    fun parsesNumericSeasons() {
        assertEquals(2, SeasonLabeler.seasonNumberFromTitle("Attack on Titan Season 2"))
        assertEquals(3, SeasonLabeler.seasonNumberFromTitle("Mushoku Tensei: Jobless Reincarnation Season 3"))
        assertEquals(4, SeasonLabeler.seasonNumberFromTitle("That Time I Got Reincarnated as a Slime Season 4"))
    }

    @Test
    fun parsesOrdinalSeasonsAndParts() {
        assertEquals(3, SeasonLabeler.seasonNumberFromTitle("Bakuman 3rd Season"))
        assertEquals(3, SeasonLabeler.seasonNumberFromTitle("Bleach: Thousand-Year Blood War Part 3"))
        assertEquals(2, SeasonLabeler.seasonNumberFromTitle("Some Anime Part II"))
        assertEquals(2, SeasonLabeler.seasonNumberFromTitle("Some Anime 2nd Cour"))
    }

    @Test
    fun parsesFinalSeasonAsTopRank() {
        assertEquals(99, SeasonLabeler.seasonNumberFromTitle("Attack on Titan: The Final Season"))
        assertEquals(99, SeasonLabeler.seasonNumberFromTitle("Some Anime Final Season"))
    }

    @Test
    fun parsesRomanNumeralTitles() {
        assertEquals(2, SeasonLabeler.seasonNumberFromTitle("Mushoku Tensei II: Jobless Reincarnation"))
        assertEquals(3, SeasonLabeler.seasonNumberFromTitle("Overlord III"))
        assertEquals(4, SeasonLabeler.seasonNumberFromTitle("Overlord IV"))
        assertEquals(1, SeasonLabeler.seasonNumberFromTitle("Some Anime Season I"))
    }

    @Test
    fun avoidsFalsePositivesFromStrayLetters() {
        assertEquals(0, SeasonLabeler.seasonNumberFromTitle("Hunter x Hunter"))
        assertEquals(0, SeasonLabeler.seasonNumberFromTitle("You and I Are Polar Opposites"))
        assertEquals(0, SeasonLabeler.seasonNumberFromTitle("Attack on Titan"))
        assertEquals(0, SeasonLabeler.seasonNumberFromTitle("Cowboy Bebop"))
    }

    @Test
    fun handlesBlankAndUnmarkedTitles() {
        assertEquals(0, SeasonLabeler.seasonNumberFromTitle(""))
        assertEquals(0, SeasonLabeler.seasonNumberFromTitle("   "))
        assertEquals(0, SeasonLabeler.seasonNumberFromTitle("ONE PIECE"))
    }

    // ---------------------------------------------------------------- seasonLabel

    @Test
    fun seasonLabelHonoursUserWordingStyle() {
        assertEquals(
            "Season 2",
            SeasonLabeler.seasonLabel("Attack on Titan Season 2", "SEQUEL", "TV", "Season")
        )
        assertEquals(
            "Part 2",
            SeasonLabeler.seasonLabel("Attack on Titan Season 2", "SEQUEL", "TV", "Part")
        )
        assertEquals(
            "Final Season",
            SeasonLabeler.seasonLabel("Attack on Titan: The Final Season", "SEQUEL", "TV", "Season")
        )
        assertEquals(
            "Final Part",
            SeasonLabeler.seasonLabel("Attack on Titan: The Final Season", "SEQUEL", "TV", "Part")
        )
    }

    @Test
    fun seasonLabelFallsBackToFriendlyRelation() {
        assertEquals(
            "Next season",
            SeasonLabeler.seasonLabel("Attack on Titan", "SEQUEL", "TV", "Season")
        )
        assertEquals(
            "Same series",
            SeasonLabeler.seasonLabel("Attack on Titan", "ADAPTATION", "TV", "Season")
        )
    }

    @Test
    fun specialsFormatsReadAsSpecials() {
        assertEquals("Specials", SeasonLabeler.seasonLabel("Some OVA", "SIDE_STORY", "OVA", "Season"))
        assertEquals("Specials", SeasonLabeler.seasonLabel("Some Special", "SIDE_STORY", "SPECIAL", "Season"))
    }

    @Test
    fun seasonLabelNeverSaysAdaptation() {
        SeasonLabeler.HIDDEN_RELATION_LABELS.forEach { raw ->
            val label = SeasonLabeler.seasonLabel("Some Anime", raw, "TV", "Season")
            assertFalse("leaked for $raw", label.contains("adaptation", ignoreCase = true))
        }
    }

    // ---------------------------------------------------------------- seasonIndexInFranchise

    @Test
    fun franchiseIndexFollowsDetectedSeasonNumbers() {
        val franchise = listOf(
            "Attack on Titan",
            "Attack on Titan Season 2",
            "Attack on Titan Season 3",
            "Attack on Titan: The Final Season"
        )
        assertEquals(2, SeasonLabeler.seasonIndexInFranchise("Attack on Titan Season 2", franchise))
        assertEquals(3, SeasonLabeler.seasonIndexInFranchise("Attack on Titan Season 3", franchise))
        assertEquals(4, SeasonLabeler.seasonIndexInFranchise("Attack on Titan: The Final Season", franchise))
    }

    @Test
    fun franchiseIndexIsZeroWhenUnknownOrEmpty() {
        assertEquals(0, SeasonLabeler.seasonIndexInFranchise("Attack on Titan", emptyList()))
        assertEquals(0, SeasonLabeler.seasonIndexInFranchise("Cowboy Bebop", listOf("Cowboy Bebop")))
    }
}
