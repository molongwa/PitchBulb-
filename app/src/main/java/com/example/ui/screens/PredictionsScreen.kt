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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ads.AdMobBannerBar
import com.example.ads.AdMobManager
import com.example.data.model.MatchPhase
import com.example.data.model.MatchPrediction
import com.example.data.model.MatchSummary
import com.example.data.model.PredictionStatus
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.TeamCrest
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.LiveCrimsonContainer
import com.example.ui.theme.MatchdayGold
import com.example.ui.theme.MatchdayGoldContainer
import com.example.ui.theme.PitchBorder
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchEmeraldContainer
import com.example.ui.theme.PitchObsidian
import com.example.ui.theme.PitchSurface
import com.example.ui.theme.PitchSurfaceElevated
import com.example.ui.theme.SlateMuted
import com.example.ui.viewmodel.UiState

@Composable
fun PredictionsScreen(
    userProfile: UserProfile?,
    predictions: List<MatchPrediction>,
    matchesState: UiState<List<MatchSummary>>,
    onOpenMatchDetail: (MatchSummary) -> Unit,
    onWatchRewardedAd: () -> Unit,
    onNotifyBanner: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var selectedFilter by remember { mutableIntStateOf(0) } // 0 = Quick Picks, 1 = My Picks

    val upcomingMatches = remember(matchesState, predictions) {
        val predictedIds = predictions.map { it.matchId }.toSet()
        (matchesState as? UiState.Success)?.data
            ?.filter { it.phase != MatchPhase.FT && !predictedIds.contains(it.id) }
            .orEmpty()
    }

    val total = userProfile?.totalPredictions ?: 0L
    val correct = userProfile?.correctPredictions ?: 0L
    val accuracyPct = if (total > 0L) ((correct * 100) / total).toInt() else 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchObsidian),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
        ) {
            item {
                // Hero Tipster Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PitchSurfaceElevated, PitchSurface)
                            )
                        )
                        .border(1.dp, PitchEmerald.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.img_prediction_trophy),
                                contentDescription = "Prediction league trophy",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PREDICTION HUB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PitchEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = userProfile?.displayName ?: "Tipster",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = ChalkWhite
                                )
                                Text(
                                    text = "${userProfile?.accuracyPoints ?: 0} pts • ${userProfile?.adBoostTokens ?: 0}x 2x Boosts",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MatchdayGold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatPillBox("ACCURACY", "$accuracyPct%", PitchEmerald, Modifier.weight(1f))
                            StatPillBox("STREAK", "${userProfile?.currentStreak ?: 0}🔥", MatchdayGold, Modifier.weight(1f))
                            StatPillBox("PICKS", "${predictions.size}", ChalkWhite, Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                AdMobManager.showRewardedAd(
                                    activity = activity,
                                    onRewardEarned = onWatchRewardedAd,
                                    onUnavailable = onNotifyBanner
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MatchdayGold,
                                contentColor = PitchObsidian
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("hub_watch_reward_ad_btn")
                        ) {
                            Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Watch Ad for +1 2x Points Boost",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedFilter == 0,
                        onClick = { selectedFilter = 0 },
                        leadingIcon = { Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Quick Picks (${upcomingMatches.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PitchEmerald,
                            selectedLabelColor = PitchObsidian,
                            containerColor = PitchSurface,
                            labelColor = SlateMuted
                        ),
                        modifier = Modifier.testTag("tab_quick_picks")
                    )
                    FilterChip(
                        selected = selectedFilter == 1,
                        onClick = { selectedFilter = 1 },
                        leadingIcon = { Icon(Icons.Rounded.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("My Picks (${predictions.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PitchEmerald,
                            selectedLabelColor = PitchObsidian,
                            containerColor = PitchSurface,
                            labelColor = SlateMuted
                        ),
                        modifier = Modifier.testTag("tab_my_picks")
                    )
                }
            }

            if (selectedFilter == 0) {
                if (upcomingMatches.isEmpty()) {
                    item {
                        EmptyStatePanel(
                            title = "All Caught Up!",
                            subtitle = "You have predicted all upcoming matches on the board, or switch dates in Matches."
                        )
                    }
                } else {
                    items(upcomingMatches.take(20), key = { it.id }) { match ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(PitchSurface)
                                .border(1.dp, PitchBorder, RoundedCornerShape(16.dp))
                                .clickable { onOpenMatchDetail(match) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${match.leagueName} • ${match.clockDisplay}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PitchEmerald
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TeamCrest(match.homeLogoUrl, match.homeShort, 22.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(match.homeTeam, style = MaterialTheme.typography.titleMedium, color = ChalkWhite)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TeamCrest(match.awayLogoUrl, match.awayShort, 22.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(match.awayTeam, style = MaterialTheme.typography.titleMedium, color = ChalkWhite)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PitchEmeraldContainer)
                                    .border(1.dp, PitchEmerald.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "PREDICT",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PitchEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                if (predictions.isEmpty()) {
                    item {
                        EmptyStatePanel(
                            title = "No Predictions Yet",
                            subtitle = "Tap any match in Quick Picks or Matches to lock in your scoreline prediction."
                        )
                    }
                } else {
                    items(predictions, key = { it.id }) { pred ->
                        val statusColor = when (pred.parsedStatus) {
                            PredictionStatus.WON -> PitchEmerald
                            PredictionStatus.LOST -> LiveCrimson
                            PredictionStatus.PENDING -> MatchdayGold
                        }
                        val statusBg = when (pred.parsedStatus) {
                            PredictionStatus.WON -> PitchEmeraldContainer
                            PredictionStatus.LOST -> LiveCrimsonContainer
                            PredictionStatus.PENDING -> MatchdayGoldContainer
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(PitchSurface)
                                .border(1.dp, PitchBorder, RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pred.leagueName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${pred.homeTeam} vs ${pred.awayTeam}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ChalkWhite
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Pick: ${pred.pick} (${pred.predictedHomeScore}-${pred.predictedAwayScore}) • ${pred.parsedConfidence.label}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PitchEmerald
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(statusBg)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (pred.parsedStatus == PredictionStatus.WON) "+${pred.pointsAwarded} PTS" else pred.status,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
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

@Composable
private fun StatPillBox(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PitchObsidian.copy(alpha = 0.7f))
            .border(1.dp, PitchBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = SlateMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = valueColor,
            fontWeight = FontWeight.Bold
        )
    }
}
