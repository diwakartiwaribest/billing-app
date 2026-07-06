package com.shop.billing.ui.screens.recyclebin

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.Customer
import com.shop.billing.data.model.CustomerPayment
import com.shop.billing.data.model.ShopItem
import com.shop.billing.ui.components.DialogConfirmButton
import com.shop.billing.ui.components.DialogOverlay
import com.shop.billing.ui.theme.Blue227ed4
import com.shop.billing.ui.theme.SurfaceGray
import com.shop.billing.ui.theme.TextPrimary
import com.shop.billing.ui.theme.TextSecondary
import com.shop.billing.util.Constants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    navController: NavController,
    viewModel: RecycleBinViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var tabIndex by remember { mutableIntStateOf(0) }
    val snackbar = state.lastRestored

    LaunchedEffect(snackbar) {
        if (snackbar != null) {
            viewModel.clearLastRestored()
        }
    }

    Scaffold(
        topBar = {
            if (state.selectionMode) {
                TopAppBar(
                    title = { Text("${state.selectedIds.size} selected", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection", tint = Color.White)
                        }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.selectAllInCurrentTab(tabIndex) }) {
                            Text("Select All", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.width(4.dp))
                        val selected = state.selectedIds
                        if (selected.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val ids = selected.toList()
                                    when (tabIndex) {
                                        0 -> viewModel.restoreBills(ids)
                                        1 -> viewModel.restoreProducts(ids)
                                        2 -> viewModel.restoreCustomers(ids)
                                        3 -> viewModel.restorePayments(ids)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Restore", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFE53935), titleContentColor = Color.White, navigationIconContentColor = Color.White)
                )
            } else {
                TopAppBar(
                    title = { Text("Recycle Bin", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFE53935), titleContentColor = Color.White)
                )
            }
        },
        containerColor = SurfaceGray
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tabIndex, containerColor = Color.White) {
                RecycleTab(0, "Bills (${state.bills.size})", tabIndex == 0) { tabIndex = 0; viewModel.clearSelection() }
                RecycleTab(1, "Items (${state.products.size})", tabIndex == 1) { tabIndex = 1; viewModel.clearSelection() }
                RecycleTab(2, "Customers (${state.customers.size})", tabIndex == 2) { tabIndex = 2; viewModel.clearSelection() }
                RecycleTab(3, "Payments (${state.payments.size})", tabIndex == 3) { tabIndex = 3; viewModel.clearSelection() }
            }

            val totalDeleted = state.bills.size + state.products.size + state.customers.size + state.payments.size
            if (totalDeleted == 0 && !state.restoring) {
                EmptyRecycleBin()
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    if (state.restoring) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            Text("Restoring...", fontSize = 14.sp, color = TextSecondary)
                        }
                    }
                    when (tabIndex) {
                        0 -> BillItems(state.bills, state.selectedIds, state.selectionMode, state.canManage, viewModel::enterSelectionMode, viewModel::toggleSelection, viewModel::restoreBill)
                        1 -> ProductItems(state.products, state.selectedIds, state.selectionMode, state.canManage, viewModel::enterSelectionMode, viewModel::toggleSelection, viewModel::restoreProduct)
                        2 -> CustomerItems(state.customers, state.selectedIds, state.selectionMode, state.canManage, viewModel::enterSelectionMode, viewModel::toggleSelection, viewModel::restoreCustomer)
                        3 -> PaymentItems(state.payments, state.selectedIds, state.selectionMode, state.canManage, viewModel::enterSelectionMode, viewModel::toggleSelection, viewModel::restorePayment)
                    }
                }
            }
        }
    }

    if (snackbar != null) {
        DialogOverlay(onDismiss = { viewModel.clearLastRestored() }) {
            Text("Restore complete", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(snackbar, fontSize = 14.sp, color = Color(0xFF6B7280))
            Spacer(modifier = Modifier.height(20.dp))
            DialogConfirmButton(text = "OK", onClick = { viewModel.clearLastRestored() })
        }
    }

    if (state.error != null) {
        DialogOverlay(onDismiss = { viewModel.clearError() }) {
            Text("Restore failed", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(state.error!!, fontSize = 14.sp, color = Color(0xFF6B7280))
            Spacer(modifier = Modifier.height(20.dp))
            DialogConfirmButton(text = "OK", onClick = { viewModel.clearError() })
        }
    }
}

@Composable
private fun RecycleTab(idx: Int, label: String, selected: Boolean, onClick: () -> Unit) {
    Tab(selected = selected, onClick = onClick, text = {
        Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
    })
}

@Composable
private fun EmptyRecycleBin() {
    Column(modifier = Modifier.fillMaxSize().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(16.dp))
        Text("No deleted items", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("Deleted bills, items, customers and payments appear here.", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 32.dp), textAlign = TextAlign.Center)
    }
}

private fun formatDate(ts: Long): String {
    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return "Deleted on ${df.format(Date(ts))}"
}

