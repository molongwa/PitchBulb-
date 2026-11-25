package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MatchDetailData
import com.example.data.model.MatchPhase
import com.example.data.model.MatchPrediction
import com.example.data.model.MatchSubscription
import com.example.data.model.MatchSummary
import com.example.data.model.NewsArticle
import com.example.data.model.PredictionConfidence
import com.example.data.model.PredictionPick
import com.example.data.model.PredictionStatus
import com.example.data.model.PlayerHistoryData
import com.example.data.model.SquadPlayer
import com.example.data.model.StandingRow
import com.example.data.model.TeamDetailData
import com.example.data.model.UserProfile
import com.example.data.model.WORLD_LEAGUES
import com.example.data.repository.PredictionRepository
import com.example.data.repository.SoccerRepository
import com.example.notifications.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class DateChipOption(
    val offsetDays: Int,
    val label: String,
    val apiDateParam: String
)

class MainViewModel(
    private val soccerRepository: SoccerRepository,
    private val predictionRepository: PredictionRepository,
    private val currentUserId: String,
    private val appContext: Context
) : ViewModel() {

    val dateOptions: List<DateChipOption> = buildDateOptions()

    private val _selectedDateOffset = MutableStateFlow(0)
    val selectedDateOffset: StateFlow<Int> = _selectedDateOffset.asStateFlow()

    private val _liveOnlyFilter = MutableStateFlow(false)
    val liveOnlyFilter: StateFlow<Boolean> = _liveOnlyFilter.asStateFlow()

    private val _selectedCountryFilter = MutableStateFlow<String?>(null)
    val selectedCountryFilter: StateFlow<String?> = _selectedCountryFilter.asStateFlow()

    private val _selectedLeagueFilter = MutableStateFlow<String?>(null)
    val selectedLeagueFilter: StateFlow<String?> = _selectedLeagueFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _matchesState = MutableStateFlow<UiState<List<MatchSummary>>>(UiState.Loading)
    val matchesState: StateFlow<UiState<List<MatchSummary>>> = _matchesState.asStateFlow()

    private val _newsState = MutableStateFlow<UiState<List<NewsArticle>>>(UiState.Loading)
    val newsState: StateFlow<UiState<List<NewsArticle>>> = _newsState.asStateFlow()

    private val _selectedStandingsLeague = MutableStateFlow("eng.1")
    val selectedStandingsLeague: StateFlow<String> = _selectedStandingsLeague.asStateFlow()

    private val _standingsState = MutableStateFlow<UiState<List<StandingRow>>>(UiState.Loading)
    val standingsState: StateFlow<UiState<List<StandingRow>>> = _standingsState.asStateFlow()

    private val _matchDetailState = MutableStateFlow<UiState<MatchDetailData>>(UiState.Loading)
    val matchDetailState: StateFlow<UiState<MatchDetailData>> = _matchDetailState.asStateFlow()

    private val _teamDetailState = MutableStateFlow<UiState<TeamDetailData>>(UiState.Loading)
    val teamDetailState: StateFlow<UiState<TeamDetailData>> = _teamDetailState.asStateFlow()

    private val _playerHistoryState = MutableStateFlow<UiState<PlayerHistoryData>?>(null)
    val playerHistoryState: StateFlow<UiState<PlayerHistoryData>?> = _playerHistoryState.asStateFlow()

    private val _actionBanner = MutableStateFlow<String?>(null)
    val actionBanner: StateFlow<String?> = _actionBanner.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile?>> = predictionRepository
        .observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing user profile", root)
            emit(UiState.Error(root.message ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val predictionsState: StateFlow<UiState<List<MatchPrediction>>> = predictionRepository
        .observePredictions(currentUserId)
        .map<List<MatchPrediction>, UiState<List<MatchPrediction>>> { list ->
            UiState.Success(list.sortedByDescending { it.updatedAt?.seconds ?: 0L })
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing predictions", root)
            emit(UiState.Error(root.message ?: "Failed to load predictions"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val subscriptionsState: StateFlow<UiState<List<MatchSubscription>>> = predictionRepository
        .observeSubscriptions(currentUserId)
        .map<List<MatchSubscription>, UiState<List<MatchSubscription>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing subscriptions", root)
            emit(UiState.Error(root.message ?: "Failed to load subscriptions"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            predictionRepository.ensureUserProfile(
                userId = currentUserId,
                displayName = Firebase.auth.currentUser?.displayName
            )
        }
        loadMatches(showLoading = true)
        loadNews()
        loadStandings(_selectedStandingsLeague.value)
        startLiveScorePolling()
    }

    fun selectDateOffset(offset: Int) {
        _selectedDateOffset.value = offset
        loadMatches(showLoading = true)
    }

    fun toggleLiveOnly() {
        _liveOnlyFilter.value = !_liveOnlyFilter.value
    }

    fun selectCountryFilter(country: String?) {
        val nextCountry = if (_selectedCountryFilter.value == country) null else country
        _selectedCountryFilter.value = nextCountry
        val currentLeague = _selectedLeagueFilter.value
        if (currentLeague != null && nextCountry != null) {
            val leagueCountry = WORLD_LEAGUES.firstOrNull { it.code == currentLeague }?.country
            if (leagueCountry != nextCountry) {
                _selectedLeagueFilter.value = null
            }
        } else if (nextCountry == null) {
            _selectedLeagueFilter.value = null
        }
        loadMatches(showLoading = true)
    }

    fun selectLeagueFilter(code: String?) {
        val next = if (_selectedLeagueFilter.value == code) null else code
        _selectedLeagueFilter.value = next
        if (next != null) {
            WORLD_LEAGUES.firstOrNull { it.code == next }?.country?.let { country ->
                if (_selectedCountryFilter.value != null) {
                    _selectedCountryFilter.value = country
                }
            }
        }
        loadMatches(showLoading = true)
    }

    fun selectCountryAndLeague(country: String?, leagueCode: String?) {
        _selectedCountryFilter.value = country
        _selectedLeagueFilter.value = leagueCode
        loadMatches(showLoading = true)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearBanner() {
        _actionBanner.value = null
    }

    fun refreshAll() {
        loadMatches(showLoading = true)
        loadNews()
        loadStandings(_selectedStandingsLeague.value)
    }

    fun loadMatches(showLoading: Boolean = false) {
        viewModelScope.launch {
            if (showLoading) {
                _matchesState.value = UiState.Loading
            }
            val dateParam = dateOptions.firstOrNull { it.offsetDays == _selectedDateOffset.value }?.apiDateParam
            val result = soccerRepository.fetchMatchesForDate(
                dateParam = dateParam,
                countryFilter = _selectedCountryFilter.value,
                leagueFilter = _selectedLeagueFilter.value
            )
            result.onSuccess { matches ->
                _matchesState.value = UiState.Success(matches)
                processLiveUpdatesAndSettlement(matches)
            }.onFailure { err ->
                if (_matchesState.value !is UiState.Success) {
                    _matchesState.value = UiState.Error(
                        err.localizedMessage ?: "Unable to reach live match feed. Check your connection."
                    )
                }
            }
        }
    }

    private fun startLiveScorePolling() {
        viewModelScope.launch {
            while (isActive) {
                delay(20_000L)
                if (_selectedDateOffset.value == 0) {
                    loadMatches(showLoading = false)
                }
            }
        }
    }

    private suspend fun processLiveUpdatesAndSettlement(matches: List<MatchSummary>) {
        val byId = matches.associateBy { it.id }

        val subs = (subscriptionsState.value as? UiState.Success)?.data.orEmpty()
        val profile = (userProfileState.value as? UiState.Success)?.data
        if (subs.isNotEmpty() && (profile?.notificationsEnabled != false)) {
            val alerts = predictionRepository.syncSubscriptionsAndDetectAlerts(
                userId = currentUserId,
                subscriptions = subs,
                matchesById = byId
            )
            alerts.forEach { alert ->
                NotificationHelper.showMatchAlertNotification(
                    context = appContext,
                    notificationId = alert.matchId.hashCode(),
                    title = alert.title,
                    body = alert.body
                )
            }
        }

        val pending = (predictionsState.value as? UiState.Success)?.data
            ?.filter { it.parsedStatus == PredictionStatus.PENDING }
            .orEmpty()
        if (pending.isNotEmpty()) {
            predictionRepository.settleFinishedPredictions(
                userId = currentUserId,
                matchesById = byId,
                pendingPredictions = pending
            ).onSuccess { count ->
                if (count > 0) {
                    _actionBanner.value = "Settled $count finished match prediction(s)!"
                }
            }
        }
    }

    fun loadNews() {
        viewModelScope.launch {
            _newsState.value = UiState.Loading
            soccerRepository.fetchNews()
                .onSuccess { _newsState.value = UiState.Success(it) }
                .onFailure {
                    _newsState.value = UiState.Error(it.localizedMessage ?: "Failed to load soccer news")
                }
        }
    }

    fun selectStandingsLeague(leagueCode: String) {
        _selectedStandingsLeague.value = leagueCode
        loadStandings(leagueCode)
    }

    fun loadStandings(leagueCode: String) {
        viewModelScope.launch {
            _standingsState.value = UiState.Loading
            soccerRepository.fetchStandings(leagueCode)
                .onSuccess { _standingsState.value = UiState.Success(it) }
                .onFailure {
                    _standingsState.value = UiState.Error(it.localizedMessage ?: "Standings unavailable")
                }
        }
    }

    fun loadMatchDetail(leagueCode: String, matchId: String) {
        viewModelScope.launch {
            _matchDetailState.value = UiState.Loading
            val cached = (_matchesState.value as? UiState.Success)?.data?.firstOrNull { it.id == matchId }
            val fallback = cached ?: soccerRepository.fetchMatchById(leagueCode, matchId)
            soccerRepository.fetchMatchDetail(leagueCode, matchId, fallback)
                .onSuccess { detail ->
                    _matchDetailState.value = UiState.Success(detail)
                }
                .onFailure { err ->
                    if (fallback != null) {
                        _matchDetailState.value = UiState.Success(
                            MatchDetailData(
                                match = fallback,
                                stats = emptyList(),
                                events = emptyList(),
                                homeLineup = emptyList(),
                                awayLineup = emptyList(),
                                referee = "TBA",
                                attendance = "N/A"
                            )
                        )
                    } else {
                        _matchDetailState.value = UiState.Error(err.localizedMessage ?: "Failed to load match details")
                    }
                }
        }
    }

    fun loadTeamDetail(
        leagueCode: String,
        teamId: String,
        teamNameHint: String = "",
        logoHint: String? = null
    ) {
        viewModelScope.launch {
            _teamDetailState.value = UiState.Loading
            soccerRepository.fetchTeamDetail(
                leagueCode = leagueCode,
                teamId = teamId,
                teamNameHint = teamNameHint,
                logoHint = logoHint
            ).onSuccess { data ->
                _teamDetailState.value = UiState.Success(data)
            }.onFailure { err ->
                _teamDetailState.value = UiState.Error(err.localizedMessage ?: "Failed to load team details")
            }
        }
    }

    fun loadPlayerHistory(leagueCode: String, player: SquadPlayer) {
        viewModelScope.launch {
            _playerHistoryState.value = UiState.Loading
            soccerRepository.fetchPlayerHistory(leagueCode, player)
                .onSuccess { _playerHistoryState.value = UiState.Success(it) }
                .onFailure { err ->
                    _playerHistoryState.value = UiState.Error(err.localizedMessage ?: "Player history unavailable")
                }
        }
    }

    fun clearPlayerHistory() {
        _playerHistoryState.value = null
    }

    fun submitPrediction(
        match: MatchSummary,
        pick: PredictionPick,
        homeScore: Int,
        awayScore: Int,
        confidence: PredictionConfidence
    ) {
        viewModelScope.launch {
            val profile = (userProfileState.value as? UiState.Success)?.data
            val wantsBoost = confidence == PredictionConfidence.BOOSTED
            if (wantsBoost && (profile?.adBoostTokens ?: 0L) <= 0L) {
                _actionBanner.value = "Watch a quick rewarded ad to unlock a 2x Boost token!"
                return@launch
            }
            val existingPred = (predictionsState.value as? UiState.Success)?.data?.firstOrNull { it.matchId == match.id }
            val shouldConsumeToken = wantsBoost && existingPred?.parsedConfidence != PredictionConfidence.BOOSTED

            predictionRepository.submitPrediction(
                userId = currentUserId,
                match = match,
                pick = pick,
                predictedHomeScore = homeScore,
                predictedAwayScore = awayScore,
                confidence = confidence,
                consumeBoostToken = shouldConsumeToken
            ).onSuccess {
                _actionBanner.value = "Prediction locked: ${match.homeShort} $homeScore - $awayScore ${match.awayShort}"
            }.onFailure { err ->
                _actionBanner.value = err.localizedMessage ?: "Could not save prediction"
            }
        }
    }

    fun toggleSubscription(match: MatchSummary) {
        viewModelScope.launch {
            predictionRepository.toggleMatchSubscription(currentUserId, match)
                .onSuccess { subscribed ->
                    if (subscribed) {
                        NotificationHelper.scheduleBackgroundMatchAlerts(appContext)
                        _actionBanner.value = "Alerts enabled for ${match.homeShort} vs ${match.awayShort}"
                    } else {
                        _actionBanner.value = "Alerts muted for ${match.homeShort} vs ${match.awayShort}"
                    }
                }
                .onFailure { err ->
                    _actionBanner.value = err.localizedMessage ?: "Failed to update alert subscription"
                }
        }
    }

    fun updateNotificationPreferences(
        notificationsEnabled: Boolean,
        goalAlertsEnabled: Boolean,
        kickoffAlertsEnabled: Boolean
    ) {
        viewModelScope.launch {
            predictionRepository.updateNotificationPreferences(
                userId = currentUserId,
                notificationsEnabled = notificationsEnabled,
                goalAlertsEnabled = goalAlertsEnabled,
                kickoffAlertsEnabled = kickoffAlertsEnabled
            ).onFailure { err ->
                _actionBanner.value = err.localizedMessage ?: "Failed to update preferences"
            }
        }
    }

    fun onRewardedAdCompleted() {
        viewModelScope.launch {
            predictionRepository.grantAdBoostToken(currentUserId)
                .onSuccess {
                    _actionBanner.value = "+1 2x Prediction Boost Token earned!"
                }
                .onFailure { err ->
                    _actionBanner.value = err.localizedMessage ?: "Failed to credit boost token"
                }
        }
    }

    fun triggerSampleGoalNotification() {
        val sampleMatch = (_matchesState.value as? UiState.Success)?.data
            ?.firstOrNull { it.phase == MatchPhase.LIVE }
            ?: (_matchesState.value as? UiState.Success)?.data?.firstOrNull()

        val title = if (sampleMatch != null) {
            "GOAL! ${sampleMatch.homeTeam} (${if (sampleMatch.phase == MatchPhase.LIVE) sampleMatch.clockDisplay else "68'"})"
        } else {
            "GOAL! Arsenal (68')"
        }
        val body = if (sampleMatch != null) {
            "${sampleMatch.homeTeam} ${sampleMatch.homeScore + 1} - ${sampleMatch.awayScore} ${sampleMatch.awayTeam} • ${sampleMatch.leagueName}"
        } else {
            "Arsenal 2 - 1 Chelsea • Premier League"
        }
        NotificationHelper.showMatchAlertNotification(
            context = appContext,
            notificationId = 9001,
            title = title,
            body = body
        )
        _actionBanner.value = "Test match alert dispatched!"
    }

    private fun buildDateOptions(): List<DateChipOption> {
        val apiFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        val labelFormat = SimpleDateFormat("EEE d", Locale.getDefault())
        return (-1..3).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
            val label = when (offset) {
                -1 -> "Yesterday"
                0 -> "Today"
                1 -> "Tomorrow"
                else -> labelFormat.format(cal.time)
            }
            DateChipOption(
                offsetDays = offset,
                label = label,
                apiDateParam = apiFormat.format(cal.time)
            )
        }
    }

    private companion object {
        const val TAG = "MainVM"
    }
}
