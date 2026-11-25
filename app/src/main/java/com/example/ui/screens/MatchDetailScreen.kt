package com.example.ui.screens

import android.app.Activity
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ads.AdMobBannerBar
import com.example.ads.AdMobManager
import com.example.data.model.MatchDetailData
import com.example.data.model.MatchEventType
import com.example.data.model.MatchPhase
import com.example.data.model.MatchPrediction
import com.example.data.model.MatchSummary
import com.example.data.model.PredictionConfidence
import com.example.data.model.PredictionPick
import com.example.data.model.PredictionStatus
import com.example.data.model.SquadPlayer
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.ErrorStatePanel
import com.example.ui.components.HeadToHeadMatchCard
import com.example.ui.components.HeadToHeadSummaryCard
import com.example.ui.components.HistoricalLineupCard
import com.example.ui.components.InjuryRowCard
import com.example.ui.components.InteractiveLineupSection
import com.example.ui.components.LivePulseBadge
import com.example.ui.components.StatComparisonRow
import com.example.ui.components.TeamCrest
import com.example.ui.components.WinProbabilityBar
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.MatchdayGold
import com.example.ui.theme.MatchdayGoldContainer
import com.example.ui.theme.PitchBorder
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchEmeraldContainer
import com.example.ui.theme.PitchObsidian
import com.example.ui.theme.PitchSurface
import com.example.ui.theme.PitchSurfaceElevated
import com.example.ui.theme.PitchSurfaceHighlight
import com.example.ui.theme.SlateDim
import com.example.ui.theme.SlateMuted
import com.example.ui.viewmodel.UiState