@Composable
private fun BillItems(
    bills: List<Bill>,
    selectedIds: Set<String>,
    selectionMode: Boolean,
    canManage: Boolean,
    onEnterSelection: (String) -> Unit,
    onToggle: (String) -> Unit,
    onRestore: (String) -> Unit,
) {
    androidx.compose.foundation.lazy.LazyColumn {
        items(bills, key = { it.id }) { b ->
            val isSelected = b.id in selectedIds
            RecycleCard(
                id = b.id,
                icon = Icons.Default.Receipt,
                title = if (b.billNumber.isNotBlank()) "Bill #${b.billNumber}" else "Bill",
                subtitle = "${b.customerName.ifBlank { "—" }} • ${Constants.CURRENCY_SYMBOL}${b.totalAmount.toLong()} • ${b.paymentStatus}",
                meta = formatDate(b.updatedAt),
                isSelected = isSelected,
                selectionMode = selectionMode,
                onLongClick = { if (canManage) onEnterSelection(b.id) },
                onClick = { if (selectionMode) onToggle(b.id) },
                action = { if (canManage && !selectionMode) RowAction("Restore", Icons.Default.Restore, onClick = { onRestore(b.id) }) }
            )
        }
    }
}

@Composable
private fun ProductItems(
    items: List<ShopItem>,
    selectedIds: Set<String>,
    selectionMode: Boolean,
    canManage: Boolean,
    onEnterSelection: (String) -> Unit,
    onToggle: (String) -> Unit,
    onRestore: (String) -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn {
        items(items, key = { it.id }) { i ->
            val isSelected = i.id in selectedIds
            RecycleCard(
                id = i.id,
                icon = Icons.Default.Inventory2,
                title = i.name,
                subtitle = "${i.category} • ${Constants.CURRENCY_SYMBOL}${i.sellingPrice.toLong()}",
                meta = formatDate(i.updatedAt),
                isSelected = isSelected,
                selectionMode = selectionMode,
                onLongClick = { if (canManage) onEnterSelection(i.id) },
                onClick = { if (selectionMode) onToggle(i.id) },
                action = { if (canManage && !selectionMode) RowAction("Restore", Icons.Default.Restore, onClick = { onRestore(i.id) }) }
            )
        }
    }
}

@Composable
private fun CustomerItems(
    items: List<Customer>,
    selectedIds: Set<String>,
    selectionMode: Boolean,
    canManage: Boolean,
    onEnterSelection: (String) -> Unit,
    onToggle: (String) -> Unit,
    onRestore: (String) -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn {
        items(items, key = { it.mobile }) { c ->
            val isSelected = c.mobile in selectedIds
            RecycleCard(
                id = c.mobile,
                icon = Icons.Default.Person,
                title = c.name.ifBlank { c.mobile },
                subtitle = "${c.mobile} • ${Constants.CURRENCY_SYMBOL}${c.totalSpent.toLong()} spent",
                meta = formatDate(c.updatedAt),
                isSelected = isSelected,
                selectionMode = selectionMode,
                onLongClick = { if (canManage) onEnterSelection(c.mobile) },
                onClick = { if (selectionMode) onToggle(c.mobile) },
                action = { if (canManage && !selectionMode) RowAction("Restore", Icons.Default.Restore, onClick = { onRestore(c.mobile) }) }
            )
        }
    }
}

@Composable
private fun PaymentItems(
    items: List<CustomerPayment>,
    selectedIds: Set<String>,
    selectionMode: Boolean,
    canManage: Boolean,
    onEnterSelection: (String) -> Unit,
    onToggle: (String) -> Unit,
    onRestore: (String) -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn {
        items(items, key = { it.uuid }) { p ->
            val isSelected = p.uuid in selectedIds
            RecycleCard(
                id = p.uuid,
                icon = Icons.Default.Payments,
                title = "Payment ${Constants.CURRENCY_SYMBOL}${p.amount.toLong()}",
                subtitle = "${p.customerMobile} • ${if (p.note.isBlank()) "—" else p.note}",
                meta = formatDate(p.updatedAt),
                isSelected = isSelected,
                selectionMode = selectionMode,
                onLongClick = { if (canManage) onEnterSelection(p.uuid) },
                onClick = { if (selectionMode) onToggle(p.uuid) },
                action = { if (canManage && !selectionMode) RowAction("Restore", Icons.Default.Restore, onClick = { onRestore(p.uuid) }) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun RecycleCard(
    id: String,
    icon: ImageVector,
    title: String,
    subtitle: String,
    meta: String,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {},
    action: @Composable () -> Unit = {}
) {
    val containerColor = when {
        isSelected -> Color(0xFFDBEAFE)
        selectionMode -> Color(0xFFF9FAFB)
        else -> Color.White
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selectionMode) onClick() else onLongClick() },
                onLongClick = { if (!selectionMode) onLongClick() }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).background(
                    if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text("✓", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                } else {
                    Icon(icon, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(meta, fontSize = 11.sp, color = Color(0xFF9CA3AF))
            }
            action()
        }
    }
}

@Composable
private fun RowAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blue227ed4)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp)
    }
}
