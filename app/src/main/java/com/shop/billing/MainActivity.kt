package com.shop.billing

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.shop.billing.data.remote.UpdateNotificationManager
import com.shop.billing.ui.navigation.AppNavigation
import com.shop.billing.ui.navigation.NavRoutes
import com.shop.billing.ui.screens.settings.SettingsViewModel
import com.shop.billing.ui.theme.BillingTheme
import com.shop.billing.ui.theme.Blue227ed4
import com.shop.billing.ui.theme.TealAccent
import com.shop.billing.data.sync.SyncEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var syncEngine: SyncEngine

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        window.statusBarColor = android.graphics.Color.parseColor("#227ed4")

        val sp = getSharedPreferences("billing_prefs", MODE_PRIVATE)
        var userId = sp.getString("user_id", null)

        if (userId.isNullOrBlank()) {
            val prefs = runBlocking { dataStore.data.first() }
            userId = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)]
            if (!userId.isNullOrBlank()) {
                sp.edit().putString("user_id", userId).apply()
            }
        }

        val startDest = if (!userId.isNullOrBlank()) NavRoutes.Home.route else NavRoutes.Auth.route
        val openSettings = intent?.getBooleanExtra(UpdateNotificationManager.EXTRA_OPEN_SETTINGS, false) == true
        if (intent?.getBooleanExtra(UpdateNotificationManager.EXTRA_AUTO_DOWNLOAD, false) == true) {
            SettingsViewModel.pendingAutoDownload = true
            SettingsViewModel.pendingDownloadUrl = intent.getStringExtra(UpdateNotificationManager.EXTRA_DOWNLOAD_URL)
            SettingsViewModel.pendingVersionName = intent.getStringExtra(UpdateNotificationManager.EXTRA_VERSION_NAME)
        }

        setContent {
            BillingTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    val splashAlpha = remember { Animatable(0f) }
                    val splashScale = remember { Animatable(0.3f) }

                    LaunchedEffect(Unit) {
                        launch { splashAlpha.animateTo(1f, tween(400)) }
                        splashScale.snapTo(1.3f)
                        splashScale.animateTo(1f, tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                        delay(1000)
                        showSplash = false
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.rupee),
                            contentDescription = null,
                            modifier = Modifier
                                .size(160.dp)
                                .scale(splashScale.value)
                                .alpha(splashAlpha.value)
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val nc = rememberNavController()
                        navController = nc
                        AppNavigation(
                            navController = nc,
                            startDestination = startDest
                        )
                        if (openSettings) {
                            LaunchedEffect(Unit) {
                                nc.navigate(NavRoutes.Settings.route)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.getBooleanExtra(UpdateNotificationManager.EXTRA_OPEN_SETTINGS, false) == true) {
            setIntent(intent)
            if (intent.getBooleanExtra(UpdateNotificationManager.EXTRA_AUTO_DOWNLOAD, false)) {
                SettingsViewModel.pendingAutoDownload = true
                SettingsViewModel.pendingDownloadUrl = intent.getStringExtra(UpdateNotificationManager.EXTRA_DOWNLOAD_URL)
                SettingsViewModel.pendingVersionName = intent.getStringExtra(UpdateNotificationManager.EXTRA_VERSION_NAME)
            }
            navController?.navigate(NavRoutes.Settings.route) {
                popUpTo(NavRoutes.Home.route)
            }
        }
    }
}
