package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.model.MatchSubscription
import com.example.data.repository.PredictionRepository
import com.example.data.repository.SoccerRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MatchAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val currentUser = Firebase.auth.currentUser ?: return Result.success()
        val uid = currentUser.uid
        if (uid.isBlank()) return Result.success()

        return runCatching {
            val db = FirebaseFirestore.getInstance(
                applicationContext.getString(R.string.firestore_database_id)
            )
            val predictionRepo = PredictionRepository(db)
            val soccerRepo = SoccerRepository()

            val subSnap = db.collection("users").document(uid)
                .collection("subscriptions")
                .get()
                .await()
            val subscriptions = subSnap.toObjects(
                MatchSubscription::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            if (subscriptions.isEmpty()) return Result.success()

            val matches = soccerRepo.fetchMatchesForDate(null).getOrNull().orEmpty()
            if (matches.isEmpty()) return Result.retry()

            val alerts = predictionRepo.syncSubscriptionsAndDetectAlerts(
                userId = uid,
                subscriptions = subscriptions,
                matchesById = matches.associateBy { it.id }
            )
            alerts.forEach { alert ->
                NotificationHelper.showMatchAlertNotification(
                    context = applicationContext,
                    notificationId = alert.matchId.hashCode(),
                    title = alert.title,
                    body = alert.body
                )
            }
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }
}
