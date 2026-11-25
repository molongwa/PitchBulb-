package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ads.AdMobBannerBar
import com.example.data.model.MatchSubscription
import com.example.data.model.MatchSummary
import com.example.data.model.StandingRow
import com.example.data.model.UserProfile
import com.example.data.model.WORLD_COUNTRIES
import com.example.data.model.WORLD_LEAGUES
import com.example.notifications.NotificationHelper
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.ErrorStatePanel
import com.example.ui.components.TeamCrest
import com.example.ui.components.WorldLeaguesBottomSheet
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.MatchdayGold
import com.example.ui.theme.PitchBorder
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchEmeraldContainer
import com.example.ui.theme.PitchObsidian
import com.example.ui.theme.PitchSurface
import com.example.ui.theme.PitchSurfaceElevated
import com.example.ui.theme.SlateMuted
import com.example.ui.viewmodel.UiState

@Composable
fun LeaguesScreen(
    selectedLeagueCode: String,
    standingsState: UiState<List<StandingRow>>,
    onSelectLeague: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenTeamDetail: (teamId: String, leagueCode: String, teamName: String, logoUrl: String?) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val activeLeague = remember(selectedLeagueCode) {
        WORLD_LEAGUES.firstOrNull { it.code == selectedLeagueCode } ?: WORLD_LEAGUES.first()
    }
    var selectedCountry by remember(activeLeague.country) { mutableStateOf(activeLeague.country) }
    var showWorldSheet by remember { mutableStateOf(false) }

    val countryLeagues = remember(selectedCountry) {
        WORLD_LEAGUES.filter { it.country == selectedCountry }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchObsidian),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PitchEmeraldContainer)
                            .border(1.dp, PitchEmerald.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Leaderboard,
                            contentDescription = null,
                            tint = PitchEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("World Leagues", style = MaterialTheme.typography.headlineSmall, color = ChalkWhite)
                        Text(
                            text = "${activeLeague.country} • ${activeLeague.name} • Tap club for history",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showWorldSheet = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("leagues_world_picker_btn")
                    ) {
                        Icon(Icons.Rounded.Public, contentDescription = "All world countries", tint = PitchEmerald)
                    }
                    IconButton(onClick = onRetry, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh standings", tint = PitchEmerald)
                    }
                }
            }

            // Countries strip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(WORLD_COUNTRIES, key = { it }) { country ->
                    val selected = country == selectedCountry
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedCountry = country
                            val firstInCountry = WORLD_LEAGUES.firstOrNull { it.country == country }
                            if (firstInCountry != null && firstInCountry.code != selectedLeagueCode) {
                                onSelectLeague(firstInCountry.code)
                            }
                        },
                        label = { Text(country) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PitchEmeraldContainer,
                            selectedLabelColor = PitchEmerald,
                            containerColor = PitchSurface,
                            labelColor = SlateMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = PitchBorder,
                            selectedBorderColor = PitchEmerald
                        ),
                        modifier = Modifier.testTag("leagues_country_chip_$country")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Leagues within selected country
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(countryLeagues, key = { it.code }) { league ->
                    val selected = league.code == selectedLeagueCode
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectLeague(league.code) },
                        label = { Text(league.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PitchEmerald,
                            selectedLabelColor = PitchObsidian,
                            containerColor = PitchSurface,
                            labelColor = SlateMuted
                        ),
                        modifier = Modifier.testTag("leagues_comp_chip_${league.code}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (standingsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PitchEmerald)
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        ErrorStatePanel(message = standingsState.message, onRetry = onRetry)
                    }
                }
                is UiState.Success -> {
                    if (standingsState.data.isEmpty()) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            EmptyStatePanel(
                                title = "Table Unavailable",
                                subtitle = "Standings for this competition are updating."
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("#", style = MaterialTheme.typography.labelSmall, color = SlateMuted, modifier = Modifier.width(24.dp))
                                    Text("CLUB", style = MaterialTheme.typography.labelSmall, color = SlateMuted, modifier = Modifier.weight(1f))
                                    Text("P", style = MaterialTheme.typography.labelSmall, color = SlateMuted, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                                    Text("W", style = MaterialTheme.typography.labelSmall, color = SlateMuted, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                                    Text("GD", style = MaterialTheme.typography.labelSmall, color = SlateMuted, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                                    Text("PTS", style = MaterialTheme.typography.labelSmall, color = PitchEmerald, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                                }
                            }
                            items(standingsState.data, key = { "${it.rank}_${it.teamName}" }) { row ->
                                val zoneColor = when {
                                    row.rank <= 4 -> PitchEmerald
                                    row.rank <= 6 -> InfoCyan
                                    row.rank >= 18 -> LiveCrimson
                                    else -> PitchBorder
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PitchSurface)
                                        .border(1.dp, PitchBorder, RoundedCornerShape(12.dp))
                                        .clickable(enabled = row.teamId.isNotBlank()) {
                                            onOpenTeamDetail(row.teamId, selectedLeagueCode, row.teamName, row.logoUrl)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("standing_team_${row.teamId.ifBlank { row.teamName }}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(20.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(zoneColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = row.rank.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ChalkWhite,
                                        modifier = Modifier.width(20.dp)
                                    )
                                    TeamCrest(row.logoUrl, row.teamShort, 22.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = row.teamName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ChalkWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(row.played.toString(), style = MaterialTheme.typography.labelMedium, color = SlateMuted, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                                    Text(row.won.toString(), style = MaterialTheme.typography.labelMedium, color = SlateMuted, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                                    Text(
                                        text = if (row.goalDiff > 0) "+${row.goalDiff}" else row.goalDiff.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SlateMuted,
                                        modifier = Modifier.width(34.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = row.points.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PitchEmerald,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(32.dp),
                                        textAlign = TextAlign.End
                                    )
                                    if (row.teamId.isNotBlank()) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = "Open ${row.teamName} history",
                                            tint = SlateMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                AdMobBannerBar()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWorldSheet) {
        WorldLeaguesBottomSheet(
            selectedCountry = selectedCountry,
            selectedLeagueCode = selectedLeagueCode,
            onSelectCountryAndLeague = { country, leagueCode ->
                if (country != null) selectedCountry = country
                val targetCode = leagueCode ?: WORLD_LEAGUES.firstOrNull { it.country == country }?.code
                if (targetCode != null) {
                    onSelectLeague(targetCode)
                }
            },
            onDismiss = { showWorldSheet = false }
        )
    }
}

@Composable
fun AlertsAndSettingsScreen(
    userProfile: UserProfile?,
    subscriptions: List<MatchSubscription>,
    onUpdatePrefs: (Boolean, Boolean, Boolean) -> Unit,
    onRemoveSubscription: (MatchSubscription) -> Unit,
    onTestGoalAlert: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            NotificationHelper.scheduleBackgroundMatchAlerts(context)
        }
    }

    val notificationsEnabled = userProfile?.notificationsEnabled ?: true
    val goalAlertsEnabled = userProfile?.goalAlertsEnabled ?: true
    val kickoffAlertsEnabled = userProfile?.kickoffAlertsEnabled ?: true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchObsidian),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PitchEmeraldContainer)
                            .border(1.dp, PitchEmerald.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = PitchEmerald, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Alerts & Monetization", style = MaterialTheme.typography.headlineSmall, color = ChalkWhite)
                        Text("Push notifications, AdMob status & account", style = MaterialTheme.typography.bodyMedium, color = SlateMuted)
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(PitchSurface)
                        .border(1.dp, PitchBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Text("PUSH NOTIFICATION SETTINGS", style = MaterialTheme.typography.labelMedium, color = PitchEmerald)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmerald, contentColor = PitchObsidian),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("request_notif_permission_btn")
                        ) {
                            Text("Allow Device Notifications", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    PrefToggleRow("All Match Alerts", notificationsEnabled) {
                        onUpdatePrefs(it, goalAlertsEnabled, kickoffAlertsEnabled)
                    }
                    PrefToggleRow("Live Goal Alerts", goalAlertsEnabled) {
                        onUpdatePrefs(notificationsEnabled, it, kickoffAlertsEnabled)
                    }
                    PrefToggleRow("Kickoff & Full-Time", kickoffAlertsEnabled) {
                        onUpdatePrefs(notificationsEnabled, goalAlertsEnabled, it)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onTestGoalAlert()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("test_goal_alert_btn")
                    ) {
                        Icon(Icons.Rounded.Send, contentDescription = null, tint = PitchEmerald, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Test Goal Notification", color = PitchEmerald)
                    }
                }
            }

            item {
                Text(
                    text = "FOLLOWED MATCHES (${subscriptions.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = PitchEmerald
                )
            }

            if (subscriptions.isEmpty()) {
                item {
                    EmptyStatePanel(
                        title = "No Followed Matches",
                        subtitle = "Tap the bell icon on any fixture in Matches to get live goal and full-time alerts."
                    )
                }
            } else {
                items(subscriptions, key = { it.id }) { sub ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PitchSurface)
                            .border(1.dp, PitchBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${sub.homeTeam} vs ${sub.awayTeam}", style = MaterialTheme.typography.titleMedium, color = ChalkWhite)
                            Text(
                                text = "Score: ${sub.lastKnownHomeScore}-${sub.lastKnownAwayScore} • Status: ${sub.lastKnownStatus}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlateMuted
                            )
                        }
                        IconButton(
                            onClick = { onRemoveSubscription(sub) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Unfollow match", tint = LiveCrimson)
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(PitchSurfaceElevated)
                        .border(1.dp, MatchdayGold.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.MonetizationOn, contentDescription = null, tint = MatchdayGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ADMOB & PLAY STORE STATUS", style = MaterialTheme.typography.labelMedium, color = MatchdayGold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Active AdMob App ID: ${context.getString(R.string.admob_app_id)}\nBanner, Interstitial, and Rewarded Ads are wired with Google test units. Swap your live AdMob IDs in strings.xml and enable FCM in Firebase Console before Google Play upload.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateMuted
                    )
                }
            }

            item {
                AdMobBannerBar()
            }

            item {
                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LiveCrimson.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = LiveCrimson)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", color = LiveCrimson, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PrefToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = ChalkWhite)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PitchObsidian,
                checkedTrackColor = PitchEmerald
            )
        )
    }
}
