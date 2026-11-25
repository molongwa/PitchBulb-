package com.example.data.model

enum class MatchPhase {
    PRE,
    LIVE,
    HT,
    FT
}

data class LeagueInfo(
    val code: String,
    val name: String,
    val shortName: String,
    val country: String,
    val region: String = "World",
    val countryCode: String = "WLD"
)

/**
 * Curated global catalog of world soccer leagues organized by country and confederation,
 * mapped directly to ESPN's public soccer endpoints.
 */
val WORLD_LEAGUES: List<LeagueInfo> = listOf(
    // England
    LeagueInfo("eng.1", "Premier League", "EPL", "England", "Europe", "ENG"),
    LeagueInfo("eng.2", "EFL Championship", "Championship", "England", "Europe", "ENG"),
    LeagueInfo("eng.3", "EFL League One", "League One", "England", "Europe", "ENG"),
    LeagueInfo("eng.fa", "FA Cup", "FA Cup", "England", "Europe", "ENG"),
    LeagueInfo("eng.league_cup", "EFL Cup", "Carabao Cup", "England", "Europe", "ENG"),

    // Spain
    LeagueInfo("esp.1", "La Liga", "La Liga", "Spain", "Europe", "ESP"),
    LeagueInfo("esp.2", "LaLiga 2", "LaLiga 2", "Spain", "Europe", "ESP"),
    LeagueInfo("esp.copa_del_rey", "Copa del Rey", "Copa del Rey", "Spain", "Europe", "ESP"),

    // Italy
    LeagueInfo("ita.1", "Serie A", "Serie A", "Italy", "Europe", "ITA"),
    LeagueInfo("ita.2", "Serie B", "Serie B", "Italy", "Europe", "ITA"),
    LeagueInfo("ita.coppa_italia", "Coppa Italia", "Coppa Italia", "Italy", "Europe", "ITA"),

    // Germany
    LeagueInfo("ger.1", "Bundesliga", "Bundesliga", "Germany", "Europe", "GER"),
    LeagueInfo("ger.2", "2. Bundesliga", "2. Bundesliga", "Germany", "Europe", "GER"),
    LeagueInfo("ger.dfb_pokal", "DFB-Pokal", "DFB-Pokal", "Germany", "Europe", "GER"),

    // France
    LeagueInfo("fra.1", "Ligue 1", "Ligue 1", "France", "Europe", "FRA"),
    LeagueInfo("fra.2", "Ligue 2", "Ligue 2", "France", "Europe", "FRA"),
    LeagueInfo("fra.coupe_de_france", "Coupe de France", "Coupe France", "France", "Europe", "FRA"),

    // Netherlands
    LeagueInfo("ned.1", "Eredivisie", "Eredivisie", "Netherlands", "Europe", "NED"),
    LeagueInfo("ned.2", "Eerste Divisie", "Eerste Div.", "Netherlands", "Europe", "NED"),

    // Portugal
    LeagueInfo("por.1", "Primeira Liga", "Liga Portugal", "Portugal", "Europe", "POR"),

    // Belgium
    LeagueInfo("bel.1", "Belgian Pro League", "Pro League", "Belgium", "Europe", "BEL"),

    // Scotland
    LeagueInfo("sco.1", "Scottish Premiership", "Premiership", "Scotland", "Europe", "SCO"),

    // Turkey
    LeagueInfo("tur.1", "Turkish Süper Lig", "Süper Lig", "Turkey", "Europe", "TUR"),

    // Greece
    LeagueInfo("gre.1", "Super League Greece", "Super League", "Greece", "Europe", "GRE"),

    // Switzerland & Austria & Nordics
    LeagueInfo("sui.1", "Swiss Super League", "Swiss SL", "Switzerland", "Europe", "SUI"),
    LeagueInfo("aut.1", "Austrian Bundesliga", "ÖBL", "Austria", "Europe", "AUT"),
    LeagueInfo("den.1", "Danish Superliga", "Superliga", "Denmark", "Europe", "DEN"),
    LeagueInfo("nor.1", "Norwegian Eliteserien", "Eliteserien", "Norway", "Europe", "NOR"),
    LeagueInfo("swe.1", "Swedish Allsvenskan", "Allsvenskan", "Sweden", "Europe", "SWE"),

    // USA & Canada
    LeagueInfo("usa.1", "Major League Soccer", "MLS", "USA & Canada", "North America", "USA"),
    LeagueInfo("usa.nwsl", "NWSL", "NWSL", "USA & Canada", "North America", "USA"),
    LeagueInfo("usa.open", "U.S. Open Cup", "US Open Cup", "USA & Canada", "North America", "USA"),

    // Mexico
    LeagueInfo("mex.1", "Liga MX", "Liga MX", "Mexico", "North America", "MEX"),

    // Brazil
    LeagueInfo("bra.1", "Brasileirão Série A", "Série A", "Brazil", "South America", "BRA"),
    LeagueInfo("bra.copa_do_brasil", "Copa do Brasil", "Copa Brasil", "Brazil", "South America", "BRA"),

    // Argentina
    LeagueInfo("arg.1", "Liga Profesional Argentina", "Liga Arg", "Argentina", "South America", "ARG"),

    // Colombia, Chile & Peru
    LeagueInfo("col.1", "Categoría Primera A", "Primera A", "Colombia", "South America", "COL"),
    LeagueInfo("chi.1", "Chilean Primera División", "Primera Chi", "Chile", "South America", "CHI"),
    LeagueInfo("per.1", "Peruvian Liga 1", "Liga 1 Peru", "Peru", "South America", "PER"),

    // Asia & Oceania
    LeagueInfo("jpn.1", "J1 League", "J1 League", "Japan", "Asia", "JPN"),
    LeagueInfo("ksa.1", "Saudi Pro League", "Saudi Pro", "Saudi Arabia", "Asia", "KSA"),
    LeagueInfo("chn.1", "Chinese Super League", "CSL", "China", "Asia", "CHN"),
    LeagueInfo("ind.1", "Indian Super League", "ISL", "India", "Asia", "IND"),
    LeagueInfo("aus.1", "A-League Men", "A-League", "Australia", "Oceania", "AUS"),

    // Africa
    LeagueInfo("rsa.1", "South African Premiership", "PSL", "South Africa", "Africa", "RSA"),

    // Continental & International
    LeagueInfo("uefa.champions", "UEFA Champions League", "UCL", "Europe (UEFA)", "International", "EUR"),
    LeagueInfo("uefa.europa", "UEFA Europa League", "UEL", "Europe (UEFA)", "International", "EUR"),
    LeagueInfo("uefa.europa.conf", "UEFA Conference League", "UECL", "Europe (UEFA)", "International", "EUR"),
    LeagueInfo("uefa.nations", "UEFA Nations League", "Nations Lg", "Europe (UEFA)", "International", "EUR"),
    LeagueInfo("conmebol.libertadores", "Copa Libertadores", "Libertadores", "South America (CONMEBOL)", "International", "SAM"),
    LeagueInfo("conmebol.sudamericana", "Copa Sudamericana", "Sudamericana", "South America (CONMEBOL)", "International", "SAM"),
    LeagueInfo("concacaf.champions", "Concacaf Champions Cup", "Concacaf CL", "North America (CONCACAF)", "International", "NAM"),
    LeagueInfo("caf.nations", "Africa Cup of Nations", "AFCON", "Africa (CAF)", "International", "AFR"),
    LeagueInfo("fifa.world", "FIFA World Cup", "World Cup", "World (FIFA)", "International", "WLD"),
    LeagueInfo("fifa.friendly", "International Friendlies", "Friendlies", "World (FIFA)", "International", "WLD")
)

