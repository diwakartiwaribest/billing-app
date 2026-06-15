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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isOwner || logoBase64 != null) {
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
                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        if (isOwner) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { logoPickerLauncher.launch(arrayOf("image/*", "image/svg+xml")) }) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Change Logo")
                                }
                                if (logoBase64 != null) {
                                    OutlinedButton(onClick = { viewModel.removeLogo() }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Remove")
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { if (isOwner) viewModel.updateShopName(it) },
                        label = { Text("Shop Name") },
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        readOnly = !isOwner
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = shopAddress,
                        onValueChange = { if (isOwner) viewModel.updateShopAddress(it) },
                        label = { Text("Address") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        readOnly = !isOwner
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = shopPhone,
                        onValueChange = { if (isOwner) viewModel.updateShopPhone(it.filter { ch -> ch.isDigit() }.take(10)) },
                        label = { Text("Phone") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = Blue227ed4, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("HTML Template", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(templatePath, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                        }
                        Icon(Icons.Default.History, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(20.dp))
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
                            leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, tint = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Blue227ed4,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
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
                        Text("Backup Data", fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Choose what to backup:", fontSize = 13.sp, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF8FAFC)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Checkbox(checked = backupBills, onCheckedChange = { viewModel.backupBills.value = it }, colors = CheckboxDefaults.colors(checkedColor = Blue227ed4))
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = Blue227ed4, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Bills & Items", fontSize = 14.sp, color = TextPrimary)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF8FAFC)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Checkbox(checked = backupShopItems, onCheckedChange = { viewModel.backupShopItems.value = it }, colors = CheckboxDefaults.colors(checkedColor = Blue227ed4))
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = Blue227ed4, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Shop Items", fontSize = 14.sp, color = TextPrimary)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF8FAFC)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Checkbox(checked = backupSettingsState, onCheckedChange = { viewModel.backupSettings.value = it }, colors = CheckboxDefaults.colors(checkedColor = Blue227ed4))
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Blue227ed4, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Shop Settings", fontSize = 14.sp, color = TextPrimary)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { backupLauncher.launch(null) },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            enabled = backupState !is BackupState.InProgress,
                            colors = ButtonDefaults.buttonColors(containerColor = Blue227ed4)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (backupState is BackupState.InProgress) "Backing up..." else "Backup")
                        }
                        if (backupState is BackupState.Success) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Backup: ${(backupState as BackupState.Success).fileName}",
                                fontSize = 12.sp,
                                color = Color(0xFF22C55E)
                            )
                        }
                        if (backupState is BackupState.Error) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = (backupState as BackupState.Error).message,
                                fontSize = 12.sp,
                                color = Color(0xFFEF4444)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0))
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Restore Data", fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Choose what to restore:", fontSize = 13.sp, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF8FAFC)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Checkbox(checked = restoreBills, onCheckedChange = { viewModel.restoreBills.value = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFF59E0B)))
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Bills & Items", fontSize = 14.sp, color = TextPrimary)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF8FAFC)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Checkbox(checked = restoreShopItems, onCheckedChange = { viewModel.restoreShopItems.value = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFF59E0B)))
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Shop Items", fontSize = 14.sp, color = TextPrimary)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF8FAFC)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Checkbox(checked = restoreSettingsState, onCheckedChange = { viewModel.restoreSettings.value = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFF59E0B)))
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Shop Settings", fontSize = 14.sp, color = TextPrimary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            enabled = restoreState !is RestoreState.InProgress,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (restoreState is RestoreState.InProgress) "Restoring..." else "Restore")
                        }
                        if (restoreState is RestoreState.Success) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Restored: ${(restoreState as RestoreState.Success).summary}",
                                fontSize = 12.sp,
                                color = Color(0xFF22C55E)
                            )
                        }
                        if (restoreState is RestoreState.Error) {
                            Spacer(Modifier.height(4.dp))
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
                            leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null, tint = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Blue227ed4,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = supabaseKey,
                            onValueChange = { viewModel.updateSupabaseKey(it) },
                            label = { Text("Supabase API Key") },
                            leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null, tint = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Blue227ed4,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = projectRef,
                            onValueChange = { viewModel.updateProjectRef(it) },
                            label = { Text("Project Reference") },
                            leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null, tint = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Blue227ed4,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = personalAccessToken,
                            onValueChange = { viewModel.updatePersonalAccessToken(it) },
                            label = { Text("Personal Access Token") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Blue227ed4,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
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
                            onValueChange = { if (isOwner) viewModel.updateShopCode(it) },
                            label = { Text("Shop Code") },
                            leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null, tint = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = !isOwner,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = shopSecret,
                            onValueChange = { if (isOwner) viewModel.updateShopSecret(it) },
                            label = { Text("Shop Secret") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = !isOwner,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
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
                        val dbName = dbDetails["db_name"] as? String ?: "postgres"
                        val dbVersion = (dbDetails["db_version"] as? String)?.take(40) ?: "—"
                        val dbSizePretty = dbDetails["db_size_pretty"] as? String ?: "—"
                        val dbSizeBytes = (dbDetails["db_size_bytes"] as? Number)?.toLong() ?: 0L

                        val rowColors = mapOf(
                            "bills" to Blue227ed4,
                            "bill_items" to Color(0xFF7C3AED),
                            "shop_items" to Color(0xFF10B981),
                            "user_shops" to Color(0xFFF59E0B),
                            "shop_settings" to Color(0xFFEF4444),
                            "customers" to Color(0xFF3B82F6),
                            "customer_payments" to Color(0xFF06B6D4)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7FF)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Blue227ed4), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Storage, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Database", fontSize = 11.sp, color = TextSecondary)
                                        Text(dbName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                }
                            }
                            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF10B981)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Cloud, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Size", fontSize = 11.sp, color = TextSecondary)
                                        Text(dbSizePretty, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF59E0B)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Version", fontSize = 11.sp, color = TextSecondary)
                                    Text(dbVersion, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Blue227ed4))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Row Counts", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                    Text("$totalRows total", fontSize = 12.sp, color = TextSecondary)
                                }
                                Spacer(Modifier.height(10.dp))

                                val rowIcons = mapOf(
                                    "bills" to Icons.Default.Receipt,
                                    "bill_items" to Icons.Default.Inventory2,
                                    "shop_items" to Icons.Default.Inventory2,
                                    "user_shops" to Icons.Default.People,
                                    "shop_settings" to Icons.Default.Settings,
                                    "customers" to Icons.Default.Person,
                                    "customer_payments" to Icons.Default.History
                                )
                                val rowLabels = mapOf(
                                    "bills" to "Bills",
                                    "bill_items" to "Bill Items",
                                    "shop_items" to "Shop Items",
                                    "user_shops" to "User Shops",
                                    "shop_settings" to "Shop Settings",
                                    "customers" to "Customers",
                                    "customer_payments" to "Customer Payments"
                                )
                                for ((key, label) in rowLabels) {
                                    val count = dbStats[key] ?: 0
                                    val color = rowColors[key] ?: Blue227ed4
                                    val icon = rowIcons[key] ?: Icons.Default.Storage
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                                                }
                                                Spacer(Modifier.width(10.dp))
                                                Text(label, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                                                Text("$count", fontSize = 14.sp, color = color, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            LinearProgressIndicator(
                                                progress = if (totalRows > 0) (count.toFloat() / totalRows).coerceIn(0.01f, 1f) else 0f,
                                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                                color = color,
                                                trackColor = Color(0xFFE2E8F0)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF7C3AED)))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Free Tier Limits", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(12.dp))

                                val freeDbBytes = 512L * 1024 * 1024
                                val dbUsagePercent = ((dbSizeBytes.toFloat() / freeDbBytes) * 100).coerceIn(0f, 100f).toInt()
                                val authUsers = (dbDetails["auth_users"] as? Number)?.toInt() ?: 0
                                val authUsagePercent = ((authUsers.toFloat() / 50000f) * 100).coerceIn(0f, 100f).toInt()
                                val activeConns = (dbDetails["active_connections"] as? Number)?.toInt() ?: 0
                                val connUsagePercent = ((activeConns.toFloat() / 60f) * 100).coerceIn(0f, 100f).toInt()
                                val bandwidthMb = totalRows * 2.0 / 1024
                                val bwUsagePercent = ((bandwidthMb / 1024.0) * 100).coerceIn(0.0, 100.0).toInt()

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CircularMetricCard(modifier = Modifier.weight(1f), label = "Storage", percent = dbUsagePercent, detail = "$dbSizePretty / 512 MB", color = Blue227ed4)
                                    CircularMetricCard(modifier = Modifier.weight(1f), label = "Auth Users", percent = authUsagePercent, detail = "$authUsers / 50K", color = Color(0xFF7C3AED))
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CircularMetricCard(modifier = Modifier.weight(1f), label = "Connections", percent = connUsagePercent, detail = "$activeConns / 60", color = Color(0xFF10B981))
                                    CircularMetricCard(modifier = Modifier.weight(1f), label = "Bandwidth", percent = bwUsagePercent, detail = "~${String.format("%.1f", bandwidthMb)} MB / 1 GB", color = Color(0xFFF59E0B))
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Performance & Health", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(12.dp))

                                val cacheHit = (dbDetails["cache_hit_ratio"] as? Number)?.toDouble()?.let { (it * 100).toInt() } ?: 100
                                val uptimeDays = (dbDetails["uptime_days"] as? Number)?.toInt() ?: 0
                                val tableCount = (dbDetails["table_count"] as? Number)?.toInt() ?: 0
                                val indexCount = (dbDetails["index_count"] as? Number)?.toInt() ?: 0
                                val deadTuples = (dbDetails["dead_tuples"] as? Number)?.toInt() ?: 0
                                val deadTuplePct = (dbDetails["dead_tuple_percent"] as? Number)?.toDouble() ?: 0.0

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CircularMetricCard(modifier = Modifier.weight(1f), label = "Cache Hit Ratio", percent = cacheHit, detail = "${cacheHit}%", color = Color(0xFF10B981))
                                    CircularMetricCard(modifier = Modifier.weight(1f), label = "Uptime", percent = 100, detail = "$uptimeDays days", color = Blue227ed4)
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CircularMetricCard(modifier = Modifier.weight(1f), label = "Tables / Indexes", percent = 100, detail = "$tableCount / $indexCount", color = Color(0xFF7C3AED))
                                    CircularMetricCard(modifier = Modifier.weight(1f), label = "Dead Tuples", percent = deadTuples, detail = "$deadTuples (${String.format("%.1f", deadTuplePct)}%)", color = Color(0xFFEF4444))
                                }

                                if (deadTuples > 0) {
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = { },
                                        modifier = Modifier.fillMaxWidth().height(44.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B))
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Run VACUUM (${String.format("%.1f", deadTuplePct)}% dead tuples)")
                                    }
                                }
                            }
                        }

                        val tables = dbDetails["tables"] as? List<*>
                        if (!tables.isNullOrEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Blue227ed4))
                                        Spacer(Modifier.width(8.dp))
                                        Text("All Tables Breakdown", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    for (table in tables) {
                                        @Suppress("UNCHECKED_CAST")
                                        val t = table as? Map<String, Any> ?: continue
                                        val name = t["name"] as? String ?: continue
                                        val totalBytes = t["total_bytes"] as? Long ?: 0
                                        val rowCount = t["row_count"] as? Long ?: 0
                                        val sizeStr = if (totalBytes > 1024 * 1024) "${String.format("%.1f", totalBytes / 1024.0 / 1024.0)} MB"
                                            else if (totalBytes > 1024) "${String.format("%.1f", totalBytes / 1024.0)} KB"
                                            else "$totalBytes B"
                                        val pct = if (dbSizeBytes > 0) (totalBytes.toFloat() / dbSizeBytes * 100) else 0f
                                        val tableColor = rowColors[name] ?: Blue227ed4
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(tableColor.copy(alpha = 0.15f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Storage, contentDescription = null, tint = tableColor, modifier = Modifier.size(16.dp))
                                                    }
                                                    Spacer(Modifier.width(10.dp))
                                                    Text(name, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                                    Text(sizeStr, fontSize = 13.sp, color = tableColor, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(Modifier.height(2.dp))
                                                Row(modifier = Modifier.fillMaxWidth()) {
                                                    Spacer(Modifier.width(38.dp))
                                                    Text("$rowCount rows", fontSize = 11.sp, color = TextSecondary)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("•", fontSize = 11.sp, color = TextSecondary)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("${String.format("%.1f", pct)}% of DB", fontSize = 11.sp, color = TextSecondary)
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                LinearProgressIndicator(
                                                    progress = if (dbSizeBytes > 0) (totalBytes.toFloat() / dbSizeBytes).coerceIn(0.01f, 1f) else 0f,
                                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                                    color = tableColor,
                                                    trackColor = Color(0xFFE2E8F0)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.loadDbStats() },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Blue227ed4)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Refresh Stats")
                            }
                            if (isOwner) {
                                OutlinedButton(
                                    onClick = { navController.navigate(com.shop.billing.ui.navigation.NavRoutes.DatabaseManager.route) },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("DB Manager")
                                }
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
                            Box(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text("No members found", fontSize = 14.sp, color = TextSecondary)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.pullMembers() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Refresh Members")
                            }
                        } else {
                            members.forEachIndexed { index, member ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (member.role == "owner") Color(0xFFF0F7FF) else Color.Transparent).padding(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (member.role == "owner")
                                                    Brush.linearGradient(listOf(Blue227ed4, Color(0xFF0EA5E9)))
                                                else
                                                    Brush.linearGradient(listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1)))
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val initial = if (member.email.isNotBlank())
                                            member.email.first().uppercase()
                                        else if (member.deviceName.isNotBlank())
                                            member.deviceName.first().uppercase()
                                        else
                                            member.userId.first().uppercase()
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(if (member.role == "owner") Blue227ed4 else Color(0xFF94A3B8))
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = member.role,
                                                fontSize = 12.sp,
                                                color = if (member.role == "owner") Blue227ed4 else TextSecondary,
                                                fontWeight = if (member.role == "owner") FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    }
                                    if (isOwner && member.role != "owner") {
                                        IconButton(onClick = { showTransferDialog = true }) {
                                            Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer Ownership", tint = Blue227ed4, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = { viewModel.removeMember(member.userId) }) {
                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove Member", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    if (member.role == "owner") {
                                        Icon(Icons.Default.Cloud, contentDescription = "Owner", tint = Blue227ed4, modifier = Modifier.size(20.dp))
                                    }
                                }
                                if (index < members.lastIndex) {
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF1F5F9))
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showLeaveDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Leave Shop")
                            }
                        }
                    }
                }
            }
        }

        if (showCreateShopDialog) {
            var code by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreateShopDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFEEF2FF)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = Blue227ed4, modifier = Modifier.size(24.dp))
                    }
                },
                title = { Text("Create New Shop", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937)) },
                text = {
                    Column {
                        Text("Enter a unique code for your shop:", fontSize = 14.sp, color = TextSecondary)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Shop Code") },
                            leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null, tint = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Blue227ed4,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.createNewShop(code)
                            showCreateShopDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateShopDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showJoinShopDialog) {
            AlertDialog(
                onDismissRequest = { showJoinShopDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFF0FDF4)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF43A047), modifier = Modifier.size(24.dp))
                    }
                },
                title = { Text("Join Shop", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937)) },
                text = {
                    Column {
                        Text("Enter the shop details from the owner:", fontSize = 14.sp, color = TextSecondary)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = joinCode,
                            onValueChange = { joinCode = it },
                            label = { Text("Shop Code") },
                            leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null, tint = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Blue227ed4,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = joinSecret,
                            onValueChange = { joinSecret = it },
                            label = { Text("Shop Secret") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Blue227ed4,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val options = ScanOptions()
                                options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                options.setPrompt("Scan shop QR code")
                                scanJoinQrLauncher.launch(options.createScanIntent(context))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Scan QR")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.joinShop(joinCode, joinSecret, urlOverride = joinUrl, keyOverride = joinKey, patOverride = joinPat, projectRefOverride = joinProjectRef)
                            showJoinShopDialog = false
                            joinCode = ""
                            joinSecret = ""
                            joinUrl = ""
                            joinKey = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Join") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showJoinShopDialog = false
                        joinCode = ""
                        joinSecret = ""
                        joinUrl = ""
                        joinKey = ""
                    }) { Text("Cancel") }
                }
            )
        }

        if (showRestoreConfirm && pendingRestoreUri != null) {
            AlertDialog(
                onDismissRequest = {
                    showRestoreConfirm = false
                    pendingRestoreUri = null
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFFF7ED)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                    }
                },
                title = { Text("Restore Data", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937)) },
                text = {
                    Text("Are you sure you want to restore from this backup? This will overwrite existing data.", fontSize = 14.sp, color = TextSecondary)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRestoreConfirm = false
                            pendingRestoreUri?.let { viewModel.restoreData(it) }
                            pendingRestoreUri = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Restore") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRestoreConfirm = false
                        pendingRestoreUri = null
                    }) { Text("Cancel") }
                }
            )
        }

        if (showTransferDialog) {
            val nonOwnerMembers = members.filter { it.role != "owner" }
            AlertDialog(
                onDismissRequest = { showTransferDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFFF7ED)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                    }
                },
                title = { Text("Transfer Ownership", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937)) },
                text = {
                    Column {
                        Text("Select a member to transfer ownership to:", fontSize = 14.sp, color = TextSecondary)
                        Spacer(Modifier.height(12.dp))
                        if (nonOwnerMembers.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                                Text("No other members available.", fontSize = 14.sp, color = TextSecondary)
                            }
                        } else {
                            nonOwnerMembers.forEach { member ->
                                Button(
                                    onClick = {
                                        viewModel.transferOwnership(member.userId)
                                        showTransferDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF8FAFC), contentColor = TextPrimary)
                                ) {
                                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(member.email.ifBlank { member.deviceName.ifBlank { "Member (${member.userId.take(8)})" } })
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showTransferDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showLeaveDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFEF2F2)), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                    }
                },
                title = { Text("Leave Shop", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937)) },
                text = { Text("Are you sure you want to leave this shop? You will lose access to all synced data.", fontSize = 14.sp, color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.leaveShop()
                            showLeaveDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) { Text("Leave") }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showDeleteShopDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteShopDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFEF2F2)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                    }
                },
                title = { Text("Delete Shop", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937)) },
                text = { Text("This will permanently delete this shop and ALL its data (bills, items, members) from the cloud. This cannot be undone.", fontSize = 14.sp, color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteShop()
                            showDeleteShopDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) { Text("Delete Everything") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteShopDialog = false }) { Text("Cancel") }
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

@Composable
private fun CircularMetricCard(modifier: Modifier = Modifier, label: String, percent: Int, detail: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                @Suppress("DEPRECATION")
                androidx.compose.material3.CircularProgressIndicator(
                    progress = percent.coerceIn(0, 100) / 100f,
                    modifier = Modifier.fillMaxSize(),
                    color = color,
                    trackColor = Color(0xFFF1F5F9),
                    strokeWidth = 5.dp
                )
                Text(
                    text = "$percent%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(text = label, fontSize = 11.sp, color = TextSecondary)
            Text(text = detail, fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        }
    }
}
