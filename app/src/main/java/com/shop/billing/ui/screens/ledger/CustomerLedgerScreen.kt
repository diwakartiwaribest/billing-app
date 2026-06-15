package com.shop.billing.ui.screens.ledger

import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shop.billing.data.model.Customer
import com.shop.billing.ui.navigation.NavRoutes
import com.shop.billing.ui.theme.Blue227ed4
import com.shop.billing.ui.theme.SurfaceGray
import com.shop.billing.ui.theme.TextPrimary
import com.shop.billing.ui.theme.TextSecondary
import com.shop.billing.util.Constants

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomerLedgerScreen(
    navController: NavController,
    viewModel: CustomerLedgerViewModel = hiltViewModel((LocalContext.current as ComponentActivity))
) {
    val customers by viewModel.customers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val totalPending by viewModel.totalPending.collectAsState()
    val totalPaid by viewModel.totalPaid.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }
    var customerToClear by remember { mutableStateOf<Customer?>(null) }

    var isMultiSelect by remember { mutableStateOf(false) }
    var selectedMobiles by remember { mutableStateOf(setOf<String>()) }
    var showMultiClearDialog by remember { mutableStateOf(false) }

    var showAddCustomer by remember { mutableStateOf(false) }
    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerMobile by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            if (isMultiSelect) {
                TopAppBar(
                    title = { Text("${selectedMobiles.size} selected", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = {
                            isMultiSelect = false
                            selectedMobiles = emptySet()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            selectedMobiles = customers.map { it.mobile }.toSet()
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = Color.White)
                        }
                        IconButton(onClick = {
                            selectedMobiles = emptySet()
                        }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Deselect All", tint = Color.White)
                        }
                        if (selectedMobiles.isNotEmpty()) {
                            IconButton(onClick = { showMultiClearDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear Selected", tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFE53935),
                        titleContentColor = Color.White
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("Customer Ledger", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            newCustomerName = ""
                            newCustomerMobile = ""
                            showAddCustomer = true
                        }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.syncPayments() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Blue227ed4,
                        titleContentColor = Color.White
                    )
                )
            }
        },
        containerColor = SurfaceGray,
        floatingActionButton = {
            if (!isMultiSelect && customers.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        isMultiSelect = true
                        selectedMobiles = emptySet()
                    },
                    containerColor = Blue227ed4,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Multi Select")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${Constants.CURRENCY_SYMBOL}${totalPending.toLong()}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                        Text(text = "Pending", fontSize = 12.sp, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${Constants.CURRENCY_SYMBOL}${totalPaid.toLong()}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF43A047)
                        )
                        Text(text = "Paid", fontSize = 12.sp, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${customers.size}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue227ed4
                        )
                        Text(text = "Customers", fontSize = 12.sp, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${Constants.CURRENCY_SYMBOL}${customers.sumOf { it.creditAmount }.toLong()}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0)
                        )
                        Text(text = "Credit", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Search by name or mobile...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = Blue227ed4,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (customers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFFBDBDBD),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No customers yet", fontSize = 16.sp, color = TextSecondary)
                        Text("Customers appear here when bills are created", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(customers, key = { it.mobile }) { customer ->
                        CustomerCard(
                            customer = customer,
                            isSelected = customer.mobile in selectedMobiles,
                            isMultiSelect = isMultiSelect,
                            onClick = {
                                if (isMultiSelect) {
                                    selectedMobiles = if (customer.mobile in selectedMobiles) {
                                        selectedMobiles - customer.mobile
                                    } else {
                                        selectedMobiles + customer.mobile
                                    }
                                    if (selectedMobiles.isEmpty()) {
                                        isMultiSelect = false
                                    }
                                } else {
                                    navController.navigate(NavRoutes.CustomerDetail.createRoute(customer.mobile))
                                }
                            },
                            onLongClick = {
                                if (!isMultiSelect) {
                                    isMultiSelect = true
                                    selectedMobiles = setOf(customer.mobile)
                                }
                            },
                            onClear = {
                                customerToClear = customer
                                showClearDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog && customerToClear != null) {
        ClearCustomerDialog(
            customerName = customerToClear!!.name.ifBlank { "Unknown" },
            onConfirm = {
                viewModel.clearPaymentHistory(customerToClear!!.mobile)
                showClearDialog = false
                customerToClear = null
            },
            onDismiss = {
                showClearDialog = false
                customerToClear = null
            }
        )
    }

    if (showMultiClearDialog) {
        ClearCustomerDialog(
            customerName = "${selectedMobiles.size} customer(s)",
            onConfirm = {
                for (mobile in selectedMobiles) {
                    viewModel.clearPaymentHistory(mobile)
                }
                showMultiClearDialog = false
                isMultiSelect = false
                selectedMobiles = emptySet()
            },
            onDismiss = {
                showMultiClearDialog = false
            }
        )
    }

    if (showAddCustomer) {
        AlertDialog(
            onDismissRequest = { showAddCustomer = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEF2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Blue227ed4,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    "Add Customer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1F2937),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCustomerName,
                        onValueChange = { newCustomerName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue227ed4,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newCustomerMobile,
                        onValueChange = { newCustomerMobile = it.filter { ch -> ch.isDigit() }.take(10) },
                        label = { Text("Mobile") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue227ed4,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCustomerName.isNotBlank() && newCustomerMobile.isNotBlank()) {
                            viewModel.addCustomer(newCustomerName.trim(), newCustomerMobile.trim())
                            showAddCustomer = false
                        }
                    },
                    enabled = newCustomerName.isNotBlank() && newCustomerMobile.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue227ed4),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) { Text("Add", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddCustomer = false },
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Cancel", color = Color(0xFF6B7280), fontWeight = FontWeight.Medium, fontSize = 14.sp) }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomerCard(
    customer: Customer,
    isSelected: Boolean = false,
    isMultiSelect: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClear: () -> Unit
) {
    val pending = customer.pendingAmount
    val credit = customer.creditAmount
    val hasPending = pending > 0
    val hasCredit = credit > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFEEF2FF) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelect) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Blue227ed4,
                        uncheckedColor = Color(0xFFBDBDBD)
                    ),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasPending) Brush.linearGradient(listOf(Color(0xFFE53935), Color(0xFFEF5350)))
                        else Brush.linearGradient(listOf(Color(0xFF43A047), Color(0xFF66BB6A)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = customer.name.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name.ifBlank { "Unknown" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = customer.mobile,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            if (!isMultiSelect) {
                Column(horizontalAlignment = Alignment.End) {
                    if (hasPending) {
                        Text(
                            text = "${Constants.CURRENCY_SYMBOL}${pending.toLong()}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                        Text(
                            text = "Pending",
                            fontSize = 11.sp,
                            color = Color(0xFFE53935)
                        )
                    } else if (hasCredit) {
                        Text(
                            text = "+${Constants.CURRENCY_SYMBOL}${credit.toLong()}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0)
                        )
                        Text(
                            text = "Credit",
                            fontSize = 11.sp,
                            color = Color(0xFF1565C0)
                        )
                    } else {
                        Text(
                            text = "${Constants.CURRENCY_SYMBOL}0",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF43A047)
                        )
                        Text(
                            text = "Settled",
                            fontSize = 11.sp,
                            color = Color(0xFF43A047)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Clear",
                        tint = Color(0xFFBDBDBD),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    if (hasPending) {
                        Text(
                            text = "${Constants.CURRENCY_SYMBOL}${pending.toLong()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                    } else if (hasCredit) {
                        Text(
                            text = "+${Constants.CURRENCY_SYMBOL}${credit.toLong()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0)
                        )
                    } else {
                        Text(
                            text = "${Constants.CURRENCY_SYMBOL}0",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF43A047)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClearCustomerDialog(
    customerName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                "Clear Payment History",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1F2937),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Delete ALL payment records and bills for",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = customerName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "This action cannot be undone",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE53935)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel", color = Color(0xFF6B7280), fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        }
    )
}