/**
 * Default featured leagues loaded on initial matchday view for fast startup.
 */
val SUPPORTED_LEAGUES: List<LeagueInfo> = listOf(
    WORLD_LEAGUES.first { it.code == "eng.1" },
    WORLD_LEAGUES.first { it.code == "uefa.champions" },
    WORLD_LEAGUES.first { it.code == "esp.1" },
    WORLD_LEAGUES.first { it.code == "ita.1" },
    WORLD_LEAGUES.first { it.code == "ger.1" },
    WORLD_LEAGUES.first { it.code == "fra.1" },
    WORLD_LEAGUES.first { it.code == "usa.1" },
    WORLD_LEAGUES.first { it.code == "mex.1" },
    WORLD_LEAGUES.first { it.code == "bra.1" },
    WORLD_LEAGUES.first { it.code == "ksa.1" }
)

val WORLD_COUNTRIES: List<String> = WORLD_LEAGUES.map { it.country }.distinct()

data class MatchSummary(
    val id: String,
    val leagueCode: String,
    val leagueName: String,
    val homeTeamId: String,
    val awayTeamId: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeShort: String,
    val awayShort: String,
    val homeLogoUrl: String?,
    val awayLogoUrl: String?,
    val homeScore: Int,
    val awayScore: Int,
    val phase: MatchPhase,
    val clockDisplay: String,
    val kickoffIso: String,
    val kickoffLocalTime: String,
    val venue: String,
    val homeRecord: String,
    val awayRecord: String,
    val homeWinProb: Int,
    val drawProb: Int,
    val awayWinProb: Int
)

