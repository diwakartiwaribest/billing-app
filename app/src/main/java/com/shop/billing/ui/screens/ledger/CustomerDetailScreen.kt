package com.shop.billing.ui.screens.ledger

import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.Customer
import com.shop.billing.data.model.CustomerPayment
import com.shop.billing.ui.navigation.NavRoutes
import com.shop.billing.ui.theme.Blue227ed4
import com.shop.billing.ui.theme.SurfaceGray
import com.shop.billing.ui.theme.TextPrimary
import com.shop.billing.ui.theme.TextSecondary
import com.shop.billing.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    mobile: String,
    navController: NavController,
    viewModel: CustomerLedgerViewModel = hiltViewModel((LocalContext.current as ComponentActivity))
) {
    val customers by viewModel.customers.collectAsState()
    val customer = customers.find { it.mobile == mobile }
        ?: Customer(name = "Unknown", mobile = mobile)

    val bills by viewModel.getBillsForCustomer(mobile).collectAsState(initial = emptyList())
    val payments by viewModel.getPaymentsForCustomer(mobile).collectAsState(initial = emptyList())

    val creditBills = bills.filter { it.paymentStatus == "credit" }
    val totalBills = creditBills.sumOf { it.totalAmount }
    val totalPaid by viewModel.getTotalPaidForCustomer(mobile).collectAsState(initial = 0.0)
    val pending = (totalBills - totalPaid).coerceAtLeast(0.0)
    val credit = (totalPaid - totalBills).coerceAtLeast(0.0)

    val userRole by viewModel.userRole.collectAsState()
    val canManage = userRole == "owner" || userRole == "admin"
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val pendingDeletedPayment by viewModel.pendingDeletedPayment.collectAsState()

    LaunchedEffect(pendingDeletedPayment) {
        pendingDeletedPayment?.let {
            val result = snackbarHostState.showSnackbar(
                message = "Payment deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDeletePayment()
            } else {
                viewModel.confirmDeletePayment()
            }
        }
    }

    val allTransactions = remember(bills, payments) {
        val billItems = bills.filter { it.paymentStatus == "credit" }.map { TransactionItem.Bill(it) }
        val paymentItems = payments.map { TransactionItem.Payment(it) }
        (billItems + paymentItems).sortedByDescending { it.transactionTime }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(customer.name.ifBlank { "Customer" }, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Blue227ed4,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPaymentDialog = true },
                containerColor = Color(0xFF43A047)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Payment", tint = Color.White)
            }
        },
        containerColor = SurfaceGray
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Blue227ed4),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = customer.name.firstOrNull()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = customer.name.ifBlank { "Unknown" },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = customer.mobile,
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SummaryItem("Total Bills", "${Constants.CURRENCY_SYMBOL}${totalBills.toLong()}", Blue227ed4)
                            SummaryItem("Paid", "${Constants.CURRENCY_SYMBOL}${totalPaid.toLong()}", Color(0xFF43A047))
                            SummaryItem("Pending", "${Constants.CURRENCY_SYMBOL}${pending.toLong()}", Color(0xFFE53935))
                            SummaryItem("Credit", "${Constants.CURRENCY_SYMBOL}${credit.toLong()}", Color(0xFF1565C0))
                        }
                        if (pending > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${Constants.CURRENCY_SYMBOL}${pending.toLong()} pending",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE53935),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.generateAndSharePendingInvoice(mobile) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFF0E6),
                        contentColor = Color(0xFFE65100)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Pending Invoice", fontSize = 16.sp)
                }
            }

            item {
                if (canManage) {
                    Button(
                        onClick = {
                            android.util.Log.d("CustomerDetail", "Clear button clicked")
                            showClearHistoryDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFEE2E2),
                            contentColor = Color(0xFFE53935)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Payment History", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${bills.size} bills, ${payments.size} payments",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            if (allTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No transactions yet", fontSize = 14.sp, color = TextSecondary)
                    }
                }
            } else {
                items(allTransactions, key = {
                    when (it) {
                        is TransactionItem.Bill -> "bill_${it.bill.id}"
                        is TransactionItem.Payment -> "pay_${it.payment.uuid}"
                    }
                }) { transaction ->
                    when (transaction) {
                        is TransactionItem.Bill -> BillTransactionCard(
                            bill = transaction.bill,
                            onClick = {
                                navController.navigate(NavRoutes.BillDetail.createRoute(transaction.bill.id))
                            }
                        )
                        is TransactionItem.Payment -> PaymentTransactionCard(
                            payment = transaction.payment,
                            onDelete = {
                                viewModel.deletePayment(transaction.payment, mobile)
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddPaymentDialog) {
        AddPaymentDialog(
            customerName = customer.name,
            onDismiss = { showAddPaymentDialog = false },
            onConfirm = { amount, note ->
                viewModel.addPayment(mobile, amount, note) {
                    showAddPaymentDialog = false
                }
            }
        )
    }

    if (showClearHistoryDialog) {
        ClearPaymentHistoryDialog(
            customerName = customer.name.ifBlank { "Unknown" },
            paymentCount = payments.size,
            billCount = bills.size,
            totalPaid = totalPaid,
            onDismiss = {
                android.util.Log.d("CustomerDetail", "Dialog dismissed")
                showClearHistoryDialog = false
            },
            onConfirm = {
                android.util.Log.d("CustomerDetail", "Dialog confirmed, clearing for: $mobile")
                viewModel.clearPaymentHistory(mobile)
                showClearHistoryDialog = false
            }
        )
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun BillTransactionCard(
    bill: Bill,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(listOf(Blue227ed4, Color(0xFF0EA5E9)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Receipt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Bill #${bill.billNumber}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (bill.paymentStatus == "credit") "CREDIT" else if (bill.paymentStatus == "invoice") "INVOICE" else "PAID",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(
                                if (bill.paymentStatus == "credit") Color(0xFFE53935)
                                else if (bill.paymentStatus == "invoice") Color(0xFFF57C00)
                                else Color(0xFF43A047),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = formatDate(bill.createdAt),
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            Text(
                text = "+${Constants.CURRENCY_SYMBOL}${bill.totalAmount.toLong()}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Blue227ed4
            )
        }
    }
}

@Composable
private fun PaymentTransactionCard(
    payment: CustomerPayment,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF43A047), Color(0xFF66BB6A)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (payment.note.isNotBlank()) payment.note else "Payment",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = formatDate(payment.createdAt),
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            Text(
                text = "-${Constants.CURRENCY_SYMBOL}${payment.amount.toLong()}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF43A047)
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete payment",
                    tint = Color(0xFFBDBDBD),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AddPaymentDialog(
    customerName: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Payment from $customerName",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Amount") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue227ed4,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("e.g. Cash, UPI, etc.") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue227ed4,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onConfirm(amt, note)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
            ) {
                Text("Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
    return sdf.format(millis)
}

@Composable
private fun ClearPaymentHistoryDialog(
    customerName: String,
    paymentCount: Int,
    billCount: Int,
    totalPaid: Double,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
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
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "This will permanently delete all records for",
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
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        WarningRow("\uD83D\uDCCB $paymentCount payment record(s) will be deleted")
                        Spacer(modifier = Modifier.height(6.dp))
                        WarningRow("\uD83D\uDCCB $billCount bill(s) will be deleted")
                        Spacer(modifier = Modifier.height(6.dp))
                        WarningRow("\uD83D\uDCB0 \u20B9${totalPaid.toLong()} paid amount will be lost")
                        Spacer(modifier = Modifier.height(6.dp))
                        WarningRow("\u26A0\uFE0F Pending amount will be recalculated")
                    }
                }
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
                shape = RoundedCornerShape(10.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(listOf(Color(0xFFE5E7EB), Color(0xFFE5E7EB)))
                )
            ) {
                Text("Cancel", color = Color(0xFF6B7280), fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        }
    )
}

@Composable
private fun WarningRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53935))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 13.sp, color = Color(0xFF374151))
    }
}

sealed class TransactionItem {
    abstract val transactionTime: Long
    data class Bill(val bill: com.shop.billing.data.model.Bill) : TransactionItem() {
        override val transactionTime get() = bill.createdAt
    }
    data class Payment(val payment: CustomerPayment) : TransactionItem() {
        override val transactionTime get() = payment.createdAt + 1L
    }
}