@Composable
fun MatchDetailScreen(
    matchId: String,
    leagueCode: String,
    detailState: UiState<MatchDetailData>,
    existingPrediction: MatchPrediction?,
    isSubscribed: Boolean,
    boostTokens: Long,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleSubscription: (MatchSummary) -> Unit,
    onSubmitPrediction: (MatchSummary, PredictionPick, Int, Int, PredictionConfidence) -> Unit,
    onWatchRewardedAd: () -> Unit,
    onNotifyBanner: (String) -> Unit,
    onOpenTeamDetail: (teamId: String, leagueCode: String, teamName: String, logoUrl: String?) -> Unit = { _, _, _, _ -> },
    onSelectPlayer: (leagueCode: String, player: SquadPlayer) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Predict", "Stats", "Timeline", "Lineups", "H2H", "Injuries")

    LaunchedEffect(matchId) {
        AdMobManager.maybeShowInterstitial(activity)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchObsidian),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            when (detailState) {
                is UiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = ChalkWhite)
                        }
                        Text("Match Center", style = MaterialTheme.typography.titleLarge, color = ChalkWhite)
                    }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PitchEmerald)
                    }
                }

                is UiState.Error -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = ChalkWhite)
                        }
                        Text("Match Center", style = MaterialTheme.typography.titleLarge, color = ChalkWhite)
                    }
                    Box(modifier = Modifier.padding(16.dp)) {
                        ErrorStatePanel(message = detailState.message, onRetry = onRetry)
                    }
                }

                is UiState.Success -> {
                    val detail = detailState.data
                    val match = detail.match

                    // Atmospheric Stadium Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PitchSurface)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_stadium_hero),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(0.dp))
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            PitchObsidian.copy(alpha = 0.72f),
                                            PitchObsidian.copy(alpha = 0.90f),
                                            PitchObsidian
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("match_detail_back")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back to matches",
                                        tint = ChalkWhite
                                    )
                                }

                                Text(
                                    text = match.leagueName.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PitchEmerald,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(
                                    onClick = { onToggleSubscription(match) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("detail_bell_button")
                                ) {
                                    Icon(
                                        imageVector = if (isSubscribed) Icons.Rounded.NotificationsActive else Icons.Outlined.NotificationsNone,
                                        contentDescription = "Toggle goal alerts",
                                        tint = if (isSubscribed) MatchdayGold else ChalkWhite
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Scoreboard Centerpiece
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            onOpenTeamDetail(match.homeTeamId, match.leagueCode, match.homeTeam, match.homeLogoUrl)
                                        }
                                        .padding(6.dp)
                                        .testTag("open_home_team_btn"),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    TeamCrest(logoUrl = match.homeLogoUrl, shortName = match.homeShort, size = 54.dp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = match.homeTeam,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ChalkWhite,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                    if (match.homeRecord != "-") {
                                        Text(
                                            text = match.homeRecord,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SlateMuted
                                        )
                                    }
                                    Text(
                                        text = "Squad & History ›",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PitchEmerald
                                    )
                                }

                                Column(
                                    modifier = Modifier.width(104.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (match.phase == MatchPhase.LIVE) {
                                        LivePulseBadge(clockDisplay = match.clockDisplay)
                                    } else {
                                        Text(
                                            text = match.clockDisplay,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (match.phase == MatchPhase.PRE) PitchEmerald else SlateMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (match.phase == MatchPhase.PRE) "VS" else "${match.homeScore} : ${match.awayScore}",
                                        style = MaterialTheme.typography.displaySmall,
                                        color = ChalkWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = match.venue,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateDim,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            onOpenTeamDetail(match.awayTeamId, match.leagueCode, match.awayTeam, match.awayLogoUrl)
                                        }
                                        .padding(6.dp)
                                        .testTag("open_away_team_btn"),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    TeamCrest(logoUrl = match.awayLogoUrl, shortName = match.awayShort, size = 54.dp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = match.awayTeam,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ChalkWhite,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                    if (match.awayRecord != "-") {
                                        Text(
                                            text = match.awayRecord,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SlateMuted
                                        )
                                    }
                                    Text(
                                        text = "Squad & History ›",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InfoCyan
                                    )
                                }
                            }
                        }
                    }

                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = PitchSurface,
                        contentColor = PitchEmerald,
                        edgePadding = 12.dp,
                        indicator = { tabPositions ->
                            SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = PitchEmerald
                            )
                        }
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selectedTab == index) PitchEmerald else SlateMuted
                                    )
                                }
                            )
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                item {
                                    PredictionCenterCard(
                                        match = match,
                                        existingPrediction = existingPrediction,
                                        boostTokens = boostTokens,
                                        onSubmitPrediction = onSubmitPrediction,
                                        onWatchRewardedAd = {
                                            AdMobManager.showRewardedAd(
                                                activity = activity,
                                                onRewardEarned = onWatchRewardedAd,
                                                onUnavailable = onNotifyBanner
                                            )
                                        }
                                    )
                                }
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(PitchSurface)
                                            .border(1.dp, PitchBorder, RoundedCornerShape(16.dp))
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "MATCH INFO",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = PitchEmerald
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        InfoLine("Stadium", match.venue)
                                        InfoLine("Referee", detail.referee)
                                        InfoLine("Attendance", detail.attendance)
                                    }
                                }
                            }

                            1 -> {
                                if (detail.stats.isEmpty()) {
                                    item {
                                        EmptyStatePanel(
                                            title = "Match Stats Pending",
                                            subtitle = "Detailed possession, shots, corners, and passing telemetry appear once kickoff begins."
                                        )
                                    }
                                } else {
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(PitchSurface)
                                                .border(1.dp, PitchBorder, RoundedCornerShape(16.dp))
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                text = "TEAM STATISTICS",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = PitchEmerald,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                            detail.stats.forEach { stat ->
                                                StatComparisonRow(stat = stat)
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> {
                                if (detail.events.isEmpty()) {
                                    item {
                                        EmptyStatePanel(
                                            title = "No Key Events Yet",
                                            subtitle = "Goals, cards, and substitutions will appear chronologically here during the match."
                                        )
                                    }
                                } else {
                                    items(detail.events) { event ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(PitchSurface)
                                                .border(1.dp, PitchBorder, RoundedCornerShape(14.dp))
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        when (event.type) {
                                                            MatchEventType.GOAL -> PitchEmeraldContainer
                                                            MatchEventType.RED_CARD -> LiveCrimson.copy(alpha = 0.2f)
                                                            MatchEventType.YELLOW_CARD -> MatchdayGoldContainer
                                                            else -> PitchSurfaceHighlight
                                                        }
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = event.minute,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = when (event.type) {
                                                        MatchEventType.GOAL -> PitchEmerald
                                                        MatchEventType.RED_CARD -> LiveCrimson
                                                        MatchEventType.YELLOW_CARD -> MatchdayGold
                                                        else -> ChalkWhite
                                                    },
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = event.text,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = ChalkWhite
                                                )
                                                if (event.teamName.isNotBlank()) {
                                                    Text(
                                                        text = event.teamName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = SlateMuted
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            3 -> {
                                if (detail.homeLineup.isNotEmpty() || detail.awayLineup.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "MATCH LINEUPS (TAP PLAYER FOR HISTORY)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = PitchEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (detail.homeLineup.isNotEmpty()) {
                                        item {
                                            InteractiveLineupSection(
                                                teamName = match.homeTeam,
                                                players = detail.homeLineup,
                                                accent = PitchEmerald,
                                                onPlayerClick = { player -> onSelectPlayer(match.leagueCode, player) }
                                            )
                                        }
                                    }
                                    if (detail.awayLineup.isNotEmpty()) {
                                        item {
                                            InteractiveLineupSection(
                                                teamName = match.awayTeam,
                                                players = detail.awayLineup,
                                                accent = InfoCyan,
                                                onPlayerClick = { player -> onSelectPlayer(match.leagueCode, player) }
                                            )
                                        }
                                    }
                                } else {
                                    item {
                                        EmptyStatePanel(
                                            title = "Match Lineups Announced ~1h Before Kickoff",
                                            subtitle = "Review both teams' recent historical starting XIs below."
                                        )
                                    }
                                }

                                if (detail.homeHistoryLineups.isNotEmpty() || detail.awayHistoryLineups.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "RECENT HISTORY LINEUPS",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MatchdayGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    items(detail.homeHistoryLineups, key = { "home_hist_${it.matchId}" }) { hist ->
                                        HistoricalLineupCard(
                                            history = hist,
                                            accent = PitchEmerald,
                                            onPlayerClick = { player -> onSelectPlayer(match.leagueCode, player) }
                                        )
                                    }
                                    items(detail.awayHistoryLineups, key = { "away_hist_${it.matchId}" }) { hist ->
                                        HistoricalLineupCard(
                                            history = hist,
                                            accent = InfoCyan,
                                            onPlayerClick = { player -> onSelectPlayer(match.leagueCode, player) }
                                        )
                                    }
                                }
                            }

                            4 -> {
                                if (detail.h2hMatches.isEmpty()) {
                                    item {
                                        EmptyStatePanel(
                                            title = "No Recent Head-to-Head Meetings",
                                            subtitle = "Historical meetings between ${match.homeTeam} and ${match.awayTeam} were not returned for this fixture."
                                        )
                                    }
                                } else {
                                    item {
                                        HeadToHeadSummaryCard(
                                            homeTeam = match.homeTeam,
                                            awayTeam = match.awayTeam,
                                            matches = detail.h2hMatches
                                        )
                                    }
                                    items(detail.h2hMatches, key = { it.id }) { h2h ->
                                        HeadToHeadMatchCard(item = h2h)
                                    }
                                }
                            }

                            5 -> {
                                if (detail.injuries.isEmpty()) {
                                    item {
                                        EmptyStatePanel(
                                            title = "No Injuries Reported",
                                            subtitle = "No active injuries or suspensions were reported for ${match.homeTeam} or ${match.awayTeam}."
                                        )
                                    }
                                } else {
                                    item {
                                        Text(
                                            text = "PLAYER INJURIES & SUSPENSIONS (${detail.injuries.size})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = LiveCrimson,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    items(detail.injuries, key = { "${it.teamName}_${it.playerName}" }) { injury ->
                                        InjuryRowCard(
                                            injury = injury,
                                            onPlayerClick = { player -> onSelectPlayer(match.leagueCode, player) }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            AdMobBannerBar()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictionCenterCard(
    match: MatchSummary,
    existingPrediction: MatchPrediction?,
    boostTokens: Long,
    onSubmitPrediction: (MatchSummary, PredictionPick, Int, Int, PredictionConfidence) -> Unit,
    onWatchRewardedAd: () -> Unit
) {
    val isSettled = existingPrediction != null && existingPrediction.parsedStatus != PredictionStatus.PENDING
    var pick by remember(existingPrediction?.id) {
        mutableStateOf(existingPrediction?.parsedPick ?: PredictionPick.HOME)
    }
    var homeGoals by remember(existingPrediction?.id) {
        mutableIntStateOf(existingPrediction?.predictedHomeScore?.toInt() ?: 2)
    }
    var awayGoals by remember(existingPrediction?.id) {
        mutableIntStateOf(existingPrediction?.predictedAwayScore?.toInt() ?: 1)
    }
    var confidence by remember(existingPrediction?.id) {
        mutableStateOf(existingPrediction?.parsedConfidence ?: PredictionConfidence.STANDARD)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PitchSurface)
            .border(1.dp, PitchBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WIN PROBABILITY & PICK",
                style = MaterialTheme.typography.labelMedium,
                color = PitchEmerald,
                fontWeight = FontWeight.Bold
            )
            if (existingPrediction != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = PitchEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = existingPrediction.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = PitchEmerald
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        WinProbabilityBar(
            homeProb = match.homeWinProb,
            drawProb = match.drawProb,
            awayProb = match.awayWinProb,
            compact = false
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Who wins?",
            style = MaterialTheme.typography.titleMedium,
            color = ChalkWhite
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PickOptionButton(
                label = match.homeShort,
                selected = pick == PredictionPick.HOME,
                enabled = !isSettled,
                onClick = {
                    pick = PredictionPick.HOME
                    if (homeGoals <= awayGoals) homeGoals = awayGoals + 1
                },
                modifier = Modifier.weight(1f).testTag("pick_home_btn")
            )
            PickOptionButton(
                label = "DRAW",
                selected = pick == PredictionPick.DRAW,
                enabled = !isSettled,
                onClick = {
                    pick = PredictionPick.DRAW
                    awayGoals = homeGoals
                },
                modifier = Modifier.weight(1f).testTag("pick_draw_btn")
            )
            PickOptionButton(
                label = match.awayShort,
                selected = pick == PredictionPick.AWAY,
                enabled = !isSettled,
                onClick = {
                    pick = PredictionPick.AWAY
                    if (awayGoals <= homeGoals) awayGoals = homeGoals + 1
                },
                modifier = Modifier.weight(1f).testTag("pick_away_btn")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Exact Score Prediction (+50 pts bonus)",
            style = MaterialTheme.typography.titleMedium,
            color = ChalkWhite
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PitchSurfaceElevated)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ScoreStepper(
                teamLabel = match.homeShort,
                score = homeGoals,
                enabled = !isSettled,
                onChange = { next ->
                    homeGoals = next
                    pick = when {
                        homeGoals > awayGoals -> PredictionPick.HOME
                        awayGoals > homeGoals -> PredictionPick.AWAY
                        else -> PredictionPick.DRAW
                    }
                }
            )
            Text(":", style = MaterialTheme.typography.headlineMedium, color = SlateMuted)
            ScoreStepper(
                teamLabel = match.awayShort,
                score = awayGoals,
                enabled = !isSettled,
                onChange = { next ->
                    awayGoals = next
                    pick = when {
                        homeGoals > awayGoals -> PredictionPick.HOME
                        awayGoals > homeGoals -> PredictionPick.AWAY
                        else -> PredictionPick.DRAW
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PredictionConfidence.entries.forEach { option ->
                val selected = confidence == option
                val isBoost = option == PredictionConfidence.BOOSTED
                PickOptionButton(
                    label = if (isBoost) "2x Boost ($boostTokens)" else option.label,
                    selected = selected,
                    enabled = !isSettled,
                    accent = if (isBoost) MatchdayGold else PitchEmerald,
                    onClick = { confidence = option },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (!isSettled) {
            Button(
                onClick = {
                    onSubmitPrediction(match, pick, homeGoals, awayGoals, confidence)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PitchEmerald,
                    contentColor = PitchObsidian
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("lock_prediction_button")
            ) {
                Icon(Icons.Rounded.SportsSoccer, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (existingPrediction == null) "Lock Prediction" else "Update Prediction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onWatchRewardedAd,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MatchdayGold.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("watch_ad_boost_button")
            ) {
                Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = MatchdayGold)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Watch Ad for +1 Boost Token (2x Pts)",
                    color = MatchdayGold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PitchEmeraldContainer)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = PitchEmerald)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Match settled: ${existingPrediction.status} (${existingPrediction.pointsAwarded} pts)",
                    style = MaterialTheme.typography.titleMedium,
                    color = PitchEmerald
                )
            }
        }
    }
}

@Composable
private fun PickOptionButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    accent: Color = PitchEmerald,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.copy(alpha = 0.2f) else PitchSurfaceElevated)
            .border(
                width = 1.dp,
                color = if (selected) accent else PitchBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) accent else ChalkWhite,
            maxLines = 1
        )
    }
}

@Composable
private fun ScoreStepper(
    teamLabel: String,
    score: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = teamLabel, style = MaterialTheme.typography.labelSmall, color = SlateMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (score > 0) onChange(score - 1) },
                enabled = enabled && score > 0,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = "Decrease score", tint = ChalkWhite)
            }
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = PitchEmerald,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(
                onClick = { if (score < 15) onChange(score + 1) },
                enabled = enabled && score < 15,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Increase score", tint = ChalkWhite)
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SlateMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = ChalkWhite, fontWeight = FontWeight.SemiBold)
    }
}