data class StatComparison(
    val label: String,
    val homeDisplay: String,
    val awayDisplay: String,
    val homeValue: Float,
    val awayValue: Float
)

data class MatchEventItem(
    val minute: String,
    val text: String,
    val teamName: String,
    val isHome: Boolean,
    val type: MatchEventType
)

enum class MatchEventType {
    GOAL,
    YELLOW_CARD,
    RED_CARD,
    SUBSTITUTION,
    INFO
}

data class LineupPlayer(
    val name: String,
    val jersey: String,
    val position: String,
    val starter: Boolean,
    val playerId: String = "",
    val headshotUrl: String? = null
)

data class HistoricalLineup(
    val matchId: String,
    val dateLabel: String,
    val opponentName: String,
    val scoreDisplay: String,
    val teamName: String,
    val starters: List<LineupPlayer>,
    val subs: List<LineupPlayer>
)

data class HeadToHeadMatch(
    val id: String,
    val dateLabel: String,
    val leagueName: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int,
    val awayScore: Int,
    val homeLogoUrl: String? = null,
    val awayLogoUrl: String? = null
)

data class PlayerInjury(
    val playerId: String,
    val playerName: String,
    val teamId: String,
    val teamName: String,
    val position: String,
    val status: String,
    val injuryType: String,
    val detail: String,
    val returnDate: String,
    val headshotUrl: String? = null
)

data class SquadPlayer(
    val id: String,
    val name: String,
    val jersey: String,
    val position: String,
    val age: String = "",
    val nationality: String = "",
    val headshotUrl: String? = null,
    val statusText: String = "Active",
    val appearances: Int = 0,
    val goals: Int = 0,
    val assists: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val minutesPlayed: Int = 0
)

data class PlayerSeasonHistoryRow(
    val seasonLabel: String,
    val teamName: String,
    val leagueName: String,
    val appearances: Int,
    val goals: Int,
    val assists: Int,
    val minutes: Int,
    val yellowCards: Int,
    val redCards: Int
)

data class PlayerHistoryData(
    val player: SquadPlayer,
    val bioSummary: String,
    val heightDisplay: String,
    val weightDisplay: String,
    val seasonHistory: List<PlayerSeasonHistoryRow>,
    val recentAppearances: List<String>
)

data class TeamDetailData(
    val teamId: String,
    val leagueCode: String,
    val teamName: String,
    val teamShort: String,
    val logoUrl: String?,
    val recordSummary: String,
    val venue: String,
    val squad: List<SquadPlayer>,
    val injuries: List<PlayerInjury>,
    val recentMatches: List<MatchSummary>,
    val historyLineups: List<HistoricalLineup>
)

data class MatchDetailData(
    val match: MatchSummary,
    val stats: List<StatComparison>,
    val events: List<MatchEventItem>,
    val homeLineup: List<LineupPlayer>,
    val awayLineup: List<LineupPlayer>,
    val referee: String,
    val attendance: String,
    val h2hMatches: List<HeadToHeadMatch> = emptyList(),
    val injuries: List<PlayerInjury> = emptyList(),
    val homeHistoryLineups: List<HistoricalLineup> = emptyList(),
    val awayHistoryLineups: List<HistoricalLineup> = emptyList()
)

data class NewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val imageUrl: String?,
    val source: String,
    val publishedLabel: String,
    val articleUrl: String,
    val leagueTag: String
)

data class StandingRow(
    val rank: Int,
    val teamName: String,
    val teamShort: String,
    val logoUrl: String?,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val goalDiff: Int,
    val points: Int,
    val note: String?,
    val teamId: String = ""
)

data class MatchAlertEvent(
    val matchId: String,
    val title: String,
    val body: String,
    val isGoal: Boolean
)
