package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PredictionPick {
    HOME,
    DRAW,
    AWAY;

    companion object {
        fun fromRaw(raw: String?): PredictionPick =
            entries.firstOrNull { it.name == raw } ?: HOME
    }
}

enum class PredictionConfidence(val label: String, val multiplier: Int) {
    STANDARD("1x Standard", 1),
    HIGH("1.5x High", 1),
    BOOSTED("2x Ad Boost", 2);

    companion object {
        fun fromRaw(raw: String?): PredictionConfidence =
            entries.firstOrNull { it.name == raw } ?: STANDARD
    }
}

enum class PredictionStatus {
    PENDING,
    WON,
    LOST;

    companion object {
        fun fromRaw(raw: String?): PredictionStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Manager",
    val accuracyPoints: Long = 0L,
    val totalPredictions: Long = 0L,
    val correctPredictions: Long = 0L,
    val currentStreak: Long = 0L,
    val adBoostTokens: Long = 1L,
    val notificationsEnabled: Boolean = true,
    val goalAlertsEnabled: Boolean = true,
    val kickoffAlertsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MatchPrediction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val matchId: String = "",
    val leagueCode: String = "",
    val leagueName: String = "",
    val homeTeam: String = "",
    val awayTeam: String = "",
    val homeLogoUrl: String? = null,
    val awayLogoUrl: String? = null,
    val kickoffIso: String = "",
    val pick: String = PredictionPick.HOME.name,
    val predictedHomeScore: Long = 1L,
    val predictedAwayScore: Long = 0L,
    val confidence: String = PredictionConfidence.STANDARD.name,
    val status: String = PredictionStatus.PENDING.name,
    val pointsAwarded: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedPick: PredictionPick get() = PredictionPick.fromRaw(pick)
    val parsedConfidence: PredictionConfidence get() = PredictionConfidence.fromRaw(confidence)
    val parsedStatus: PredictionStatus get() = PredictionStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class MatchSubscription(
    @DocumentId val id: String = "",
    val userId: String = "",
    val matchId: String = "",
    val leagueCode: String = "",
    val homeTeam: String = "",
    val awayTeam: String = "",
    val lastKnownHomeScore: Long = 0L,
    val lastKnownAwayScore: Long = 0L,
    val lastKnownStatus: String = MatchPhase.PRE.name,
    val notifyGoals: Boolean = true,
    val notifyKickoff: Boolean = true,
    val notifyFullTime: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
