package com.example.data.remote

import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface EspnSoccerApi {
    @GET("apis/site/v2/sports/soccer/{league}/scoreboard")
    suspend fun getScoreboard(
        @Path("league") league: String,
        @Query("dates") dates: String? = null
    ): EspnScoreboardResponse

    @GET("apis/site/v2/sports/soccer/{league}/summary")
    suspend fun getMatchSummary(
        @Path("league") league: String,
        @Query("event") eventId: String
    ): EspnSummaryResponse

    @GET("apis/site/v2/sports/soccer/{league}/summary")
    suspend fun getMatchSummaryRaw(
        @Path("league") league: String,
        @Query("event") eventId: String
    ): ResponseBody

    @GET("apis/site/v2/sports/soccer/{league}/injuries")
    suspend fun getLeagueInjuriesRaw(
        @Path("league") league: String
    ): ResponseBody

    @GET("apis/site/v2/sports/soccer/{league}/teams/{teamId}")
    suspend fun getTeamDetailRaw(
        @Path("league") league: String,
        @Path("teamId") teamId: String
    ): ResponseBody

    @GET("apis/site/v2/sports/soccer/{league}/teams/{teamId}/roster")
    suspend fun getTeamRosterRaw(
        @Path("league") league: String,
        @Path("teamId") teamId: String
    ): ResponseBody

    @GET("apis/site/v2/sports/soccer/{league}/teams/{teamId}/schedule")
    suspend fun getTeamScheduleRaw(
        @Path("league") league: String,
        @Path("teamId") teamId: String
    ): ResponseBody

    @GET
    suspend fun getRawUrl(
        @Url url: String
    ): ResponseBody

    @GET("apis/site/v2/sports/soccer/{league}/news")
    suspend fun getNews(
        @Path("league") league: String
    ): EspnNewsResponse

    @GET
    suspend fun getStandings(
        @Url url: String
    ): EspnStandingsResponse
}

@JsonClass(generateAdapter = true)
data class EspnScoreboardResponse(
    val leagues: List<EspnLeagueDto>? = null,
    val events: List<EspnEventDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnLeagueDto(
    val id: String? = null,
    val name: String? = null,
    val abbreviation: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnEventDto(
    val id: String? = null,
    val date: String? = null,
    val name: String? = null,
    val shortName: String? = null,
    val competitions: List<EspnCompetitionDto>? = null,
    val status: EspnStatusDto? = null
)

@JsonClass(generateAdapter = true)
data class EspnCompetitionDto(
    val id: String? = null,
    val date: String? = null,
    val venue: EspnVenueDto? = null,
    val attendance: Int? = null,
    val competitors: List<EspnCompetitorDto>? = null,
    val status: EspnStatusDto? = null,
    val details: List<EspnKeyEventDto>? = null,
    val officials: List<EspnOfficialDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnVenueDto(
    val fullName: String? = null,
    val address: EspnAddressDto? = null
)

@JsonClass(generateAdapter = true)
data class EspnAddressDto(
    val city: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnOfficialDto(
    val displayName: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnCompetitorDto(
    val id: String? = null,
    val homeAway: String? = null,
    val score: String? = null,
    val form: String? = null,
    val team: EspnTeamDto? = null,
    val records: List<EspnRecordDto>? = null,
    val statistics: List<EspnStatDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnTeamDto(
    val id: String? = null,
    val displayName: String? = null,
    val shortDisplayName: String? = null,
    val abbreviation: String? = null,
    val logo: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnRecordDto(
    val summary: String? = null,
    val type: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnStatusDto(
    val displayClock: String? = null,
    val type: EspnStatusTypeDto? = null
)

@JsonClass(generateAdapter = true)
data class EspnStatusTypeDto(
    val name: String? = null,
    val state: String? = null,
    val completed: Boolean? = null,
    val shortDetail: String? = null,
    val detail: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnKeyEventDto(
    val clock: EspnClockDto? = null,
    val type: EspnEventTypeDto? = null,
    val team: EspnTeamRefDto? = null,
    val scoringPlay: Boolean? = null,
    val redCard: Boolean? = null,
    val yellowCard: Boolean? = null,
    val penaltyKick: Boolean? = null,
    val ownGoal: Boolean? = null,
    val athletesInvolved: List<EspnAthleteDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnClockDto(
    val displayValue: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnEventTypeDto(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnTeamRefDto(
    val id: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnAthleteDto(
    val id: String? = null,
    val displayName: String? = null,
    val shortName: String? = null,
    val headshot: EspnImageDto? = null
)

@JsonClass(generateAdapter = true)
data class EspnSummaryResponse(
    val header: EspnSummaryHeaderDto? = null,
    val boxscore: EspnBoxscoreDto? = null,
    val rosters: List<EspnRosterDto>? = null,
    val gameInfo: EspnGameInfoDto? = null,
    val keyEvents: List<EspnSummaryKeyEventDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnSummaryHeaderDto(
    val id: String? = null,
    val competitions: List<EspnCompetitionDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnBoxscoreDto(
    val teams: List<EspnBoxscoreTeamDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnBoxscoreTeamDto(
    val team: EspnTeamDto? = null,
    val statistics: List<EspnStatDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnStatDto(
    val name: String? = null,
    val displayValue: String? = null,
    val label: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnRosterDto(
    val homeAway: String? = null,
    val team: EspnTeamDto? = null,
    val roster: List<EspnRosterEntryDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnRosterEntryDto(
    val starter: Boolean? = null,
    val jersey: String? = null,
    val athlete: EspnAthleteDto? = null,
    val position: EspnPositionDto? = null
)

@JsonClass(generateAdapter = true)
data class EspnPositionDto(
    val abbreviation: String? = null,
    val displayName: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnGameInfoDto(
    val venue: EspnVenueDto? = null,
    val attendance: Int? = null,
    val officials: List<EspnOfficialDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnSummaryKeyEventDto(
    val text: String? = null,
    val shortText: String? = null,
    val clock: EspnClockDto? = null,
    val team: EspnTeamDto? = null,
    val type: EspnEventTypeDto? = null,
    val scoringPlay: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class EspnNewsResponse(
    val articles: List<EspnArticleDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnArticleDto(
    val id: Long? = null,
    val headline: String? = null,
    val description: String? = null,
    val published: String? = null,
    val byline: String? = null,
    val images: List<EspnImageDto>? = null,
    val links: EspnLinksDto? = null
)

@JsonClass(generateAdapter = true)
data class EspnImageDto(
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnLinksDto(
    val web: EspnWebLinkDto? = null
)

@JsonClass(generateAdapter = true)
data class EspnWebLinkDto(
    val href: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnStandingsResponse(
    val children: List<EspnStandingGroupDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnStandingGroupDto(
    val name: String? = null,
    val standings: EspnStandingsEntriesWrapperDto? = null
)

@JsonClass(generateAdapter = true)
data class EspnStandingsEntriesWrapperDto(
    val entries: List<EspnStandingEntryDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnStandingEntryDto(
    val team: EspnTeamDto? = null,
    val note: EspnStandingNoteDto? = null,
    val stats: List<EspnStandingStatDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnStandingNoteDto(
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnStandingStatDto(
    val name: String? = null,
    val displayValue: String? = null,
    val value: Double? = null
)
