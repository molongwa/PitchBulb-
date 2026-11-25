package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.MatchPhase
import com.example.data.model.MatchPrediction
import com.example.data.model.MatchSummary
import com.example.data.model.StatComparison
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.LiveCrimsonContainer
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

@Composable
fun TeamCrest(
    logoUrl: String?,
    shortName: String,
    size: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    val badgeBg = PitchSurfaceHighlight
    if (logoUrl.isNullOrBlank()) {
        FallbackCrest(shortName = shortName, size = size, modifier = modifier)
    } else {
        SubcomposeAsyncImage(
            model = logoUrl,
            contentDescription = "$shortName crest",
            contentScale = ContentScale.Fit,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(badgeBg.copy(alpha = 0.35f))
                .padding(2.dp),
            loading = {
                FallbackCrest(shortName = shortName, size = size)
            },
            error = {
                FallbackCrest(shortName = shortName, size = size)
            }
        )
    }
}

@Composable
private fun FallbackCrest(
    shortName: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(PitchSurfaceHighlight)
            .border(1.dp, PitchBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = shortName.take(3).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = ChalkWhite,
            maxLines = 1
        )
    }
}

@Composable
fun LivePulseBadge(
    clockDisplay: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "live_pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(LiveCrimsonContainer)
            .border(1.dp, LiveCrimson.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(alpha)
                .clip(CircleShape)
                .background(LiveCrimson)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = clockDisplay,
            style = MaterialTheme.typography.labelSmall,
            color = LiveCrimson,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MatchRowCard(
    match: MatchSummary,
    userPrediction: MatchPrediction?,
    isSubscribed: Boolean,
    onMatchClick: () -> Unit,
    onToggleSubscribe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bellTint by animateColorAsState(
        targetValue = if (isSubscribed) MatchdayGold else SlateMuted,
        label = "bell_tint"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PitchSurface)
            .border(
                width = 1.dp,
                color = if (match.phase == MatchPhase.LIVE) LiveCrimson.copy(alpha = 0.35f) else PitchBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onMatchClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("match_card_${match.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            when (match.phase) {
                MatchPhase.LIVE -> LivePulseBadge(clockDisplay = match.clockDisplay)
                MatchPhase.HT -> StatusPill(text = "HT", bg = MatchdayGoldContainerCompat, fg = MatchdayGold)
                MatchPhase.FT -> StatusPill(text = "FT", bg = PitchSurfaceHighlight, fg = SlateMuted)
                MatchPhase.PRE -> StatusPill(text = match.kickoffLocalTime, bg = PitchSurfaceElevated, fg = PitchEmerald)
            }

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
                        .testTag("bell_match_${match.id}")
                ) {
                    Icon(
                        imageVector = if (isSubscribed) Icons.Rounded.NotificationsActive else Icons.Outlined.NotificationsNone,
                        contentDescription = if (isSubscribed) "Mute match alerts" else "Enable match alerts",
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
            // Home side
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

            // Center score / VS
            Box(
                modifier = Modifier
                    .width(74.dp)
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

            // Away side
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

private val MatchdayGoldContainerCompat = Color(0xFF3A2C0E)

@Composable
private fun StatusPill(
    text: String,
    bg: Color,
    fg: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun WinProbabilityBar(
    homeProb: Int,
    drawProb: Int,
    awayProb: Int,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val total = (homeProb + drawProb + awayProb).coerceAtLeast(1)
    val hWeight = (homeProb.toFloat() / total).coerceAtLeast(0.08f)
    val dWeight = (drawProb.toFloat() / total).coerceAtLeast(0.08f)
    val aWeight = (awayProb.toFloat() / total).coerceAtLeast(0.08f)

    Column(modifier = modifier.fillMaxWidth()) {
        if (!compact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Home $homeProb%",
                    style = MaterialTheme.typography.labelSmall,
                    color = PitchEmerald
                )
                Text(
                    text = "Draw $drawProb%",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateMuted
                )
                Text(
                    text = "Away $awayProb%",
                    style = MaterialTheme.typography.labelSmall,
                    color = InfoCyan
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 4.dp else 8.dp)
                .clip(RoundedCornerShape(50)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(hWeight)
                    .fillMaxHeight()
                    .background(PitchEmerald)
            )
            Box(
                modifier = Modifier
                    .weight(dWeight)
                    .fillMaxHeight()
                    .background(SlateDim)
            )
            Box(
                modifier = Modifier
                    .weight(aWeight)
                    .fillMaxHeight()
                    .background(InfoCyan)
            )
        }
    }
}

@Composable
fun StatComparisonRow(
    stat: StatComparison,
    modifier: Modifier = Modifier
) {
    val sum = (stat.homeValue + stat.awayValue).coerceAtLeast(1f)
    val homeFrac = (stat.homeValue / sum).coerceIn(0.05f, 0.95f)
    val awayFrac = (stat.awayValue / sum).coerceIn(0.05f, 0.95f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stat.homeDisplay,
                style = MaterialTheme.typography.labelMedium,
                color = if (stat.homeValue >= stat.awayValue) PitchEmerald else ChalkWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stat.label,
                style = MaterialTheme.typography.bodyMedium,
                color = SlateMuted
            )
            Text(
                text = stat.awayDisplay,
                style = MaterialTheme.typography.labelMedium,
                color = if (stat.awayValue > stat.homeValue) InfoCyan else ChalkWhite,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(PitchSurfaceHighlight),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(homeFrac)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp))
                    .background(PitchEmerald)
            )
            Box(
                modifier = Modifier
                    .weight(awayFrac)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp))
                    .background(InfoCyan)
            )
        }
    }
}

@Composable
fun EmptyStatePanel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PitchSurface)
            .border(1.dp, PitchBorder, RoundedCornerShape(20.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(PitchEmeraldContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SportsSoccer,
                contentDescription = null,
                tint = PitchEmerald,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = ChalkWhite,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = SlateMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorStatePanel(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PitchSurface)
            .border(1.dp, LiveCrimson.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.WifiOff,
            contentDescription = null,
            tint = LiveCrimson,
            modifier = Modifier.size(34.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = ChalkWhite,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = PitchEmerald,
                contentColor = PitchObsidian
            ),
            modifier = Modifier.minimumInteractiveComponentSize()
        ) {
            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Retry")
        }
    }
}
