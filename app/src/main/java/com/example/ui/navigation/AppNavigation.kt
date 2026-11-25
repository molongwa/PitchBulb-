package com.example.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.data.model.MatchPhase
import com.example.data.model.MatchSummary
import com.example.data.repository.PredictionRepository
import com.example.data.repository.SoccerRepository
import com.example.notifications.NotificationHelper
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.NewsArticleDetailScreen
import com.example.ui.components.NewsFeed
import com.example.ui.components.PlayerHistoryBottomSheet
import com.example.ui.screens.AlertsAndSettingsScreen
import com.example.ui.screens.LeaguesScreen
import com.example.ui.screens.MatchDetailScreen
import com.example.ui.screens.MatchesScreen
import com.example.ui.screens.PredictionsScreen
import com.example.ui.screens.TeamDetailScreen
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchEmeraldContainer
import com.example.ui.theme.PitchObsidian
import com.example.ui.theme.PitchSurface
import com.example.ui.theme.SlateMuted
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.serialization.Serializable

@Serializable
object MainTabsRoute

@Serializable
data class MatchDetailRoute(
    val matchId: String,
    val leagueCode: String
)

@Serializable
data class TeamDetailRoute(
    val teamId: String,
    val leagueCode: String,
    val teamName: String = "",
    val logoUrl: String = ""
)

@Serializable
data class NewsArticleRoute(
    val articleId: String
)

private data class NavTabItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val MAIN_TABS = listOf(
    NavTabItem("Matches", Icons.Rounded.SportsSoccer, Icons.Outlined.SportsSoccer, "nav_matches"),
    NavTabItem("News", Icons.Rounded.Newspaper, Icons.Outlined.Newspaper, "nav_news"),
    NavTabItem("Predict", Icons.Rounded.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_predict"),
    NavTabItem("Leagues", Icons.Rounded.Leaderboard, Icons.Outlined.Leaderboard, "nav_leagues"),
    NavTabItem("Alerts", Icons.Rounded.NotificationsActive, Icons.Outlined.Notifications, "nav_alerts")
)

@Composable
fun AppNavigation() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        key(user.uid) {
            AuthenticatedRoot(currentUserId = user.uid)
        }
    }
}

