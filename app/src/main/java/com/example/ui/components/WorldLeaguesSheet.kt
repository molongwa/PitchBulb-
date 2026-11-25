package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.WORLD_LEAGUES
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.PitchBorder
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchEmeraldContainer
import com.example.ui.theme.PitchObsidian
import com.example.ui.theme.PitchSurface
import com.example.ui.theme.PitchSurfaceElevated
import com.example.ui.theme.SlateMuted

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorldLeaguesBottomSheet(
    selectedCountry: String?,
    selectedLeagueCode: String?,
    onSelectCountryAndLeague: (country: String?, leagueCode: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    val groupedByCountry = remember(query) {
        val q = query.trim().lowercase()
        WORLD_LEAGUES
            .filter { league ->
                q.isEmpty() ||
                    league.country.lowercase().contains(q) ||
                    league.name.lowercase().contains(q) ||
                    league.region.lowercase().contains(q)
            }
            .groupBy { it.country }
            .entries
            .toList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PitchSurface,
        contentColor = ChalkWhite,
        modifier = Modifier.testTag("world_leagues_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Public, contentDescription = null, tint = PitchEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "World Leagues by Country",
                            style = MaterialTheme.typography.titleLarge,
                            color = ChalkWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${WORLD_LEAGUES.size} competitions across ${WORLD_LEAGUES.map { it.country }.distinct().size} countries & regions",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close world leagues", tint = SlateMuted)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search country or competition...", color = SlateMuted) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = SlateMuted) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PitchEmerald,
                    unfocusedBorderColor = PitchBorder,
                    focusedTextColor = ChalkWhite,
                    unfocusedTextColor = ChalkWhite,
                    focusedContainerColor = PitchObsidian,
                    unfocusedContainerColor = PitchObsidian
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("world_leagues_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick reset to All Featured World Matches
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedCountry == null && selectedLeagueCode == null) PitchEmeraldContainer else PitchSurfaceElevated)
                    .border(
                        1.dp,
                        if (selectedCountry == null && selectedLeagueCode == null) PitchEmerald else PitchBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        onSelectCountryAndLeague(null, null)
                        onDismiss()
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "All Featured Global Leagues",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selectedCountry == null && selectedLeagueCode == null) PitchEmerald else ChalkWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                items(groupedByCountry, key = { it.key }) { (country, leagues) ->
                    val countrySelected = selectedCountry == country && selectedLeagueCode == null
                    val countryCode = leagues.firstOrNull()?.countryCode ?: "WLD"
                    val region = leagues.firstOrNull()?.region ?: "World"

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PitchObsidian)
                            .border(1.dp, PitchBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectCountryAndLeague(country, null)
                                    onDismiss()
                                }
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (countrySelected) PitchEmerald else PitchSurfaceElevated)
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = countryCode,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (countrySelected) PitchObsidian else PitchEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = country,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (countrySelected) PitchEmerald else ChalkWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = region,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateMuted
                                    )
                                }
                            }
                            Text(
                                text = "All (${leagues.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = PitchEmerald
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            leagues.forEach { league ->
                                val selected = selectedLeagueCode == league.code
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        onSelectCountryAndLeague(country, league.code)
                                        onDismiss()
                                    },
                                    label = { Text(league.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PitchEmerald,
                                        selectedLabelColor = PitchObsidian,
                                        containerColor = PitchSurfaceElevated,
                                        labelColor = ChalkWhite
                                    ),
                                    modifier = Modifier.testTag("world_league_chip_${league.code}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
