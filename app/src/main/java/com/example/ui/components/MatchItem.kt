package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.data.model.MatchPhase
import com.example.data.model.MatchPrediction
import com.example.data.model.MatchSummary
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.MatchdayGold
import com.example.ui.theme.MatchdayGoldContainer
import com.example.ui.theme.PitchBorder
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchEmeraldContainer
import com.example.ui.theme.PitchSurface
import com.example.ui.theme.PitchSurfaceElevated
import com.example.ui.theme.PitchSurfaceHighlight
import com.example.ui.theme.SlateDim
import com.example.ui.theme.SlateMuted

@Composable
fun MatchItem(
    match: MatchSummary,
    userPrediction: MatchPrediction? = null,
    isSubscribed: Boolean = false,
    onMatchClick: () -> Unit,
    onToggleSubscribe: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bellTint by animateColorAsState(
        targetValue = if (isSubscribed) MatchdayGold else SlateMuted,
        label = "match_item_bell_tint"
    )
    val borderColor = if (match.phase == MatchPhase.LIVE) {
        LiveCrimson.copy(alpha = 0.4f)
    } else {
        PitchBorder
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PitchSurface)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onMatchClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("match_item_${match.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MatchStatusIndicator(
                phase = match.phase,
                clockDisplay = match.clockDisplay,
                kickoffLocalTime = match.kickoffLocalTime
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (userPrediction != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(PitchEmeraldContainer)
                            .border(1.dp, PitchEmerald.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "PICK ${userPrediction.predictedHomeScore}-${userPrediction.predictedAwayScore}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PitchEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                IconButton(
                    onClick = onToggleSubscribe,
                    modifier = Modifier
                        .size(36.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("match_item_bell_${match.id}")
                ) {
                    Icon(
                        imageVector = if (isSubscribed) {
                            Icons.Rounded.NotificationsActive
                        } else {
                            Icons.Outlined.NotificationsNone
                        },
                        contentDescription = if (isSubscribed) {
                            "Mute alerts for ${match.homeTeam} vs ${match.awayTeam}"
                        } else {
                            "Enable alerts for ${match.homeTeam} vs ${match.awayTeam}"
                        },
                        tint = bellTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home team
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = match.homeTeam,
                    style = MaterialTheme.typography.titleMedium,
                    color = ChalkWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TeamCrest(logoUrl = match.homeLogoUrl, shortName = match.homeShort, size = 30.dp)
            }

            // Live score or upcoming VS
            Box(
                modifier = Modifier
                    .width(78.dp)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (match.phase == MatchPhase.PRE) {
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.labelMedium,
                        color = SlateDim
                    )
                } else {
                    Text(
                        text = "${match.homeScore} - ${match.awayScore}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (match.phase == MatchPhase.LIVE) PitchEmerald else ChalkWhite
                    )
                }
            }

            // Away team
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                TeamCrest(logoUrl = match.awayLogoUrl, shortName = match.awayShort, size = 30.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = match.awayTeam,
                    style = MaterialTheme.typography.titleMedium,
                    color = ChalkWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        WinProbabilityBar(
            homeProb = match.homeWinProb,
            drawProb = match.drawProb,
            awayProb = match.awayWinProb,
            compact = true
        )
    }
}

@Composable
fun MatchStatusIndicator(
    phase: MatchPhase,
    clockDisplay: String,
    kickoffLocalTime: String,
    modifier: Modifier = Modifier
) {
    when (phase) {
        MatchPhase.LIVE -> LivePulseBadge(clockDisplay = clockDisplay, modifier = modifier)
        MatchPhase.HT -> MatchStatusBadge(
            text = "HT",
            containerColor = MatchdayGoldContainer,
            contentColor = MatchdayGold,
            modifier = modifier
        )
        MatchPhase.FT -> MatchStatusBadge(
            text = "FT",
            containerColor = PitchSurfaceHighlight,
            contentColor = SlateMuted,
            modifier = modifier
        )
        MatchPhase.PRE -> MatchStatusBadge(
            text = kickoffLocalTime,
            containerColor = PitchSurfaceElevated,
            contentColor = PitchEmerald,
            modifier = modifier
        )
    }
}

@Composable
private fun MatchStatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E0C)
@Composable
private fun MatchItemLivePreview() {
    com.example.ui.theme.PitchPulseTheme {
        MatchItem(
            match = MatchSummary(
                id = "704382",
                leagueCode = "eng.1",
                leagueName = "Premier League",
                homeTeamId = "359",
                awayTeamId = "363",
                homeTeam = "Arsenal",
                awayTeam = "Chelsea",
                homeShort = "ARS",
                awayShort = "CHE",
                homeLogoUrl = null,
                awayLogoUrl = null,
                homeScore = 2,
                awayScore = 1,
                phase = MatchPhase.LIVE,
                clockDisplay = "67'",
                kickoffIso = "2026-11-24T19:45:00Z",
                kickoffLocalTime = "19:45",
                venue = "Emirates Stadium",
                homeRecord = "WWWDW",
                awayRecord = "WDLWW",
                homeWinProb = 54,
                drawProb = 24,
                awayWinProb = 22
            ),
            isSubscribed = true,
            onMatchClick = {},
            onToggleSubscribe = {},
            modifier = Modifier.padding(12.dp)
        )
    }
}
