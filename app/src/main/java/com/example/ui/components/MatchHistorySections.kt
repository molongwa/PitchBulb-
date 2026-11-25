package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.HeadToHeadMatch
import com.example.data.model.HistoricalLineup
import com.example.data.model.LineupPlayer
import com.example.data.model.PlayerInjury
import com.example.data.model.SquadPlayer
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.LiveCrimsonContainer
import com.example.ui.theme.MatchdayGold
import com.example.ui.theme.MatchdayGoldContainer
import com.example.ui.theme.PitchBorder
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchSurface
import com.example.ui.theme.PitchSurfaceElevated
import com.example.ui.theme.PitchSurfaceHighlight
import com.example.ui.theme.SlateDim
import com.example.ui.theme.SlateMuted

@Composable
fun InteractiveLineupSection(
    teamName: String,
    players: List<LineupPlayer>,
    accent: Color,
    subtitle: String? = null,
    onPlayerClick: (SquadPlayer) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PitchSurface)
            .border(1.dp, PitchBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = teamName.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateMuted
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        players.take(18).forEach { player ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        onPlayerClick(
                            SquadPlayer(
                                id = player.playerId,
                                name = player.name,
                                jersey = player.jersey,
                                position = player.position,
                                headshotUrl = player.headshotUrl
                            )
                        )
                    }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PitchSurfaceHighlight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player.jersey,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ChalkWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (player.starter) "${player.position} • XI" else "${player.position} • SUB",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (player.starter) PitchEmerald else SlateMuted
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "View player history",
                    tint = SlateDim,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun HistoricalLineupCard(
    history: HistoricalLineup,
    accent: Color,
    onPlayerClick: (SquadPlayer) -> Unit
) {
    val combined = (history.starters + history.subs).ifEmpty { emptyList() }
    InteractiveLineupSection(
        teamName = "${history.teamName} (${history.opponentName})",
        players = combined,
        accent = accent,
        subtitle = "${history.dateLabel} • ${history.scoreDisplay}",
        onPlayerClick = onPlayerClick
    )
}

@Composable
fun HeadToHeadSummaryCard(
    homeTeam: String,
    awayTeam: String,
    matches: List<HeadToHeadMatch>
) {
    val homeWins = matches.count {
        val hIsHome = it.homeTeam.contains(homeTeam, ignoreCase = true) || homeTeam.contains(it.homeTeam, ignoreCase = true)
        if (hIsHome) it.homeScore > it.awayScore else it.awayScore > it.homeScore
    }
    val awayWins = matches.count {
        val aIsAway = it.awayTeam.contains(awayTeam, ignoreCase = true) || awayTeam.contains(it.awayTeam, ignoreCase = true)
        if (aIsAway) it.awayScore > it.homeScore else it.homeScore > it.awayScore
    }
    val draws = (matches.size - homeWins - awayWins).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PitchSurface)
            .border(1.dp, PitchBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "HEAD-TO-HEAD (${matches.size} MEETINGS)",
            style = MaterialTheme.typography.labelMedium,
            color = PitchEmerald,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            H2HTallyBlock(label = homeTeam, count = homeWins, color = PitchEmerald)
            H2HTallyBlock(label = "Draws", count = draws, color = SlateMuted)
            H2HTallyBlock(label = awayTeam, count = awayWins, color = InfoCyan)
        }
    }
}

@Composable
private fun H2HTallyBlock(
    label: String,
    count: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SlateMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HeadToHeadMatchCard(
    item: HeadToHeadMatch
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PitchSurface)
            .border(1.dp, PitchBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(item.leagueName, style = MaterialTheme.typography.labelSmall, color = PitchEmerald)
            Text(item.dateLabel, style = MaterialTheme.typography.labelSmall, color = SlateMuted)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.homeTeam,
                style = MaterialTheme.typography.bodyLarge,
                color = ChalkWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PitchSurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${item.homeScore} - ${item.awayScore}",
                    style = MaterialTheme.typography.titleMedium,
                    color = ChalkWhite,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = item.awayTeam,
                style = MaterialTheme.typography.bodyLarge,
                color = ChalkWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun InjuryRowCard(
    injury: PlayerInjury,
    onPlayerClick: (SquadPlayer) -> Unit
) {
    val isOut = injury.status.contains("out", ignoreCase = true) ||
        injury.status.contains("susp", ignoreCase = true)
    val badgeBg = if (isOut) LiveCrimsonContainer else MatchdayGoldContainer
    val badgeFg = if (isOut) LiveCrimson else MatchdayGold

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PitchSurface)
            .border(1.dp, PitchBorder, RoundedCornerShape(14.dp))
            .clickable {
                onPlayerClick(
                    SquadPlayer(
                        id = injury.playerId,
                        name = injury.playerName,
                        jersey = "-",
                        position = injury.position,
                        headshotUrl = injury.headshotUrl,
                        statusText = injury.status
                    )
                )
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerAvatar(headshotUrl = injury.headshotUrl, name = injury.playerName, size = 40)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = injury.playerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = ChalkWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (injury.position.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = injury.position,
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateMuted
                    )
                }
            }
            val meta = listOfNotNull(
                injury.teamName.takeIf { it.isNotBlank() },
                injury.injuryType.takeIf { it.isNotBlank() },
                injury.detail.takeIf { it.isNotBlank() && it != injury.injuryType }
            ).joinToString(" • ")
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = SlateMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (injury.returnDate.isNotBlank()) {
                Text(
                    text = "Est. return: ${injury.returnDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = InfoCyan
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(badgeBg)
                .padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MedicalServices,
                contentDescription = null,
                tint = badgeFg,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = injury.status.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = badgeFg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
