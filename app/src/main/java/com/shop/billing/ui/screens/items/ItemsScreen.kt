package com.shop.billing.ui.screens.items

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.shop.billing.ui.components.EmptyState
import com.shop.billing.ui.components.SearchBar
import com.shop.billing.ui.theme.Blue227ed4
import com.shop.billing.ui.theme.SurfaceGray
import com.shop.billing.ui.theme.TextPrimary
import com.shop.billing.ui.theme.TextSecondary
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

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedItems.size} selected", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = { selectedItems.clear() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit selection", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Blue227ed4,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = Color.White)
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
                        containerColor = Blue227ed4,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                    actions = {
                        if (isOwner || isAdmin) {
                            IconButton(onClick = { showManageCategories = true }) {
                                Icon(Icons.Default.Category, contentDescription = "Manage categories", tint = Color.White)
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
                        containerColor = Blue227ed4,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Scan barcode")
                    }
                    FloatingActionButton(
                        onClick = { showDialog = true },
                        containerColor = Blue227ed4,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add item")
                    }
                }
            }
        },
        containerColor = SurfaceGray
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
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Delete item?", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text("Delete \"${item.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteItem(item.id)
                    itemToDelete = null
                }) {
                    Text("Delete", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Delete items?", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text("Delete ${selectedItems.size} selected item${if (selectedItems.size != 1) "s" else ""}? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteItems(selectedItems.keys.toList())
                    selectedItems.clear()
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            }
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
            onSave = { name, price, category, stockQty, threshold, barcode ->
                if (editingItem != null) {
                    viewModel.updateItem(editingItem!!.copy(name = name, price = price, category = category, stockQuantity = stockQty, lowStockThreshold = threshold))
                } else {
                    viewModel.addItem(name, price, category, stockQty, threshold, barcode)
                }
                showDialog = false
                editingItem = null
                scannedBarcode = ""
            }
        )
    }

    if (showManageCategories) {
        ManageCategoriesDialog(
            categories = allCategories,
            customCategories = viewModel.customCategories.collectAsState().value,
            onAdd = { viewModel.addCategory(it) },
            onDelete = { viewModel.deleteCategory(it) },
            onDismiss = { showManageCategories = false }
        )
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
                if (isSelected) Modifier.border(2.dp, Blue227ed4, RoundedCornerShape(12.dp))
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                tint = Color.White,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(listOf(Blue227ed4, Color(0xFF0EA5E9)))
                    )
                    .padding(10.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.category,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                val isLow = item.stockQuantity > 0 && item.stockQuantity <= item.lowStockThreshold
                Text(
                    text = "Stock: ${item.stockQuantity}${if (isLow) " (low ≤ ${item.lowStockThreshold})" else ""}",
                    fontSize = 11.sp,
                    color = if (item.stockQuantity <= 0) Color(0xFFDC2626) else if (isLow) Color(0xFFF59E0B) else Color(0xFF6B7280)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${Constants.CURRENCY_SYMBOL}${item.price.toLong()}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Blue227ed4
            )
            Spacer(modifier = Modifier.width(4.dp))
            if (!isSelectionMode && canDelete) {
                IconButton(onClick = { onEdit(item) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ManageCategoriesDialog(
    categories: List<String>,
    customCategories: List<String>,
    onAdd: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newCategory by remember { mutableStateOf("") }
    var confirmDeleteCategory by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Manage Categories", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("New category") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = Blue227ed4,
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onAdd(newCategory.trim())
                            newCategory = ""
                        },
                        enabled = newCategory.trim().isNotBlank()
                    ) {
                        Text("Add", color = Blue227ed4, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (categories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    categories.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cat,
                                fontSize = 14.sp,
                                color = Color(0xFF374151),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { confirmDeleteCategory = cat },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = Blue227ed4, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = null
    )

    confirmDeleteCategory?.let { cat ->
        AlertDialog(
            onDismissRequest = { confirmDeleteCategory = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Delete Category?", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text("This will delete \"$cat\" and ALL items in it. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(cat)
                    confirmDeleteCategory = null
                }) {
                    Text("Delete", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteCategory = null }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            }
        )
    }
}
