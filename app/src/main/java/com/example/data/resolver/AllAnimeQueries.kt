package com.example.data.resolver

/**
 * The GraphQL documents used by the Aniyomi `AllAnime` extension, recovered verbatim from
 * `AllAnimeQueriesKt` in the decompiled extension APK.
 *
 * The extension's `buildQuery` runs `trimIndent()` and then swaps its `%` sign placeholder for
 * `$`, which is exactly what `${'$'}` does here. The `Vaild...` spelling is the server's own
 * schema name and is kept as-is.
 */
internal object AllAnimeQueries {

    val SEARCH = """
        query(
            ${'$'}search: SearchInput
            ${'$'}limit: Int
            ${'$'}page: Int
            ${'$'}translationType: VaildTranslationTypeEnumType
            ${'$'}countryOrigin: VaildCountryOriginEnumType
        ) {
            shows(
                search: ${'$'}search
                limit: ${'$'}limit
                page: ${'$'}page
                translationType: ${'$'}translationType
                countryOrigin: ${'$'}countryOrigin
            ) {
                pageInfo {
                    total
                }
                edges {
                    _id
                    name
                    thumbnail
                    englishName
                    nativeName
                    slugTime
                }
            }
        }
    """.trimIndent()

    val EPISODES = """
        query (${'$'}_id: String!) {
            show(
                _id: ${'$'}_id
            ) {
                _id
                availableEpisodesDetail
            }
        }
    """.trimIndent()

    val STREAMS = """
        query(
            ${'$'}showId: String!,
            ${'$'}translationType: VaildTranslationTypeEnumType!,
            ${'$'}episodeString: String!
        ) {
            episode(
                showId: ${'$'}showId
                translationType: ${'$'}translationType
                episodeString: ${'$'}episodeString
            ) {
                sourceUrls
            }
        }
    """.trimIndent()
}
