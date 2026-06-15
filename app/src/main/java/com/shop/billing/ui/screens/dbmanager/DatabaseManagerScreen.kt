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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

private data class TabItem(val title: String, val icon: ImageVector, val count: Int)

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

    LaunchedEffect(Unit) { viewModel.loadAll() }

    val tabs = listOf(
        TabItem("Shops", Icons.Default.Store, shops.length()),
        TabItem("Bills", Icons.Default.Receipt, bills.length()),
        TabItem("Customers", Icons.Default.Person, customers.length()),
        TabItem("Items", Icons.Default.Inventory2, shopItems.length()),
        TabItem("Members", Icons.Default.Group, members.length()),
        TabItem("Settings", Icons.Default.Settings, 0)
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Blue227ed4)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs.size) { index ->
                    val tab = tabs[index]
                    val selected = selectedTab == index
                    val bgColor = if (selected) Blue227ed4 else Blue227ed4.copy(alpha = 0.1f)
                    val contentColor = if (selected) Color.White else Blue227ed4

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 0.dp),
                        onClick = { selectedTab = index }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(tab.icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(tab.title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = contentColor, maxLines = 1)
                            if (tab.count > 0) {
                                Spacer(Modifier.width(4.dp))
                                Text("${tab.count}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White.copy(alpha = 0.9f) else Blue227ed4)
                            }
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
private fun ShopsTab(shops: JSONArray, onDelete: (String) -> Unit, currentShopCode: String) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteCode by remember { mutableStateOf<String?>(null) }

    if (showDeleteDialog) {
        val targetCode = pendingDeleteCode ?: ""
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Shop") },
            text = {
                Column {
                    Text("Delete shop '$targetCode' and all its data?")
                    if (targetCode == currentShopCode) {
                        Spacer(Modifier.height(8.dp))
                        Text("This is your CURRENT shop!", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
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
        EmptyState("No shops found", Icons.Default.Store)
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(shops.length()) { index ->
                val shop = shops.getJSONObject(index)
                val shopCode = shop.optString("code", "")
                val createdAt = shop.optString("created_at", "")
                val isCurrent = shopCode == currentShopCode

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) Color(0xFFEFF6FF) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(if (isCurrent) 3.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(Blue227ed4.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = Blue227ed4, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(shopCode, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Blue227ed4)
                                if (isCurrent) {
                                    Spacer(Modifier.width(8.dp))
                                    Text("CURRENT", fontSize = 9.sp, color = Blue227ed4, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.background(Color(0xFFDBEAFE), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            if (createdAt.isNotBlank()) {
                                Text(createdAt, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        IconButton(onClick = { pendingDeleteCode = shopCode; showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BillsTab(bills: JSONArray, billItems: JSONArray, onDelete: (String) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Bill") },
            text = { Text("Permanently delete this bill and all its items.") },
            confirmButton = {
                TextButton(onClick = { pendingDeleteId?.let { onDelete(it) }; showDeleteDialog = false }) {
                    Text("Delete", color = Color(0xFFEF4444))
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (bills.length() == 0) {
        EmptyState("No bills found", Icons.Default.Receipt)
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(bills.length()) { index ->
                val bill = bills.getJSONObject(index)
                val billId = bill.optString("id", "")
                val billNum = bill.optString("bill_number", "")
                val customer = bill.optString("customer_name", "")
                val mobile = bill.optString("customer_mobile", "")
                val total = bill.optDouble("total_amount", 0.0)
                val paymentStatus = bill.optString("payment_status", "paid")
                val createdAtRaw = bill.optString("created_at", "")
                val createdAtMs = try {
                    createdAtRaw.toLongOrNull() ?: try {
                        val clean = createdAtRaw.replace(Regex("\\.\\d+"), "").replace(Regex("[Z]|[+-]\\d{2}:\\d{2}$"), "")
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(clean)?.time ?: 0L
                    } catch (_: Exception) { 0L }
                } catch (_: Exception) { 0L }
                val dateStr = if (createdAtMs > 0) SimpleDateFormat("dd MMM yy, hh:mm a", Locale.getDefault()).format(Date(createdAtMs)) else ""

                val itemCount = billItems.let { arr ->
                    (0 until arr.length()).count { arr.getJSONObject(it).optString("bill_id") == billId }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(
                                if (paymentStatus == "credit") Color(0xFFFEF2F2) else Color(0xFFF0FDF4)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Receipt, contentDescription = null,
                                tint = if (paymentStatus == "credit") Color(0xFFEF4444) else Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("#$billNum", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Blue227ed4)
                                if (paymentStatus == "credit") {
                                    Spacer(Modifier.width(6.dp))
                                    Text("CREDIT", fontSize = 9.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold,
                                        modifier = Modifier.background(Color(0xFFFEE2E2), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp))
                                }
                            }
                            if (customer.isNotBlank()) Text(customer, fontSize = 12.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row {
                                if (mobile.isNotBlank()) { Text(mobile, fontSize = 11.sp, color = TextSecondary); Spacer(Modifier.width(8.dp)) }
                                Text("$itemCount items", fontSize = 11.sp, color = TextSecondary)
                                if (dateStr.isNotBlank()) { Spacer(Modifier.width(8.dp)); Text(dateStr, fontSize = 11.sp, color = TextSecondary) }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${String.format("%.0f", total)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            IconButton(onClick = { pendingDeleteId = billId; showDeleteDialog = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444).copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomersTab(customers: JSONArray, bills: JSONArray, payments: JSONArray, onDelete: (String) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    val creditByMobile = remember(bills) {
        val map = mutableMapOf<String, Double>()
        for (i in 0 until bills.length()) {
            val b = bills.getJSONObject(i)
            if (b.optString("payment_status", "paid") == "credit") {
                val m = b.optString("customer_mobile", "")
                map[m] = (map[m] ?: 0.0) + b.optDouble("total_amount", 0.0)
            }
        }
        map
    }

    val paymentsByMobile = remember(payments) {
        val map = mutableMapOf<String, Double>()
        for (i in 0 until payments.length()) {
            val p = payments.getJSONObject(i)
            val m = p.optString("customer_mobile", "")
            map[m] = (map[m] ?: 0.0) + p.optDouble("amount", 0.0)
        }
        map
    }

    val pendingByMobile = remember(creditByMobile, paymentsByMobile) {
        creditByMobile.mapValues { (mobile, creditTotal) ->
            ((creditTotal - (paymentsByMobile[mobile] ?: 0.0)).coerceAtLeast(0.0))
        }
    }

    val creditByMobileMap = remember(creditByMobile, paymentsByMobile) {
        val map = mutableMapOf<String, Double>()
        for ((mobile, creditTotal) in creditByMobile) {
            val credit = ((paymentsByMobile[mobile] ?: 0.0) - creditTotal).coerceAtLeast(0.0)
            if (credit > 0) map[mobile] = credit
        }
        for ((mobile, totalPaid) in paymentsByMobile) {
            if (!creditByMobile.containsKey(mobile) && totalPaid > 0) map[mobile] = totalPaid
        }
        map
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Customer") },
            text = { Text("Permanently delete this customer?") },
            confirmButton = {
                TextButton(onClick = { pendingDeleteId?.let { onDelete(it) }; showDeleteDialog = false }) {
                    Text("Delete", color = Color(0xFFEF4444))
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (customers.length() == 0) {
        EmptyState("No customers yet", Icons.Default.Person)
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(customers.length()) { index ->
                val c = customers.getJSONObject(index)
                val name = c.optString("name", "")
                val mobile = c.optString("mobile", "")
                val totalBills = c.optInt("total_bills", 0)
                val pendingAmount = pendingByMobile[mobile] ?: 0.0
                val creditAmount = creditByMobileMap[mobile] ?: 0.0

                val (statusColor, statusText) = when {
                    pendingAmount > 0 -> Color(0xFFEF4444) to "₹${String.format("%.0f", pendingAmount)} pending"
                    creditAmount > 0 -> Color(0xFF10B981) to "+₹${String.format("%.0f", creditAmount)} Credit"
                    else -> Color(0xFF22C55E) to "₹0 Settled"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = statusColor, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name.ifBlank { "Unknown" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(mobile, fontSize = 12.sp, color = TextSecondary)
                                Spacer(Modifier.width(8.dp))
                                Text("${totalBills} bills", fontSize = 11.sp, color = TextSecondary)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                statusText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = statusColor,
                                modifier = Modifier.background(statusColor.copy(alpha = 0.08f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                            )
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
private fun ShopItemsTab(shopItems: JSONArray, onDelete: (String) -> Unit, onUpdate: (String, String, Double, String) -> Unit) {
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
            text = { Text("Delete this item permanently?") },
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
                        onUpdate(it.optString("id", ""), editName, editPrice.toDoubleOrNull() ?: 0.0, editCategory)
                    }
                    showEditDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancel") } }
        )
    }

    if (shopItems.length() == 0) {
        EmptyState("No items found", Icons.Default.Inventory2)
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(shopItems.length()) { index ->
                val item = shopItems.getJSONObject(index)
                val name = item.optString("name", "")
                val price = item.optDouble("price", 0.0)
                val category = item.optString("category", "")
                val itemId = item.optString("id", "")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (category.isNotBlank()) {
                                Text(category, fontSize = 11.sp, color = TextSecondary,
                                    modifier = Modifier.background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 1.dp))
                            }
                        }
                        Text("₹${String.format("%.2f", price)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
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
private fun MembersTab(members: JSONArray, onRemove: (String) -> Unit) {
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
        EmptyState("No members found", Icons.Default.Group)
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(members.length()) { index ->
                val member = members.getJSONObject(index)
                val userId = member.optString("user_id", "")
                val role = member.optString("role", "")
                val email = member.optString("email", "")
                val deviceName = member.optString("device_name", "")
                val displayName = email.ifBlank { deviceName.ifBlank { "User (${userId.take(8)})" } }
                val isOwner = role == "owner"
                val avatarColor = if (isOwner) Blue227ed4 else Color(0xFF8B5CF6)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOwner) Color(0xFFEFF6FF) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(avatarColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(displayName.first().uppercase(), color = avatarColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isOwner) Blue227ed4.copy(alpha = 0.1f) else Color(0xFFF1F5F9),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(role.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                        color = if (isOwner) Blue227ed4 else TextSecondary)
                                }
                                if (deviceName.isNotBlank()) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(deviceName, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        if (!isOwner) {
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
private fun SettingsTab(shopSettings: JSONObject?, onUpdate: (String, String) -> Unit) {
    if (shopSettings == null) {
        EmptyState("No settings found", Icons.Default.Settings)
        return
    }

    var shopName by remember(shopSettings) { mutableStateOf(shopSettings.optString("shop_name", "")) }
    var shopAddress by remember(shopSettings) { mutableStateOf(shopSettings.optString("shop_address", "")) }
    var shopPhone by remember(shopSettings) { mutableStateOf(shopSettings.optString("shop_phone", "")) }
    var invoiceMessage by remember(shopSettings) { mutableStateOf(shopSettings.optString("invoice_message", "")) }
    var edited by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Blue227ed4, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Shop Settings", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            }
        }
        item {
            OutlinedTextField(value = shopName, onValueChange = { shopName = it; edited = true }, label = { Text("Shop Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(10.dp))
        }
        item {
            OutlinedTextField(value = shopAddress, onValueChange = { shopAddress = it; edited = true }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(10.dp))
        }
        item {
            OutlinedTextField(value = shopPhone, onValueChange = { shopPhone = it.filter { ch -> ch.isDigit() }.take(10); edited = true }, label = { Text("Phone") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(10.dp))
        }
        item {
            OutlinedTextField(value = invoiceMessage, onValueChange = { invoiceMessage = it; edited = true }, label = { Text("Invoice Message") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, shape = RoundedCornerShape(10.dp))
        }
        if (edited) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clickable {
                        onUpdate("shop_name", shopName)
                        onUpdate("shop_address", shopAddress)
                        onUpdate("shop_phone", shopPhone)
                        onUpdate("invoice_message", invoiceMessage)
                        edited = false
                    }.background(Blue227ed4, RoundedCornerShape(10.dp)).padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String, icon: ImageVector) {
    Box(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(12.dp))
            Text(message, fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}
