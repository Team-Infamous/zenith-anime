package com.example.ui.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Anime
import com.example.data.model.Episode
import com.example.data.model.WatchProgress
import com.example.data.model.WatchStatus
import com.example.data.repository.AnimeRepository
import com.example.ui.components.AnimeCard
import com.example.ui.components.AnimeQuickActionDialog
import com.example.ui.components.ContinueWatchingAllSheet
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalThemeAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class HomeCollectionSection(val title: String, val subtitle: String) {
    TRENDING("Top 10 Trending This Week", "Highest momentum and trending releases right now"),
    TOP_RATED("Top Rated Masterpieces", "Critically acclaimed series rated 8.0 and above"),
    POPULAR("Popular Anime Catalog", "Complete catalog of fan-favorite anime series")
}

@Composable
fun HomeScreen(
    repository: AnimeRepository,
    onAnimeClick: (String) -> Unit,
    onPlayClick: (String, Int) -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeAccent = LocalThemeAccent.current
    val allAnime by repository.allAnime.collectAsState(initial = emptyList())
    val homeSettings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())
    val continueWatchingRaw by repository.continueWatching.collectAsState(initial = emptyList())
    val continueWatchingLatest by repository.continueWatchingLatest.collectAsState(initial = emptyList())
    val continueWatchingAll = remember(continueWatchingRaw, continueWatchingLatest, homeSettings) {
        if (homeSettings.continueWatchingOneCard) continueWatchingLatest else continueWatchingRaw
    }
    val continueWatching = remember(continueWatchingAll, homeSettings) {
        continueWatchingAll.take(homeSettings.continueWatchingLimit.coerceAtLeast(1))
    }
    val scope = rememberCoroutineScope()

    var showAllContinueWatchingSheet by remember { mutableStateOf(false) }
    var selectedQuickActionAnime by remember { mutableStateOf<Anime?>(null) }
    var selectedContinueProgress by remember { mutableStateOf<WatchProgress?>(null) }
    var expandedCollectionSection by remember { mutableStateOf<HomeCollectionSection?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var featuredIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        repository.selfHealWatchProgress()
        if (allAnime.isEmpty()) {
            isRefreshing = true
            repository.ensureCatalogPopulated()
            isRefreshing = false
        }
    }

    val trendingList = remember(allAnime) {
        allAnime.sortedByDescending { it.rating }.take(10)
    }

    val topRatedList = remember(allAnime) {
        allAnime.filter { it.rating >= 8.0 }.sortedByDescending { it.rating }
    }

    val popularCatalogList = remember(allAnime) {
        allAnime
    }

    val featuredAnime = remember(allAnime, featuredIndex) {
        if (allAnime.isNotEmpty()) allAnime[featuredIndex % allAnime.size] else null
    }

    LaunchedEffect(allAnime) {
        while (allAnime.isNotEmpty()) {
            delay(8000L)
            featuredIndex = (featuredIndex + 1) % allAnime.size
        }
    }

    BackHandler(enabled = expandedCollectionSection != null) {
        expandedCollectionSection = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        AnimatedContent(
            targetState = expandedCollectionSection,
            transitionSpec = {
                val duration = if (homeSettings.reduceAnimations) 0 else 220
                fadeIn(tween(duration)) togetherWith fadeOut(tween(duration))
            },
            label = "home_collection_transition"
        ) { activeSection ->
            if (activeSection != null) {
                val sectionItems = when (activeSection) {
                    HomeCollectionSection.TRENDING -> trendingList
                    HomeCollectionSection.TOP_RATED -> topRatedList
                    HomeCollectionSection.POPULAR -> popularCatalogList
                }
                HomeCollectionExpandedView(
                    section = activeSection,
                    items = sectionItems,
                    showRank = activeSection == HomeCollectionSection.TRENDING,
                    onBackClick = { expandedCollectionSection = null },
                    onAnimeClick = onAnimeClick,
                    onAnimeLongClick = { selectedQuickActionAnime = it }
                )
            } else if (allAnime.isEmpty() && isRefreshing) {
                HomeSkeletonScreen()
            } else if (allAnime.isEmpty() && !isRefreshing) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_zenith_logo),
                            contentDescription = "Zenith Anime Emblem",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "Zenith Anime",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(DarkSurfaceVariant, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    tint = themeAccent,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Discover Anime Stream",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Instant high-definition streaming library ready to load.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    isRefreshing = true
                                    scope.launch {
                                        repository.ensureCatalogPopulated()
                                        isRefreshing = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = themeAccent,
                                    contentColor = DarkBackground
                                ),
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Populate Anime Catalog", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(450.dp)
                        ) {
                            if (featuredAnime != null) {
                                AsyncImage(
                                    model = featuredAnime.bannerImage.ifEmpty { featuredAnime.coverImage },
                                    contentDescription = featuredAnime.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xB30B0E14),
                                                Color(0x330B0E14),
                                                Color(0xCC0B0E14),
                                                DarkBackground
                                            )
                                        )
                                    )
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_zenith_logo),
                                    contentDescription = "Zenith Anime Emblem",
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = "Zenith Anime",
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                IconButton(
                                    onClick = onSearchClick,
                                    modifier = Modifier.testTag("home_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = Color.White
                                    )
                                }

                                IconButton(
                                    onClick = onNotificationsClick,
                                    modifier = Modifier.testTag("home_notifications_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = "Notifications",
                                        tint = Color.White
                                    )
                                }
                            }

                            if (featuredAnime != null) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(themeAccent, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "SPOTLIGHT",
                                                color = DarkBackground,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .background(Color(0x991E2436), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB800), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = String.format("%.1f", featuredAnime.rating),
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        val epText = when {
                                            featuredAnime.isUpcoming || featuredAnime.availableEpisodes <= 0 -> "Upcoming"
                                            featuredAnime.isAiring -> "${featuredAnime.availableEpisodes} EPS AVAILABLE"
                                            else -> "${featuredAnime.availableEpisodes} EPS"
                                        }
                                        Text(
                                            text = "$epText • ${featuredAnime.quality}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = featuredAnime.title,
                                        color = TextPrimary,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 28.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable { onAnimeClick(featuredAnime.id) }
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = featuredAnime.genres,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val hasAired = featuredAnime.availableEpisodes > 0
                                        Button(
                                            onClick = {
                                                if (hasAired) {
                                                    onPlayClick(featuredAnime.id, 1)
                                                } else {
                                                    Toast.makeText(context, "This anime has not aired yet.", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (hasAired) themeAccent else DarkSurfaceVariant,
                                                contentColor = if (hasAired) DarkBackground else TextSecondary
                                            ),
                                            shape = RoundedCornerShape(24.dp),
                                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                                            modifier = Modifier.testTag("featured_play_button")
                                        ) {
                                            Icon(
                                                imageVector = if (hasAired) Icons.Default.PlayArrow else Icons.Default.Schedule,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (hasAired) "Watch Ep 1" else "Coming Soon",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    val nextState = !featuredAnime.isInWatchlist
                                                    repository.toggleWatchlist(
                                                        featuredAnime.id,
                                                        nextState,
                                                        featuredAnime
                                                    )
                                                    Toast.makeText(
                                                        context,
                                                        if (nextState) "Added to My List" else "Removed from My List",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(24.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                                            modifier = Modifier.testTag("featured_watchlist_button")
                                        ) {
                                            Icon(
                                                imageVector = if (featuredAnime.isInWatchlist) Icons.Default.Check else Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (featuredAnime.isInWatchlist) "In List" else "+ My List",
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        IconButton(
                                            onClick = { onAnimeClick(featuredAnime.id) },
                                            modifier = Modifier
                                                .background(Color(0x66000000), CircleShape)
                                                .testTag("featured_details_button")
                                        ) {
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = "Details",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (continueWatching.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionHeader(
                                title = "Continue Watching (${continueWatchingAll.size})",
                                onSeeAllClick = if (continueWatchingAll.size > 1) {
                                    { showAllContinueWatchingSheet = true }
                                } else null
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(continueWatching, key = { it.id }) { item ->
                                    ContinueWatchingCard(
                                        progress = item,
                                        onPlayClick = { onPlayClick(item.animeId, item.episodeNumber) },
                                        onDismissClick = {
                                            scope.launch {
                                                repository.deleteProgressByAnime(item.animeId)
                                                Toast.makeText(context, "Removed from Continue Watching", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onInfoClick = {
                                            onAnimeClick(item.animeId)
                                        },
                                        onLongClick = {
                                            selectedContinueProgress = item
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }

                    if (trendingList.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = HomeCollectionSection.TRENDING.title,
                                onSeeAllClick = { expandedCollectionSection = HomeCollectionSection.TRENDING }
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                itemsIndexed(trendingList, key = { _, anime -> anime.id }) { index, anime ->
                                    AnimeCard(
                                        anime = anime,
                                        cardWidth = 140f,
                                        rank = index + 1,
                                        onClick = { onAnimeClick(anime.id) },
                                        onLongClick = { selectedQuickActionAnime = anime }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }

                    if (topRatedList.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = HomeCollectionSection.TOP_RATED.title,
                                onSeeAllClick = { expandedCollectionSection = HomeCollectionSection.TOP_RATED }
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(topRatedList, key = { it.id }) { anime ->
                                    AnimeCard(
                                        anime = anime,
                                        cardWidth = 135f,
                                        onClick = { onAnimeClick(anime.id) },
                                        onLongClick = { selectedQuickActionAnime = anime }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }

                    if (popularCatalogList.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = HomeCollectionSection.POPULAR.title,
                                onSeeAllClick = { expandedCollectionSection = HomeCollectionSection.POPULAR }
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(popularCatalogList, key = { it.id }) { anime ->
                                    AnimeCard(
                                        anime = anime,
                                        cardWidth = 135f,
                                        onClick = { onAnimeClick(anime.id) },
                                        onLongClick = { selectedQuickActionAnime = anime }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }

        if (selectedContinueProgress != null) {
            val prog = selectedContinueProgress!!
            val matchingAnime = allAnime.firstOrNull { it.id == prog.animeId }
            ContinueWatchingContextMenuDialog(
                progress = prog,
                anime = matchingAnime,
                onDismiss = { selectedContinueProgress = null },
                onResumeEpisode = {
                    selectedContinueProgress = null
                    onPlayClick(prog.animeId, prog.episodeNumber)
                },
                onRestartEpisode = {
                    selectedContinueProgress = null
                    scope.launch {
                        if (matchingAnime != null) {
                            repository.saveWatchProgress(
                                anime = matchingAnime,
                                episode = Episode(
                                    id = "${prog.animeId}_${prog.episodeNumber}",
                                    animeId = prog.animeId,
                                    number = prog.episodeNumber,
                                    title = prog.episodeTitle.ifBlank { "Episode ${prog.episodeNumber}" },
                                    thumbnail = prog.coverImage,
                                    durationSec = prog.durationSec
                                ),
                                currentTimeSec = 0L,
                                durationSec = prog.durationSec,
                                language = prog.language,
                                completed = false
                            )
                        }
                        onPlayClick(prog.animeId, prog.episodeNumber)
                    }
                },
                onMarkCompleted = {
                    selectedContinueProgress = null
                    scope.launch {
                        val targetAnime = matchingAnime ?: Anime(
                            id = prog.animeId,
                            title = prog.animeTitle,
                            coverImage = prog.coverImage,
                            bannerImage = prog.coverImage,
                            synopsis = ""
                        )
                        repository.saveWatchProgress(
                            anime = targetAnime,
                            episode = Episode(
                                id = "${prog.animeId}_${prog.episodeNumber}",
                                animeId = prog.animeId,
                                number = prog.episodeNumber,
                                title = prog.episodeTitle.ifBlank { "Episode ${prog.episodeNumber}" },
                                thumbnail = prog.coverImage,
                                durationSec = prog.durationSec
                            ),
                            currentTimeSec = prog.durationSec.coerceAtLeast(1440L),
                            durationSec = prog.durationSec.coerceAtLeast(1440L),
                            language = prog.language,
                            completed = true
                        )
                        Toast.makeText(context, "Episode ${prog.episodeNumber} marked as watched", Toast.LENGTH_SHORT).show()
                    }
                },
                onViewAnimeDetails = {
                    selectedContinueProgress = null
                    onAnimeClick(prog.animeId)
                },
                onRemoveFromContinueWatching = {
                    val animeId = prog.animeId
                    val animeTitle = prog.animeTitle
                    selectedContinueProgress = null
                    scope.launch {
                        repository.deleteProgressByAnime(animeId)
                        Toast.makeText(context, "Removed $animeTitle from Continue Watching", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        if (selectedQuickActionAnime != null) {
            val target = selectedQuickActionAnime!!
            AnimeQuickActionDialog(
                anime = target,
                onDismiss = { selectedQuickActionAnime = null },
                onPlay = { onPlayClick(target.id, 1) },
                onViewDetails = { onAnimeClick(target.id) },
                onToggleFavorite = {
                    scope.launch {
                        repository.toggleFavorite(target.id, !target.isFavorite)
                        Toast.makeText(context, if (!target.isFavorite) "Added to Favorites" else "Removed from Favorites", Toast.LENGTH_SHORT).show()
                    }
                },
                onToggleWatchlist = {
                    scope.launch {
                        repository.toggleWatchlist(target.id, !target.isInWatchlist, target)
                        Toast.makeText(context, if (!target.isInWatchlist) "Added to Watchlist" else "Removed from Watchlist", Toast.LENGTH_SHORT).show()
                    }
                },
                onChangeWatchStatus = { status ->
                    scope.launch {
                        repository.updateWatchStatus(target.id, status.name)
                        Toast.makeText(context, "Moved to ${status.displayName}", Toast.LENGTH_SHORT).show()
                    }
                },
                onDownload = {
                    scope.launch {
                        val ep = Episode(
                            id = "${target.id}_1",
                            animeId = target.id,
                            number = 1,
                            title = "Episode 1",
                            thumbnail = target.coverImage
                        )
                        repository.addDownload(target, ep)
                        Toast.makeText(context, "Saved Episode 1 for offline", Toast.LENGTH_SHORT).show()
                    }
                },
                onShare = {
                    Toast.makeText(context, "Sharing is coming soon!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showAllContinueWatchingSheet) {
            ContinueWatchingAllSheet(
                items = continueWatchingAll,
                onPlayClick = { animeId, episodeNum ->
                    showAllContinueWatchingSheet = false
                    onPlayClick(animeId, episodeNum)
                },
                onDismissItem = { progressId ->
                    val animeId = continueWatchingAll.firstOrNull { it.id == progressId }?.animeId
                    scope.launch {
                        if (animeId != null) {
                            repository.deleteProgressByAnime(animeId)
                        } else {
                            repository.deleteProgress(progressId)
                        }
                    }
                },
                onAnimeClick = { animeId ->
                    showAllContinueWatchingSheet = false
                    onAnimeClick(animeId)
                },
                onClearAll = {
                    scope.launch {
                        repository.clearHistory()
                    }
                },
                onDismiss = { showAllContinueWatchingSheet = false }
            )
        }
    }
}

@Composable
private fun HomeCollectionExpandedView(
    section: HomeCollectionSection,
    items: List<Anime>,
    showRank: Boolean,
    onBackClick: () -> Unit,
    onAnimeClick: (String) -> Unit,
    onAnimeLongClick: (Anime) -> Unit
) {
    val themeAccent = LocalThemeAccent.current
    var filterQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf("Default") }

    val displayed = remember(items, filterQuery, sortOption) {
        val filtered = if (filterQuery.isBlank()) items else {
            val q = filterQuery.trim()
            items.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.genres.contains(q, ignoreCase = true)
            }
        }
        when (sortOption) {
            "Score" -> filtered.sortedByDescending { it.rating }
            "A-Z" -> filtered.sortedBy { it.title.lowercase() }
            "Year" -> filtered.sortedByDescending { it.year }
            else -> filtered
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .testTag("home_collection_expanded_view")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("home_collection_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.title,
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${displayed.size} series • ${section.subtitle}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = { Text("Filter within ${section.title}...", color = TextSecondary, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (filterQuery.isNotEmpty()) {
                        IconButton(onClick = { filterQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedBorderColor = themeAccent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sort:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                listOf("Default", "Score", "A-Z", "Year").forEach { option ->
                    val isSel = sortOption == option
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSel) themeAccent else DarkSurfaceVariant)
                            .border(
                                width = 0.5.dp,
                                color = if (isSel) themeAccent else DarkBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { sortOption = option }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = option,
                            color = if (isSel) DarkBackground else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            itemsIndexed(displayed, key = { _, anime -> anime.id }) { index, anime ->
                AnimeCard(
                    anime = anime,
                    modifier = Modifier.fillMaxWidth(),
                    rank = if (showRank && sortOption == "Default" && filterQuery.isBlank()) index + 1 else null,
                    onClick = { onAnimeClick(anime.id) },
                    onLongClick = { onAnimeLongClick(anime) }
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingContextMenuDialog(
    progress: WatchProgress,
    anime: Anime?,
    onDismiss: () -> Unit,
    onResumeEpisode: () -> Unit,
    onRestartEpisode: () -> Unit,
    onMarkCompleted: () -> Unit,
    onViewAnimeDetails: () -> Unit,
    onRemoveFromContinueWatching: () -> Unit
) {
    val themeAccent = LocalThemeAccent.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = progress.coverImage,
                    contentDescription = progress.animeTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 52.dp, height = 72.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = progress.animeTitle,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Episode ${progress.episodeNumber} • ${progress.formattedCurrentTime()} / ${progress.formattedDuration()}",
                        color = themeAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress.progressFraction },
                        color = themeAccent,
                        trackColor = DarkBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(4.dp))

                ContextMenuRow(
                    icon = Icons.Default.PlayArrow,
                    label = if (progress.currentTimeSec > 0L) "Resume Episode ${progress.episodeNumber}" else "Play Episode ${progress.episodeNumber}",
                    tint = themeAccent,
                    onClick = onResumeEpisode
                )

                ContextMenuRow(
                    icon = Icons.Default.Info,
                    label = "Go to Anime Details",
                    tint = TextPrimary,
                    onClick = onViewAnimeDetails
                )

                if (progress.currentTimeSec > 0L) {
                    ContextMenuRow(
                        icon = Icons.Default.Replay,
                        label = "Restart Episode from Beginning",
                        tint = TextPrimary,
                        onClick = onRestartEpisode
                    )
                }

                ContextMenuRow(
                    icon = Icons.Default.CheckCircle,
                    label = "Mark Episode ${progress.episodeNumber} as Watched",
                    tint = TextPrimary,
                    onClick = onMarkCompleted
                )

                ContextMenuRow(
                    icon = Icons.Default.DeleteOutline,
                    label = "Remove from Continue Watching",
                    tint = AccentRed,
                    onClick = onRemoveFromContinueWatching
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ContextMenuRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = if (tint == AccentRed) AccentRed else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
