package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.MatchAlertEvent
import com.example.data.model.MatchPhase
import com.example.data.model.MatchPrediction
import com.example.data.model.MatchSubscription
import com.example.data.model.MatchSummary
import com.example.data.model.PredictionConfidence
import com.example.data.model.PredictionPick
import com.example.data.model.PredictionStatus
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

class PredictionRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeDocId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return cleaned.ifBlank { "match_unknown" }.take(120)
    }

    suspend fun ensureUserProfile(userId: String, displayName: String?): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val ref = db.collection("users").document(uid)
            val existing = ref.get().await()
            if (!existing.exists()) {
                val safeName = displayName?.trim()?.takeIf { it.isNotBlank() }?.take(60) ?: "PitchBulb Tipster"
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "accuracyPoints" to 0L,
                    "totalPredictions" to 0L,
                    "correctPredictions" to 0L,
                    "currentStreak" to 0L,
                    "adBoostTokens" to 1L,
                    "notificationsEnabled" to true,
                    "goalAlertsEnabled" to true,
                    "kickoffAlertsEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observePredictions(userId: String): Flow<List<MatchPrediction>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$uid/predictions"
        emitAll(
            db.collection("users").document(uid).collection("predictions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MatchPrediction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeSubscriptions(userId: String): Flow<List<MatchSubscription>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$uid/subscriptions"
        emitAll(
            db.collection("users").document(uid).collection("subscriptions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MatchSubscription::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun submitPrediction(
        userId: String,
        match: MatchSummary,
        pick: PredictionPick,
        predictedHomeScore: Int,
        predictedAwayScore: Int,
        confidence: PredictionConfidence,
        consumeBoostToken: Boolean
    ): Result<Unit> {
        val docId = sanitizeDocId(match.id)
        val docPath = "users/$userId/predictions/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val safeHome = predictedHomeScore.coerceIn(0, 20).toLong()
            val safeAway = predictedAwayScore.coerceIn(0, 20).toLong()
            val predRef = db.collection("users").document(uid).collection("predictions").document(docId)
            val existing = predRef.get().await()

            if (existing.exists()) {
                val currentStatus = existing.getString("status") ?: PredictionStatus.PENDING.name
                require(currentStatus == PredictionStatus.PENDING.name) {
                    "Settled predictions cannot be modified"
                }
                predRef.update(
                    mapOf(
                        "pick" to pick.name,
                        "predictedHomeScore" to safeHome,
                        "predictedAwayScore" to safeAway,
                        "confidence" to confidence.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "matchId" to docId,
                    "leagueCode" to match.leagueCode.take(64),
                    "leagueName" to match.leagueName.take(100),
                    "homeTeam" to match.homeTeam.take(100),
                    "awayTeam" to match.awayTeam.take(100),
                    "homeLogoUrl" to match.homeLogoUrl?.take(500),
                    "awayLogoUrl" to match.awayLogoUrl?.take(500),
                    "kickoffIso" to match.kickoffIso.ifBlank { "TBA" }.take(64),
                    "pick" to pick.name,
                    "predictedHomeScore" to safeHome,
                    "predictedAwayScore" to safeAway,
                    "confidence" to confidence.name,
                    "status" to PredictionStatus.PENDING.name,
                    "pointsAwarded" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>

                predRef.set(payload).await()
            }

            if (consumeBoostToken) {
                val userRef = db.collection("users").document(uid)
                val userSnap = userRef.get().await()
                val currentTokens = userSnap.getLong("adBoostTokens") ?: 0L
                if (currentTokens > 0L) {
                    userRef.update(
                        mapOf(
                            "adBoostTokens" to (currentTokens - 1L).coerceAtLeast(0L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun settleFinishedPredictions(
        userId: String,
        matchesById: Map<String, MatchSummary>,
        pendingPredictions: List<MatchPrediction>
    ): Result<Int> {
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            var settledCount = 0
            var totalPointsDelta = 0L
            var correctDelta = 0L
            var totalDelta = 0L

            for (prediction in pendingPredictions) {
                if (prediction.parsedStatus != PredictionStatus.PENDING) continue
                val finished = matchesById[prediction.matchId] ?: continue
                if (finished.phase != MatchPhase.FT) continue

                val actualPick = when {
                    finished.homeScore > finished.awayScore -> PredictionPick.HOME
                    finished.awayScore > finished.homeScore -> PredictionPick.AWAY
                    else -> PredictionPick.DRAW
                }
                val isOutcomeCorrect = prediction.parsedPick == actualPick
                val isExactScore =
                    prediction.predictedHomeScore.toInt() == finished.homeScore &&
                        prediction.predictedAwayScore.toInt() == finished.awayScore

                val basePoints = when {
                    isExactScore -> 50L
                    isOutcomeCorrect -> 20L
                    else -> 0L
                }
                val awarded = (basePoints * prediction.parsedConfidence.multiplier).coerceIn(0L, 100L)
                val newStatus = if (isOutcomeCorrect) PredictionStatus.WON else PredictionStatus.LOST

                val predRef = db.collection("users").document(uid)
                    .collection("predictions").document(prediction.id)
                predRef.update(
                    mapOf(
                        "status" to newStatus.name,
                        "pointsAwarded" to awarded,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                settledCount++
                totalDelta++
                if (isOutcomeCorrect) correctDelta++
                totalPointsDelta += awarded
            }

            if (settledCount > 0) {
                val userRef = db.collection("users").document(uid)
                val snap = userRef.get().await()
                val curPts = snap.getLong("accuracyPoints") ?: 0L
                val curTot = snap.getLong("totalPredictions") ?: 0L
                val curCor = snap.getLong("correctPredictions") ?: 0L
                val curStreak = snap.getLong("currentStreak") ?: 0L
                val nextStreak = if (correctDelta > 0L) curStreak + correctDelta else 0L

                userRef.update(
                    mapOf(
                        "accuracyPoints" to (curPts + totalPointsDelta).coerceAtMost(1000000L),
                        "totalPredictions" to (curTot + totalDelta).coerceAtMost(100000L),
                        "correctPredictions" to (curCor + correctDelta).coerceAtMost(100000L),
                        "currentStreak" to nextStreak.coerceAtMost(10000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(settledCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId/predictions")
            Result.failure(e)
        }
    }

    suspend fun toggleMatchSubscription(userId: String, match: MatchSummary): Result<Boolean> {
        val docId = sanitizeDocId(match.id)
        val docPath = "users/$userId/subscriptions/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val ref = db.collection("users").document(uid).collection("subscriptions").document(docId)
            val existing = ref.get().await()
            if (existing.exists()) {
                ref.delete().await()
                Result.success(false)
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "matchId" to docId,
                    "leagueCode" to match.leagueCode.take(64),
                    "homeTeam" to match.homeTeam.take(100),
                    "awayTeam" to match.awayTeam.take(100),
                    "lastKnownHomeScore" to match.homeScore.coerceIn(0, 50).toLong(),
                    "lastKnownAwayScore" to match.awayScore.coerceIn(0, 50).toLong(),
                    "lastKnownStatus" to match.phase.name,
                    "notifyGoals" to true,
                    "notifyKickoff" to true,
                    "notifyFullTime" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateNotificationPreferences(
        userId: String,
        notificationsEnabled: Boolean,
        goalAlertsEnabled: Boolean,
        kickoffAlertsEnabled: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            db.collection("users").document(uid).update(
                mapOf(
                    "notificationsEnabled" to notificationsEnabled,
                    "goalAlertsEnabled" to goalAlertsEnabled,
                    "kickoffAlertsEnabled" to kickoffAlertsEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun grantAdBoostToken(userId: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val userRef = db.collection("users").document(uid)
            val snap = userRef.get().await()
            val curTokens = snap.getLong("adBoostTokens") ?: 0L
            userRef.update(
                mapOf(
                    "adBoostTokens" to (curTokens + 1L).coerceAtMost(1000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncSubscriptionsAndDetectAlerts(
        userId: String,
        subscriptions: List<MatchSubscription>,
        matchesById: Map<String, MatchSummary>
    ): List<MatchAlertEvent> {
        val alerts = mutableListOf<MatchAlertEvent>()
        val uid = auth.currentUser?.uid ?: return emptyList()
        if (uid != userId) return emptyList()

        for (sub in subscriptions) {
            val live = matchesById[sub.matchId] ?: continue
            val prevHome = sub.lastKnownHomeScore.toInt()
            val prevAway = sub.lastKnownAwayScore.toInt()
            val prevStatus = sub.lastKnownStatus
            val curStatus = live.phase.name

            val scoreChanged = live.homeScore != prevHome || live.awayScore != prevAway
            val statusChanged = curStatus != prevStatus

            if (scoreChanged && sub.notifyGoals && (live.phase == MatchPhase.LIVE || live.phase == MatchPhase.HT || live.phase == MatchPhase.FT)) {
                val scorerSide = when {
                    abs(live.homeScore - prevHome) > abs(live.awayScore - prevAway) -> live.homeTeam
                    else -> live.awayTeam
                }
                alerts.add(
                    MatchAlertEvent(
                        matchId = live.id,
                        title = "GOAL! $scorerSide (${live.clockDisplay})",
                        body = "${live.homeTeam} ${live.homeScore} - ${live.awayScore} ${live.awayTeam} (${live.leagueName})",
                        isGoal = true
                    )
                )
            } else if (statusChanged) {
                if (prevStatus == MatchPhase.PRE.name && live.phase == MatchPhase.LIVE && sub.notifyKickoff) {
                    alerts.add(
                        MatchAlertEvent(
                            matchId = live.id,
                            title = "Kickoff! ${live.homeTeam} vs ${live.awayTeam}",
                            body = "${live.leagueName} match is now underway.",
                            isGoal = false
                        )
                    )
                } else if (prevStatus != MatchPhase.FT.name && live.phase == MatchPhase.FT && sub.notifyFullTime) {
                    alerts.add(
                        MatchAlertEvent(
                            matchId = live.id,
                            title = "Full Time: ${live.homeTeam} ${live.homeScore} - ${live.awayScore} ${live.awayTeam}",
                            body = "Final whistle in ${live.leagueName}.",
                            isGoal = false
                        )
                    )
                }
            }

            if (scoreChanged || statusChanged) {
                runCatching {
                    db.collection("users").document(uid)
                        .collection("subscriptions").document(sub.id)
                        .update(
                            mapOf(
                                "lastKnownHomeScore" to live.homeScore.coerceIn(0, 50).toLong(),
                                "lastKnownAwayScore" to live.awayScore.coerceIn(0, 50).toLong(),
                                "lastKnownStatus" to curStatus,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                }
            }
        }
        return alerts
    }
}
