package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.PlayerHistoryData
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.MatchdayGold
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerHistoryBottomSheet(
    playerHistoryState: UiState<PlayerHistoryData>?,
    onDismiss: () -> Unit
) {
    if (playerHistoryState == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PitchSurface,
        contentColor = ChalkWhite,
        modifier = Modifier.testTag("player_history_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp)
        ) {
            when (playerHistoryState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PitchEmerald)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Loading player career & stats...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlateMuted
                            )
                        }
                    }
                }

                is UiState.Error -> {
                    EmptyStatePanel(
                        title = "Player History Unavailable",
                        subtitle = playerHistoryState.message
                    )
                }

                is UiState.Success -> {
                    val data = playerHistoryState.data
                    val player = data.player

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerAvatar(headshotUrl = player.headshotUrl, name = player.name, size = 56)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = player.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = ChalkWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = data.bioSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PitchEmerald
                            )
                            val physical = listOfNotNull(
                                player.jersey.takeIf { it != "-" }?.let { "#$it" },
                                data.heightDisplay.takeIf { it.isNotBlank() },
                                data.weightDisplay.takeIf { it.isNotBlank() }
                            ).joinToString(" • ")
                            if (physical.isNotBlank()) {
                                Text(
                                    text = physical,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateMuted
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close player history", tint = SlateMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Current season quick summary strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PitchObsidian)
                            .border(1.dp, PitchBorder, RoundedCornerShape(14.dp))
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickStatMetric("APPS", player.appearances.toString(), ChalkWhite)
                        QuickStatMetric("GOALS", player.goals.toString(), PitchEmerald)
                        QuickStatMetric("ASSISTS", player.assists.toString(), InfoCyan)
                        QuickStatMetric("YC", player.yellowCards.toString(), MatchdayGold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    ) {
                        item {
                            Text(
                                text = "CAREER & SEASON HISTORY",
                                style = MaterialTheme.typography.labelMedium,
                                color = PitchEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (data.seasonHistory.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(PitchSurfaceElevated)
                                        .border(1.dp, PitchBorder, RoundedCornerShape(14.dp))
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = "Current Season Telemetry",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ChalkWhite
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${player.appearances} appearances • ${player.goals} goals • ${player.assists} assists • ${player.minutesPlayed} mins played",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SlateMuted
                                    )
                                }
                            }
                        } else {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("SEASON / CLUB", style = MaterialTheme.typography.labelSmall, color = SlateDim, modifier = Modifier.weight(1f))
                                    Text("APP", style = MaterialTheme.typography.labelSmall, color = SlateDim, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                                    Text("G", style = MaterialTheme.typography.labelSmall, color = PitchEmerald, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                                    Text("A", style = MaterialTheme.typography.labelSmall, color = InfoCyan, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                                    Text("MIN", style = MaterialTheme.typography.labelSmall, color = SlateDim, modifier = Modifier.width(42.dp), textAlign = TextAlign.End)
                                }
                            }
                            items(data.seasonHistory) { row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PitchSurfaceElevated)
                                        .border(1.dp, PitchBorder, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = row.seasonLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = ChalkWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = listOf(row.teamName, row.leagueName).filter { it.isNotBlank() }.joinToString(" • "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SlateMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(row.appearances.toString(), style = MaterialTheme.typography.labelMedium, color = ChalkWhite, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                                    Text(row.goals.toString(), style = MaterialTheme.typography.labelMedium, color = PitchEmerald, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                                    Text(row.assists.toString(), style = MaterialTheme.typography.labelMedium, color = InfoCyan, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                                    Text(row.minutes.toString(), style = MaterialTheme.typography.labelSmall, color = SlateMuted, modifier = Modifier.width(42.dp), textAlign = TextAlign.End)
                                }
                            }
                        }

                        if (data.recentAppearances.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "RECENT APPEARANCES",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PitchEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(data.recentAppearances) { matchLog ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PitchObsidian)
                                        .border(1.dp, PitchBorder, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = matchLog,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ChalkWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerAvatar(
    headshotUrl: String?,
    name: String,
    size: Int = 36
) {
    if (headshotUrl.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(PitchEmeraldContainer)
                .border(1.dp, PitchBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = name,
                tint = PitchEmerald,
                modifier = Modifier.size((size * 0.55f).dp)
            )
        }
    } else {
        SubcomposeAsyncImage(
            model = headshotUrl,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(PitchSurfaceHighlight)
                .border(1.dp, PitchBorder, CircleShape),
            loading = {
                Box(
                    modifier = Modifier
                        .size(size.dp)
                        .background(PitchSurfaceHighlight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = SlateMuted)
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .size(size.dp)
                        .background(PitchSurfaceHighlight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = SlateMuted)
                }
            }
        )
    }
}

@Composable
private fun QuickStatMetric(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = accent,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SlateMuted
        )
    }
}
