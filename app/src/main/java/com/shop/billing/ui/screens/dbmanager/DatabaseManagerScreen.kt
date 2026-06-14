package com.shop.billing.ui.screens.dbmanager

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shop.billing.ui.theme.Blue227ed4
import com.shop.billing.ui.theme.TextPrimary
import com.shop.billing.ui.theme.TextSecondary
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class TabItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseManagerScreen(
    navController: NavController,
    initialTab: Int = 0,
    viewModel: DatabaseManagerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val bills by viewModel.bills.collectAsState()
    val billItems by viewModel.billItems.collectAsState()
    val shopItems by viewModel.shopItems.collectAsState()
    val shops by viewModel.shops.collectAsState()
    val members by viewModel.members.collectAsState()
    val shopSettings by viewModel.shopSettings.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val currentShopCode by viewModel.currentShopCode.collectAsState()

    var selectedTab by remember { mutableIntStateOf(initialTab) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadAll()
    }

    val tabs = listOf(
        TabItem("Shops", Icons.Default.Store),
        TabItem("Bills", Icons.Default.Receipt),
        TabItem("Customers", Icons.Default.Person),
        TabItem("Items", Icons.Default.Inventory2),
        TabItem("Members", Icons.Default.Group),
        TabItem("Settings", Icons.Default.Settings)
    )

    statusMessage?.let {
        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        viewModel.clearStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Database Manager", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Blue227ed4
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8F9FA))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs.size) { index ->
                    val tab = tabs[index]
                    val selected = selectedTab == index
                    val bgColor = if (selected) Blue227ed4 else Color.White
                    val contentColor = if (selected) Color.White else TextSecondary
                    val iconTint = if (selected) Color.White else Blue227ed4

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 1.dp),
                        onClick = { selectedTab = index }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = tab.title,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = contentColor
                            )
                        }
                    }
                }
            }

            Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)

            when (selectedTab) {
                0 -> ShopsTab(shops, onDelete = { viewModel.deleteShopByCode(it) }, currentShopCode = currentShopCode)
                1 -> BillsTab(bills, billItems, onDelete = { viewModel.deleteBill(it) })
                2 -> CustomersTab(customers, bills, payments, onDelete = { viewModel.deleteCustomer(it) })
                3 -> ShopItemsTab(shopItems, onDelete = { viewModel.deleteShopItem(it) },
                    onUpdate = { id, name, price, cat -> viewModel.updateShopItem(id, name, price, cat) })
                4 -> MembersTab(members, onRemove = { viewModel.removeMember(it) })
                5 -> SettingsTab(shopSettings, onUpdate = { field, value -> viewModel.updateShopSetting(field, value) })
            }
        }
    }
}

