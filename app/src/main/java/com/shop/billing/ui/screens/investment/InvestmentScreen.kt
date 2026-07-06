package com.shop.billing.ui.screens.investment

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shop.billing.ui.screens.newbill.ContinuousScannerActivity
import com.shop.billing.ui.components.ConfirmDialogOverlay
import com.shop.billing.ui.components.DialogCancelButton
import com.shop.billing.ui.components.DialogConfirmButton
import com.shop.billing.ui.components.DialogOverlay
import com.shop.billing.ui.theme.Blue227ed4
import com.shop.billing.util.Constants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private enum class ScanState { Idle, Scanning, CreatingProduct, Confirming }

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
    var confirmedItems = remember { mutableStateListOf<PurchaseItem>() }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    var showProductPicker by remember { mutableStateOf(false) }
    var productList by remember { mutableStateOf<List<ResolvedProduct>>(emptyList()) }
    var dialogCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    var unknownBarcode by remember { mutableStateOf("") }
    var unknownQty by remember { mutableIntStateOf(0) }
    var isManualEntry by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var editingPendingItem by remember { mutableStateOf<PurchaseItem?>(null) }
    val scope = rememberCoroutineScope()

    val barcodeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scanState = ScanState.Idle
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult

        val scannedItems = result.data?.getSerializableExtra("SCANNED_ITEMS") as? HashMap<String, Int>
        val singleBarcode = result.data?.getStringExtra("SINGLE_SCAN_BARCODE")

        scope.launch {
            if (scannedItems != null && scannedItems.isNotEmpty()) {
                for ((barcode, qty) in scannedItems) {
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
                    }
                }
            }
            if (singleBarcode != null) {
                val qty = result.data?.getIntExtra("SINGLE_SCAN_QTY", 0) ?: 0
                if (scannedItems?.containsKey(singleBarcode) != true) {
                    val resolved = viewModel.resolveBarcode(singleBarcode)
                    if (resolved != null) {
                        confirmedItems.add(
                            PurchaseItem(
                                productId = resolved.product.id,
                                productName = resolved.product.name,
                                quantity = qty,
                                purchasePrice = resolved.lastPurchasePrice,
                                sellingPrice = resolved.product.sellingPrice,
                                barcode = singleBarcode
                            )
                        )
                    } else {
                        unknownBarcode = singleBarcode
                        unknownQty = qty
                        dialogCategories = viewModel.getExistingCategories()
                        scanState = ScanState.CreatingProduct
                    }
                }
            }
        }
    }

    fun launchScanner() {
        val intent = Intent(context, ContinuousScannerActivity::class.java).apply {
            putExtra("SINGLE_SCAN_MODE", true)
            putStringArrayListExtra("KNOWN_BARCODES", ArrayList(knownBarcodes))
        }
        barcodeLauncher.launch(intent)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Investment History", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Blue227ed4)
            )
        },
        floatingActionButton = {
            if (scanState == ScanState.Idle) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FloatingActionButton(
                        onClick = { launchScanner() },
                        containerColor = Blue227ed4
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Scan Barcode", tint = MaterialTheme.colorScheme.onPrimary)
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
                        Icon(Icons.Default.Inventory2, contentDescription = "Select Product", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    FloatingActionButton(
                        onClick = {
                            unknownBarcode = ""
                            unknownQty = 0
                            scope.launch {
                                dialogCategories = viewModel.getExistingCategories()
                                isManualEntry = true
                                scanState = ScanState.CreatingProduct
                            }
                        },
                        containerColor = Blue227ed4
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Manually", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                            tint = MaterialTheme.colorScheme.onPrimary,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (confirmedItems.isNotEmpty() && scanState == ScanState.Idle) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { launchScanner() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan Next (${confirmedItems.size})")
                    }
                    Button(
                        onClick = { scanState = ScanState.Confirming },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                    ) {
                        Text("Finish & Confirm")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (scanState == ScanState.Scanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Waiting for scanner...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (investments.isEmpty() && confirmedItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No purchases yet.\nTap the camera button to scan & record a purchase.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (scanState != ScanState.Scanning) {
                val allItems = confirmedItems.toList()
                if (selectedIds.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${selectedIds.size} selected", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Blue227ed4)
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { showBatchDeleteConfirm = true }) {
                            Text("Delete Selected", color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        TextButton(onClick = { selectedIds = emptySet() }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (allItems.isNotEmpty()) {
                    Text(
                        "Pending (${allItems.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allItems, key = { it.productId + "_pending" }) { item ->
                            PendingItemCard(
                                item = item,
                                onClick = { editingPendingItem = item },
                                onRemove = { confirmedItems.remove(item) }
                            )
                        }
                        if (investments.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "History",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                        items(investments, key = { it.id }) { investment ->
                            InvestmentItem(
                                amount = investment.amount,
                                createdAt = investment.createdAt,
                                productName = investment.productName,
                                quantity = investment.quantity,
                                purchasePrice = investment.purchasePrice,
                                id = investment.id,
                                isSelected = investment.id in selectedIds,
                                isSelectionActive = selectedIds.isNotEmpty(),
                                onLongClick = {
                                    selectedIds = setOf(investment.id)
                                },
                                onToggleSelect = {
                                    selectedIds = if (investment.id in selectedIds)
                                        selectedIds - investment.id
                                    else
                                        selectedIds + investment.id
                                },
                                onDelete = { showDeleteConfirm = investment.id }
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(investments, key = { it.id }) { investment ->
                            InvestmentItem(
                                amount = investment.amount,
                                createdAt = investment.createdAt,
                                productName = investment.productName,
                                quantity = investment.quantity,
                                purchasePrice = investment.purchasePrice,
                                id = investment.id,
                                isSelected = investment.id in selectedIds,
                                isSelectionActive = selectedIds.isNotEmpty(),
                                onLongClick = {
                                    selectedIds = setOf(investment.id)
                                },
                                onToggleSelect = {
                                    selectedIds = if (investment.id in selectedIds)
                                        selectedIds - investment.id
                                    else
                                        selectedIds + investment.id
                                },
                                onDelete = { showDeleteConfirm = investment.id }
                            )
}
                }
            }
        }
    }
}

        if (scanState == ScanState.CreatingProduct) {
            AddProductForPurchaseDialog(
                barcode = unknownBarcode,
                existingCategories = dialogCategories,
                initialSellingPrice = 0.0,
                initialQuantity = unknownQty,
                onSave = { name, buyingPrice, sellingPrice, category, qty ->
                    confirmedItems.add(
                        PurchaseItem(
                            productId = "",
                            productName = name,
                            quantity = qty,
                            purchasePrice = buyingPrice,
                            sellingPrice = sellingPrice,
                            barcode = unknownBarcode,
                            category = category
                        )
                    )
                    scanState = ScanState.Idle
                    if (!isManualEntry) launchScanner()
                    isManualEntry = false
                },
                onDismiss = {
                    scanState = ScanState.Idle
                    isManualEntry = false
                }
            )
        }

        if (scanState == ScanState.Confirming) {
            PurchaseConfirmationDialog(
                items = confirmedItems.toList(),
                onSave = { items ->
                    viewModel.recordPurchases(items)
                    scanState = ScanState.Idle
                    confirmedItems.clear()
                },
                onDismiss = {
                    scanState = ScanState.Idle
                    confirmedItems.clear()
                },
                onEmpty = {
                    confirmedItems.clear()
                    scanState = ScanState.Idle
                    scope.launch {
                        productList = viewModel.getAllProducts()
                        showProductPicker = true
                    }
                }
            )
        }

        showDeleteConfirm?.let { id ->
            ConfirmDialogOverlay(
                title = "Delete Purchase",
                message = "Are you sure you want to delete this purchase entry? Stock will not be reversed.",
                confirmText = "Delete",
                onConfirm = { viewModel.deleteInvestment(id); showDeleteConfirm = null },
                onDismiss = { showDeleteConfirm = null },
                destructive = true
            )
        }

        if (showBatchDeleteConfirm) {
            ConfirmDialogOverlay(
                title = "Delete ${selectedIds.size} Purchases?",
                message = "Are you sure you want to delete ${selectedIds.size} purchase entr${if (selectedIds.size == 1) "y" else "ies"}? Stock will not be reversed.",
                confirmText = "Delete All",
                onConfirm = {
                    viewModel.deleteInvestments(selectedIds.toList())
                    selectedIds = emptySet()
                    showBatchDeleteConfirm = false
                },
                onDismiss = { showBatchDeleteConfirm = false },
                destructive = true
            )
        }

        editingPendingItem?.let { original ->
            EditPendingItemDialog(
                item = original,
                onSave = { updated ->
                    val idx = confirmedItems.indexOf(original)
                    if (idx >= 0) confirmedItems[idx] = updated
                    editingPendingItem = null
                },
                onDismiss = { editingPendingItem = null }
            )
        }

        if (showProductPicker) {
            val categories by viewModel.allCategories.collectAsState()
            ProductPickerDialog(
                products = productList,
                categories = categories,
                onSelect = { selected ->
                    confirmedItems.clear()
                    confirmedItems.addAll(selected.map { resolved ->
                        PurchaseItem(
                            productId = resolved.product.id,
                            productName = resolved.product.name,
                            quantity = 0,
                            purchasePrice = resolved.lastPurchasePrice,
                            sellingPrice = resolved.product.sellingPrice,
                            barcode = resolved.product.barcode
                        )
                    })
                    showProductPicker = false
                    scanState = ScanState.Confirming
                },
                onDismiss = { showProductPicker = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingItemCard(item: PurchaseItem, onClick: () -> Unit = {}, onRemove: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${item.quantity}x ${Constants.CURRENCY_SYMBOL}${item.purchasePrice.toLong()}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.barcode.isNotBlank()) {
                    Text(
                        text = "Barcode: ${item.barcode}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InvestmentItem(
    amount: Double,
    createdAt: Long,
    productName: String,
    quantity: Int,
    purchasePrice: Double,
    id: String,
    isSelected: Boolean = false,
    isSelectionActive: Boolean = false,
    onLongClick: () -> Unit = {},
    onToggleSelect: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isSelectionActive) onToggleSelect() },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Checkbox(
                    checked = true,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = Blue227ed4),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = productName.ifBlank { "Investment" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (quantity > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${quantity}x ${Constants.CURRENCY_SYMBOL}${purchasePrice.toLong()}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isSelectionActive) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPendingItemDialog(
    item: PurchaseItem,
    onSave: (PurchaseItem) -> Unit,
    onDismiss: () -> Unit
) {
    var qtyText by remember(item.barcode) { mutableStateOf(item.quantity.toString()) }
    var priceText by remember(item.barcode) {
        mutableStateOf(if (item.purchasePrice > 0) item.purchasePrice.toLong().toString() else "")
    }

    val subtotal = (priceText.toDoubleOrNull() ?: 0.0) * (qtyText.toIntOrNull()?.coerceAtLeast(1) ?: 0)

    DialogOverlay(onDismiss = { }) {
        Text("Edit Pending Item", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(item.productName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = qtyText,
                onValueChange = { input ->
                    qtyText = input.filter { it.isDigit() }
                },
                label = { Text("Quantity", fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp)
            )
            OutlinedTextField(
                value = priceText,
                onValueChange = { input ->
                    priceText = input.filter { it.isDigit() || it == '.' }
                },
                label = { Text("Purchase Price", fontSize = 12.sp) },
                prefix = { Text("${Constants.CURRENCY_SYMBOL} ", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                text = "Subtotal: ${Constants.CURRENCY_SYMBOL}${subtotal.toLong()}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Blue227ed4
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DialogCancelButton(onClick = onDismiss, modifier = Modifier.weight(1f), text = "Cancel")
            DialogConfirmButton(
                text = "Save",
                modifier = Modifier.weight(1f),
                onClick = {
                    val newQty = qtyText.toIntOrNull()?.coerceAtLeast(1) ?: item.quantity
                    val newPrice = priceText.toDoubleOrNull() ?: item.purchasePrice
                    onSave(item.copy(quantity = newQty, purchasePrice = newPrice))
                }
            )
        }
    }
}
