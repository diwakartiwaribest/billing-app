package com.shop.billing.ui.screens.items

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.journeyapps.barcodescanner.CaptureActivity
import com.shop.billing.data.model.ShopItem
import com.shop.billing.ui.components.CategoryFilter
import com.shop.billing.ui.components.ConfirmDialogOverlay
import com.shop.billing.ui.components.DialogOverlay
import com.shop.billing.ui.components.EmptyState
import com.shop.billing.ui.components.SearchBar
import androidx.compose.material3.MaterialTheme
import com.shop.billing.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    navController: NavController,
    stockFilter: String = "",
    viewModel: ItemsViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isOwner by viewModel.isOwner.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShopItem?>(null) }
    var showManageCategories by remember { mutableStateOf(false) }
    var scannedBarcode by remember { mutableStateOf("") }
    val selectedItems = remember { mutableStateMapOf<String, ShopItem>() }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<ShopItem?>(null) }
    val isSelectionMode by remember { derivedStateOf { selectedItems.isNotEmpty() } }
    val context = LocalContext.current

    val barcodeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val barcode = (result.data?.getStringExtra("SCAN_RESULT") ?: "").trim()
            if (barcode.isNotBlank()) {
                viewModel.getItemByBarcode(barcode) { item ->
                    if (item != null) {
                        editingItem = item
                        showDialog = true
                    } else {
                        scannedBarcode = barcode
                        showDialog = true
                    }
                }
            }
        }
    }

    LaunchedEffect(stockFilter) {
        viewModel.setStockFilter(stockFilter)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                        title = { Text("${selectedItems.size} selected", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = { selectedItems.clear() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit selection", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        val title = when (stockFilter) {
                            "low" -> "Low Stock Items"
                            "out" -> "Out of Stock Items"
                            else -> "Shop Items"
                        }
                        Text(title, fontWeight = FontWeight.SemiBold)
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        if (isOwner || isAdmin) {
                            IconButton(onClick = { showManageCategories = true }) {
                                Icon(Icons.Default.Category, contentDescription = "Manage categories", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FloatingActionButton(
                        onClick = {
                            val intent = Intent(context, CaptureActivity::class.java).apply {
                                putExtra("SCAN_MODE", "PRODUCT_MODE")
                            }
                            barcodeLauncher.launch(intent)
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Scan barcode")
                    }
                    FloatingActionButton(
                        onClick = { showDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add item")
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange
            )
            CategoryFilter(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = viewModel::onCategorySelected
            )

            if (items.isEmpty()) {
                EmptyState(
                    title = "No items yet",
                    subtitle = "Tap + to add your first shop item"
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 144.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ItemListItem(
                            item = item,
                            isSelected = item.id in selectedItems,
                            isSelectionMode = isSelectionMode,
                            onEdit = { editingItem = it },
                            onDelete = { itemToDelete = it },
                            onLongClick = { if (isOwner || isAdmin) selectedItems[it.id] = it },
                            onToggleSelection = {
                                if (it.id in selectedItems) selectedItems.remove(it.id)
                                else selectedItems[it.id] = it
                            },
                            canDelete = isOwner || isAdmin
                        )
                    }
                }
            }
        }
    }

    itemToDelete?.let { item ->
        ConfirmDialogOverlay(
            title = "Delete item?",
            message = "Delete \"${item.name}\"? This cannot be undone.",
            confirmText = "Delete",
            onConfirm = { viewModel.deleteItem(item.id); itemToDelete = null },
            onDismiss = { itemToDelete = null },
            destructive = true
        )
    }

    if (showDeleteConfirm) {
        ConfirmDialogOverlay(
            title = "Delete items?",
            message = "Delete ${selectedItems.size} selected item${if (selectedItems.size != 1) "s" else ""}? This cannot be undone.",
            confirmText = "Delete",
            onConfirm = { viewModel.deleteItems(selectedItems.keys.toList()); selectedItems.clear(); showDeleteConfirm = false },
            onDismiss = { showDeleteConfirm = false },
            destructive = true
        )
    }

    if (showDialog || editingItem != null) {
        AddEditItemDialog(
            existingItem = editingItem,
            existingCategories = allCategories,
            isOwner = isOwner || isAdmin,
            barcode = if (editingItem != null) editingItem!!.barcode else scannedBarcode,
            onDismiss = {
                showDialog = false
                editingItem = null
                scannedBarcode = ""
            },
            onSave = { name, sellingPrice, buyingPrice, category, stockQty, threshold, barcode ->
                if (editingItem != null) {
                    viewModel.updateItem(editingItem!!.copy(name = name, sellingPrice = sellingPrice, buyingPrice = buyingPrice, category = category, stockQuantity = stockQty, lowStockThreshold = threshold))
                } else {
                    viewModel.addItem(name, sellingPrice, buyingPrice, category, stockQty, threshold, barcode)
                }
                showDialog = false
                editingItem = null
                scannedBarcode = ""
            }
        )
    }

    if (showManageCategories) {
        ManageCategoriesOverlay(
            categories = allCategories,
            onAdd = { viewModel.addCategory(it) },
            onDelete = { viewModel.deleteCategory(it) },
            onDismiss = { showManageCategories = false }
        )
    }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemListItem(
    item: ShopItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onEdit: (ShopItem) -> Unit,
    onDelete: (ShopItem) -> Unit,
    onLongClick: (ShopItem) -> Unit,
    onToggleSelection: (ShopItem) -> Unit,
    canDelete: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelection(item)
                },
                onLongClick = {
                    if (!isSelectionMode && canDelete) onLongClick(item)
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection(item) },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Icon(
                Icons.Default.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF0EA5E9)))
                    )
                    .padding(10.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.category,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                val isLow = item.stockQuantity > 0 && item.stockQuantity <= item.lowStockThreshold
                Text(
                    text = "Stock: ${item.stockQuantity}${if (isLow) " (low ≤ ${item.lowStockThreshold})" else ""}",
                    fontSize = 11.sp,
                    color = if (item.stockQuantity <= 0) Color(0xFFDC2626) else if (isLow) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${Constants.CURRENCY_SYMBOL}${item.sellingPrice.toLong()}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (item.buyingPrice > 0) {
                    Text(
                        text = "Buy: ${Constants.CURRENCY_SYMBOL}${item.buyingPrice.toLong()}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            if (!isSelectionMode && canDelete) {
                IconButton(onClick = { onEdit(item) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ManageCategoriesOverlay(
    categories: List<String>,
    onAdd: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newCategory by remember { mutableStateOf("") }
    var confirmDeleteCategory by remember { mutableStateOf<String?>(null) }

        DialogOverlay(onDismiss = onDismiss) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Categories", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newCategory, onValueChange = { newCategory = it },
                    label = { Text("New category") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, focusedContainerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onAdd(newCategory.trim()); newCategory = "" },
                    enabled = newCategory.trim().isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.height(58.dp).offset(y = 3.dp)
                ) { Text("Add", fontWeight = FontWeight.SemiBold) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("All Categories", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("${categories.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E7EB)))
            val catScrollState = rememberScrollState()
            var catContainerHeight by remember { mutableIntStateOf(0) }
            val density = LocalDensity.current
            Row(modifier = Modifier.weight(1f, fill = false).onSizeChanged { catContainerHeight = it.height }) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(catScrollState).padding(end = 8.dp)) {
                    if (categories.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            Text("No categories yet.\nAdd one above.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        categories.forEachIndexed { index, cat ->
                            Row(
                                modifier = Modifier.fillMaxWidth().background(if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = cat, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                IconButton(onClick = { confirmDeleteCategory = cat }, modifier = Modifier.size(36.dp)) {
                                    Box(modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                }
                if (catScrollState.maxValue > 0) {
                    val cH = catContainerHeight.coerceAtLeast(1).toFloat()
                    val totalContent = cH + catScrollState.maxValue
                    val thumbRatio = cH / totalContent
                    val thumbH = (cH * thumbRatio).coerceAtLeast(32f)
                    val maxOff = (cH - thumbH).coerceAtLeast(0f)
                    val thumbOff = if (catScrollState.maxValue > 0)
                        (catScrollState.value.toFloat() / catScrollState.maxValue) * maxOff else 0f
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(with(density) { thumbH.toDp() })
                                .offset(y = with(density) { thumbOff.toDp() })
                                .background(Color(0xFFCBD5E1), RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDismiss, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Done", fontWeight = FontWeight.SemiBold)
            }
        }

    confirmDeleteCategory?.let { cat ->
        ConfirmDialogOverlay(
            title = "Delete Category?",
            message = "This will delete \"$cat\" and ALL items in it. This cannot be undone.",
            confirmText = "Delete",
            onConfirm = { onDelete(cat); confirmDeleteCategory = null },
            onDismiss = { confirmDeleteCategory = null },
            destructive = true
        )
    }
}
