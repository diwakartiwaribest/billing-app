package com.shop.billing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.shop.billing.ui.navigation.AppNavigation
import com.shop.billing.ui.navigation.NavRoutes
import com.shop.billing.ui.theme.BillingTheme
import com.shop.billing.ui.theme.Blue227ed4
import com.shop.billing.ui.theme.TealAccent
import dagger.hilt.android.AndroidEntryPoint
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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

        setContent {
            BillingTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    val scale = remember { Animatable(0.6f) }

                    LaunchedEffect(Unit) {
                        scale.animateTo(
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                    }

                    LaunchedEffect(Unit) {
                        delay(2000)
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
                                .size(140.dp)
                                .scale(scale.value)
                                .clip(RoundedCornerShape(32.dp))
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        AppNavigation(
                            navController = navController,
                            startDestination = startDest
                        )
                    }
                }
            }
        }
    }
}
