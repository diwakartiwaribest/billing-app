package com.shop.billing.ui.screens.recyclebin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
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
import com.shop.billing.util.Constants
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Red = Color(0xFFE53935)
private val RedLight = Color(0xFFFFEBEE)
private val RedDeep = Color(0xFFC62828)
private val Green = Color(0xFF22C55E)
private val GreenLight = Color(0xFFF0FDF4)
private val Blue = Color(0xFF3B82F6)
private val BlueLight = Color(0xFFDBEAFE)
private val Amber = Color(0xFFF59E0B)
private val AmberLight = Color(0xFFFFFBEB)
private val Purple = Color(0xFF8B5CF6)
private val PurpleLight = Color(0xFFF5F3FF)

private data class TabStyle(val label: String, val icon: ImageVector, val accent: Color, val accentBg: Color)

private val tabs = listOf(
    TabStyle("Bills", Icons.Default.Receipt, Red, RedLight),
    TabStyle("Items", Icons.Default.Inventory2, Blue, BlueLight),
    TabStyle("Customers", Icons.Default.Person, Green, GreenLight),
    TabStyle("Payments", Icons.Default.Payments, Amber, AmberLight)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    navController: NavController,
    viewModel: RecycleBinViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var tabIndex by remember { mutableIntStateOf(0) }
    val snackbar = state.lastRestored
    val tabCounts = listOf(state.bills.size, state.products.size, state.customers.size, state.payments.size)
    val totalDeleted = tabCounts.sum()

    Scaffold(
        topBar = {
            if (state.selectionMode) {
                TopAppBar(
                    title = {
                        Column {
                            Text("${state.selectedIds.size} selected", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
                            Text("Tap to toggle, long-press for more", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f), fontWeight = FontWeight.Normal)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    actions = {
                        if (tabCounts[tabIndex] > 0) {
                            TextButton(onClick = { viewModel.selectAllInCurrentTab(tabIndex) }) {
                                Text("Select All", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.width(4.dp))
                            if (state.selectedIds.isNotEmpty()) {
                                val count = state.selectedIds.size
                                Button(
                                    onClick = {
                                        val ids = state.selectedIds.toList()
                                        when (tabIndex) {
                                            0 -> viewModel.restoreBills(ids)
                                            1 -> viewModel.restoreProducts(ids)
                                            2 -> viewModel.restoreCustomers(ids)
                                            3 -> viewModel.restorePayments(ids)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(16.dp), tint = RedDeep)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Restore $count", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RedDeep)
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = RedDeep, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RestoreFromTrash, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Recycle Bin", fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = RedDeep, titleContentColor = MaterialTheme.colorScheme.onPrimary)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = tabs[tabIndex].accent,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                        color = tabs[tabIndex].accent
                    )
                }
            ) {
                tabs.forEachIndexed { i, tab ->
                    RecycleTab(tab.label, tabCounts[i], tabIndex == i, tab.accent) { tabIndex = i; viewModel.clearSelection() }
                }
            }

            if (totalDeleted == 0) {
                EmptyRecycleBin(tabIndex)
            } else {
                AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
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
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(28.dp)).background(GreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Green, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Restored", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(snackbar, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            DialogConfirmButton(text = "OK", onClick = { viewModel.clearLastRestored() })
        }
    }

    if (state.error != null) {
        DialogOverlay(onDismiss = { viewModel.clearError() }) {
            Text("Restore failed", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            Text(state.error!!, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            DialogConfirmButton(text = "OK", onClick = { viewModel.clearError() })
        }
    }
}

@Composable
private fun RecycleTab(label: String, count: Int, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Tab(
        selected = selected,
        onClick = onClick,
        selectedContentColor = accent,
        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(if (selected) accent else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text("$count", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}

@Composable
private fun EmptyRecycleBin(tabIndex: Int) {
    val tab = tabs[tabIndex]
    val messages = listOf(
        "No deleted bills will stay here\nuntil you clear them from records.",
        "No deleted items will stay here\nuntil you clear them from your inventory.",
        "No deleted customers will stay here\nuntil you remove them from your contacts.",
        "No deleted payments will stay here\nuntil you clear them from the payments log."
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(tab.accent.copy(alpha = 0.12f), tab.accentBg.copy(alpha = 0.4f)),
                        radius = 80f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(tab.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(tab.icon, contentDescription = null, tint = tab.accent, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Nothing deleted", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        Text(
            messages[tabIndex],
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

private enum class DateBucket(val label: String) {
    TODAY("Today"), YESTERDAY("Yesterday"), THIS_WEEK("This Week"), OLDER("Older")
}

private fun getBucket(ts: Long): DateBucket {
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { time = Date(ts) }
    val sameYear = now.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
    val dayDiff = now.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR)
    return when {
        sameYear && dayDiff == 0 -> DateBucket.TODAY
        sameYear && dayDiff == 1 -> DateBucket.YESTERDAY
        sameYear && dayDiff in 2..6 -> DateBucket.THIS_WEEK
        else -> DateBucket.OLDER
    }
}

private fun <T> groupByBucket(items: List<T>, ts: (T) -> Long): List<Pair<DateBucket, List<T>>> {
    return items.groupBy { getBucket(ts(it)) }
        .toList()
        .sortedBy { (bucket, _) -> bucket.ordinal }
}

private fun formatDate(ts: Long): String {
    val date = Date(ts)
    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
    val bucket = getBucket(ts)
    return when (bucket) {
        DateBucket.TODAY -> "Today, $timeStr"
        DateBucket.YESTERDAY -> "Yesterday, $timeStr"
        DateBucket.THIS_WEEK -> SimpleDateFormat("EEEE", Locale.getDefault()).format(date)
        DateBucket.OLDER -> df.format(date)
    }
}

private fun formatAmount(amount: Double): String = "${Constants.CURRENCY_SYMBOL}${amount.toLong()}"

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BillItems(
    bills: List<Bill>, selectedIds: Set<String>, selectionMode: Boolean, canManage: Boolean,
    onEnterSelection: (String) -> Unit, onToggle: (String) -> Unit, onRestore: (String) -> Unit
) {
    if (bills.isEmpty()) return
    val grouped = remember(bills) { groupByBucket(bills) { it.updatedAt } }
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        grouped.forEach { (bucket, items) ->
            stickyHeader(bucket = bucket, count = items.size)
            items(items, key = { it.id }) { b ->
                val isSelected = b.id in selectedIds
                RecycleCard(
                    modifier = Modifier.animateItemPlacement(),
                    accentColor = Red,
                    isSelected = isSelected, selectionMode = selectionMode,
                    icon = Icons.Default.Receipt,
                    title = if (b.billNumber.isNotBlank()) "Bill #${b.billNumber}" else "Bill",
                    subtitle = b.customerName.ifBlank { "—" },
                    detail = "${formatAmount(b.totalAmount)} \u2022 ${b.paymentStatus}",
                    metaTime = b.updatedAt,
                    onLongClick = { if (canManage) onEnterSelection(b.id) },
                    onClick = { if (selectionMode) onToggle(b.id) },
                    action = { if (canManage && !selectionMode) RowAction("Restore", Icons.Default.RestoreFromTrash, onClick = { onRestore(b.id) }) }
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductItems(
    items: List<ShopItem>, selectedIds: Set<String>, selectionMode: Boolean, canManage: Boolean,
    onEnterSelection: (String) -> Unit, onToggle: (String) -> Unit, onRestore: (String) -> Unit
) {
    if (items.isEmpty()) return
    val grouped = remember(items) { groupByBucket(items) { it.updatedAt } }
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        grouped.forEach { (bucket, bucketItems) ->
            stickyHeader(bucket = bucket, count = bucketItems.size)
            items(bucketItems, key = { it.id }) { i ->
                val isSelected = i.id in selectedIds
                RecycleCard(
                    modifier = Modifier.animateItemPlacement(),
                    accentColor = Blue,
                    isSelected = isSelected, selectionMode = selectionMode,
                    icon = Icons.Default.Inventory2,
                    title = i.name,
                    subtitle = i.category,
                    detail = formatAmount(i.sellingPrice),
                    metaTime = i.updatedAt,
                    onLongClick = { if (canManage) onEnterSelection(i.id) },
                    onClick = { if (selectionMode) onToggle(i.id) },
                    action = { if (canManage && !selectionMode) RowAction("Restore", Icons.Default.RestoreFromTrash, onClick = { onRestore(i.id) }) }
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomerItems(
    items: List<Customer>, selectedIds: Set<String>, selectionMode: Boolean, canManage: Boolean,
    onEnterSelection: (String) -> Unit, onToggle: (String) -> Unit, onRestore: (String) -> Unit
) {
    if (items.isEmpty()) return
    val grouped = remember(items) { groupByBucket(items) { it.updatedAt } }
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        grouped.forEach { (bucket, bucketItems) ->
            stickyHeader(bucket = bucket, count = bucketItems.size)
            items(bucketItems, key = { it.mobile }) { c ->
                val isSelected = c.mobile in selectedIds
                RecycleCard(
                    modifier = Modifier.animateItemPlacement(),
                    accentColor = Green,
                    isSelected = isSelected, selectionMode = selectionMode,
                    icon = Icons.Default.Person,
                    title = c.name.ifBlank { c.mobile },
                    subtitle = c.mobile,
                    detail = "${formatAmount(c.totalSpent)} spent",
                    metaTime = c.updatedAt,
                    onLongClick = { if (canManage) onEnterSelection(c.mobile) },
                    onClick = { if (selectionMode) onToggle(c.mobile) },
                    action = { if (canManage && !selectionMode) RowAction("Restore", Icons.Default.RestoreFromTrash, onClick = { onRestore(c.mobile) }) }
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PaymentItems(
    items: List<CustomerPayment>, selectedIds: Set<String>, selectionMode: Boolean, canManage: Boolean,
    onEnterSelection: (String) -> Unit, onToggle: (String) -> Unit, onRestore: (String) -> Unit
) {
    if (items.isEmpty()) return
    val grouped = remember(items) { groupByBucket(items) { it.updatedAt } }
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        grouped.forEach { (bucket, bucketItems) ->
            stickyHeader(bucket = bucket, count = bucketItems.size)
            items(bucketItems, key = { it.uuid }) { p ->
                val isSelected = p.uuid in selectedIds
                RecycleCard(
                    modifier = Modifier.animateItemPlacement(),
                    accentColor = Amber,
                    isSelected = isSelected, selectionMode = selectionMode,
                    icon = Icons.Default.Payments,
                    title = "Payment ${formatAmount(p.amount)}",
                    subtitle = p.customerMobile,
                    detail = if (p.note.isBlank()) "—" else p.note,
                    metaTime = p.updatedAt,
                    onLongClick = { if (canManage) onEnterSelection(p.uuid) },
                    onClick = { if (selectionMode) onToggle(p.uuid) },
                    action = { if (canManage && !selectionMode) RowAction("Restore", Icons.Default.RestoreFromTrash, onClick = { onRestore(p.uuid) }) }
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.stickyHeader(bucket: DateBucket, count: Int) {
    stickyHeader {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(top = 12.dp, bottom = 6.dp, start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 14.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(8.dp))
            Text(bucket.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text("$count", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecycleCard(
    modifier: Modifier = Modifier,
    accentColor: Color,
    icon: ImageVector,
    title: String,
    subtitle: String,
    detail: String,
    metaTime: Long,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {},
    action: @Composable () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .then(if (isSelected) Modifier.border(2.dp, accentColor, RoundedCornerShape(16.dp)) else Modifier)
            .clickable { if (selectionMode) onClick() else onLongClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(colors = listOf(accentColor, accentColor.copy(alpha = 0.5f))),
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(icon = icon, accentColor = accentColor)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(formatDate(metaTime), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (detail.isNotBlank() && selectionMode) {
                            Spacer(Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(detail, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = accentColor, maxLines = 1)
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.widthIn(min = 72.dp)
                ) {
                    if (!selectionMode && detail.isNotBlank()) {
                        Text(detail, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accentColor, maxLines = 1)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (selectionMode) {
                        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onClick() },
                                colors = CheckboxDefaults.colors(checkedColor = accentColor, uncheckedColor = accentColor.copy(alpha = 0.4f))
                            )
                        }
                    } else {
                        action()
                    }
                }
            }
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector, accentColor: Color) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(accentColor, accentColor.copy(alpha = 0.65f)),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun RowAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
