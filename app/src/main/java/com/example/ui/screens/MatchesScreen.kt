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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ads.AdMobBannerBar
import com.example.data.model.MatchPhase
import com.example.data.model.MatchPrediction
import com.example.data.model.MatchSubscription
import com.example.data.model.MatchSummary
import com.example.data.model.SUPPORTED_LEAGUES
import com.example.data.model.WORLD_COUNTRIES
import com.example.data.model.WORLD_LEAGUES
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.ErrorStatePanel
import com.example.ui.components.MatchItem
import com.example.ui.components.WorldLeaguesBottomSheet
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.LiveCrimsonContainer
import com.example.ui.theme.MatchdayGold
import com.example.ui.theme.PitchBorder
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchEmeraldContainer
import com.example.ui.theme.PitchObsidian
import com.example.ui.theme.PitchSurface
import com.example.ui.theme.PitchSurfaceElevated
import com.example.ui.theme.SlateMuted
import com.example.ui.viewmodel.DateChipOption
import com.example.ui.viewmodel.UiState

@Composable
fun MatchesScreen(
    matchesState: UiState<List<MatchSummary>>,
    dateOptions: List<DateChipOption>,
    selectedDateOffset: Int,
    liveOnlyFilter: Boolean,
    selectedCountryFilter: String?,
    selectedLeagueFilter: String?,
    searchQuery: String,
    predictions: List<MatchPrediction>,
    subscriptions: List<MatchSubscription>,
    userPoints: Long,
    onSelectDate: (Int) -> Unit,
    onToggleLiveOnly: () -> Unit,
    onSelectCountry: (String?) -> Unit,
    onSelectLeague: (String?) -> Unit,
    onSelectCountryAndLeague: (String?, String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenMatchDetail: (MatchSummary) -> Unit,
    onToggleSubscription: (MatchSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val predByMatchId = remember(predictions) { predictions.associateBy { it.matchId } }
    val subscribedIds = remember(subscriptions) { subscriptions.map { it.matchId }.toSet() }
    var showWorldSheet by remember { mutableStateOf(false) }

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
            // Top FotMob-style Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
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
                            imageVector = Icons.Rounded.SportsSoccer,
                            contentDescription = null,
                            tint = PitchEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PitchBulb",
                            style = MaterialTheme.typography.headlineSmall,
                            color = ChalkWhite
                        )
                        Text(
                            text = "Live Scores & Matchday",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateMuted
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(PitchSurfaceElevated)
                            .border(1.dp, MatchdayGold.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = MatchdayGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$userPoints pts",
                            style = MaterialTheme.typography.labelMedium,
                            color = MatchdayGold
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("refresh_matches_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh live scores",
                            tint = PitchEmerald
                        )
                    }
                }
            }

            // Always-visible search bar to filter live and scheduled matches by team name
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_matches_placeholder),
                        color = SlateMuted,
                        maxLines = 1
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.search_icon_desc),
                        tint = if (searchQuery.isNotEmpty()) PitchEmerald else SlateMuted
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onSearchQueryChange("")
                                focusManager.clearFocus()
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.action_clear_search),
                                tint = ChalkWhite
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PitchEmerald,
                    unfocusedBorderColor = PitchBorder,
                    focusedTextColor = ChalkWhite,
                    unfocusedTextColor = ChalkWhite,
                    cursorColor = PitchEmerald,
                    focusedContainerColor = PitchSurface,
                    unfocusedContainerColor = PitchSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp)
                    .testTag("matches_search_input")
            )

            // Date selector strip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(dateOptions, key = { it.offsetDays }) { option ->
                    val selected = option.offsetDays == selectedDateOffset
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectDate(option.offsetDays) },
                        label = {
                            Text(
                                text = option.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PitchEmerald,
                            selectedLabelColor = PitchObsidian,
                            containerColor = PitchSurface,
                            labelColor = SlateMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = PitchBorder,
                            selectedBorderColor = PitchEmerald
                        ),
                        modifier = Modifier.testTag("date_chip_${option.offsetDays}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Live toggle + World Countries & League filter chips
            val liveCount = (matchesState as? UiState.Success)?.data
                ?.count { it.phase == MatchPhase.LIVE || it.phase == MatchPhase.HT } ?: 0

            val activeLeagues = remember(selectedCountryFilter, selectedLeagueFilter) {
                val base = if (selectedCountryFilter != null) {
                    WORLD_LEAGUES.filter { it.country == selectedCountryFilter }
                } else {
                    SUPPORTED_LEAGUES
                }
                if (selectedLeagueFilter != null && base.none { it.code == selectedLeagueFilter }) {
                    val selectedInfo = WORLD_LEAGUES.firstOrNull { it.code == selectedLeagueFilter }
                    if (selectedInfo != null) listOf(selectedInfo) + base else base
                } else {
                    base
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCountryFilter != null || selectedLeagueFilter != null,
                        onClick = { showWorldSheet = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Public,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = selectedCountryFilter ?: "World (${WORLD_COUNTRIES.size})",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PitchEmerald,
                            selectedLabelColor = PitchObsidian,
                            selectedLeadingIconColor = PitchObsidian,
                            containerColor = PitchSurfaceElevated,
                            labelColor = ChalkWhite,
                            iconColor = PitchEmerald
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedCountryFilter != null || selectedLeagueFilter != null,
                            borderColor = PitchEmerald.copy(alpha = 0.5f),
                            selectedBorderColor = PitchEmerald
                        ),
                        modifier = Modifier.testTag("open_world_leagues_button")
                    )
                }

                item {
                    FilterChip(
                        selected = liveOnlyFilter,
                        onClick = onToggleLiveOnly,
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (liveOnlyFilter) ChalkWhite else LiveCrimson)
                            )
                        },
                        label = {
                            Text("LIVE ($liveCount)", fontWeight = FontWeight.Bold)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LiveCrimson,
                            selectedLabelColor = ChalkWhite,
                            containerColor = LiveCrimsonContainer.copy(alpha = 0.55f),
                            labelColor = LiveCrimson
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = liveOnlyFilter,
                            borderColor = LiveCrimson.copy(alpha = 0.45f),
                            selectedBorderColor = LiveCrimson
                        ),
                        modifier = Modifier.testTag("live_filter_chip")
                    )
                }

                items(activeLeagues, key = { it.code }) { league ->
                    val selected = selectedLeagueFilter == league.code
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectLeague(league.code) },
                        label = { Text(league.shortName) },
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
                        modifier = Modifier.testTag("league_chip_${league.code}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Country strip for rapid global switching
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val allSelected = selectedCountryFilter == null && selectedLeagueFilter == null
                    FilterChip(
                        selected = allSelected,
                        onClick = { onSelectCountryAndLeague(null, null) },
                        label = { Text("All Countries") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PitchEmeraldContainer,
                            selectedLabelColor = PitchEmerald,
                            containerColor = PitchSurface,
                            labelColor = SlateMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = allSelected,
                            borderColor = PitchBorder,
                            selectedBorderColor = PitchEmerald
                        ),
                        modifier = Modifier.testTag("country_chip_all")
                    )
                }
                items(WORLD_COUNTRIES, key = { it }) { country ->
                    val selected = selectedCountryFilter == country
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectCountry(country) },
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
                        modifier = Modifier.testTag("country_chip_$country")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (matchesState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = PitchEmerald)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Fetching live matchday feed...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateMuted
                                )
                            }
                        }
                    }

                    is UiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            ErrorStatePanel(
                                message = matchesState.message,
                                onRetry = onRefresh
                            )
                        }
                    }

                    is UiState.Success -> {
                        val countryLeagueCodes = remember(selectedCountryFilter) {
                            selectedCountryFilter?.let { country ->
                                WORLD_LEAGUES.filter { it.country == country }.map { it.code }.toSet()
                            }
                        }

                        val filtered = remember(
                            matchesState.data,
                            liveOnlyFilter,
                            selectedCountryFilter,
                            selectedLeagueFilter,
                            searchQuery,
                            countryLeagueCodes
                        ) {
                            matchesState.data.filter { match ->
                                val matchesLive = !liveOnlyFilter ||
                                    match.phase == MatchPhase.LIVE ||
                                    match.phase == MatchPhase.HT
                                val matchesLeague = when {
                                    selectedLeagueFilter != null -> match.leagueCode == selectedLeagueFilter
                                    countryLeagueCodes != null -> match.leagueCode in countryLeagueCodes
                                    else -> true
                                }
                                val q = searchQuery.trim().lowercase()
                                val matchesSearch = q.isEmpty() ||
                                    match.homeTeam.lowercase().contains(q) ||
                                    match.awayTeam.lowercase().contains(q) ||
                                    match.homeShort.lowercase().contains(q) ||
                                    match.awayShort.lowercase().contains(q) ||
                                    match.leagueName.lowercase().contains(q)
                                matchesLive && matchesLeague && matchesSearch
                            }
                        }

                        val liveMatches = remember(filtered) {
                            filtered.filter { it.phase == MatchPhase.LIVE || it.phase == MatchPhase.HT }
                        }
                        val nonLiveGroupedByLeague = remember(filtered, liveOnlyFilter) {
                            if (liveOnlyFilter) {
                                emptyMap()
                            } else {
                                filtered
                                    .filterNot { it.phase == MatchPhase.LIVE || it.phase == MatchPhase.HT }
                                    .groupBy { it.leagueName }
                            }
                        }

                        if (filtered.isEmpty()) {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    EmptyStatePanel(
                                        title = when {
                                            searchQuery.isNotBlank() -> "No Matches For \"${searchQuery.trim()}\""
                                            liveOnlyFilter -> "No Live Matches Right Now"
                                            else -> "No Fixtures Found"
                                        },
                                        subtitle = when {
                                            searchQuery.isNotBlank() ->
                                                "No live or scheduled teams matched your search. Clear the search or adjust your filters."
                                            liveOnlyFilter ->
                                                "Tap the LIVE chip again to view upcoming kickoffs and completed results."
                                            else ->
                                                "Try selecting another matchday date or clearing your league filter."
                                        }
                                    )
                                }
                                if (searchQuery.isNotBlank()) {
                                    item {
                                        OutlinedButton(
                                            onClick = {
                                                onSearchQueryChange("")
                                                focusManager.clearFocus()
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = PitchEmerald
                                            ),
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("empty_clear_search_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.action_clear_search))
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = 12.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (liveMatches.isNotEmpty()) {
                                    item(key = "header_playing_now") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp, bottom = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(LiveCrimson)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "PLAYING NOW • LIVE SCORES",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = LiveCrimson,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = "${liveMatches.size} live",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LiveCrimson
                                            )
                                        }
                                    }

                                    items(liveMatches, key = { "live_${it.id}" }) { match ->
                                        MatchItem(
                                            match = match,
                                            userPrediction = predByMatchId[match.id],
                                            isSubscribed = subscribedIds.contains(match.id),
                                            onMatchClick = { onOpenMatchDetail(match) },
                                            onToggleSubscribe = { onToggleSubscription(match) }
                                        )
                                    }
                                }

                                nonLiveGroupedByLeague.forEach { (leagueName, leagueMatches) ->
                                    item(key = "header_$leagueName") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp, bottom = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = leagueName.uppercase(),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = PitchEmerald,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${leagueMatches.size} matches",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SlateMuted
                                            )
                                        }
                                    }

                                    items(leagueMatches, key = { it.id }) { match ->
                                        MatchItem(
                                            match = match,
                                            userPrediction = predByMatchId[match.id],
                                            isSubscribed = subscribedIds.contains(match.id),
                                            onMatchClick = { onOpenMatchDetail(match) },
                                            onToggleSubscribe = { onToggleSubscription(match) }
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

    if (showWorldSheet) {
        WorldLeaguesBottomSheet(
            selectedCountry = selectedCountryFilter,
            selectedLeagueCode = selectedLeagueFilter,
            onSelectCountryAndLeague = onSelectCountryAndLeague,
            onDismiss = { showWorldSheet = false }
        )
    }
}
