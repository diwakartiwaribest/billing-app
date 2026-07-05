package com.shop.billing.ui.screens.recyclebin

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
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
            TopAppBar(
                title = { Text("Recycle Bin", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFE53935), titleContentColor = Color.White)
            )
        },
        containerColor = SurfaceGray
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tabIndex, containerColor = Color.White) {
                RecycleTab(0, "Bills (${state.bills.size})", tabIndex == 0) { tabIndex = 0 }
                RecycleTab(1, "Items (${state.products.size})", tabIndex == 1) { tabIndex = 1 }
                RecycleTab(2, "Customers (${state.customers.size})", tabIndex == 2) { tabIndex = 2 }
                RecycleTab(3, "Payments (${state.payments.size})", tabIndex == 3) { tabIndex = 3 }
            }

            if (state.loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading...", fontSize = 14.sp, color = TextSecondary)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    val totalDeleted = state.bills.size + state.products.size + state.customers.size + state.payments.size
                    if (totalDeleted == 0) {
                        EmptyRecycleBin()
                    } else {
                        when (tabIndex) {
                            0 -> BillsList(state.bills, state.canManage) { viewModel.restoreBill(it) }
                            1 -> ProductsList(state.products, state.canManage) { viewModel.restoreProduct(it) }
                            2 -> CustomersList(state.customers, state.canManage) { viewModel.restoreCustomer(it) }
                            3 -> PaymentsList(state.payments, state.canManage) { viewModel.restorePayment(it) }
                        }
                        Spacer(Modifier.height(80.dp))
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
        Text("Deleted bills, items, customers and payments appear here.", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun BillsList(bills: List<Bill>, canManage: Boolean, onRestore: (String) -> Unit) {
    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    bills.forEach { b -> RecycleCard(
        icon = Icons.Default.Receipt,
        title = if (b.billNumber.isNotBlank()) "Bill #${b.billNumber}" else "Bill",
        subtitle = "${b.customerName.ifBlank { "—" }} • ${Constants.CURRENCY_SYMBOL}${b.totalAmount.toLong()} • ${b.paymentStatus}",
        meta = "Deleted on ${df.format(Date(b.updatedAt))}"
    ) { if (canManage) RowAction("Restore", Icons.Default.Restore, onClick = { onRestore(b.id) }) }
    }
}

@Composable
private fun ProductsList(items: List<ShopItem>, canManage: Boolean, onRestore: (String) -> Unit) {
    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    items.forEach { i -> RecycleCard(
        icon = Icons.Default.Inventory2,
        title = i.name,
        subtitle = "${i.category} • ${Constants.CURRENCY_SYMBOL}${i.sellingPrice.toLong()}",
        meta = "Deleted on ${df.format(Date(i.updatedAt))}"
    ) { if (canManage) RowAction("Restore", Icons.Default.Restore, onClick = { onRestore(i.id) }) }
    }
}

@Composable
private fun CustomersList(items: List<Customer>, canManage: Boolean, onRestore: (String) -> Unit) {
    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    items.forEach { c -> RecycleCard(
        icon = Icons.Default.Person,
        title = c.name.ifBlank { c.mobile },
        subtitle = "${c.mobile} • ${Constants.CURRENCY_SYMBOL}${c.totalSpent.toLong()} spent",
        meta = "Deleted on ${df.format(Date(c.updatedAt))}"
    ) { if (canManage) RowAction("Restore", Icons.Default.Restore, onClick = { onRestore(c.mobile) }) }
    }
}

@Composable
private fun PaymentsList(items: List<CustomerPayment>, canManage: Boolean, onRestore: (String) -> Unit) {
    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    items.forEach { p -> RecycleCard(
        icon = Icons.Default.Payments,
        title = "Payment ${Constants.CURRENCY_SYMBOL}${p.amount.toLong()}",
        subtitle = "${p.customerMobile} • ${if (p.note.isBlank()) "—" else p.note}",
        meta = "Deleted on ${df.format(Date(p.updatedAt))}"
    ) { if (canManage) RowAction("Restore", Icons.Default.Restore, onClick = { onRestore(p.uuid) }) }
    }
}

@Composable
private fun RecycleCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    meta: String,
    action: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).background(Color(0xFFEF4444).copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
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
private fun RowAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
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
