package com.shop.billing.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shop.billing.data.remote.DownloadState
import com.shop.billing.ui.navigation.NavRoutes
import com.shop.billing.ui.theme.Blue227ed4
import com.shop.billing.ui.theme.SurfaceGray
import com.shop.billing.ui.theme.TextPrimary
import com.shop.billing.ui.theme.TextSecondary
import com.shop.billing.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val itemCount by viewModel.itemCount.collectAsState()
    val billCount by viewModel.billCount.collectAsState()
    val totalSales by viewModel.totalSales.collectAsState()
    val customerCount by viewModel.customerCount.collectAsState()
    val shopName by viewModel.shopName.collectAsState()
    val syncEnabled by viewModel.syncEnabled.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val logEntries by viewModel.logEntries.collectAsState()
    val apiOnline by viewModel.apiOnline.collectAsState()
    val websocketOnline by viewModel.websocketOnline.collectAsState()
    val showLog by viewModel.showLog.collectAsState()
    val updateAvailable by viewModel.updateAvailable.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val logListState = rememberLazyListState()

    val isDownloading = downloadState.isDownloading || downloadState.isComplete
    val showDownloadOverlay = isDownloading || downloadState.error != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            TopAppBar(
            title = {
                Text(
                    text = shopName.ifBlank { "My Shop" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            actions = {
                // Update button (only show if update is available)
                if (updateAvailable != null) {
                    IconButton(onClick = { viewModel.downloadUpdate() }) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "Update Available",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                IconButton(onClick = { viewModel.syncNow() }, enabled = !isSyncing) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Blue227ed4,
                titleContentColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                onClick = { navController.navigate(NavRoutes.NewBill.route) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(listOf(Blue227ed4, Color(0xFF0EA5E9)))
                            )
                            .padding(12.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "New Bill",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Create a new invoice",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Items",
                    value = "$itemCount",
                    icon = Icons.Default.Inventory2,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(NavRoutes.Items.route) }
                )
                StatCard(
                    label = "Bills",
                    value = "$billCount",
                    icon = Icons.Default.Receipt,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(NavRoutes.History.route) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Customers",
                    value = "$customerCount",
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(NavRoutes.DatabaseManager.createRoute(2)) }
                )
                StatCard(
                    label = "Sales",
                    value = "${Constants.CURRENCY_SYMBOL}${totalSales.toLong()}",
                    icon = Icons.Default.Add,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(NavRoutes.History.route) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Ledger",
                    value = "$customerCount",
                    icon = Icons.Default.MenuBook,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(NavRoutes.CustomerLedger.route) }
                )
                StatCard(
                    label = "Settings",
                    value = "\u2699",
                    icon = Icons.Default.Settings,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(NavRoutes.Settings.route) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (apiOnline) Color(0xFF43A047) else Color(0xFFE53935))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (apiOnline) "API Online" else "API Offline",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (websocketOnline) Color(0xFF43A047) else Color(0xFFE53935))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (websocketOnline) "WebSocket Online" else "WebSocket Offline",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
            }

            if (showLog) {
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Realtime Log",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${logEntries.size} entries",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.clearLog() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Clear Log",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.toggleLog() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close Log",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE5E7EB)))
                        LazyColumn(
                            state = logListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFFFAFBFC)),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            if (logEntries.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No logs yet",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            } else {
                                items(logEntries) { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (entry.type) {
                                                        LogType.SUCCESS -> Color(0xFF43A047)
                                                        LogType.ERROR -> Color(0xFFE53935)
                                                        LogType.INFO -> Blue227ed4
                                                    }
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = entry.timestamp,
                                            fontSize = 10.sp,
                                            color = Color(0xFF9CA3AF),
                                            modifier = Modifier.width(50.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = entry.message,
                                            fontSize = 11.sp,
                                            color = TextPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    onClick = { viewModel.toggleLog() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Show Log (${logEntries.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        }

        // Download progress overlay
        if (showDownloadOverlay) {
            DownloadProgressOverlay(
                downloadState = downloadState,
                onCancel = { viewModel.cancelDownload() },
                onInstall = {
                    downloadState.uri?.let { uri ->
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/vnd.android.package-archive")
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                    viewModel.dismissUpdate()
                },
                onDismiss = { viewModel.dismissUpdate() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(Blue227ed4, Color(0xFF0EA5E9)))
                    )
                    .padding(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024f)
        else -> String.format("%.1f MB", bytes / (1024f * 1024f))
    }
}

@Composable
private fun DownloadProgressOverlay(
    downloadState: DownloadState,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val animatedProgress = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(downloadState.progress) {
        animatedProgress.animateTo(
            targetValue = downloadState.progress,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 300,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 340.dp)
                .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (downloadState.isComplete) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Download Complete",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF22C55E)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ready to install",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        } else if (downloadState.error != null) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Download Failed",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = downloadState.error,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        } else {
                            // Animated circular progress
                            Box(
                                modifier = Modifier.size(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    progress = animatedProgress.value,
                                    modifier = Modifier.fillMaxSize(),
                                    color = Color(0xFF227ED4),
                                    trackColor = Color(0xFFE5E7EB),
                                    strokeWidth = 8.dp,
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${(downloadState.progress * 100).toInt()}%",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF227ED4)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Downloading Update",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (downloadState.totalBytes > 0) {
                                Text(
                                    text = "${formatFileSize(downloadState.bytesDownloaded)} / ${formatFileSize(downloadState.totalBytes)}",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (downloadState.isComplete) {
                            Button(
                                onClick = onInstall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                            ) {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Install", fontWeight = FontWeight.SemiBold)
                            }
                        } else if (downloadState.error != null) {
                            Button(
                                onClick = { onDismiss() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Text("Dismiss", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Button(
                                onClick = onCancel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B7280))
                            ) {
                                Text("Cancel", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Close button - top right corner
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(36.dp)
                            .padding(top = 8.dp, end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
    }
}
