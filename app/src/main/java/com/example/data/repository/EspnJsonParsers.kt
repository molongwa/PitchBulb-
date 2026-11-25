package com.example.data.repository

import com.example.data.model.HeadToHeadMatch
import com.example.data.model.MatchPhase
import com.example.data.model.MatchSummary
import com.example.data.model.PlayerHistoryData
import com.example.data.model.PlayerInjury
import com.example.data.model.PlayerSeasonHistoryRow
import com.example.data.model.SquadPlayer
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object EspnJsonParsers {

    fun espnHeadshotUrl(playerId: String): String? =
        if (playerId.isNotBlank() && playerId.all { it.isDigit() }) {
            "https://a.espncdn.com/i/headshots/soccer/players/full/$playerId.png"
        } else {
            null
        }

    fun parseHeadToHeadFromSummary(
        root: JSONObject,
        homeTeamName: String,
        awayTeamName: String,
        homeTeamId: String,
        awayTeamId: String
    ): List<HeadToHeadMatch> {
        val h2hArray = root.optJSONArray("headToHeadGames") ?: return emptyList()
        val results = mutableListOf<HeadToHeadMatch>()
        val seenIds = mutableSetOf<String>()

        for (i in 0 until h2hArray.length()) {
            val item = h2hArray.optJSONObject(i) ?: continue
            val groupTeam = item.optJSONObject("team")
            val groupTeamId = groupTeam?.optString("id").orEmpty()
            val groupTeamName = groupTeam?.optString("shortDisplayName")
                ?.ifBlank { groupTeam.optString("displayName") }
                .orEmpty()
            val groupTeamLogo = groupTeam?.optString("logo")?.takeIf { it.isNotBlank() }

            val events = item.optJSONArray("events")
            if (events != null) {
                for (j in 0 until events.length()) {
                    val ev = events.optJSONObject(j) ?: continue
                    val parsed = parseH2HEvent(
                        ev = ev,
                        primaryTeamId = groupTeamId.ifBlank { homeTeamId },
                        primaryTeamName = groupTeamName.ifBlank { homeTeamName },
                        primaryTeamLogo = groupTeamLogo,
                        fallbackOpponentName = awayTeamName
                    ) ?: continue
                    if (seenIds.add(parsed.id)) {
                        results.add(parsed)
                    }
                }
            } else if (item.has("competitors") || item.has("opponent") || item.has("score")) {
                val parsed = parseH2HEvent(
                    ev = item,
                    primaryTeamId = homeTeamId,
                    primaryTeamName = homeTeamName,
                    primaryTeamLogo = null,
                    fallbackOpponentName = awayTeamName
                ) ?: continue
                if (seenIds.add(parsed.id)) {
                    results.add(parsed)
                }
            }
        }
        return results
    }

    private fun parseH2HEvent(
        ev: JSONObject,
        primaryTeamId: String,
        primaryTeamName: String,
        primaryTeamLogo: String?,
        fallbackOpponentName: String
    ): HeadToHeadMatch? {
        val id = ev.optString("id").ifBlank { "${ev.optString("gameDate")}_${ev.optString("score")}" }
        val rawDate = ev.optString("gameDate").ifBlank { ev.optString("date") }
        val dateLabel = formatShortDate(rawDate)
        val leagueName = ev.optString("leagueName").ifBlank {
            ev.optJSONObject("league")?.optString("abbreviation").orEmpty()
        }.ifBlank { "Match" }

        val competitors = ev.optJSONArray("competitors")
        if (competitors != null && competitors.length() >= 2) {
            val c0 = competitors.optJSONObject(0) ?: return null
            val c1 = competitors.optJSONObject(1) ?: return null
            val homeComp = if (c0.optString("homeAway") == "home") c0 else if (c1.optString("homeAway") == "home") c1 else c0
            val awayComp = if (homeComp === c0) c1 else c0
            val hTeam = homeComp.optJSONObject("team")
            val aTeam = awayComp.optJSONObject("team")
            return HeadToHeadMatch(
                id = id,
                dateLabel = dateLabel,
                leagueName = leagueName,
                homeTeam = hTeam?.optString("shortDisplayName")?.ifBlank { hTeam.optString("displayName") } ?: primaryTeamName,
                awayTeam = aTeam?.optString("shortDisplayName")?.ifBlank { aTeam.optString("displayName") } ?: fallbackOpponentName,
                homeScore = parseScoreInt(homeComp.opt("score")),
                awayScore = parseScoreInt(awayComp.opt("score")),
                homeLogoUrl = hTeam?.optString("logo")?.takeIf { it.isNotBlank() },
                awayLogoUrl = aTeam?.optString("logo")?.takeIf { it.isNotBlank() }
            )
        }

        val opp = ev.optJSONObject("opponent")
        val oppName = opp?.optString("shortDisplayName")?.ifBlank { opp.optString("displayName") }
            ?: fallbackOpponentName
        val oppLogo = opp?.optString("logo")?.takeIf { it.isNotBlank() }
        val atVs = ev.optString("atVs")
        val isPrimaryHome = atVs.equals("vs", ignoreCase = true) || atVs.isBlank()

        var homeScore = ev.optInt("homeTeamScore", -1)
        var awayScore = ev.optInt("awayTeamScore", -1)
        if (homeScore < 0 || awayScore < 0) {
            val scoreStr = ev.optString("score")
            val nums = Regex("\\d+").findAll(scoreStr).mapNotNull { it.value.toIntOrNull() }.toList()
            if (nums.size >= 2) {
                homeScore = nums[0]
                awayScore = nums[1]
            } else {
                homeScore = 0
                awayScore = 0
            }
        }

        return if (isPrimaryHome) {
            HeadToHeadMatch(
                id = id,
                dateLabel = dateLabel,
                leagueName = leagueName,
                homeTeam = primaryTeamName,
                awayTeam = oppName,
                homeScore = homeScore,
                awayScore = awayScore,
                homeLogoUrl = primaryTeamLogo,
                awayLogoUrl = oppLogo
            )
        } else {
            HeadToHeadMatch(
                id = id,
                dateLabel = dateLabel,
                leagueName = leagueName,
                homeTeam = oppName,
                awayTeam = primaryTeamName,
                homeScore = homeScore,
                awayScore = awayScore,
                homeLogoUrl = oppLogo,
                awayLogoUrl = primaryTeamLogo
            )
        }
    }

    fun parseInjuriesFromJson(
        root: JSONObject,
        filterTeamIds: Set<String> = emptySet(),
        filterTeamNames: Set<String> = emptySet()
    ): List<PlayerInjury> {
        val results = mutableListOf<PlayerInjury>()
        val seenKeys = mutableSetOf<String>()
        val topInjuries = root.optJSONArray("injuries") ?: return emptyList()

        for (i in 0 until topInjuries.length()) {
            val group = topInjuries.optJSONObject(i) ?: continue
            val nestedInjuries = group.optJSONArray("injuries")
            if (nestedInjuries != null) {
                val teamObj = group.optJSONObject("team")
                val teamId = teamObj?.optString("id").orEmpty().ifBlank { group.optString("id") }
                val teamName = teamObj?.optString("shortDisplayName")
                    ?.ifBlank { teamObj.optString("displayName") }
                    ?.ifBlank { group.optString("displayName") }
                    ?: group.optString("displayName").ifBlank { "Club" }

                if (matchesTeamFilter(teamId, teamName, filterTeamIds, filterTeamNames)) {
                    for (j in 0 until nestedInjuries.length()) {
                        val injObj = nestedInjuries.optJSONObject(j) ?: continue
                        parseSingleInjury(injObj, teamId, teamName)?.let { inj ->
                            val key = "${inj.teamName}_${inj.playerName}"
                            if (seenKeys.add(key)) results.add(inj)
                        }
                    }
                }
            } else if (group.has("athlete")) {
                val teamObj = group.optJSONObject("team")
                val teamId = teamObj?.optString("id").orEmpty()
                val teamName = teamObj?.optString("shortDisplayName")
                    ?.ifBlank { teamObj.optString("displayName") }
                    .orEmpty()
                if (matchesTeamFilter(teamId, teamName, filterTeamIds, filterTeamNames)) {
                    parseSingleInjury(group, teamId, teamName)?.let { inj ->
                        val key = "${inj.teamName}_${inj.playerName}"
                        if (seenKeys.add(key)) results.add(inj)
                    }
                }
            }
        }
        return results
    }

    private fun matchesTeamFilter(
        teamId: String,
        teamName: String,
        filterTeamIds: Set<String>,
        filterTeamNames: Set<String>
    ): Boolean {
        if (filterTeamIds.isEmpty() && filterTeamNames.isEmpty()) return true
        if (teamId.isNotBlank() && filterTeamIds.contains(teamId)) return true
        val lower = teamName.lowercase(Locale.US)
        return filterTeamNames.any { target ->
            val t = target.lowercase(Locale.US)
            t.isNotBlank() && (lower.contains(t) || t.contains(lower))
        }
    }

    private fun parseSingleInjury(
        obj: JSONObject,
        teamId: String,
        teamName: String
    ): PlayerInjury? {
        val athlete = obj.optJSONObject("athlete") ?: return null
        val playerId = athlete.optString("id")
        val playerName = athlete.optString("displayName")
            .ifBlank { athlete.optString("fullName") }
            .ifBlank { athlete.optString("shortName") }
        if (playerName.isBlank()) return null

        val position = athlete.optJSONObject("position")?.optString("abbreviation")
            ?.ifBlank { athlete.optJSONObject("position")?.optString("displayName").orEmpty() }
            .orEmpty()
        val status = obj.optString("status").ifBlank { "Out" }
        val typeObj = obj.optJSONObject("type")
        val injuryType = typeObj?.optString("description")
            ?.ifBlank { typeObj.optString("name") }
            ?: obj.optString("longComment")
            .ifBlank { obj.optString("shortComment") }
            .ifBlank { "Injury" }

        val details = obj.optJSONObject("details")
        val detail = details?.optString("detail")
            ?.ifBlank { details.optString("type") }
            ?.ifBlank { obj.optString("shortComment") }
            .orEmpty()
        val returnDate = details?.optString("returnDate")?.takeIf { it.isNotBlank() }
            ?.let { formatShortDate(it) }
            .orEmpty()
        val headshot = athlete.optJSONObject("headshot")?.optString("href")?.takeIf { it.isNotBlank() }
            ?: espnHeadshotUrl(playerId)

        return PlayerInjury(
            playerId = playerId,
            playerName = playerName,
            teamId = teamId,
            teamName = teamName,
            position = position,
            status = status,
            injuryType = injuryType,
            detail = detail,
            returnDate = returnDate,
            headshotUrl = headshot
        )
    }

    fun parseRosterFromJson(root: JSONObject): List<SquadPlayer> {
        val rawAthletes = root.optJSONArray("athletes") ?: return emptyList()
        val players = mutableListOf<SquadPlayer>()
        val seenIds = mutableSetOf<String>()

        for (i in 0 until rawAthletes.length()) {
            val entry = rawAthletes.optJSONObject(i) ?: continue
            val items = entry.optJSONArray("items")
            if (items != null) {
                for (j in 0 until items.length()) {
                    val athleteObj = items.optJSONObject(j) ?: continue
                    parseSquadPlayer(athleteObj)?.let { player ->
                        if (seenIds.add(player.id.ifBlank { player.name })) {
                            players.add(player)
                        }
                    }
                }
            } else {
                parseSquadPlayer(entry)?.let { player ->
                    if (seenIds.add(player.id.ifBlank { player.name })) {
                        players.add(player)
                    }
                }
            }
        }
        return players
    }

    private fun parseSquadPlayer(obj: JSONObject): SquadPlayer? {
        val id = obj.optString("id")
        val name = obj.optString("displayName")
            .ifBlank { obj.optString("fullName") }
            .ifBlank { obj.optString("shortName") }
        if (name.isBlank()) return null

        val jersey = obj.optString("jersey").ifBlank { "-" }
        val posObj = obj.optJSONObject("position")
        val position = posObj?.optString("abbreviation")
            ?.ifBlank { posObj.optString("displayName") }
            .orEmpty()
        val ageVal = obj.optInt("age", 0)
        val age = if (ageVal > 0) ageVal.toString() else ""
        val nationality = obj.optString("citizenship")
            .ifBlank { obj.optJSONObject("flag")?.optString("alt").orEmpty() }
            .ifBlank { obj.optJSONObject("birthPlace")?.optString("country").orEmpty() }
        val headshot = obj.optJSONObject("headshot")?.optString("href")?.takeIf { it.isNotBlank() }
            ?: espnHeadshotUrl(id)
        val statusText = obj.optJSONObject("status")?.optString("name")?.ifBlank { "Active" } ?: "Active"

        val statsMap = extractStatsMap(obj.optJSONObject("statistics"))
        return SquadPlayer(
            id = id.ifBlank { name },
            name = name,
            jersey = jersey,
            position = position,
            age = age,
            nationality = nationality,
            headshotUrl = headshot,
            statusText = statusText,
            appearances = statsMap["appearances"] ?: statsMap["gamesPlayed"] ?: 0,
            goals = statsMap["totalGoals"] ?: statsMap["goals"] ?: 0,
            assists = statsMap["goalAssists"] ?: statsMap["assists"] ?: 0,
            yellowCards = statsMap["yellowCards"] ?: 0,
            redCards = statsMap["redCards"] ?: 0,
            minutesPlayed = statsMap["minutesPlayed"] ?: statsMap["minutes"] ?: 0
        )
    }

    private fun extractStatsMap(statsObj: JSONObject?): Map<String, Int> {
        if (statsObj == null) return emptyMap()
        val map = mutableMapOf<String, Int>()
        val categories = statsObj.optJSONObject("splits")?.optJSONArray("categories") ?: return map
        for (c in 0 until categories.length()) {
            val cat = categories.optJSONObject(c) ?: continue
            val stats = cat.optJSONArray("stats") ?: continue
            for (s in 0 until stats.length()) {
                val st = stats.optJSONObject(s) ?: continue
                val key = st.optString("name")
                val v = st.optDouble("value", Double.NaN)
                val intVal = if (!v.isNaN()) v.toInt() else st.optString("displayValue").toIntOrNull()
                if (key.isNotBlank() && intVal != null) {
                    map[key] = intVal
                }
            }
        }
        return map
    }

    fun parseTeamScheduleMatches(
        root: JSONObject,
        leagueCode: String,
        defaultLeagueName: String
    ): List<MatchSummary> {
        val events = root.optJSONArray("events") ?: return emptyList()
        val list = mutableListOf<MatchSummary>()
        for (i in 0 until events.length()) {
            val ev = events.optJSONObject(i) ?: continue
            val id = ev.optString("id")
            if (id.isBlank()) continue
            val comp = ev.optJSONArray("competitions")?.optJSONObject(0) ?: continue
            val competitors = comp.optJSONArray("competitors") ?: continue
            if (competitors.length() < 2) continue
            val c0 = competitors.optJSONObject(0) ?: continue
            val c1 = competitors.optJSONObject(1) ?: continue
            val home = if (c0.optString("homeAway") == "home") c0 else if (c1.optString("homeAway") == "home") c1 else c0
            val away = if (home === c0) c1 else c0

            val hTeam = home.optJSONObject("team")
            val aTeam = away.optJSONObject("team")
            val statusObj = comp.optJSONObject("status") ?: ev.optJSONObject("status")
            val statusType = statusObj?.optJSONObject("type")
            val state = statusType?.optString("state").orEmpty().lowercase(Locale.US)
            val completed = statusType?.optBoolean("completed") == true
            val phase = when {
                completed || state == "post" -> MatchPhase.FT
                statusType?.optString("shortDetail").equals("HT", ignoreCase = true) -> MatchPhase.HT
                state == "in" -> MatchPhase.LIVE
                else -> MatchPhase.PRE
            }
            val dateIso = ev.optString("date")
            val leagueObj = ev.optJSONObject("league")
            val evLeagueCode = leagueObj?.optString("slug")?.ifBlank { leagueCode } ?: leagueCode
            val evLeagueName = leagueObj?.optString("name")?.ifBlank { defaultLeagueName } ?: defaultLeagueName
            val clock = when (phase) {
                MatchPhase.FT -> "FT • ${formatShortDate(dateIso)}"
                MatchPhase.HT -> "HT"
                MatchPhase.LIVE -> statusObj?.optString("displayClock")?.ifBlank { "LIVE" } ?: "LIVE"
                MatchPhase.PRE -> formatShortDate(dateIso)
            }

            list.add(
                MatchSummary(
                    id = id,
                    leagueCode = evLeagueCode,
                    leagueName = evLeagueName,
                    homeTeamId = hTeam?.optString("id").orEmpty(),
                    awayTeamId = aTeam?.optString("id").orEmpty(),
                    homeTeam = hTeam?.optString("shortDisplayName")?.ifBlank { hTeam.optString("displayName") } ?: "Home",
                    awayTeam = aTeam?.optString("shortDisplayName")?.ifBlank { aTeam.optString("displayName") } ?: "Away",
                    homeShort = hTeam?.optString("abbreviation")?.ifBlank { "HOM" } ?: "HOM",
                    awayShort = aTeam?.optString("abbreviation")?.ifBlank { "AWY" } ?: "AWY",
                    homeLogoUrl = extractLogo(hTeam),
                    awayLogoUrl = extractLogo(aTeam),
                    homeScore = parseScoreInt(home.opt("score")),
                    awayScore = parseScoreInt(away.opt("score")),
                    phase = phase,
                    clockDisplay = clock,
                    kickoffIso = dateIso,
                    kickoffLocalTime = formatShortDate(dateIso),
                    venue = comp.optJSONObject("venue")?.optString("fullName").orEmpty().ifBlank { "Stadium TBA" },
                    homeRecord = "-",
                    awayRecord = "-",
                    homeWinProb = 34,
                    drawProb = 32,
                    awayWinProb = 34
                )
            )
        }
        return list
    }

    fun parsePlayerHistoryFromOverview(
        root: JSONObject,
        fallbackPlayer: SquadPlayer
    ): PlayerHistoryData {
        val athleteObj = root.optJSONObject("athlete") ?: root
        val name = athleteObj.optString("displayName").ifBlank { fallbackPlayer.name }
        val jersey = athleteObj.optString("jersey").ifBlank { fallbackPlayer.jersey }
        val position = athleteObj.optJSONObject("position")?.optString("displayName")
            ?.ifBlank { fallbackPlayer.position }
            ?: fallbackPlayer.position
        val age = athleteObj.optInt("age", 0).let { if (it > 0) it.toString() else fallbackPlayer.age }
        val citizenship = athleteObj.optString("citizenship")
            .ifBlank { athleteObj.optJSONObject("birthPlace")?.optString("country").orEmpty() }
            .ifBlank { fallbackPlayer.nationality }
        val height = athleteObj.optString("displayHeight")
        val weight = athleteObj.optString("displayWeight")
        val headshot = athleteObj.optJSONObject("headshot")?.optString("href")?.takeIf { it.isNotBlank() }
            ?: fallbackPlayer.headshotUrl

        val seasonRows = mutableListOf<PlayerSeasonHistoryRow>()
        val statsNode = root.optJSONObject("statistics") ?: athleteObj.optJSONObject("statistics")
        val labels = mutableListOf<String>()
        statsNode?.optJSONArray("labels")?.let { arr ->
            for (i in 0 until arr.length()) labels.add(arr.optString(i))
        }
        val names = mutableListOf<String>()
        statsNode?.optJSONArray("names")?.let { arr ->
            for (i in 0 until arr.length()) names.add(arr.optString(i))
        }

        val splits = statsNode?.optJSONArray("splits")
        if (splits != null) {
            for (i in 0 until splits.length()) {
                val split = splits.optJSONObject(i) ?: continue
                val seasonLabel = split.optString("displayName")
                    .ifBlank { split.optJSONObject("season")?.optString("displayName").orEmpty() }
                    .ifBlank { "Season" }
                val teamName = split.optJSONObject("team")?.optString("displayName")
                    ?.ifBlank { split.optString("teamName") }
                    .orEmpty()
                val leagueName = split.optJSONObject("league")?.optString("name").orEmpty()
                val statsArr = split.optJSONArray("stats") ?: continue
                fun statByKey(vararg keys: String): Int {
                    val idx = names.indexOfFirst { n -> keys.any { k -> n.equals(k, ignoreCase = true) } }
                        .takeIf { it >= 0 }
                        ?: labels.indexOfFirst { l -> keys.any { k -> l.equals(k, ignoreCase = true) } }
                    if (idx < 0 || idx >= statsArr.length()) return 0
                    return statsArr.optString(idx).toDoubleOrNull()?.toInt() ?: 0
                }
                seasonRows.add(
                    PlayerSeasonHistoryRow(
                        seasonLabel = seasonLabel,
                        teamName = teamName.ifBlank { "Club" },
                        leagueName = leagueName,
                        appearances = statByKey("appearances", "GP", "gamesPlayed", "A"),
                        goals = statByKey("totalGoals", "goals", "G"),
                        assists = statByKey("goalAssists", "assists", "AST"),
                        minutes = statByKey("minutesPlayed", "minutes", "MIN"),
                        yellowCards = statByKey("yellowCards", "YC"),
                        redCards = statByKey("redCards", "RC")
                    )
                )
            }
        }

        val recentList = mutableListOf<String>()
        val gameLog = root.optJSONObject("gameLog")
        val eventsMap = gameLog?.optJSONObject("events")
        val logStats = gameLog?.optJSONArray("statistics")
        if (logStats != null) {
            for (i in 0 until logStats.length()) {
                val seasonBlock = logStats.optJSONObject(i) ?: continue
                val evs = seasonBlock.optJSONArray("events") ?: continue
                for (j in 0 until kotlin.math.min(evs.length(), 8)) {
                    val eObj = evs.optJSONObject(j) ?: continue
                    val evId = eObj.optString("eventId")
                    val evMeta = eventsMap?.optJSONObject(evId)
                    val opp = evMeta?.optJSONObject("opponent")?.optString("displayName")
                        ?: evMeta?.optString("atVs").orEmpty()
                    val score = evMeta?.optString("score").orEmpty()
                    val dt = formatShortDate(evMeta?.optString("gameDate"))
                    if (opp.isNotBlank() || score.isNotBlank()) {
                        recentList.add("$dt • vs $opp ($score)".trim())
                    }
                }
            }
        }

        val updatedPlayer = fallbackPlayer.copy(
            name = name,
            jersey = jersey,
            position = position,
            age = age,
            nationality = citizenship,
            headshotUrl = headshot
        )

        val bioParts = listOfNotNull(
            position.takeIf { it.isNotBlank() },
            citizenship.takeIf { it.isNotBlank() },
            age.takeIf { it.isNotBlank() }?.let { "Age $it" }
        )

        return PlayerHistoryData(
            player = updatedPlayer,
            bioSummary = bioParts.joinToString(" • ").ifBlank { "First Team Squad" },
            heightDisplay = height,
            weightDisplay = weight,
            seasonHistory = seasonRows,
            recentAppearances = recentList
        )
    }

    private fun extractLogo(teamObj: JSONObject?): String? {
        if (teamObj == null) return null
        val direct = teamObj.optString("logo")
        if (direct.isNotBlank()) return direct
        return teamObj.optJSONArray("logos")?.optJSONObject(0)?.optString("href")?.takeIf { it.isNotBlank() }
    }

    private fun parseScoreInt(raw: Any?): Int {
        return when (raw) {
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull() ?: 0
            is JSONObject -> raw.optDouble("value", 0.0).toInt()
            else -> 0
        }
    }

    fun formatShortDate(iso: String?): String {
        if (iso.isNullOrBlank()) return "Recent"
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm'Z'",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            val parsed: Date? = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(iso)
            }.getOrNull()
            if (parsed != null) {
                return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(parsed)
            }
        }
        return iso.take(10)
    }
}