@Composable
private fun AuthenticatedRoot(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MainViewModel(
                    soccerRepository = SoccerRepository(),
                    predictionRepository = PredictionRepository(db),
                    currentUserId = currentUserId,
                    appContext = app.applicationContext
                )
            }
        }
    )

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            NotificationHelper.scheduleBackgroundMatchAlerts(context)
        }
    }

    LaunchedEffect(currentUserId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationHelper.hasNotificationPermission(context)
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            NotificationHelper.scheduleBackgroundMatchAlerts(context)
        }
    }

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val actionBanner by viewModel.actionBanner.collectAsStateWithLifecycle()

    LaunchedEffect(actionBanner) {
        val message = actionBanner ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearBanner()
    }

    val matchesState by viewModel.matchesState.collectAsStateWithLifecycle()
    val selectedDateOffset by viewModel.selectedDateOffset.collectAsStateWithLifecycle()
    val liveOnlyFilter by viewModel.liveOnlyFilter.collectAsStateWithLifecycle()
    val selectedCountryFilter by viewModel.selectedCountryFilter.collectAsStateWithLifecycle()
    val selectedLeagueFilter by viewModel.selectedLeagueFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val newsState by viewModel.newsState.collectAsStateWithLifecycle()
    val selectedStandingsLeague by viewModel.selectedStandingsLeague.collectAsStateWithLifecycle()
    val standingsState by viewModel.standingsState.collectAsStateWithLifecycle()
    val matchDetailState by viewModel.matchDetailState.collectAsStateWithLifecycle()
    val teamDetailState by viewModel.teamDetailState.collectAsStateWithLifecycle()
    val playerHistoryState by viewModel.playerHistoryState.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val predictionsState by viewModel.predictionsState.collectAsStateWithLifecycle()
    val subscriptionsState by viewModel.subscriptionsState.collectAsStateWithLifecycle()

    val userProfile = (userProfileState as? UiState.Success)?.data
    val predictions = (predictionsState as? UiState.Success)?.data.orEmpty()
    val subscriptions = (subscriptionsState as? UiState.Success)?.data.orEmpty()

    NavHost(
        navController = navController,
        startDestination = MainTabsRoute
    ) {
        composable<MainTabsRoute> {
            var selectedTab by remember { mutableIntStateOf(0) }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PitchObsidian)
            ) {
                val useNavRail = maxWidth >= 680.dp

                Scaffold(
                    containerColor = PitchObsidian,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (!useNavRail) {
                            NavigationBar(
                                containerColor = PitchSurface,
                                tonalElevation = 0.dp
                            ) {
                                MAIN_TABS.forEachIndexed { index, item ->
                                    val selected = selectedTab == index
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { selectedTab = index },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = PitchEmerald,
                                            selectedTextColor = PitchEmerald,
                                            indicatorColor = PitchEmeraldContainer,
                                            unselectedIconColor = SlateMuted,
                                            unselectedTextColor = SlateMuted
                                        ),
                                        modifier = Modifier.testTag(item.tag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useNavRail) {
                            NavigationRail(
                                containerColor = PitchSurface,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .statusBarsPadding()
                            ) {
                                MAIN_TABS.forEachIndexed { index, item ->
                                    val selected = selectedTab == index
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { selectedTab = index },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = { Text(item.label) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = PitchEmerald,
                                            selectedTextColor = PitchEmerald,
                                            indicatorColor = PitchEmeraldContainer,
                                            unselectedIconColor = SlateMuted,
                                            unselectedTextColor = SlateMuted
                                        ),
                                        modifier = Modifier.testTag(item.tag)
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            when (selectedTab) {
                                0 -> MatchesScreen(
                                    matchesState = matchesState,
                                    dateOptions = viewModel.dateOptions,
                                    selectedDateOffset = selectedDateOffset,
                                    liveOnlyFilter = liveOnlyFilter,
                                    selectedCountryFilter = selectedCountryFilter,
                                    selectedLeagueFilter = selectedLeagueFilter,
                                    searchQuery = searchQuery,
                                    predictions = predictions,
                                    subscriptions = subscriptions,
                                    userPoints = userProfile?.accuracyPoints ?: 0L,
                                    onSelectDate = viewModel::selectDateOffset,
                                    onToggleLiveOnly = viewModel::toggleLiveOnly,
                                    onSelectCountry = viewModel::selectCountryFilter,
                                    onSelectLeague = viewModel::selectLeagueFilter,
                                    onSelectCountryAndLeague = viewModel::selectCountryAndLeague,
                                    onSearchQueryChange = viewModel::updateSearchQuery,
                                    onRefresh = { viewModel.loadMatches(showLoading = true) },
                                    onOpenMatchDetail = { match ->
                                        viewModel.loadMatchDetail(match.leagueCode, match.id)
                                        navController.navigate(MatchDetailRoute(match.id, match.leagueCode))
                                    },
                                    onToggleSubscription = viewModel::toggleSubscription
                                )

                                1 -> NewsFeed(
                                    newsState = newsState,
                                    onFetchNews = viewModel::loadNews,
                                    onArticleClick = { article ->
                                        navController.navigate(NewsArticleRoute(article.id))
                                    }
                                )

                                2 -> PredictionsScreen(
                                    userProfile = userProfile,
                                    predictions = predictions,
                                    matchesState = matchesState,
                                    onOpenMatchDetail = { match ->
                                        viewModel.loadMatchDetail(match.leagueCode, match.id)
                                        navController.navigate(MatchDetailRoute(match.id, match.leagueCode))
                                    },
                                    onWatchRewardedAd = viewModel::onRewardedAdCompleted,
                                    onNotifyBanner = { msg ->
                                        // Displayed via banner/snackbar
                                    }
                                )

                                3 -> LeaguesScreen(
                                    selectedLeagueCode = selectedStandingsLeague,
                                    standingsState = standingsState,
                                    onSelectLeague = viewModel::selectStandingsLeague,
                                    onRetry = { viewModel.loadStandings(selectedStandingsLeague) },
                                    onOpenTeamDetail = { teamId, leagueCode, teamName, logoUrl ->
                                        viewModel.loadTeamDetail(leagueCode, teamId, teamName, logoUrl)
                                        navController.navigate(
                                            TeamDetailRoute(
                                                teamId = teamId,
                                                leagueCode = leagueCode,
                                                teamName = teamName,
                                                logoUrl = logoUrl.orEmpty()
                                            )
                                        )
                                    }
                                )

                                4 -> AlertsAndSettingsScreen(
                                    userProfile = userProfile,
                                    subscriptions = subscriptions,
                                    onUpdatePrefs = viewModel::updateNotificationPreferences,
                                    onRemoveSubscription = { sub ->
                                        viewModel.toggleSubscription(
                                            MatchSummary(
                                                id = sub.matchId,
                                                leagueCode = sub.leagueCode,
                                                leagueName = sub.leagueCode,
                                                homeTeamId = "",
                                                awayTeamId = "",
                                                homeTeam = sub.homeTeam,
                                                awayTeam = sub.awayTeam,
                                                homeShort = sub.homeTeam.take(3).uppercase(),
                                                awayShort = sub.awayTeam.take(3).uppercase(),
                                                homeLogoUrl = null,
                                                awayLogoUrl = null,
                                                homeScore = sub.lastKnownHomeScore.toInt(),
                                                awayScore = sub.lastKnownAwayScore.toInt(),
                                                phase = MatchPhase.PRE,
                                                clockDisplay = "",
                                                kickoffIso = "",
                                                kickoffLocalTime = "",
                                                venue = "",
                                                homeRecord = "-",
                                                awayRecord = "-",
                                                homeWinProb = 34,
                                                drawProb = 32,
                                                awayWinProb = 34
                                            )
                                        )
                                    },
                                    onTestGoalAlert = viewModel::triggerSampleGoalNotification,
                                    onSignOut = {
                                        signOutUser(
                                            credentialManager = credentialManager,
                                            scope = scope
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        composable<MatchDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<MatchDetailRoute>()
            val existingPrediction = remember(predictions, route.matchId) {
                predictions.firstOrNull { it.matchId == route.matchId }
            }
            val isSubscribed = remember(subscriptions, route.matchId) {
                subscriptions.any { it.matchId == route.matchId }
            }

            Scaffold(
                containerColor = PitchObsidian,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { _ ->
                MatchDetailScreen(
                    matchId = route.matchId,
                    leagueCode = route.leagueCode,
                    detailState = matchDetailState,
                    existingPrediction = existingPrediction,
                    isSubscribed = isSubscribed,
                    boostTokens = userProfile?.adBoostTokens ?: 0L,
                    onBack = { navController.popBackStack() },
                    onRetry = { viewModel.loadMatchDetail(route.leagueCode, route.matchId) },
                    onToggleSubscription = viewModel::toggleSubscription,
                    onSubmitPrediction = viewModel::submitPrediction,
                    onWatchRewardedAd = viewModel::onRewardedAdCompleted,
                    onNotifyBanner = { /* snackbar handled */ },
                    onOpenTeamDetail = { teamId, teamLeagueCode, teamName, logoUrl ->
                        viewModel.loadTeamDetail(teamLeagueCode, teamId, teamName, logoUrl)
                        navController.navigate(
                            TeamDetailRoute(
                                teamId = teamId,
                                leagueCode = teamLeagueCode,
                                teamName = teamName,
                                logoUrl = logoUrl.orEmpty()
                            )
                        )
                    },
                    onSelectPlayer = viewModel::loadPlayerHistory
                )
            }
        }

        composable<TeamDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TeamDetailRoute>()
            Scaffold(
                containerColor = PitchObsidian,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { _ ->
                TeamDetailScreen(
                    teamNameHint = route.teamName,
                    logoHint = route.logoUrl.ifBlank { null },
                    teamState = teamDetailState,
                    onBack = { navController.popBackStack() },
                    onRetry = {
                        viewModel.loadTeamDetail(
                            leagueCode = route.leagueCode,
                            teamId = route.teamId,
                            teamNameHint = route.teamName,
                            logoHint = route.logoUrl.ifBlank { null }
                        )
                    },
                    onOpenMatchDetail = { match ->
                        viewModel.loadMatchDetail(match.leagueCode, match.id)
                        navController.navigate(MatchDetailRoute(match.id, match.leagueCode))
                    },
                    onSelectPlayer = viewModel::loadPlayerHistory
                )
            }
        }

        composable<NewsArticleRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<NewsArticleRoute>()
            val article = remember(newsState, route.articleId) {
                (newsState as? UiState.Success)?.data?.firstOrNull { it.id == route.articleId }
            }
            NewsArticleDetailScreen(
                article = article,
                onBack = { navController.popBackStack() }
            )
        }
    }

    PlayerHistoryBottomSheet(
        playerHistoryState = playerHistoryState,
        onDismiss = viewModel::clearPlayerHistory
    )
}