@Composable
fun ShopsTab(shops: JSONArray, onDelete: (String) -> Unit, currentShopCode: String) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteCode by remember { mutableStateOf<String?>(null) }

    if (showDeleteDialog) {
        val targetCode = pendingDeleteCode ?: ""
        val isCurrent = targetCode == currentShopCode
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Shop") },
            text = {
                Column {
                    Text("This will permanently delete shop '$targetCode' and all its data (bills, items, members, settings).")
                    if (isCurrent) {
                        Spacer(Modifier.height(8.dp))
                        Text("WARNING: This is your current shop! You will lose access.", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pendingDeleteCode?.let { onDelete(it) }; showDeleteDialog = false }) {
                    Text("Delete", color = Color(0xFFEF4444))
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (shops.length() == 0) {
        EmptyState("No shops found")
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shops.length()) { index ->
                val shop = shops.getJSONObject(index)
                val shopCode = shop.optString("code", "")
                val createdAt = shop.optString("created_at", "")
                val isCurrent = shopCode == currentShopCode

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) Color(0xFFEFF6FF) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(shopCode, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Blue227ed4)
                                if (isCurrent) {
                                    Spacer(Modifier.width(8.dp))
                                    Text("CURRENT", fontSize = 9.sp, color = Blue227ed4, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.background(Color(0xFFDBEAFE), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            if (createdAt.isNotBlank()) Text(createdAt, fontSize = 11.sp, color = TextSecondary)
                        }
                        IconButton(onClick = { pendingDeleteCode = shopCode; showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Shop", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BillsTab(bills: JSONArray, billItems: JSONArray, onDelete: (String) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Bill") },
            text = { Text("This will permanently delete this bill and all its items.") },
            confirmButton = {
                TextButton(onClick = { pendingDeleteId?.let { onDelete(it) }; showDeleteDialog = false }) {
                    Text("Delete", color = Color(0xFFEF4444))
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (bills.length() == 0) {
        EmptyState("No bills found")
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(bills.length()) { index ->
                val bill = bills.getJSONObject(index)
                val billId = bill.optString("id", "")
                val billNum = bill.optString("bill_number", "")
                val customer = bill.optString("customer_name", "")
                val mobile = bill.optString("customer_mobile", "")
                val total = bill.optDouble("total_amount", 0.0)
                val createdAtRaw = bill.optString("created_at", "")
                val createdAtMs = try {
                    createdAtRaw.toLongOrNull() ?: try {
                        val clean = createdAtRaw
                            .replace(Regex("\\.\\d+"), "")
                            .replace(Regex("[Z]|[+-]\\d{2}:\\d{2}$"), "")
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                        sdf.parse(clean)?.time ?: 0L
                    } catch (_: Exception) { 0L }
                } catch (_: Exception) { 0L }
                val dateStr = if (createdAtMs > 0) SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(createdAtMs)) else ""

                val itemCount = billItems.let { arr ->
                    var count = 0
                    for (i in 0 until arr.length()) {
                        if (arr.getJSONObject(i).optString("bill_id") == billId) count++
                    }
                    count
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("#$billNum", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Blue227ed4)
                            if (customer.isNotBlank()) Text(customer, fontSize = 12.sp, color = TextPrimary)
                            if (mobile.isNotBlank()) Text(mobile, fontSize = 11.sp, color = TextSecondary)
                            Text("$itemCount items • $dateStr", fontSize = 11.sp, color = TextSecondary)
                        }
                        Text("₹${String.format("%.0f", total)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        IconButton(onClick = { pendingDeleteId = billId; showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomersTab(customers: JSONArray, bills: JSONArray, payments: JSONArray, onDelete: (String) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    val creditByMobile = remember(bills) {
        val map = mutableMapOf<String, Double>()
        for (i in 0 until bills.length()) {
            val b = bills.getJSONObject(i)
            if (b.optString("payment_status", "paid") == "credit") {
                val m = b.optString("customer_mobile", "")
                val amt = b.optDouble("total_amount", 0.0)
                map[m] = (map[m] ?: 0.0) + amt
            }
        }
        map
    }

    val paymentsByMobile = remember(payments) {
        val map = mutableMapOf<String, Double>()
        for (i in 0 until payments.length()) {
            val p = payments.getJSONObject(i)
            val m = p.optString("customer_mobile", "")
            val amt = p.optDouble("amount", 0.0)
            map[m] = (map[m] ?: 0.0) + amt
        }
        map
    }

    val pendingByMobile = remember(creditByMobile, paymentsByMobile) {
        val map = mutableMapOf<String, Double>()
        for ((mobile, creditTotal) in creditByMobile) {
            val totalPaid = paymentsByMobile[mobile] ?: 0.0
            val pending = (creditTotal - totalPaid).coerceAtLeast(0.0)
            map[mobile] = pending
        }
        map
    }

    val creditByMobileMap = remember(creditByMobile, paymentsByMobile) {
        val map = mutableMapOf<String, Double>()
        for ((mobile, creditTotal) in creditByMobile) {
            val totalPaid = paymentsByMobile[mobile] ?: 0.0
            val credit = (totalPaid - creditTotal).coerceAtLeast(0.0)
            map[mobile] = credit
        }
        // Also add customers with payments but no credit bills
        for ((mobile, totalPaid) in paymentsByMobile) {
            if (!creditByMobile.containsKey(mobile)) {
                val credit = totalPaid.coerceAtLeast(0.0)
                map[mobile] = credit
            }
        }
        map
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Customer") },
            text = { Text("This will permanently delete this customer record.") },
            confirmButton = {
                TextButton(onClick = { pendingDeleteId?.let { onDelete(it) }; showDeleteDialog = false }) {
                    Text("Delete", color = Color(0xFFEF4444))
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (customers.length() == 0) {
        EmptyState("No customers yet")
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(customers.length()) { index ->
                val c = customers.getJSONObject(index)
                val name = c.optString("name", "")
                val mobile = c.optString("mobile", "")
                val totalBills = c.optInt("total_bills", 0)
                val totalSpent = c.optDouble("total_spent", 0.0)
                val pendingAmount = pendingByMobile[mobile] ?: 0.0
                val creditAmount = creditByMobileMap[mobile] ?: 0.0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name.ifBlank { "Unknown" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(mobile, fontSize = 12.sp, color = TextSecondary)
                            when {
                                pendingAmount > 0 -> Text("₹${String.format("%.0f", pendingAmount)} pending", fontSize = 11.sp, color = Color(0xFFEF4444))
                                creditAmount > 0 -> Text("+₹${String.format("%.0f", creditAmount)} Credit", fontSize = 11.sp, color = Color(0xFF10B981))
                                else -> Text("₹0 Settled", fontSize = 11.sp, color = Color(0xFF22C55E))
                            }
                        }
                        IconButton(onClick = { pendingDeleteId = c.optString("id", ""); showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShopItemsTab(shopItems: JSONArray, onDelete: (String) -> Unit, onUpdate: (String, String, Double, String) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<JSONObject?>(null) }
    var editName by remember { mutableStateOf("") }
    var editPrice by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("") }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Item") },
            text = { Text("Delete this shop item permanently?") },
            confirmButton = {
                TextButton(onClick = { pendingDeleteId?.let { onDelete(it) }; showDeleteDialog = false }) {
                    Text("Delete", color = Color(0xFFEF4444))
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showEditDialog && editItem != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Item") },
            text = {
                Column {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = editPrice, onValueChange = { editPrice = it }, label = { Text("Price") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = editCategory, onValueChange = { editCategory = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editItem?.let {
                        val id = it.optString("id", "")
                        val price = editPrice.toDoubleOrNull() ?: 0.0
                        onUpdate(id, editName, price, editCategory)
                    }
                    showEditDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancel") } }
        )
    }

    if (shopItems.length() == 0) {
        EmptyState("No shop items found")
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shopItems.length()) { index ->
                val item = shopItems.getJSONObject(index)
                val name = item.optString("name", "")
                val price = item.optDouble("price", 0.0)
                val category = item.optString("category", "")
                val itemId = item.optString("id", "")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            if (category.isNotBlank()) Text(category, fontSize = 11.sp, color = TextSecondary)
                        }
                        Text("₹${String.format("%.2f", price)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        IconButton(onClick = {
                            editItem = item; editName = name; editPrice = price.toString(); editCategory = category; showEditDialog = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Blue227ed4, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { pendingDeleteId = itemId; showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MembersTab(members: JSONArray, onRemove: (String) -> Unit) {
    var showRemoveDialog by remember { mutableStateOf(false) }
    var pendingRemoveId by remember { mutableStateOf<String?>(null) }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Member") },
            text = { Text("Remove this member from the shop?") },
            confirmButton = {
                TextButton(onClick = { pendingRemoveId?.let { onRemove(it) }; showRemoveDialog = false }) {
                    Text("Remove", color = Color(0xFFEF4444))
                }
            },
            dismissButton = { TextButton(onClick = { showRemoveDialog = false }) { Text("Cancel") } }
        )
    }

    if (members.length() == 0) {
        EmptyState("No members found")
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(members.length()) { index ->
                val member = members.getJSONObject(index)
                val userId = member.optString("user_id", "")
                val role = member.optString("role", "")
                val email = member.optString("email", "")
                val deviceName = member.optString("device_name", "")
                val displayName = email.ifBlank { deviceName.ifBlank { "User (${userId.take(8)})" } }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Blue227ed4),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(displayName.first().uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text(role, fontSize = 12.sp, color = if (role == "owner") Blue227ed4 else TextSecondary,
                                fontWeight = if (role == "owner") FontWeight.SemiBold else FontWeight.Normal)
                        }
                        if (role != "owner") {
                            IconButton(onClick = { pendingRemoveId = userId; showRemoveDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(shopSettings: JSONObject?, onUpdate: (String, String) -> Unit) {
    if (shopSettings == null) {
        EmptyState("No settings found")
        return
    }

    var shopName by remember(shopSettings) { mutableStateOf(shopSettings.optString("shop_name", "")) }
    var shopAddress by remember(shopSettings) { mutableStateOf(shopSettings.optString("shop_address", "")) }
    var shopPhone by remember(shopSettings) { mutableStateOf(shopSettings.optString("shop_phone", "")) }
    var invoiceMessage by remember(shopSettings) { mutableStateOf(shopSettings.optString("invoice_message", "")) }
    var edited by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Shop Settings", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
        item {
            OutlinedTextField(value = shopName, onValueChange = { shopName = it; edited = true }, label = { Text("Shop Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        item {
            OutlinedTextField(value = shopAddress, onValueChange = { shopAddress = it; edited = true }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        item {
            OutlinedTextField(value = shopPhone, onValueChange = { shopPhone = it; edited = true }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        item {
            OutlinedTextField(value = invoiceMessage, onValueChange = { invoiceMessage = it; edited = true }, label = { Text("Invoice Message") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
        }
        if (edited) {
            item {
                TextButton(
                    onClick = {
                        onUpdate("shop_name", shopName)
                        onUpdate("shop_address", shopAddress)
                        onUpdate("shop_phone", shopPhone)
                        onUpdate("invoice_message", invoiceMessage)
                        edited = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes", color = Blue227ed4, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
    }
}
