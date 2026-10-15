package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.AnimeRepository
import com.example.ui.components.BottomNavBar
import com.example.ui.details.AnimeDetailsScreen
import com.example.ui.download.DownloadScreen
import com.example.ui.home.HomeScreen
import com.example.ui.mylist.MyListScreen
import com.example.ui.navigation.Screen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.player.PlayerScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.schedule.ScheduleScreen
import com.example.ui.search.SearchScreen
import com.example.ui.settings.ConfirmationsSettingsScreen
import com.example.ui.settings.DownloadSettingsScreen
import com.example.ui.settings.LibrarySettingsScreen
import com.example.ui.settings.NotificationSettingsScreen
import com.example.ui.settings.PlayerSettingsScreen
import com.example.ui.settings.SubtitleSettingsScreen
import com.example.ui.splash.AnimatedSplashScreen
import com.example.ui.sync.MalSyncScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = AnimeRepository(applicationContext)

        setContent {
            val settings by repository.settings.collectAsState(initial = com.example.data.model.AppSettings())
            MyApplicationTheme(
                themeAccent = settings.themeAccent,
                amoled = settings.amoledMode
            ) {
                MainApp(repository = repository)
            }
        }
    }
}

@Composable
fun MainApp(repository: AnimeRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        repository.freshStartWipe()
    }

    // Android 13+ drops every notification from an app that has not asked for POST_NOTIFICATIONS,
    // which made download and episode alerts silently vanish. Ask once, right after launch.
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                context,
                "Notifications are off — turn them on in system settings to get download alerts",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        val missingOnAndroid13Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (missingOnAndroid13Plus) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val bottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.Schedule.route,
        Screen.MyList.route,
        Screen.Download.route,
        Screen.Profile.route
    )

    val showBottomBar = currentRoute in bottomBarRoutes

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DarkBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(animationSpec = tween(220)) },
                exitTransition = { fadeOut(animationSpec = tween(220)) },
                popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                popExitTransition = { fadeOut(animationSpec = tween(220)) }
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        repository = repository,
                        onAnimeClick = { animeId ->
                            navController.navigate(Screen.Details.createRoute(animeId))
                        },
                        onPlayClick = { animeId, episodeNum ->
                            navController.navigate(Screen.Player.createRoute(animeId, episodeNum, "sub"))
                        },
                        onSearchClick = {
                            navController.navigate(Screen.Search.route)
                        },
                        onNotificationsClick = {
                            navController.navigate(Screen.Notifications.route)
                        }
                    )
                }

                composable(Screen.Schedule.route) {
                    ScheduleScreen(
                        repository = repository,
                        onAnimeClick = { animeId ->
                            navController.navigate(Screen.Details.createRoute(animeId))
                        },
                        onSearchClick = {
                            navController.navigate(Screen.Search.route)
                        }
                    )
                }

                composable(Screen.MyList.route) {
                    MyListScreen(
                        repository = repository,
                        onAnimeClick = { animeId ->
                            navController.navigate(Screen.Details.createRoute(animeId))
                        },
                        onSearchClick = {
                            navController.navigate(Screen.Search.route)
                        }
                    )
                }

                composable(Screen.Download.route) {
                    DownloadScreen(
                        repository = repository,
                        onPlayClick = { animeId, episodeNum ->
                            navController.navigate(Screen.Player.createRoute(animeId, episodeNum, "sub"))
                        },
                        onSearchClick = {
                            navController.navigate(Screen.Search.route)
                        }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        repository = repository,
                        onMalSyncClick = {
                            navController.navigate(Screen.MalSync.route)
                        },
                        onPlayerSettingsClick = {
                            navController.navigate(Screen.PlayerSettings.route)
                        },
                        onSubtitleSettingsClick = {
                            navController.navigate(Screen.SubtitleSettings.route)
                        },
                        onDownloadSettingsClick = {
                            navController.navigate(Screen.DownloadSettings.route)
                        },
                        onLibrarySettingsClick = {
                            navController.navigate(Screen.LibrarySettings.route)
                        },
                        onNotificationSettingsClick = {
                            navController.navigate(Screen.NotificationSettings.route)
                        },
                        onConfirmationsSettingsClick = {
                            navController.navigate(Screen.ConfirmationsSettings.route)
                        }
                    )
                }

                composable(Screen.MalSync.route) {
                    MalSyncScreen(
                        repository = repository,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.PlayerSettings.route) {
                    PlayerSettingsScreen(
                        repository = repository,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.SubtitleSettings.route) {
                    SubtitleSettingsScreen(
                        repository = repository,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.DownloadSettings.route) {
                    DownloadSettingsScreen(
                        repository = repository,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.LibrarySettings.route) {
                    LibrarySettingsScreen(
                        repository = repository,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.NotificationSettings.route) {
                    NotificationSettingsScreen(
                        repository = repository,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.ConfirmationsSettings.route) {
                    ConfirmationsSettingsScreen(
                        repository = repository,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        repository = repository,
                        onAnimeClick = { animeId ->
                            navController.navigate(Screen.Details.createRoute(animeId))
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.Notifications.route) {
                    NotificationsScreen(
                        repository = repository,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = Screen.Details.route,
                    arguments = listOf(navArgument("animeId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val animeId = backStackEntry.arguments?.getString("animeId") ?: ""
                    AnimeDetailsScreen(
                        animeId = animeId,
                        repository = repository,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onPlayEpisode = { aid, epNum, lang ->
                            navController.navigate(Screen.Player.createRoute(aid, epNum, lang))
                        },
                        onRelatedAnimeClick = { relatedId ->
                            navController.navigate(Screen.Details.createRoute(relatedId))
                        }
                    )
                }

                composable(
                    route = Screen.Player.route,
                    arguments = listOf(
                        navArgument("animeId") { type = NavType.StringType },
                        navArgument("episodeNumber") { type = NavType.IntType },
                        navArgument("language") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val animeId = backStackEntry.arguments?.getString("animeId") ?: ""
                    val episodeNum = backStackEntry.arguments?.getInt("episodeNumber") ?: 1
                    val lang = backStackEntry.arguments?.getString("language") ?: "sub"

                    PlayerScreen(
                        animeId = animeId,
                        initialEpisodeNumber = episodeNum,
                        initialLanguage = lang,
                        repository = repository,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showSplash,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(400)) + scaleOut(targetScale = 1.05f)
    ) {
        AnimatedSplashScreen(onFinish = { showSplash = false })
    }
}
}
