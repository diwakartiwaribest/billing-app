package com.shop.billing.ui.screens.settings

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.journeyapps.barcodescanner.ScanOptions
import com.shop.billing.ui.theme.Blue227ed4
import com.shop.billing.ui.theme.SurfaceGray
import com.shop.billing.ui.theme.TextPrimary
import com.shop.billing.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val shopName by viewModel.shopName.collectAsState()
    val shopAddress by viewModel.shopAddress.collectAsState()
    val shopPhone by viewModel.shopPhone.collectAsState()
    val templatePath by viewModel.templatePath.collectAsState()
    val logoBase64 by viewModel.logoUri.collectAsState()
    val supabaseUrl by viewModel.supabaseUrl.collectAsState()
    val supabaseKey by viewModel.supabaseKey.collectAsState()
    val shopCode by viewModel.shopCode.collectAsState()
    val shopSecret by viewModel.shopSecret.collectAsState()
    val syncEnabled by viewModel.syncEnabled.collectAsState()
    val members by viewModel.members.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val restoreState by viewModel.restoreState.collectAsState()
    val joinStatus by viewModel.joinStatus.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val isOwner = userRole == "owner"
    val projectRef by viewModel.projectRef.collectAsState()
    val personalAccessToken by viewModel.personalAccessToken.collectAsState()
    val dbStats by viewModel.dbStats.collectAsState()
    val dbDetails by viewModel.dbDetails.collectAsState()

    val viewModelQrBitmap by viewModel.qrBitmap.collectAsState()
    val qrBitmap = viewModelQrBitmap?.asImageBitmap()

    var showCreateShopDialog by remember { mutableStateOf(false) }
    var showJoinShopDialog by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteShopDialog by remember { mutableStateOf(false) }
    val backupBills by viewModel.backupBills.collectAsState()
    val backupShopItems by viewModel.backupShopItems.collectAsState()
    val backupSettingsState by viewModel.backupSettings.collectAsState()
    val restoreBills by viewModel.restoreBills.collectAsState()
    val restoreShopItems by viewModel.restoreShopItems.collectAsState()
    val restoreSettingsState by viewModel.restoreSettings.collectAsState()
    val invoiceMessage by viewModel.invoiceMessage.collectAsState()
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var joinCode by remember { mutableStateOf("") }
    var joinSecret by remember { mutableStateOf("") }
    var joinUrl by remember { mutableStateOf("") }
    var joinKey by remember { mutableStateOf("") }
    var joinPat by remember { mutableStateOf("") }
    var joinProjectRef by remember { mutableStateOf("") }

    val context = LocalContext.current

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.backupData(uri)
        }
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
        if (uri != null) {
            viewModel.saveLogo(uri)
        }
    }

    val scanJoinQrLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val raw = result.data?.getStringExtra("SCAN_RESULT") ?: ""
            val content = try { java.net.URLDecoder.decode(raw, "UTF-8") } catch (_: Exception) { raw }
            val parts = content.split(":")
            if (parts.size >= 6 && parts[0] == "BILLING_SYNC") {
                joinCode = parts[1]
                joinSecret = parts[2]
                joinUrl = "${parts[3]}:${parts[4]}"
                joinKey = parts[5]
                joinPat = if (parts.size >= 7) parts[6] else ""
                joinProjectRef = if (parts.size >= 8) parts[7] else ""
            }
        }
    }

    LaunchedEffect(shopCode) {
        if (shopCode.isNotBlank()) {
            viewModel.pullMembers()
            viewModel.loadDbStats()
            viewModel.startMembersAutoRefresh()
        }
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

    LaunchedEffect(joinStatus) {
        if (joinStatus is JoinStatus.Success) {
            Toast.makeText(context, "Joined shop successfully!", Toast.LENGTH_SHORT).show()
            viewModel.resetJoinStatus()
        } else if (joinStatus is JoinStatus.Error) {
            Toast.makeText(context, (joinStatus as JoinStatus.Error).message, Toast.LENGTH_SHORT).show()
            viewModel.resetJoinStatus()
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader(icon = Icons.Default.Settings, title = "Shop Details")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    if (isOwner) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (logoBase64 != null) {
                                val logoBitmap = remember(logoBase64) {
                                    try {
                                        val bytes = Base64.decode(logoBase64, Base64.DEFAULT)
                                        val head = String(bytes, 0, minOf(bytes.size, 200)).lowercase()
                                        if ("<svg" in head) {
                                            val svg = com.caverock.androidsvg.SVG.getFromString(String(bytes))
                                            val picture = svg.renderToPicture()
                                            val scale = 200f / maxOf(picture.width, picture.height)
                                            val w = (picture.width * scale).toInt().coerceAtLeast(1)
                                            val h = (picture.height * scale).toInt().coerceAtLeast(1)
                                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                            val canvas = Canvas(bmp)
                                            canvas.scale(scale, scale)
                                            canvas.drawPicture(picture)
                                            bmp
                                        } else {
                                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        }
                                    } catch (e: Exception) { null }
                                }
                                if (logoBitmap != null) {
                                    Image(
                                        bitmap = logoBitmap.asImageBitmap(),
                                        contentDescription = "Shop Logo",
                                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }
                            }
                            OutlinedButton(onClick = { logoPickerLauncher.launch(arrayOf("image/*", "image/svg+xml")) }) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Pick Logo")
                            }
                            if (logoBase64 != null) {
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(onClick = { viewModel.removeLogo() }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Remove")
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    } else if (logoBase64 != null) {
                        val logoBitmap = remember(logoBase64) {
                            try {
                                val bytes = Base64.decode(logoBase64, Base64.DEFAULT)
                                val head = String(bytes, 0, minOf(bytes.size, 200)).lowercase()
                                if ("<svg" in head) {
                                    val svg = com.caverock.androidsvg.SVG.getFromString(String(bytes))
                                    val picture = svg.renderToPicture()
                                    val scale = 120f / maxOf(picture.width, picture.height)
                                    val w = (picture.width * scale).toInt().coerceAtLeast(1)
                                    val h = (picture.height * scale).toInt().coerceAtLeast(1)
                                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                    val canvas = Canvas(bmp)
                                    canvas.scale(scale, scale)
                                    canvas.drawPicture(picture)
                                    bmp
                                } else {
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                }
                            } catch (e: Exception) { null }
                        }
                        if (logoBitmap != null) {
                            Image(
                                bitmap = logoBitmap.asImageBitmap(),
                                contentDescription = "Shop Logo",
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { if (isOwner) viewModel.updateShopName(it) },
                        label = { Text("Shop Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        readOnly = !isOwner
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = shopAddress,
                        onValueChange = { if (isOwner) viewModel.updateShopAddress(it) },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        readOnly = !isOwner
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = shopPhone,
                        onValueChange = { if (isOwner) viewModel.updateShopPhone(it) },
                        label = { Text("Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        readOnly = !isOwner
                    )
                }
            }

            if (isOwner) {
                SectionHeader(icon = Icons.Default.Receipt, title = "Invoice Template")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Edit the invoice template HTML file to customize the invoice layout.",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = templatePath,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                SectionHeader(icon = Icons.Default.Receipt, title = "Invoice Message")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        OutlinedTextField(
                            value = invoiceMessage,
                            onValueChange = { viewModel.updateInvoiceMessage(it) },
                            label = { Text("Invoice Message") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Shown at the bottom of the invoice",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (isOwner) {
                SectionHeader(icon = Icons.Default.History, title = "Backup & Restore")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("Select data to backup:", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = backupBills, onCheckedChange = { viewModel.backupBills.value = it })
                            Text("Bills", fontSize = 14.sp, color = TextPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = backupShopItems, onCheckedChange = { viewModel.backupShopItems.value = it })
                            Text("Shop Items", fontSize = 14.sp, color = TextPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = backupSettingsState, onCheckedChange = { viewModel.backupSettings.value = it })
                            Text("Settings", fontSize = 14.sp, color = TextPrimary)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { backupLauncher.launch(null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = backupState !is BackupState.InProgress
                        ) {
                            Text(if (backupState is BackupState.InProgress) "Backing up..." else "Backup")
                        }
                        if (backupState is BackupState.Success) {
                            Text(
                                text = "Backup: ${(backupState as BackupState.Success).fileName}",
                                fontSize = 12.sp,
                                color = Color(0xFF22C55E)
                            )
                        }
                        if (backupState is BackupState.Error) {
                            Text(
                                text = (backupState as BackupState.Error).message,
                                fontSize = 12.sp,
                                color = Color(0xFFEF4444)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                        Spacer(Modifier.height(12.dp))
                        Text("Select data to restore:", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = restoreBills, onCheckedChange = { viewModel.restoreBills.value = it })
                            Text("Bills", fontSize = 14.sp, color = TextPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = restoreShopItems, onCheckedChange = { viewModel.restoreShopItems.value = it })
                            Text("Shop Items", fontSize = 14.sp, color = TextPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = restoreSettingsState, onCheckedChange = { viewModel.restoreSettings.value = it })
                            Text("Settings", fontSize = 14.sp, color = TextPrimary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = restoreState !is RestoreState.InProgress
                        ) {
                            Text(if (restoreState is RestoreState.InProgress) "Restoring..." else "Restore")
                        }
                        if (restoreState is RestoreState.Success) {
                            Text(
                                text = "Restored: ${(restoreState as RestoreState.Success).summary}",
                                fontSize = 12.sp,
                                color = Color(0xFF22C55E)
                            )
                        }
                        if (restoreState is RestoreState.Error) {
                            Text(
                                text = (restoreState as RestoreState.Error).message,
                                fontSize = 12.sp,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }

            SectionHeader(icon = Icons.Default.CloudUpload, title = "Cloud Database")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    if (isOwner) {
                        OutlinedTextField(
                            value = supabaseUrl,
                            onValueChange = { viewModel.updateSupabaseUrl(it) },
                            label = { Text("Supabase URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = supabaseKey,
                            onValueChange = { viewModel.updateSupabaseKey(it) },
                            label = { Text("Supabase API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = projectRef,
                            onValueChange = { viewModel.updateProjectRef(it) },
                            label = { Text("Project Reference") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = personalAccessToken,
                            onValueChange = { viewModel.updatePersonalAccessToken(it) },
                            label = { Text("Personal Access Token") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                        var creatingTables by remember { mutableStateOf(false) }
                        var createTablesResult by remember { mutableStateOf<String?>(null) }
                        val tableCreationScope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                creatingTables = true
                                createTablesResult = null
                                tableCreationScope.launch {
                                    val result = viewModel.createTablesAutomatically(personalAccessToken, projectRef)
                                    creatingTables = false
                                    createTablesResult = if (result) "Tables created successfully!" else "Failed to create tables. Check PAT and Project Ref."
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !creatingTables && personalAccessToken.isNotBlank() && projectRef.isNotBlank()
                        ) {
                            if (creatingTables) {
                                Text("Creating Tables...")
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Auto Create Tables")
                            }
                        }
                        if (createTablesResult != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = createTablesResult!!,
                                fontSize = 12.sp,
                                color = if (createTablesResult!!.startsWith("Tables")) Color(0xFF22C55E) else Color(0xFFEF4444)
                            )
                        }
                    }
                    if (shopCode.isNotBlank()) {
                        Spacer(Modifier.height(if (isOwner) 12.dp else 0.dp))
                        OutlinedTextField(
                            value = shopCode,
                            onValueChange = {},
                            label = { Text("Shop Code") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = shopSecret,
                            onValueChange = {},
                            label = { Text("Shop Secret") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true
                        )
                    }
                    if (isOwner && shopCode.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        if (qrBitmap != null) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Image(
                                    bitmap = qrBitmap!!,
                                    contentDescription = "Shop QR Code",
                                    modifier = Modifier.size(200.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Shop Config", if (personalAccessToken.isNotBlank() && projectRef.isNotBlank()) "BILLING_SYNC:$shopCode:$shopSecret:$supabaseUrl:$supabaseKey:$personalAccessToken:$projectRef" else "BILLING_SYNC:$shopCode:$shopSecret:$supabaseUrl:$supabaseKey")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Config copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share Config QR")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Sync Enabled", fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                        if (isOwner) {
                            Switch(checked = syncEnabled, onCheckedChange = { viewModel.toggleSync() })
                        } else {
                            Text(if (syncEnabled) "On" else "Off", fontSize = 14.sp, color = TextSecondary)
                        }
                    }
                    if (syncStatus is SyncStatus.Error) {
                        Text(
                            text = (syncStatus as SyncStatus.Error).message,
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444)
                        )
                    }
                    if (syncStatus is SyncStatus.Syncing) {
                        Text("Syncing...", fontSize = 12.sp, color = TextSecondary)
                    }
                    if (syncStatus is SyncStatus.Connected) {
                        Text("Connected", fontSize = 12.sp, color = Color(0xFF22C55E))
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.manualSync() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = syncStatus !is SyncStatus.Syncing
                    ) {
                        Text("Sync Now")
                    }
                    if (isOwner && shopCode.isBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { showCreateShopDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create New Shop")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showJoinShopDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Join Existing Shop")
                        }
                    }
                    if (!isOwner && shopCode.isBlank()) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showJoinShopDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Join Shop")
                        }
                    }
                    if (shopCode.isNotBlank() && isOwner) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showDeleteShopDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete Shop Permanently")
                        }
                    }
                }
            }

            if (shopCode.isNotBlank() && isOwner) {
                SectionHeader(icon = Icons.Default.Storage, title = "Manage Database")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        val totalRows = dbStats.values.sum()

                        Text("Database Info", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        val dbName = dbDetails["db_name"] as? String ?: "—"
                        val dbVersion = (dbDetails["db_version"] as? String)?.take(30) ?: "—"
                        val dbSizePretty = dbDetails["db_size_pretty"] as? String ?: "—"
                        Text("Name: $dbName", fontSize = 12.sp, color = TextSecondary)
                        Text("Version: $dbVersion", fontSize = 12.sp, color = TextSecondary)
                        Text("Size: $dbSizePretty", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)

                        Spacer(Modifier.height(12.dp))
                        Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                        Spacer(Modifier.height(12.dp))

                        Text("Row Counts", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        val tableLabels = mapOf(
                            "bills" to "Bills",
                            "bill_items" to "Bill Items",
                            "shop_items" to "Shop Items",
                            "user_shops" to "User Shops",
                            "shop_settings" to "Shop Settings"
                        )
                        for ((key, label) in tableLabels) {
                            val count = dbStats[key] ?: 0
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, fontSize = 13.sp, color = TextSecondary)
                                Text("$count", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                            }
                        }

                        val tables = dbDetails["tables"] as? List<*>
                        if (!tables.isNullOrEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Table Sizes", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            for (table in tables) {
                                @Suppress("UNCHECKED_CAST")
                                val t = table as? Map<String, Any> ?: continue
                                val name = t["name"] as? String ?: continue
                                val totalBytes = t["total_bytes"] as? Long ?: 0
                                val rowCount = t["row_count"] as? Long ?: 0
                                val sizeStr = if (totalBytes > 1024 * 1024) "${String.format("%.1f", totalBytes / 1024.0 / 1024.0)} MB"
                                    else if (totalBytes > 1024) "${String.format("%.1f", totalBytes / 1024.0)} KB"
                                    else "$totalBytes B"
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                                    Text("$rowCount rows", fontSize = 12.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                                    Text(sizeStr, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                        Spacer(Modifier.height(12.dp))

                        Text("Free Tier Limits", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))

                        val dbSizeBytes = (dbDetails["db_size_bytes"] as? Number)?.toLong() ?: 0L
                        val freeDbBytes = 512L * 1024 * 1024
                        val dbUsageFraction = (dbSizeBytes.toFloat() / freeDbBytes).coerceIn(0f, 1f)
                        Text("Database Storage: ${dbDetails["db_size_pretty"] ?: "—"} / 512 MB", fontSize = 12.sp, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(progress = dbUsageFraction, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = if (dbUsageFraction > 0.8f) Color(0xFFEF4444) else Blue227ed4, trackColor = Color(0xFFE2E8F0))

                        Spacer(Modifier.height(8.dp))
                        val authUsers = (dbDetails["auth_users"] as? Number)?.toInt() ?: 0
                        val authUsageFraction = (authUsers.toFloat() / 50000f).coerceIn(0f, 1f)
                        Text("Auth Users: $authUsers / 50,000", fontSize = 12.sp, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(progress = authUsageFraction, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = if (authUsageFraction > 0.8f) Color(0xFFEF4444) else Blue227ed4, trackColor = Color(0xFFE2E8F0))

                        Spacer(Modifier.height(8.dp))
                        val activeConns = (dbDetails["active_connections"] as? Number)?.toInt() ?: 0
                        Text("Active DB Connections: $activeConns / 60", fontSize = 12.sp, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(progress = (activeConns.toFloat() / 60f).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = if (activeConns > 50) Color(0xFFEF4444) else Blue227ed4, trackColor = Color(0xFFE2E8F0))

                        Spacer(Modifier.height(8.dp))
                        Text("Bandwidth: ~${String.format("%.1f", totalRows * 2.0 / 1024)} MB / 1 GB (estimated)", fontSize = 12.sp, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(progress = ((totalRows * 2.0 / 1024) / 1024.0).coerceIn(0.0, 1.0).toFloat(), modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = Blue227ed4, trackColor = Color(0xFFE2E8F0))

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.loadDbStats() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Refresh Stats")
                        }
                        if (isOwner) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { navController.navigate(com.shop.billing.ui.navigation.NavRoutes.DatabaseManager.route) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Database Manager")
                            }
                        }
                    }
                }
            }

            if (shopCode.isNotBlank()) {
                SectionHeader(icon = Icons.Default.People, title = "Shop Members")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        if (members.isEmpty()) {
                            Text("No members found", fontSize = 14.sp, color = TextSecondary)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.pullMembers() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Refresh Members")
                            }
                        } else {
                            members.forEachIndexed { index, member ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    val initial = if (member.email.isNotBlank())
                                        member.email.first().uppercase()
                                    else if (member.deviceName.isNotBlank())
                                        member.deviceName.first().uppercase()
                                    else
                                        member.userId.first().uppercase()
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Blue227ed4),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        val displayName = member.email.ifBlank {
                                            member.deviceName.ifBlank {
                                                "Member (${member.userId.take(8)})"
                                            }
                                        }
                                        Text(
                                            text = displayName,
                                            fontSize = 14.sp,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = member.role,
                                            fontSize = 12.sp,
                                            color = if (member.role == "owner") Blue227ed4 else TextSecondary,
                                            fontWeight = if (member.role == "owner") FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                    if (isOwner && member.role != "owner") {
                                        IconButton(onClick = { showTransferDialog = true }) {
                                            Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer Ownership", tint = Blue227ed4, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = { viewModel.removeMember(member.userId) }) {
                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove Member", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                                if (index < members.lastIndex) {
                                    Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showLeaveDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Leave Shop")
                        }
                    }
                }
            }
        }

        if (showCreateShopDialog) {
            var code by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreateShopDialog = false },
                title = { Text("Create New Shop") },
                text = {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Shop Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.createNewShop(code)
                        showCreateShopDialog = false
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateShopDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showJoinShopDialog) {
            AlertDialog(
                onDismissRequest = { showJoinShopDialog = false },
                title = { Text("Join Shop") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = joinCode,
                            onValueChange = { joinCode = it },
                            label = { Text("Shop Code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = joinSecret,
                            onValueChange = { joinSecret = it },
                            label = { Text("Shop Secret") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val options = ScanOptions()
                                options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                options.setPrompt("Scan shop QR code")
                                scanJoinQrLauncher.launch(options.createScanIntent(context))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Scan QR")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.joinShop(joinCode, joinSecret, urlOverride = joinUrl, keyOverride = joinKey, patOverride = joinPat, projectRefOverride = joinProjectRef)
                        showJoinShopDialog = false
                        joinCode = ""
                        joinSecret = ""
                        joinUrl = ""
                        joinKey = ""
                    }) {
                        Text("Join")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showJoinShopDialog = false
                        joinCode = ""
                        joinSecret = ""
                        joinUrl = ""
                        joinKey = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showRestoreConfirm && pendingRestoreUri != null) {
            AlertDialog(
                onDismissRequest = {
                    showRestoreConfirm = false
                    pendingRestoreUri = null
                },
                title = { Text("Restore Data") },
                text = { Text("Are you sure you want to restore from this backup? This will overwrite existing data.") },
                confirmButton = {
                    Button(onClick = {
                        showRestoreConfirm = false
                        pendingRestoreUri?.let { viewModel.restoreData(it) }
                        pendingRestoreUri = null
                    }) {
                        Text("Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRestoreConfirm = false
                        pendingRestoreUri = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showTransferDialog) {
            val nonOwnerMembers = members.filter { it.role != "owner" }
            AlertDialog(
                onDismissRequest = { showTransferDialog = false },
                title = { Text("Transfer Ownership") },
                text = {
                    Column {
                        Text("Select a member to transfer ownership to:", fontSize = 14.sp, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        if (nonOwnerMembers.isEmpty()) {
                            Text("No other members available.", fontSize = 14.sp, color = TextSecondary)
                        } else {
                            nonOwnerMembers.forEach { member ->
                                Button(
                                    onClick = {
                                        viewModel.transferOwnership(member.userId)
                                        showTransferDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(member.email.ifBlank { member.deviceName.ifBlank { "Member (${member.userId.take(8)})" } })
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showTransferDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showLeaveDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveDialog = false },
                title = { Text("Leave Shop") },
                text = { Text("Are you sure you want to leave this shop? You will lose access to all synced data.") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.leaveShop()
                        showLeaveDialog = false
                    }) {
                        Text("Leave")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeleteShopDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteShopDialog = false },
                title = { Text("Delete Shop") },
                text = { Text("This will permanently delete this shop and ALL its data (bills, items, members) from the cloud. This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteShop()
                            showDeleteShopDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("Delete Everything")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteShopDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
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
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(listOf(Blue227ed4, Color(0xFF0EA5E9)))
                )
                .padding(6.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}
