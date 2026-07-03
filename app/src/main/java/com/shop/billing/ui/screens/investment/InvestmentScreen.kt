package com.shop.billing.ui.screens.investment

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shop.billing.ui.screens.newbill.ContinuousScannerActivity
import com.shop.billing.ui.theme.Blue227ed4
import com.shop.billing.ui.theme.SurfaceGray
import com.shop.billing.ui.theme.TextSecondary
import com.shop.billing.util.Constants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private enum class ScanState { Idle, Scanning, Processing, CreatingProduct, Confirming }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentScreen(
    navController: NavController,
    viewModel: InvestmentViewModel = hiltViewModel()
) {
    val investments by viewModel.investments.collectAsState()
    val totalInvestment by viewModel.totalInvestment.collectAsState()
    val knownBarcodes by viewModel.knownBarcodes.collectAsState()
    val context = LocalContext.current

    var scanState by remember { mutableStateOf(ScanState.Idle) }
    var scannedBarcodes by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var confirmedItems = remember { mutableStateListOf<PurchaseItem>() }
    var unknownBarcodes = remember { mutableStateListOf<Pair<String, Int>>() }
    var processingUnknownIndex by remember { mutableIntStateOf(0) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    var showProductPicker by remember { mutableStateOf(false) }
    var productList by remember { mutableStateOf<List<ResolvedProduct>>(emptyList()) }
    var dialogCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()

    val barcodeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val raw = result.data?.getSerializableExtra("SCANNED_ITEMS")
            @Suppress("UNCHECKED_CAST")
            val map = (raw as? HashMap<String, Int>)?.toMap() ?: emptyMap()
            if (map.isNotEmpty()) {
                scannedBarcodes = map
                scanState = ScanState.Processing
            }
        }
    }

    LaunchedEffect(scanState) {
        if (scanState != ScanState.Processing) return@LaunchedEffect
        if (scannedBarcodes.isEmpty()) {
            scanState = ScanState.Idle
            return@LaunchedEffect
        }

        confirmedItems.clear()
        unknownBarcodes.clear()

        for ((barcode, qty) in scannedBarcodes) {
            val resolved = viewModel.resolveBarcode(barcode)
            if (resolved != null) {
                confirmedItems.add(
                    PurchaseItem(
                        productId = resolved.product.id,
                        productName = resolved.product.name,
                        quantity = qty,
                        purchasePrice = resolved.lastPurchasePrice,
                        sellingPrice = resolved.product.sellingPrice,
                        barcode = barcode
                    )
                )
            } else {
                unknownBarcodes.add(Pair(barcode, qty))
            }
        }

        processingUnknownIndex = 0
        if (unknownBarcodes.isEmpty()) {
            scanState = ScanState.Confirming
        } else {
            dialogCategories = viewModel.getExistingCategories()
            scanState = ScanState.CreatingProduct
        }
    }

    var unknownDialogBarcode by remember { mutableStateOf("") }
    var unknownDialogQty by remember { mutableIntStateOf(1) }

    if (scanState == ScanState.CreatingProduct) {
        val currentUnknown = unknownBarcodes.getOrNull(processingUnknownIndex)
        if (currentUnknown != null) {
            unknownDialogBarcode = currentUnknown.first
            unknownDialogQty = currentUnknown.second
            AddProductForPurchaseDialog(
                barcode = currentUnknown.first,
                existingCategories = dialogCategories,
                initialSellingPrice = 0.0,
                onSave = { name, buyingPrice, sellingPrice, category, qty ->
                    viewModel.createNewProductAndQueue(
                        name, buyingPrice, sellingPrice, category, qty, currentUnknown.first
                    ) { purchaseItem ->
                        confirmedItems.add(purchaseItem)
                        processingUnknownIndex++
                        if (processingUnknownIndex >= unknownBarcodes.size) {
                            scanState = ScanState.Confirming
                        }
                    }
                },
                onDismiss = {
                    processingUnknownIndex++
                    if (processingUnknownIndex >= unknownBarcodes.size) {
                        scanState = ScanState.Confirming
                    }
                }
            )
        }
    }

    if (scanState == ScanState.Confirming) {
        PurchaseConfirmationDialog(
            items = confirmedItems.toList(),
            onSave = { items ->
                viewModel.recordPurchases(items)
                scanState = ScanState.Idle
                confirmedItems.clear()
                unknownBarcodes.clear()
            },
            onDismiss = {
                scanState = ScanState.Idle
                confirmedItems.clear()
                unknownBarcodes.clear()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Investment History", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Blue227ed4)
            )
        },
        floatingActionButton = {
            if (scanState == ScanState.Idle) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FloatingActionButton(
                        onClick = {
                            val intent = Intent(context, ContinuousScannerActivity::class.java).apply {
                                putStringArrayListExtra("KNOWN_BARCODES", ArrayList(knownBarcodes))
                            }
                            barcodeLauncher.launch(intent)
                        },
                        containerColor = Blue227ed4
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Scan Barcode", tint = Color.White)
                    }
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                productList = viewModel.getAllProducts()
                                showProductPicker = true
                            }
                        },
                        containerColor = Blue227ed4
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = "Select Product", tint = Color.White)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceGray)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Blue227ed4),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "${Constants.CURRENCY_SYMBOL}${totalInvestment.toLong()}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue227ed4
                        )
                        Text(
                            text = "Total Invested",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (scanState == ScanState.Processing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Resolving barcodes...", fontSize = 14.sp, color = TextSecondary)
                }
            } else if (investments.isEmpty() && scanState == ScanState.Idle) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No purchases yet.\nTap the camera button to scan & record a purchase.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (scanState != ScanState.Processing) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(investments, key = { it.id }) { investment ->
                        InvestmentItem(
                            amount = investment.amount,
                            createdAt = investment.createdAt,
                            productName = investment.productName,
                            quantity = investment.quantity,
                            purchasePrice = investment.purchasePrice,
                            onDelete = { showDeleteConfirm = investment.id }
                        )
                    }
                }
            }
        }
    }

    showDeleteConfirm?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Delete Purchase", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this purchase entry? Stock will not be reversed.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteInvestment(id)
                    showDeleteConfirm = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            }
        )
    }

    if (showProductPicker) {
        ProductPickerDialog(
            products = productList,
            onSelect = { resolved ->
                confirmedItems.clear()
                confirmedItems.add(
                    PurchaseItem(
                        productId = resolved.product.id,
                        productName = resolved.product.name,
                        quantity = 0,
                        purchasePrice = resolved.lastPurchasePrice,
                        sellingPrice = resolved.product.sellingPrice,
                        barcode = resolved.product.barcode
                    )
                )
                showProductPicker = false
                scanState = ScanState.Confirming
            },
            onDismiss = { showProductPicker = false }
        )
    }
}

@Composable
private fun InvestmentItem(
    amount: Double,
    createdAt: Long,
    productName: String,
    quantity: Int,
    purchasePrice: Double,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = productName.ifBlank { "Investment" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827)
                )
                if (quantity > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${quantity}x ${Constants.CURRENCY_SYMBOL}${purchasePrice.toLong()}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "= ${Constants.CURRENCY_SYMBOL}${amount.toLong()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF22C55E)
                        )
                    }
                } else {
                    Text(
                        text = "${Constants.CURRENCY_SYMBOL}${amount.toLong()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22C55E)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateFormat.format(Date(createdAt)),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
