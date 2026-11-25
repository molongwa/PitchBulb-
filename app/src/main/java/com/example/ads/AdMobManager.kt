package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.theme.PitchBorder
import com.example.ui.theme.PitchSurfaceElevated
import com.example.ui.theme.SlateDim
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AdMobManager {
    private const val TAG = "PitchPulseAds"
    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var initialized = false
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var lastInterstitialShownMs: Long = 0L
    private const val INTERSTITIAL_COOLDOWN_MS = 120_000L

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        val appContext = context.applicationContext
        initScope.launch {
            runCatching {
                MobileAds.initialize(appContext) {
                    Log.d(TAG, "Google Mobile Ads SDK initialized")
                    initScope.launch(Dispatchers.Main) {
                        loadRewardedAd(appContext)
                        loadInterstitialAd(appContext)
                    }
                }
            }.onFailure {
                Log.w(TAG, "MobileAds init error: ${it.message}")
            }
        }
    }

    fun loadRewardedAd(context: Context) {
        val unitId = context.getString(R.string.admob_rewarded_unit_id)
        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context.applicationContext,
            unitId,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded ad ready")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.w(TAG, "Rewarded ad failed: ${error.message}")
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity?,
        onRewardEarned: () -> Unit,
        onUnavailable: (String) -> Unit
    ) {
        val ad = rewardedAd
        if (activity == null || ad == null) {
            if (activity != null) loadRewardedAd(activity)
            onUnavailable("Ad is still loading. Try again in a moment.")
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewardedAd(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                loadRewardedAd(activity)
            }
        }
        ad.show(activity) {
            onRewardEarned()
        }
    }

    fun loadInterstitialAd(context: Context) {
        val unitId = context.getString(R.string.admob_interstitial_unit_id)
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            unitId,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun maybeShowInterstitial(activity: Activity?) {
        val now = System.currentTimeMillis()
        if (activity == null || now - lastInterstitialShownMs < INTERSTITIAL_COOLDOWN_MS) return
        val ad = interstitialAd ?: return
        lastInterstitialShownMs = now
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitialAd(activity)
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                interstitialAd = null
                loadInterstitialAd(activity)
            }
        }
        ad.show(activity)
    }
}

@Composable
fun AdMobBannerBar(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val unitId = remember(context) { context.getString(R.string.admob_banner_unit_id) }
    val adView = remember(context, unitId) {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = unitId
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(lifecycleOwner, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admob_banner_bar")
            .clip(RoundedCornerShape(12.dp))
            .background(PitchSurfaceElevated)
            .border(1.dp, PitchBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SPONSORED",
            style = MaterialTheme.typography.labelSmall,
            color = SlateDim,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { adView }
            )
        }
    }
}
