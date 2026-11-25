package com.example.data.repository

import com.example.data.model.HeadToHeadMatch
import com.example.data.model.HistoricalLineup
import com.example.data.model.LineupPlayer
import com.example.data.model.MatchDetailData
import com.example.data.model.MatchEventItem
import com.example.data.model.MatchEventType
import com.example.data.model.MatchPhase
import com.example.data.model.MatchSummary
import com.example.data.model.NewsArticle
import com.example.data.model.PlayerHistoryData
import com.example.data.model.PlayerInjury
import com.example.data.model.SUPPORTED_LEAGUES
import com.example.data.model.SquadPlayer
import com.example.data.model.StandingRow
import com.example.data.model.StatComparison
import com.example.data.model.TeamDetailData
import com.example.data.model.WORLD_LEAGUES
import com.example.data.remote.EspnCompetitionDto
import com.example.data.remote.EspnCompetitorDto
import com.example.data.remote.EspnEventDto
import com.example.data.remote.EspnRosterDto
import com.example.data.remote.EspnSoccerApi
import com.example.data.remote.EspnStatusDto
import com.example.data.remote.EspnSummaryResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class SoccerRepository(
    private val api: EspnSoccerApi = createDefaultApi()
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val summaryAdapter by lazy { moshi.adapter(EspnSummaryResponse::class.java) }

    suspend fun fetchMatchesForDate(
        dateParam: String?,
        countryFilter: String? = null,
        leagueFilter: String? = null
    ): Result<List<MatchSummary>> = runCatching {
        val leaguesToFetch = when {
            leagueFilter != null -> {
                val found = WORLD_LEAGUES.firstOrNull { it.code == leagueFilter }
                if (found != null) listOf(found) else SUPPORTED_LEAGUES
            }
            countryFilter != null -> {
                WORLD_LEAGUES.filter { it.country == countryFilter }.ifEmpty { SUPPORTED_LEAGUES }
            }
            else -> SUPPORTED_LEAGUES
        }

        val todayParam = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date())
        val isTodayOrDefault = dateParam == null || dateParam == todayParam
        val extraWorldLeagues = if (leagueFilter == null && countryFilter == null && isTodayOrDefault) {
            val primaryCodes = leaguesToFetch.map { it.code }.toSet()
            WORLD_LEAGUES.filter { it.code !in primaryCodes }
        } else {
            emptyList()
        }

        coroutineScope {
            val primaryDeferred = leaguesToFetch.map { league ->
                async {
                    runCatching {
                        val response = api.getScoreboard(league = league.code, dates = dateParam)
                        response.events.orEmpty().mapNotNull { event ->
                            mapEventToMatch(event, league.code, league.name)
                        }
                    }.getOrElse { emptyList() }
                }
            }
            val extraLiveDeferred = extraWorldLeagues.map { league ->
                async {
                    runCatching {
                        val response = api.getScoreboard(league = league.code, dates = dateParam)
                        response.events.orEmpty().mapNotNull { event ->
                            mapEventToMatch(event, league.code, league.name)
                        }.filter { it.phase == MatchPhase.LIVE || it.phase == MatchPhase.HT }
                    }.getOrElse { emptyList() }
                }
            }
            (primaryDeferred.awaitAll() + extraLiveDeferred.awaitAll())
                .flatten()
                .distinctBy { it.id }
                .sortedWith(
                    compareBy<MatchSummary> { phaseOrder(it.phase) }
                        .thenBy { it.kickoffIso }
                )
        }
    }

    suspend fun fetchMatchById(leagueCode: String, matchId: String): MatchSummary? {
        return runCatching {
            val leagueName = leagueDisplayName(leagueCode)
            val response = api.getScoreboard(league = leagueCode, dates = null)
            response.events.orEmpty()
                .firstOrNull { it.id == matchId }
                ?.let { mapEventToMatch(it, leagueCode, leagueName) }
        }.getOrNull()
    }

    suspend fun fetchMatchDetail(
        leagueCode: String,
        matchId: String,
        fallbackSummary: MatchSummary?
    ): Result<MatchDetailData> = runCatching {
        coroutineScope {
            val rawSummaryDeferred = async { api.getMatchSummaryRaw(league = leagueCode, eventId = matchId).string() }
            val leagueInjuriesDeferred = async {
                runCatching { api.getLeagueInjuriesRaw(leagueCode).string() }.getOrNull()
            }

            val rawSummary = rawSummaryDeferred.await()
            val summary = summaryAdapter.fromJson(rawSummary)
                ?: throw IllegalStateException("Match data unavailable")
            val summaryJson = runCatching { JSONObject(rawSummary) }.getOrNull() ?: JSONObject()

            val leagueName = leagueDisplayName(leagueCode)
            val headerComp = summary.header?.competitions?.firstOrNull()
            val mappedMatch = if (headerComp != null) {
                val syntheticEvent = EspnEventDto(
                    id = summary.header.id ?: matchId,
                    date = headerComp.date ?: fallbackSummary?.kickoffIso,
                    competitions = listOf(headerComp),
                    status = headerComp.status
                )
                mapEventToMatch(syntheticEvent, leagueCode, leagueName) ?: fallbackSummary
            } else {
                fallbackSummary
            } ?: throw IllegalStateException("Match data unavailable")

            val boxTeams = summary.boxscore?.teams.orEmpty()
            val homeBox = boxTeams.firstOrNull { it.team?.id == mappedMatch.homeTeamId } ?: boxTeams.getOrNull(0)
            val awayBox = boxTeams.firstOrNull { it.team?.id == mappedMatch.awayTeamId } ?: boxTeams.getOrNull(1)
            val stats = buildStatComparisons(
                homeStats = homeBox?.statistics.orEmpty().associateBy { it.name.orEmpty() },
                awayStats = awayBox?.statistics.orEmpty().associateBy { it.name.orEmpty() }
            )

            val events = summary.keyEvents.orEmpty().mapNotNull { keyEvent ->
                val clock = keyEvent.clock?.displayValue ?: ""
                val text = keyEvent.shortText ?: keyEvent.text ?: return@mapNotNull null
                val teamName = keyEvent.team?.shortDisplayName ?: keyEvent.team?.displayName ?: ""
                val isHome = keyEvent.team?.id == mappedMatch.homeTeamId
                val typeName = keyEvent.type?.text.orEmpty().lowercase(Locale.US)
                val eventType = when {
                    keyEvent.scoringPlay == true || typeName.contains("goal") -> MatchEventType.GOAL
                    typeName.contains("red") -> MatchEventType.RED_CARD
                    typeName.contains("yellow") -> MatchEventType.YELLOW_CARD
                    typeName.contains("sub") -> MatchEventType.SUBSTITUTION
                    else -> MatchEventType.INFO
                }
                MatchEventItem(
                    minute = if (clock.isNotBlank()) clock else "-",
                    text = text,
                    teamName = teamName,
                    isHome = isHome,
                    type = eventType
                )
            }

            val homeRoster = mapRosterDto(
                summary.rosters.orEmpty()
                    .firstOrNull { it.homeAway == "home" || it.team?.id == mappedMatch.homeTeamId }
            )
            val awayRoster = mapRosterDto(
                summary.rosters.orEmpty()
                    .firstOrNull { it.homeAway == "away" || it.team?.id == mappedMatch.awayTeamId }
            )

            // Fetch team schedules concurrently for historical lineups & fallback H2H
            val homeScheduleDeferred = async {
                fetchScheduleMatchesSafe(leagueCode, mappedMatch.homeTeamId, leagueName)
            }
            val awayScheduleDeferred = async {
                fetchScheduleMatchesSafe(leagueCode, mappedMatch.awayTeamId, leagueName)
            }

            // Injuries from match summary + league injuries filtered to these 2 teams
            val summaryInjuries = EspnJsonParsers.parseInjuriesFromJson(summaryJson)
            val rawLeagueInjuries = leagueInjuriesDeferred.await()
            val leagueFilteredInjuries = if (rawLeagueInjuries != null) {
                runCatching {
                    EspnJsonParsers.parseInjuriesFromJson(
                        root = JSONObject(rawLeagueInjuries),
                        filterTeamIds = setOf(mappedMatch.homeTeamId, mappedMatch.awayTeamId),
                        filterTeamNames = setOf(mappedMatch.homeTeam, mappedMatch.awayTeam)
                    )
                }.getOrElse { emptyList() }
            } else {
                emptyList()
            }
            val combinedInjuries = (summaryInjuries + leagueFilteredInjuries)
                .distinctBy { "${it.teamName.lowercase(Locale.US)}_${it.playerName.lowercase(Locale.US)}" }

            val homeSchedule = homeScheduleDeferred.await()
            val awaySchedule = awayScheduleDeferred.await()

            val summaryH2H = EspnJsonParsers.parseHeadToHeadFromSummary(
                root = summaryJson,
                homeTeamName = mappedMatch.homeTeam,
                awayTeamName = mappedMatch.awayTeam,
                homeTeamId = mappedMatch.homeTeamId,
                awayTeamId = mappedMatch.awayTeamId
            )
            val h2hList = if (summaryH2H.isNotEmpty()) {
                summaryH2H
            } else {
                buildFallbackH2HFromSchedule(
                    schedule = homeSchedule + awaySchedule,
                    homeTeamId = mappedMatch.homeTeamId,
                    awayTeamId = mappedMatch.awayTeamId,
                    currentMatchId = mappedMatch.id
                )
            }

            val homeHistDeferred = async {
                fetchRecentHistoryLineups(
                    leagueCode = leagueCode,
                    teamId = mappedMatch.homeTeamId,
                    teamName = mappedMatch.homeTeam,
                    schedule = homeSchedule,
                    excludeMatchId = mappedMatch.id,
                    limit = 2
                )
            }
            val awayHistDeferred = async {
                fetchRecentHistoryLineups(
                    leagueCode = leagueCode,
                    teamId = mappedMatch.awayTeamId,
                    teamName = mappedMatch.awayTeam,
                    schedule = awaySchedule,
                    excludeMatchId = mappedMatch.id,
                    limit = 2
                )
            }

            val referee = summary.gameInfo?.officials?.firstOrNull()?.displayName ?: "TBA"
            val attendanceVal = summary.gameInfo?.attendance ?: headerComp?.attendance
            val attendance = if (attendanceVal != null && attendanceVal > 0) {
                String.format(Locale.US, "%,d", attendanceVal)
            } else {
                "N/A"
            }

            MatchDetailData(
                match = mappedMatch,
                stats = stats,
                events = events,
                homeLineup = homeRoster,
                awayLineup = awayRoster,
                referee = referee,
                attendance = attendance,
                h2hMatches = h2hList,
                injuries = combinedInjuries,
                homeHistoryLineups = homeHistDeferred.await(),
                awayHistoryLineups = awayHistDeferred.await()
            )
        }
    }

    suspend fun fetchTeamDetail(
        leagueCode: String,
        teamId: String,
        teamNameHint: String = "",
        logoHint: String? = null
    ): Result<TeamDetailData> = runCatching {
        coroutineScope {
            val teamRawDef = async { runCatching { api.getTeamDetailRaw(leagueCode, teamId).string() }.getOrNull() }
            val rosterRawDef = async { runCatching { api.getTeamRosterRaw(leagueCode, teamId).string() }.getOrNull() }
            val scheduleDef = async { fetchScheduleMatchesSafe(leagueCode, teamId, leagueDisplayName(leagueCode)) }
            val injuriesRawDef = async { runCatching { api.getLeagueInjuriesRaw(leagueCode).string() }.getOrNull() }

            val teamJson = teamRawDef.await()?.let { runCatching { JSONObject(it) }.getOrNull() }
            val teamNode = teamJson?.optJSONObject("team")
            val resolvedName = teamNode?.optString("displayName")
                ?.ifBlank { teamNode.optString("shortDisplayName") }
                ?.ifBlank { teamNameHint }
                ?: teamNameHint.ifBlank { "Club" }
            val resolvedShort = teamNode?.optString("abbreviation")
                ?.ifBlank { resolvedName.take(3).uppercase(Locale.US) }
                ?: resolvedName.take(3).uppercase(Locale.US)
            val resolvedLogo = teamNode?.optJSONArray("logos")?.optJSONObject(0)?.optString("href")
                ?.takeIf { it.isNotBlank() }
                ?: logoHint
            val recordSummary = teamNode?.optJSONObject("record")?.optJSONArray("items")
                ?.optJSONObject(0)?.optString("summary")
                ?.ifBlank { "-" } ?: "-"
            val venue = teamNode?.optJSONObject("franchise")?.optJSONObject("venue")?.optString("fullName")
                ?.ifBlank { "Home Stadium" } ?: "Home Stadium"

            val squad = rosterRawDef.await()?.let { raw ->
                runCatching { EspnJsonParsers.parseRosterFromJson(JSONObject(raw)) }.getOrElse { emptyList() }
            }.orEmpty()

            val injuries = injuriesRawDef.await()?.let { raw ->
                runCatching {
                    EspnJsonParsers.parseInjuriesFromJson(
                        root = JSONObject(raw),
                        filterTeamIds = setOf(teamId),
                        filterTeamNames = setOf(resolvedName, teamNameHint)
                    )
                }.getOrElse { emptyList() }
            }.orEmpty()

            val schedule = scheduleDef.await()
            val historyLineups = fetchRecentHistoryLineups(
                leagueCode = leagueCode,
                teamId = teamId,
                teamName = resolvedName,
                schedule = schedule,
                excludeMatchId = "",
                limit = 3
            )

            TeamDetailData(
                teamId = teamId,
                leagueCode = leagueCode,
                teamName = resolvedName,
                teamShort = resolvedShort,
                logoUrl = resolvedLogo,
                recordSummary = recordSummary,
                venue = venue,
                squad = squad,
                injuries = injuries,
                recentMatches = schedule.sortedByDescending { it.kickoffIso },
                historyLineups = historyLineups
            )
        }
    }

    suspend fun fetchPlayerHistory(
        leagueCode: String,
        player: SquadPlayer
    ): Result<PlayerHistoryData> = runCatching {
        val urls = listOf(
            "https://site.web.api.espn.com/apis/common/v3/sports/soccer/$leagueCode/athletes/${player.id}/overview",
            "https://site.web.api.espn.com/apis/common/v3/sports/soccer/athletes/${player.id}/overview"
        )
        for (url in urls) {
            if (player.id.isBlank()) break
            val raw = runCatching { api.getRawUrl(url).string() }.getOrNull() ?: continue
            val parsed = runCatching {
                EspnJsonParsers.parsePlayerHistoryFromOverview(JSONObject(raw), player)
            }.getOrNull()
            if (parsed != null && (parsed.seasonHistory.isNotEmpty() || parsed.recentAppearances.isNotEmpty() || parsed.heightDisplay.isNotBlank())) {
                return@runCatching parsed
            }
        }
        EspnJsonParsers.parsePlayerHistoryFromOverview(JSONObject(), player)
    }

    private suspend fun fetchScheduleMatchesSafe(
        leagueCode: String,
        teamId: String,
        leagueName: String
    ): List<MatchSummary> {
        if (teamId.isBlank() || teamId == "home" || teamId == "away") return emptyList()
        return runCatching {
            val raw = api.getTeamScheduleRaw(leagueCode, teamId).string()
            EspnJsonParsers.parseTeamScheduleMatches(JSONObject(raw), leagueCode, leagueName)
        }.getOrElse { emptyList() }
    }

    private suspend fun fetchRecentHistoryLineups(
        leagueCode: String,
        teamId: String,
        teamName: String,
        schedule: List<MatchSummary>,
        excludeMatchId: String,
        limit: Int
    ): List<HistoricalLineup> = coroutineScope {
        val completed = schedule
            .filter { it.phase == MatchPhase.FT && it.id != excludeMatchId }
            .sortedByDescending { it.kickoffIso }
            .take(limit)

        completed.map { pastMatch ->
            async {
                runCatching {
                    val summary = api.getMatchSummary(pastMatch.leagueCode.ifBlank { leagueCode }, pastMatch.id)
                    val roster = summary.rosters.orEmpty().firstOrNull {
                        it.team?.id == teamId ||
                            it.team?.shortDisplayName.equals(teamName, ignoreCase = true) ||
                            it.team?.displayName.equals(teamName, ignoreCase = true)
                    } ?: return@runCatching null
                    val players = mapRosterDto(roster)
                    if (players.isEmpty()) return@runCatching null
                    val isHome = pastMatch.homeTeamId == teamId || pastMatch.homeTeam.equals(teamName, ignoreCase = true)
                    val opponent = if (isHome) pastMatch.awayTeam else pastMatch.homeTeam
                    HistoricalLineup(
                        matchId = pastMatch.id,
                        dateLabel = pastMatch.kickoffLocalTime,
                        opponentName = if (isHome) "vs $opponent" else "@ $opponent",
                        scoreDisplay = "${pastMatch.homeScore} - ${pastMatch.awayScore}",
                        teamName = teamName,
                        starters = players.filter { it.starter },
                        subs = players.filterNot { it.starter }
                    )
                }.getOrNull()
            }
        }.awaitAll().filterNotNull()
    }

    private fun buildFallbackH2HFromSchedule(
        schedule: List<MatchSummary>,
        homeTeamId: String,
        awayTeamId: String,
        currentMatchId: String
    ): List<HeadToHeadMatch> {
        return schedule
            .filter {
                it.id != currentMatchId &&
                    it.phase == MatchPhase.FT &&
                    ((it.homeTeamId == homeTeamId && it.awayTeamId == awayTeamId) ||
                        (it.homeTeamId == awayTeamId && it.awayTeamId == homeTeamId))
            }
            .distinctBy { it.id }
            .sortedByDescending { it.kickoffIso }
            .map { m ->
                HeadToHeadMatch(
                    id = m.id,
                    dateLabel = m.kickoffLocalTime,
                    leagueName = m.leagueName,
                    homeTeam = m.homeTeam,
                    awayTeam = m.awayTeam,
                    homeScore = m.homeScore,
                    awayScore = m.awayScore,
                    homeLogoUrl = m.homeLogoUrl,
                    awayLogoUrl = m.awayLogoUrl
                )
            }
    }

    private fun mapRosterDto(rosterDto: EspnRosterDto?): List<LineupPlayer> {
        return rosterDto?.roster.orEmpty().map { entry ->
            val pid = entry.athlete?.id.orEmpty()
            val headshot = entry.athlete?.headshot?.url ?: EspnJsonParsers.espnHeadshotUrl(pid)
            LineupPlayer(
                name = entry.athlete?.shortName ?: entry.athlete?.displayName ?: "Player",
                jersey = entry.jersey ?: "-",
                position = entry.position?.abbreviation ?: entry.position?.displayName ?: "",
                starter = entry.starter == true,
                playerId = pid,
                headshotUrl = headshot
            )
        }
    }

    suspend fun fetchNews(): Result<List<NewsArticle>> = runCatching {
        coroutineScope {
            val leaguesToFetch = listOf(
                "eng.1" to "Premier League",
                "uefa.champions" to "Champions League",
                "esp.1" to "La Liga",
                "ita.1" to "Serie A",
                "ger.1" to "Bundesliga"
            )
            val deferred = leaguesToFetch.map { (code, tag) ->
                async {
                    runCatching {
                        api.getNews(code).articles.orEmpty().mapNotNull { article ->
                            val headline = article.headline?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            NewsArticle(
                                id = (article.id ?: headline.hashCode().toLong()).toString(),
                                title = headline,
                                summary = article.description ?: "Tap to read the full match report and tactical breakdown.",
                                imageUrl = article.images?.firstOrNull()?.url,
                                source = article.byline?.takeIf { it.isNotBlank() } ?: "ESPN FC",
                                publishedLabel = formatRelativeDate(article.published),
                                articleUrl = article.links?.web?.href ?: "https://www.espn.com/soccer/",
                                leagueTag = tag
                            )
                        }
                    }.getOrElse { emptyList() }
                }
            }
            deferred.awaitAll()
                .flatten()
                .distinctBy { it.title }
                .take(28)
        }
    }

    suspend fun fetchStandings(leagueCode: String): Result<List<StandingRow>> = runCatching {
        val url = "https://site.api.espn.com/apis/v2/sports/soccer/$leagueCode/standings"
        val response = api.getStandings(url)
        val entries = response.children.orEmpty()
            .firstOrNull()
            ?.standings
            ?.entries
            .orEmpty()

        entries.mapIndexed { index, entry ->
            val statsMap = entry.stats.orEmpty().associateBy { it.name.orEmpty() }
            fun statInt(key: String, fallback: Int = 0): Int =
                statsMap[key]?.value?.roundToInt()
                    ?: statsMap[key]?.displayValue?.toIntOrNull()
                    ?: fallback

            StandingRow(
                rank = statInt("rank", index + 1),
                teamName = entry.team?.displayName ?: "Club",
                teamShort = entry.team?.abbreviation ?: entry.team?.shortDisplayName ?: "FC",
                logoUrl = entry.team?.logo,
                played = statInt("gamesPlayed"),
                won = statInt("wins"),
                drawn = statInt("ties"),
                lost = statInt("losses"),
                goalDiff = statInt("pointDifferential"),
                points = statInt("points"),
                note = entry.note?.description,
                teamId = entry.team?.id.orEmpty()
            )
        }.sortedBy { it.rank }
    }

    private fun leagueDisplayName(leagueCode: String): String =
        WORLD_LEAGUES.firstOrNull { it.code == leagueCode }?.name ?: leagueCode.uppercase(Locale.US)

    private fun mapEventToMatch(
        event: EspnEventDto,
        leagueCode: String,
        leagueName: String
    ): MatchSummary? {
        val id = event.id ?: return null
        val comp: EspnCompetitionDto = event.competitions?.firstOrNull() ?: return null
        val home = comp.competitors?.firstOrNull { it.homeAway == "home" }
            ?: comp.competitors?.getOrNull(0)
            ?: return null
        val away = comp.competitors?.firstOrNull { it.homeAway == "away" }
            ?: comp.competitors?.getOrNull(1)
            ?: return null

        val status = comp.status ?: event.status
        val phase = parsePhase(status)
        val clockDisplay = buildClockDisplay(phase, status, event.date)
        val probs = estimateWinProbabilities(home, away)

        return MatchSummary(
            id = id,
            leagueCode = leagueCode,
            leagueName = leagueName,
            homeTeamId = home.team?.id ?: home.id ?: "home",
            awayTeamId = away.team?.id ?: away.id ?: "away",
            homeTeam = home.team?.shortDisplayName ?: home.team?.displayName ?: "Home",
            awayTeam = away.team?.shortDisplayName ?: away.team?.displayName ?: "Away",
            homeShort = home.team?.abbreviation ?: "HOM",
            awayShort = away.team?.abbreviation ?: "AWY",
            homeLogoUrl = home.team?.logo,
            awayLogoUrl = away.team?.logo,
            homeScore = home.score?.toIntOrNull() ?: 0,
            awayScore = away.score?.toIntOrNull() ?: 0,
            phase = phase,
            clockDisplay = clockDisplay,
            kickoffIso = event.date.orEmpty(),
            kickoffLocalTime = formatKickoffTime(event.date),
            venue = comp.venue?.fullName ?: comp.venue?.address?.city ?: "Stadium TBA",
            homeRecord = home.form ?: home.records?.firstOrNull()?.summary ?: "-",
            awayRecord = away.form ?: away.records?.firstOrNull()?.summary ?: "-",
            homeWinProb = probs.first,
            drawProb = probs.second,
            awayWinProb = probs.third
        )
    }

    private fun parsePhase(status: EspnStatusDto?): MatchPhase {
        val typeName = status?.type?.name.orEmpty().uppercase(Locale.US)
        val state = status?.type?.state.orEmpty().lowercase(Locale.US)
        val shortDetail = status?.type?.shortDetail.orEmpty().uppercase(Locale.US)
        return when {
            status?.type?.completed == true || state == "post" || typeName.contains("FULL") || typeName.contains("FINAL") -> MatchPhase.FT
            shortDetail == "HT" || typeName.contains("HALFTIME") -> MatchPhase.HT
            state == "in" || typeName.contains("IN_PROGRESS") || typeName.contains("HALFTIME") -> MatchPhase.LIVE
            else -> MatchPhase.PRE
        }
    }

    private fun buildClockDisplay(phase: MatchPhase, status: EspnStatusDto?, isoDate: String?): String {
        return when (phase) {
            MatchPhase.FT -> "FT"
            MatchPhase.HT -> "HT"
            MatchPhase.LIVE -> status?.displayClock?.takeIf { it.isNotBlank() }
                ?: status?.type?.shortDetail?.takeIf { it.isNotBlank() }
                ?: "LIVE"
            MatchPhase.PRE -> formatKickoffTime(isoDate)
        }
    }

    private fun estimateWinProbabilities(
        home: EspnCompetitorDto,
        away: EspnCompetitorDto
    ): Triple<Int, Int, Int> {
        fun formStrength(comp: EspnCompetitorDto): Double {
            val form = comp.form.orEmpty()
            if (form.isNotBlank()) {
                var pts = 0.0
                form.forEach { ch ->
                    when (ch.uppercaseChar()) {
                        'W' -> pts += 3.0
                        'D' -> pts += 1.0
                    }
                }
                return (pts / (form.length * 3.0)).coerceIn(0.15, 0.90)
            }
            val summary = comp.records?.firstOrNull()?.summary.orEmpty()
            val parts = summary.split("-").mapNotNull { it.toIntOrNull() }
            if (parts.size >= 3) {
                val total = (parts[0] + parts[1] + parts[2]).coerceAtLeast(1)
                return ((parts[0] * 3.0 + parts[1]) / (total * 3.0)).coerceIn(0.15, 0.90)
            }
            return 0.50
        }

        val hScore = formStrength(home) + 0.08
        val aScore = formStrength(away)
        val diff = (hScore - aScore).coerceIn(-0.45, 0.45)
        val drawProb = (28 - (kotlin.math.abs(diff) * 20).roundToInt()).coerceIn(18, 32)
        val remaining = 100 - drawProb
        val homeProb = ((0.5 + diff * 0.75) * remaining).roundToInt().coerceIn(12, remaining - 12)
        val awayProb = remaining - homeProb
        return Triple(homeProb, drawProb, awayProb)
    }

    private fun buildStatComparisons(
        homeStats: Map<String, com.example.data.remote.EspnStatDto>,
        awayStats: Map<String, com.example.data.remote.EspnStatDto>
    ): List<StatComparison> {
        val candidates = listOf(
            "possessionPct" to "Possession %",
            "totalShots" to "Total Shots",
            "shotsOnTarget" to "Shots on Target",
            "wonCorners" to "Corners",
            "foulsCommitted" to "Fouls",
            "yellowCards" to "Yellow Cards",
            "offsides" to "Offsides",
            "accuratePasses" to "Accurate Passes"
        )
        return candidates.mapNotNull { (key, label) ->
            val h = homeStats[key]?.displayValue
            val a = awayStats[key]?.displayValue
            if (h == null && a == null) return@mapNotNull null
            val hDisp = h ?: "0"
            val aDisp = a ?: "0"
            val hNum = hDisp.replace("%", "").toFloatOrNull() ?: 0f
            val aNum = aDisp.replace("%", "").toFloatOrNull() ?: 0f
            StatComparison(
                label = label,
                homeDisplay = if (key == "possessionPct" && !hDisp.endsWith("%")) "$hDisp%" else hDisp,
                awayDisplay = if (key == "possessionPct" && !aDisp.endsWith("%")) "$aDisp%" else aDisp,
                homeValue = hNum,
                awayValue = aNum
            )
        }
    }

    private fun phaseOrder(phase: MatchPhase): Int = when (phase) {
        MatchPhase.LIVE -> 0
        MatchPhase.HT -> 1
        MatchPhase.PRE -> 2
        MatchPhase.FT -> 3
    }

    private fun formatKickoffTime(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "TBA"
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsed: Date = parser.parse(isoDate)
                ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(isoDate)
                ?: return "TBA"
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(parsed)
        }.getOrDefault("TBA")
    }

    private fun formatRelativeDate(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "Today"
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(isoDate) ?: return "Recent"
            SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(date)
        }.getOrDefault("Recent")
    }

    companion object {
        fun createDefaultApi(): EspnSoccerApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
            return Retrofit.Builder()
                .baseUrl("https://site.api.espn.com/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(EspnSoccerApi::class.java)
        }
    }
}
