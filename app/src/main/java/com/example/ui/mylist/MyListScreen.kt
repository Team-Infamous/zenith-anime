package com.example.ui.mylist

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Anime
import com.example.data.repository.AnimeRepository
import com.example.data.util.LibrarySort
import com.example.ui.components.AnimeCard
import com.example.ui.components.AnimeQuickActionDialog
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.LocalThemeAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun MyListScreen(
    repository: AnimeRepository,
    onAnimeClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeAccent = LocalThemeAccent.current
    val favorites by repository.favorites.collectAsState(initial = emptyList())
    val watchlist by repository.watchlist.collectAsState(initial = emptyList())
    val completed by repository.completed.collectAsState(initial = emptyList())

    val settings by repository.settings.collectAsState(
        initial = repository.settingsManager.getSettings()
    )

    val tabs = listOf("All Saved", "Favorites", "Watchlist", "Completed")
    var selectedTabIndex by remember { mutableStateOf(0) }
    var selectedQuickActionAnime by remember { mutableStateOf<Anime?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    // De-duplicate first and sort second: ordering before distinct() is what made the grid look
    // like it reshuffled itself as the saved lists changed.
    val displayedAnime: List<Anime> = LibrarySort.apply(
        when (selectedTabIndex) {
            0 -> (favorites + watchlist + completed).distinctBy { it.id }
            1 -> favorites
            2 -> watchlist
            3 -> completed
            else -> emptyList()
        },
        settings.librarySortOrder
    )

    val gridState = rememberLazyGridState()

    // A new order makes the grid keep its previously visible card as an anchor, which drops you into
    // the middle of the list and makes it look like the sort did nothing. Changing tab or order
    // always starts from the top so the new order is actually visible.
    LaunchedEffect(selectedTabIndex, settings.librarySortOrder) {
        gridState.scrollToItem(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_zenith_logo),
                contentDescription = "Zenith Anime Logo",
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "My List",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.testTag("mylist_sort_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort by ${settings.librarySortOrder}",
                        tint = if (showSortMenu) themeAccent else Color.White
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    LibrarySort.ALL.forEach { order ->
                        val isActive = order == settings.librarySortOrder
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = order,
                                    color = if (isActive) themeAccent else TextPrimary,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon = {
                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = themeAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            onClick = {
                                showSortMenu = false
                                repository.settingsManager.update {
                                    it.copy(librarySortOrder = order)
                                }
                            },
                            modifier = Modifier.testTag(
                                "mylist_sort_" + order.lowercase()
                                    .replace(Regex("[^a-z0-9]+"), "_")
                                    .trim('_')
                            )
                        )
                    }
                }
            }

            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.testTag("mylist_search_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White
                )
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = DarkBackground,
            contentColor = TextPrimary,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = themeAccent,
                    height = 3.dp
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == index) themeAccent else TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (displayedAnime.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No saved anime in ${tabs[selectedTabIndex]}",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Browse or search anime and tap '+ My List' or hold down on any card to add titles.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onSearchClick,
                        colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = DarkBackground),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Search Anime", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedAnime, key = { it.id }) { anime ->
                    AnimeCard(
                        anime = anime,
                        onClick = { onAnimeClick(anime.id) },
                        onLongClick = { selectedQuickActionAnime = anime }
                    )
                }
            }
        }
    }

    if (selectedQuickActionAnime != null) {
        val target = selectedQuickActionAnime!!
        AnimeQuickActionDialog(
            anime = target,
            onDismiss = { selectedQuickActionAnime = null },
            onPlay = { onAnimeClick(target.id) },
            onToggleFavorite = {
                scope.launch {
                    repository.toggleFavorite(target.id, !target.isFavorite)
                    Toast.makeText(context, if (!target.isFavorite) "Added to Favorites" else "Removed from Favorites", Toast.LENGTH_SHORT).show()
                }
            },
            onToggleWatchlist = {
                scope.launch {
                    repository.toggleWatchlist(target.id, !target.isInWatchlist)
                    Toast.makeText(context, if (!target.isInWatchlist) "Added to Watchlist" else "Removed from Watchlist", Toast.LENGTH_SHORT).show()
                }
            },
            onDownload = {
                scope.launch {
                    val ep = com.example.data.model.Episode(
                        id = "${target.id}_1",
                        animeId = target.id,
                        number = 1,
                        title = "Episode 1",
                        thumbnail = target.coverImage
                    )
                    repository.addDownload(target, ep)
                    Toast.makeText(context, "Saved Episode 1 for offline", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
