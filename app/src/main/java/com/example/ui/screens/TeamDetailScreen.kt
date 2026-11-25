package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ads.AdMobBannerBar
import com.example.data.model.MatchSummary
import com.example.data.model.SquadPlayer
import com.example.data.model.TeamDetailData
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.ErrorStatePanel
import com.example.ui.components.HistoricalLineupCard
import com.example.ui.components.InjuryRowCard
import com.example.ui.components.MatchItem
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.TeamCrest
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.PitchBorder
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchObsidian
import com.example.ui.theme.PitchSurface
import com.example.ui.theme.PitchSurfaceElevated
import com.example.ui.theme.SlateDim
import com.example.ui.theme.SlateMuted
import com.example.ui.viewmodel.UiState

@Composable
fun TeamDetailScreen(
    teamNameHint: String,
    logoHint: String?,
    teamState: UiState<TeamDetailData>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenMatchDetail: (MatchSummary) -> Unit,
    onSelectPlayer: (String, SquadPlayer) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Players History", "History Lineups", "Injuries", "Fixtures & H2H")

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
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PitchSurface)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("team_detail_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = ChalkWhite
                    )
                }

                val data = (teamState as? UiState.Success)?.data
                TeamCrest(
                    logoUrl = data?.logoUrl ?: logoHint,
                    shortName = data?.teamShort ?: teamNameHint.take(3).uppercase(),
                    size = 40.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data?.teamName ?: teamNameHint.ifBlank { "Club Profile" },
                        style = MaterialTheme.typography.titleLarge,
                        color = ChalkWhite,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sub = if (data != null) {
                        listOfNotNull(
                            data.recordSummary.takeIf { it != "-" }?.let { "Record $it" },
                            data.venue.takeIf { it.isNotBlank() }
                        ).joinToString(" • ").ifBlank { "Squad, Lineups & Injuries" }
                    } else {
                        "Squad, Lineups & Injuries"
                    }
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh club", tint = PitchEmerald)
                }
            }

            when (teamState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PitchEmerald)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Loading squad, injuries & lineups...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlateMuted
                            )
                        }
                    }
                }

                is UiState.Error -> {
                    Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                        ErrorStatePanel(message = teamState.message, onRetry = onRetry)
                    }
                }

                is UiState.Success -> {
                    val detail = teamState.data

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
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                if (detail.squad.isEmpty()) {
                                    item {
                                        EmptyStatePanel(
                                            title = "Squad Data Updating",
                                            subtitle = "Player roster and season history for this competition are not available yet."
                                        )
                                    }
                                } else {
                                    item {
                                        Text(
                                            text = "TAP ANY PLAYER FOR CAREER & SEASON HISTORY (${detail.squad.size})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = PitchEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    items(detail.squad, key = { "${it.id}_${it.name}" }) { player ->
                                        SquadPlayerCard(
                                            player = player,
                                            onClick = { onSelectPlayer(detail.leagueCode, player) }
                                        )
                                    }
                                }
                            }

                            1 -> {
                                if (detail.historyLineups.isEmpty()) {
                                    item {
                                        EmptyStatePanel(
                                            title = "No Recent Match Lineups",
                                            subtitle = "Recent starting XIs will appear here once completed match summaries are available."
                                        )
                                    }
                                } else {
                                    items(detail.historyLineups, key = { it.matchId }) { hist ->
                                        HistoricalLineupCard(
                                            history = hist,
                                            accent = PitchEmerald,
                                            onPlayerClick = { player -> onSelectPlayer(detail.leagueCode, player) }
                                        )
                                    }
                                }
                            }

                            2 -> {
                                if (detail.injuries.isEmpty()) {
                                    item {
                                        EmptyStatePanel(
                                            title = "No Reported Club Injuries",
                                            subtitle = "No active injuries or suspensions are currently listed for ${detail.teamName}."
                                        )
                                    }
                                } else {
                                    items(detail.injuries, key = { "${it.playerName}_${it.status}" }) { injury ->
                                        InjuryRowCard(
                                            injury = injury,
                                            onPlayerClick = { player -> onSelectPlayer(detail.leagueCode, player) }
                                        )
                                    }
                                }
                            }

                            3 -> {
                                if (detail.recentMatches.isEmpty()) {
                                    item {
                                        EmptyStatePanel(
                                            title = "No Scheduled Fixtures",
                                            subtitle = "Recent results and upcoming fixtures for ${detail.teamName} will appear here."
                                        )
                                    }
                                } else {
                                    item {
                                        Text(
                                            text = "TAP ANY FIXTURE FOR H2H & MATCH STATS",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = PitchEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    items(detail.recentMatches.take(20), key = { it.id }) { match ->
                                        MatchItem(
                                            match = match,
                                            userPrediction = null,
                                            isSubscribed = false,
                                            onMatchClick = { onOpenMatchDetail(match) },
                                            onToggleSubscribe = {}
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PitchObsidian)
            ) {
                HorizontalDivider(color = PitchBorder, thickness = 1.dp)
                AdMobBannerBar(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SquadPlayerCard(
    player: SquadPlayer,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PitchSurface)
            .border(1.dp, PitchBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("squad_player_${player.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerAvatar(headshotUrl = player.headshotUrl, name = player.name, size = 44)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (player.jersey != "-") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PitchSurfaceElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "#${player.jersey}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PitchEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = ChalkWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            val meta = listOfNotNull(
                player.position.takeIf { it.isNotBlank() },
                player.age.takeIf { it.isNotBlank() }?.let { "Age $it" },
                player.nationality.takeIf { it.isNotBlank() }
            ).joinToString(" • ")
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (player.appearances > 0 || player.goals > 0 || player.assists > 0) {
                Text(
                    text = "${player.appearances} apps • ${player.goals}G • ${player.assists}A",
                    style = MaterialTheme.typography.labelSmall,
                    color = InfoCyan
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "View player history",
            tint = SlateDim
        )
    }
}
