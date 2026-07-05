package com.shop.billing.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shop.billing.ui.navigation.NavRoutes
import com.shop.billing.data.sync.LogEntry
import com.shop.billing.data.sync.LogType
import com.shop.billing.ui.components.ConfirmDialogOverlay
import com.shop.billing.ui.components.DialogConfirmButton
import com.shop.billing.ui.components.DialogOverlay
import com.shop.billing.ui.theme.Blue227ed4
import com.shop.billing.ui.theme.SurfaceGray
import com.shop.billing.ui.theme.TextPrimary
import com.shop.billing.ui.theme.TextSecondary
import com.shop.billing.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val shopName by viewModel.shopName.collectAsState()
    val shopAddress by viewModel.shopAddress.collectAsState()
    val shopPhone by viewModel.shopPhone.collectAsState()
    val logoBase64 by viewModel.logoUri.collectAsState()
    val shopCode by viewModel.shopCode.collectAsState()
    val shopSecret by viewModel.shopSecret.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val restoreState by viewModel.restoreState.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val isOwner = userRole == "owner"
    val isAdmin = userRole == "admin"
    val databaseStats by viewModel.databaseStats.collectAsState()
    val members by viewModel.members.collectAsState()
    val memberActionState by viewModel.memberActionState.collectAsState()
    val currentVersionName by viewModel.currentVersionName.collectAsState()
    val updateAvailable by viewModel.updateAvailable.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val updateCheckError by viewModel.updateCheckError.collectAsState()
    val updateDismissed by viewModel.updateDismissed.collectAsState()

    LaunchedEffect(Unit) {
        if (SettingsViewModel.pendingAutoDownload) {
            SettingsViewModel.pendingAutoDownload = false
            viewModel.autoCheckAndDownload()
        }
    }

    val viewModelQrBitmap by viewModel.qrBitmap.collectAsState()
    val qrBitmap = viewModelQrBitmap?.asImageBitmap()

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }
    var showPurgeConfirm by remember { mutableStateOf(false) }
    val initialPurgeDays by viewModel.purgeDays.collectAsState()
    var purgeDaysInput by remember(initialPurgeDays) { mutableStateOf(initialPurgeDays.toString()) }
    val purgeInProgress by viewModel.purgeInProgress.collectAsState()
    val purgeResult by viewModel.purgeResult.collectAsState()

    val context = LocalContext.current

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) viewModel.backupData(uri)
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirm = true
        }
    }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.saveLogo(uri)
    }

    LaunchedEffect(backupState) {
        if (backupState is BackupState.Success) {
            Toast.makeText(context, "Backup: ${(backupState as BackupState.Success).fileName}", Toast.LENGTH_SHORT).show()
            viewModel.resetBackupState()
        } else if (backupState is BackupState.Error) {
            Toast.makeText(context, (backupState as BackupState.Error).message, Toast.LENGTH_SHORT).show()
            viewModel.resetBackupState()
        }
    }

    LaunchedEffect(restoreState) {
        if (restoreState is RestoreState.Success) {
            Toast.makeText(context, "Restored: ${(restoreState as RestoreState.Success).summary}", Toast.LENGTH_SHORT).show()
            viewModel.resetRestoreState()
        } else if (restoreState is RestoreState.Error) {
            Toast.makeText(context, (restoreState as RestoreState.Error).message, Toast.LENGTH_SHORT).show()
            viewModel.resetRestoreState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Blue227ed4)
            )
        },
        containerColor = SurfaceGray
    ) { padding ->
        val scrollState = rememberScrollState()
        LaunchedEffect(downloadState.isDownloading) {
            if (downloadState.isDownloading) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Shop Information
            SectionHeader(icon = Icons.Default.Store, title = "Shop Information")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Logo at top, centered, bigger
                    if (logoBase64 != null) {
                        val logoBitmap = remember(logoBase64) {
                            try {
                                val bytes = android.util.Base64.decode(logoBase64, android.util.Base64.NO_WRAP)
                                val head = String(bytes, 0, minOf(bytes.size, 200)).lowercase()
                                if ("<svg" in head) {
                                    val svg = com.caverock.androidsvg.SVG.getFromInputStream(bytes.inputStream())
                                    val w = (svg.documentWidth.takeIf { it > 0 } ?: 400f).toInt()
                                    val h = (svg.documentHeight.takeIf { it > 0 } ?: 400f).toInt()
                                    val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                                    svg.renderToCanvas(android.graphics.Canvas(bmp))
                                    bmp.asImageBitmap()
                                } else {
                                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                }
                            } catch (_: Exception) { null }
                        }
                        if (logoBitmap != null) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Image(
                                    bitmap = logoBitmap,
                                    contentDescription = "Logo",
                                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    // Choose/Remove buttons below logo
                    if (isOwner || isAdmin) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Button(
                                onClick = { logoPickerLauncher.launch(arrayOf("image/*", "image/svg+xml")) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF8FAFC), contentColor = TextPrimary)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Choose Logo")
                            }
                            if (logoBase64 != null) {
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(onClick = { viewModel.removeLogo() }) {
                                    Text("Remove")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { if (isOwner || isAdmin) viewModel.updateShopName(it) },
                        label = { Text("Shop Name") },
                        singleLine = true,
                        readOnly = !(isOwner || isAdmin),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue227ed4, unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = shopAddress,
                        onValueChange = { if (isOwner || isAdmin) viewModel.updateShopAddress(it) },
                        label = { Text("Address") },
                        singleLine = true,
                        readOnly = !(isOwner || isAdmin),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue227ed4, unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = shopPhone,
                        onValueChange = { if (isOwner || isAdmin) viewModel.updateShopPhone(it.filter { c -> c.isDigit() }.take(10)) },
                        label = { Text("Phone") },
                        singleLine = true,
                        readOnly = !(isOwner || isAdmin),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue227ed4, unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.invoiceMessage.collectAsState().value,
                        onValueChange = { if (isOwner || isAdmin) viewModel.updateInvoiceMessage(it) },
                        label = { Text("Invoice Message") },
                        singleLine = true,
                        readOnly = !(isOwner || isAdmin),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue227ed4, unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            // Shop Code & Secret
            SectionHeader(icon = Icons.Default.Storage, title = "Shop Code")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Your Shop Code", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = shopCode.ifBlank { "Not set" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (shopCode.isBlank()) TextSecondary else Blue227ed4,
                                letterSpacing = 2.sp
                            )
                        }
                        if (shopCode.isNotBlank()) {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Shop Code", shopCode))
                                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                            }) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Blue227ed4.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = Blue227ed4,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (shopSecret.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Divider(color = Color(0xFFE2E8F0))
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Shop Secret", fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    text = shopSecret,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    letterSpacing = 1.5.sp
                                )
                            }
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Shop Secret", shopSecret))
                                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                            }) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Blue227ed4.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = Blue227ed4,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Share secret only with trusted users", fontSize = 11.sp, color = TextSecondary)
                    }
                    if ((isOwner || isAdmin) && qrBitmap != null) {
                        Spacer(Modifier.height(16.dp))
                        Divider(color = Color(0xFFE2E8F0))
                        Spacer(Modifier.height(16.dp))
                        Text("Scan to Join Shop", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Image(bitmap = qrBitmap, contentDescription = "Shop QR", modifier = Modifier.size(180.dp))
                        }
                    }
                }
            }

            // Shop Members
            if (isOwner || isAdmin) {
                SectionHeader(icon = Icons.Default.Group, title = "Shop Members")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (members.isEmpty()) {
                            Text("Loading members...", fontSize = 13.sp, color = TextSecondary)
                        } else {
                            members.forEach { member ->
                                MemberRow(
                                    member = member,
                                    currentUserId = viewModel.currentUserId.collectAsState().value,
                                    canManage = isOwner || isAdmin,
                                    isCurrentUserOwner = isOwner,
                                    shopOwnerId = viewModel.shopOwnerId.collectAsState().value,
                                    onPromote = { viewModel.updateMemberRole(member.userId, "admin") },
                                    onDemote = { viewModel.updateMemberRole(member.userId, "member") },
                                    onRemove = { viewModel.removeMember(member.userId) },
                                    onTransferOwnership = { viewModel.transferOwnership(member.userId) }
                                )
                                if (member != members.last()) {
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }

                        memberActionState?.let { msg ->
                            Spacer(Modifier.height(8.dp))
                            LaunchedEffect(msg) {
                                kotlinx.coroutines.delay(2000)
                                viewModel.resetMemberActionState()
                            }
                            Text(msg, fontSize = 12.sp, color = Color(0xFF22C55E))
                        }
                    }
                }
            }

            // Backup & Restore
            if (isOwner || isAdmin) {
                SectionHeader(icon = Icons.Default.Cloud, title = "Backup & Restore")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Backup Options", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = viewModel.backupBills.collectAsState().value, onCheckedChange = { viewModel.backupBills.value = it }, colors = CheckboxDefaults.colors(checkedColor = Blue227ed4))
                            Text("Bills", fontSize = 14.sp, color = TextPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = viewModel.backupShopItems.collectAsState().value, onCheckedChange = { viewModel.backupShopItems.value = it }, colors = CheckboxDefaults.colors(checkedColor = Blue227ed4))
                            Text("Shop Items", fontSize = 14.sp, color = TextPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = viewModel.backupSettings.collectAsState().value, onCheckedChange = { viewModel.backupSettings.value = it }, colors = CheckboxDefaults.colors(checkedColor = Blue227ed4))
                            Text("Settings", fontSize = 14.sp, color = TextPrimary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { backupLauncher.launch(null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue227ed4)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Backup to File", fontWeight = FontWeight.SemiBold)
                        }
                        if (backupState is BackupState.InProgress) {
                            Text("Backing up...", fontSize = 12.sp, color = TextSecondary)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Restore Options", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = viewModel.restoreBills.collectAsState().value, onCheckedChange = { viewModel.restoreBills.value = it }, colors = CheckboxDefaults.colors(checkedColor = Blue227ed4))
                            Text("Bills", fontSize = 14.sp, color = TextPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = viewModel.restoreShopItems.collectAsState().value, onCheckedChange = { viewModel.restoreShopItems.value = it }, colors = CheckboxDefaults.colors(checkedColor = Blue227ed4))
                            Text("Shop Items", fontSize = 14.sp, color = TextPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = viewModel.restoreSettings.collectAsState().value, onCheckedChange = { viewModel.restoreSettings.value = it }, colors = CheckboxDefaults.colors(checkedColor = Blue227ed4))
                            Text("Settings", fontSize = 14.sp, color = TextPrimary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { restoreLauncher.launch(arrayOf("application/zip", "*/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue227ed4)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Restore from File", fontWeight = FontWeight.SemiBold)
                        }
                        if (restoreState is RestoreState.InProgress) {
                            Text("Restoring...", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // Data Retention (purge)
            if (isOwner || isAdmin) {
                SectionHeader(icon = Icons.Default.Outbox, title = "Data Retention")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Soft-deleted records (bills, customers, items, payments) are kept in the cloud until purged.", fontSize = 12.sp, color = TextSecondary)
                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Auto-purge after", fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = purgeDaysInput,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(3)
                                    purgeDaysInput = digits
                                },
                                label = { Text("Days") },
                                singleLine = true,
                                modifier = Modifier.width(110.dp),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Blue227ed4, unfocusedBorderColor = Color(0xFFE2E8F0))
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val days = purgeDaysInput.toIntOrNull() ?: 0
                                    viewModel.updatePurgeDays(days)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Blue227ed4)
                            ) { Text("Save") }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "0 = disabled. Range: 0 – ${Constants.MAX_PURGE_DAYS}. Default: ${Constants.DEFAULT_PURGE_DAYS}.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { showPurgeConfirm = true },
                            enabled = !purgeInProgress,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Icon(Icons.Default.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (purgeInProgress) "Purging..." else "Purge All Deleted Data Now", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Recycle Bin
            if (shopCode.isNotBlank()) {
                SectionHeader(icon = Icons.Default.DeleteOutline, title = "Recycle Bin")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Restore deleted bills, items, customers, or payments from the cloud.", fontSize = 12.sp, color = TextSecondary)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { navController.navigate(NavRoutes.RecycleBin.route) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue227ed4)
                        ) {
                            Icon(Icons.Default.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Open Recycle Bin", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Database Stats
            if (isOwner || isAdmin) {
                SectionHeader(icon = Icons.Default.Storage, title = "Database Stats")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // --- RECORDS ---
                        StatsCategoryHeader("RECORDS")
                        Spacer(Modifier.height(8.dp))
                        StatsRow("Products", "${databaseStats.products}")
                        StatsRow("Customers", "${databaseStats.customers}")
                        StatsRow("Invoices", "${databaseStats.invoices}")
                        StatsRow("Invoice Items", "${databaseStats.invoiceItems}")
                        StatsRow("Payments", "${databaseStats.payments}")
                        StatsRow("Investments", "${databaseStats.investments}")

                        Spacer(Modifier.height(16.dp))

                        // --- GROWTH & ACTIVITY ---
                        StatsCategoryHeader("GROWTH & ACTIVITY")
                        Spacer(Modifier.height(8.dp))
                        StatsRow("Today's Sales", formatCurrency(databaseStats.todaySales), Color(0xFF059669))
                        StatsRow("Avg Invoice Value", formatCurrency(databaseStats.avgInvoiceValue))
                        StatsRow(
                            "Profit Margin",
                            if (databaseStats.profitMarginPercent >= 0) "${"%,.1f".format(databaseStats.profitMarginPercent)}%"
                            else "${"%,.1f".format(databaseStats.profitMarginPercent)}%",
                            if (databaseStats.profitMarginPercent >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                        )

                        Spacer(Modifier.height(16.dp))

                        // --- FINANCIALS ---
                        StatsCategoryHeader("FINANCIALS")
                        Spacer(Modifier.height(8.dp))
                        StatsRow("Total Sales", formatCurrency(databaseStats.totalSales))
                        StatsRow("Total Paid", formatCurrency(databaseStats.totalPayments))
                        StatsRow(
                            "Outstanding Credit",
                            formatCurrency(databaseStats.creditAmount),
                            if (databaseStats.creditAmount > 0) Color(0xFFDC2626) else Blue227ed4
                        )
                        StatsRow("Total Invested", formatCurrency(databaseStats.totalInvested))
                        val netProfit = databaseStats.totalSales - databaseStats.totalInvested
                        StatsRow(
                            "Net Profit",
                            formatCurrency(netProfit),
                            if (netProfit >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                        )

                        Spacer(Modifier.height(16.dp))

                        // --- INVENTORY DEPTH ---
                        StatsCategoryHeader("INVENTORY DEPTH")
                        Spacer(Modifier.height(8.dp))
                        StatsRow("Total Stock Value (Cost)", formatCurrency(databaseStats.totalStockValue))
                        StatsRow("Total Stock Value (MRP)", formatCurrency(databaseStats.totalStockMrp))
                        StatsRow("Categories", "${databaseStats.categoryCount}")
                        StatsRow(
                            "Low Stock Products",
                            "${databaseStats.lowStockProducts}",
                            if (databaseStats.lowStockProducts > 0) Color(0xFFDC2626) else Blue227ed4
                        )

                        Spacer(Modifier.height(16.dp))

                        // --- ALERTS ---
                        StatsCategoryHeader("ALERTS")
                        Spacer(Modifier.height(8.dp))
                        StatsRow(
                            "Out of Stock Products",
                            "${databaseStats.outOfStockProducts}",
                            if (databaseStats.outOfStockProducts > 0) Color(0xFFDC2626) else Blue227ed4
                        )

                        Spacer(Modifier.height(16.dp))

                        // --- DATABASE HEALTH ---
                        StatsCategoryHeader("DATABASE HEALTH")
                        Spacer(Modifier.height(8.dp))
                        StatsRow("Database Size", databaseStats.dbFileSizeFormatted)
                        StatsRow(
                            "Pending Sync Items",
                            "${databaseStats.pendingSyncItems}",
                            if (databaseStats.pendingSyncItems > 0) Color(0xFFDC2626) else Blue227ed4
                        )

                        Spacer(Modifier.height(16.dp))

                        // --- PURGE-ELIGIBLE ---
                        StatsCategoryHeader("PURGE-ELIGIBLE")
                        Spacer(Modifier.height(8.dp))
                        StatsRow("Deleted Products", "${databaseStats.deletedProducts}")
                        StatsRow("Deleted Customers", "${databaseStats.deletedCustomers}")
                        StatsRow("Deleted Invoices", "${databaseStats.deletedInvoices}")
                        StatsRow("Deleted Items", "${databaseStats.deletedInvoiceItems}")
                        StatsRow("Deleted Payments", "${databaseStats.deletedPayments}")

                        Spacer(Modifier.height(8.dp))
                        Text("Updates automatically", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            // Realtime Log & Leave Shop
            if (shopCode.isNotBlank()) {
                SectionHeader(icon = Icons.Default.Refresh, title = "Activity Log")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Button(
                        onClick = { showLogDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue227ed4)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("View Activity Log (${viewModel.logEntries.collectAsState().value.size})")
                    }
                }

                if (showLogDialog) {
                    ActivityLogDialog(
                        logEntries = viewModel.logEntries.collectAsState().value,
                        onDismiss = { showLogDialog = false },
                        onClear = { viewModel.clearLog() }
                    )
                }

                Spacer(Modifier.height(8.dp))

                SectionHeader(icon = Icons.AutoMirrored.Filled.ExitToApp, title = "Shop")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = { showLeaveDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Leave Shop", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // App Update
            SectionHeader(icon = Icons.Default.FileDownload, title = "App Update")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Version", fontSize = 13.sp, color = TextSecondary)
                        Text(currentVersionName.ifBlank { "Unknown" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Spacer(Modifier.height(16.dp))
                    when {
                        isCheckingUpdate -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val infiniteTransition = rememberInfiniteTransition()
                                val angle by infiniteTransition.animateFloat(
                                    initialValue = 0f, targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing)
                                    )
                                )
                                Canvas(modifier = Modifier.size(16.dp)) {
                                    drawArc(Blue227ed4, angle, 270f, false, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                                }
                                Spacer(Modifier.width(10.dp))
                                Text("Checking for updates...", fontSize = 13.sp, color = TextSecondary)
                            }
                        }
                        updateAvailable != null -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Update ${updateAvailable!!.versionName} available", fontSize = 13.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.height(12.dp))

                            val downloadProgress by animateFloatAsState(
                                targetValue = downloadState.progress,
                                animationSpec = tween(300), label = "progress"
                            )
                            val isDownloading = downloadState.isDownloading
                            val hasError = downloadState.error != null

                            if (isDownloading) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Canvas(modifier = Modifier.size(120.dp)) {
                                            val sweep = downloadProgress * 360f
                                            val stroke = 8.dp.toPx()
                                            drawArc(Color(0xFFE2E8F0), -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                                            drawArc(Blue227ed4, -90f, sweep, false, style = Stroke(stroke, cap = StrokeCap.Round))
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                "${(downloadProgress * 100).toInt()}",
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Blue227ed4
                                            )
                                            Text(
                                                "%",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Downloading...",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { viewModel.cancelDownload() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Cancel")
                                }
                            } else if (!updateDismissed) {
                                val isInstalled = downloadState.isComplete || viewModel.isUpdateDownloaded
                                Button(
                                    onClick = { if (isInstalled) viewModel.installUpdate() else viewModel.downloadUpdate() },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Blue227ed4)
                                ) {
                                    Icon(
                                        if (isInstalled) Icons.Default.CheckCircle else Icons.Default.FileDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (isInstalled) "Install" else "Download ${updateAvailable!!.versionName}",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            if (hasError) {
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp)).padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(downloadState.error!!, fontSize = 12.sp, color = Color(0xFFDC2626), modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            if (!isDownloading && !updateDismissed) {
                                OutlinedButton(
                                    onClick = { viewModel.dismissUpdate() },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Dismiss") }
                            } else if (!isDownloading && updateDismissed) {
                                Button(
                                    onClick = { viewModel.checkForUpdates() },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Blue227ed4)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Check for Updates", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (updateCheckError != null) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(updateCheckError!!, fontSize = 13.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Medium)
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("App up to date", fontSize = 13.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Medium)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.checkForUpdates() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Blue227ed4)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Check for Updates", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showRestoreConfirm && pendingRestoreUri != null) {
        ConfirmDialogOverlay(
            title = "Restore Data",
            message = "Are you sure you want to restore from this backup? Existing data may be overwritten.",
            confirmText = "Restore",
            onConfirm = { viewModel.restoreData(pendingRestoreUri!!); showRestoreConfirm = false; pendingRestoreUri = null },
            onDismiss = { showRestoreConfirm = false; pendingRestoreUri = null }
        )
    }

    if (showLeaveDialog) {
        ConfirmDialogOverlay(
            title = "Leave Shop",
            message = "Are you sure you want to leave this shop? You will lose access to all synced data.",
            confirmText = "Leave",
            onConfirm = { viewModel.leaveShop(); showLeaveDialog = false },
            onDismiss = { showLeaveDialog = false },
            destructive = true
        )
    }

    if (showPurgeConfirm) {
        ConfirmDialogOverlay(
            title = "Purge deleted data",
            message = "This will permanently delete ALL soft-deleted records (from this device and Firebase).\n\n" +
                "If the records don't exist in Firebase yet (syncing pending), they may not be purged until the next sync.\n\n" +
                "This cannot be undone.",
            confirmText = "Purge",
            onConfirm = { showPurgeConfirm = false; viewModel.purgeNow() },
            onDismiss = { showPurgeConfirm = false },
            destructive = true
        )
    }

    if (!purgeInProgress && purgeResult != null) {
        val r = purgeResult!!
        DialogOverlay(onDismiss = { viewModel.clearPurgeResult() }) {
            Text("Purge complete", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            val msg = buildString {
                append("Purged: ")
                val parts = mutableListOf<String>()
                if (r.bills > 0) parts.add("${r.bills} bill(s)")
                if (r.customers > 0) parts.add("${r.customers} customer(s)")
                if (r.products > 0) parts.add("${r.products} item(s)")
                if (r.payments > 0) parts.add("${r.payments} payment(s)")
                if (parts.isEmpty()) parts.add("nothing to purge")
                append(parts.joinToString(", "))
            }
            Text(msg, fontSize = 14.sp, color = Color(0xFF6B7280))
            Spacer(modifier = Modifier.height(20.dp))
            DialogConfirmButton(text = "OK", onClick = { viewModel.clearPurgeResult() })
        }
    }
}

@Composable
private fun MemberRow(
    member: com.shop.billing.data.remote.FirebaseClient.ShopMember,
    currentUserId: String,
    canManage: Boolean,
    isCurrentUserOwner: Boolean,
    shopOwnerId: String,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    onRemove: () -> Unit,
    onTransferOwnership: () -> Unit
) {
    val isFirstOwner = member.userId == shopOwnerId
    val isOwner = member.role == "owner"
    val roleLabel = when (member.role) {
        "owner" -> if (isFirstOwner) "Owner" else "Co-Owner"
        "admin" -> "Admin"
        else -> "Member"
    }
    val roleColor = when (member.role) {
        "owner" -> Color(0xFFF59E0B)
        "admin" -> Blue227ed4
        else -> TextSecondary
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(
                    when (member.role) {
                        "owner" -> Color(0xFFF59E0B).copy(alpha = 0.12f)
                        "admin" -> Blue227ed4.copy(alpha = 0.12f)
                        else -> Color(0xFFE2E8F0)
                    }
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isOwner) Icons.Default.Star else Icons.Default.Person,
                    contentDescription = null,
                    tint = when (member.role) {
                        "owner" -> Color(0xFFF59E0B)
                        "admin" -> Blue227ed4
                        else -> TextSecondary
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.email.ifBlank { "No email" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1
                )
                if (member.email.isBlank() && member.userId.isNotBlank()) {
                    Text(member.userId.take(16) + if (member.userId.length > 16) "..." else "", fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                }
            }
            Text(
                text = roleLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = roleColor
            )
            if (canManage) {
                Spacer(Modifier.width(4.dp))
                Box {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        val isSelf = member.userId == currentUserId
                        // Promote to admin (for members only)
                        if (!isSelf && member.role != "admin" && member.role != "owner") {
                            DropdownMenuItem(
                                text = { Text("Promote to Admin") },
                                onClick = { showMenu = false; onPromote() }
                            )
                        }
                        // Demote (for admins, or owners that aren't first owner)
                        if (!isSelf && member.role != "member") {
                            if (member.role == "admin" || !isFirstOwner) {
                                DropdownMenuItem(
                                    text = { Text("Demote to Member") },
                                    onClick = { showMenu = false; onDemote() }
                                )
                            }
                        }
                        // Transfer ownership only for first owner and only if current user is owner
                        if (isFirstOwner && isCurrentUserOwner) {
                            DropdownMenuItem(
                                text = { Text("Transfer Ownership", color = Color(0xFFF59E0B)) },
                                onClick = { showMenu = false; onTransferOwnership() }
                            )
                        }
                        if (!isSelf) {
                            DropdownMenuItem(
                                text = { Text("Remove", color = Color(0xFFDC2626)) },
                                onClick = { showMenu = false; onRemove() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityLogDialog(
    logEntries: List<LogEntry>,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Blue227ed4)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Activity Log (${logEntries.size})",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClear) {
                    Text("Clear", color = Color.White, fontSize = 14.sp)
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                itemsIndexed(logEntries.reversed()) { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (index % 2 == 0) Color(0xFFF8FAFC) else Color.White)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(5.dp).clip(CircleShape).background(
                                when (entry.type) {
                                    LogType.SUCCESS -> Color(0xFF43A047)
                                    LogType.ERROR -> Color(0xFFE53935)
                                    LogType.INFO -> Blue227ed4
                                }
                            )
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            entry.timestamp,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            entry.message,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Icon(
            imageVector = icon, contentDescription = null, tint = Blue227ed4,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun StatsCategoryHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(Blue227ed4, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = TextSecondary, letterSpacing = 1.sp
        )
    }
}

@Composable
private fun StatsRow(label: String, value: String, valueColor: Color = Blue227ed4) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
    Spacer(Modifier.height(4.dp))
}

private fun formatCurrency(amount: Double): String {
    val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault()).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }
    return "${Constants.CURRENCY_SYMBOL}${formatter.format(amount.toLong())}"
}
