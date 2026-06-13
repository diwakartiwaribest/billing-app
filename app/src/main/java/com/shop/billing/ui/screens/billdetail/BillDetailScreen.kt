package com.shop.billing.ui.screens.billdetail

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shop.billing.data.model.BillItem
import com.shop.billing.ui.theme.Blue227ed4
import com.shop.billing.ui.theme.SurfaceGray
import com.shop.billing.ui.theme.TextPrimary
import com.shop.billing.ui.theme.TextSecondary
import com.shop.billing.util.Constants
import com.shop.billing.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailScreen(
    billId: String,
    navController: NavController,
    viewModel: BillDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    var shopName by remember { mutableStateOf(Constants.DEFAULT_SHOP_NAME) }
    var shopAddress by remember { mutableStateOf(Constants.DEFAULT_SHOP_ADDRESS) }
    var shopPhone by remember { mutableStateOf(Constants.DEFAULT_SHOP_PHONE) }

    LaunchedEffect(billId) {
        viewModel.loadBill(billId)
        val (name, address, phone) = viewModel.getShopSettings()
        shopName = name
        shopAddress = address
        shopPhone = phone
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice Details", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = SurfaceGray
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...", color = TextSecondary)
            }
        } else if (state.error != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: ${state.error}", textAlign = TextAlign.Center, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }, shape = RoundedCornerShape(10.dp)) {
                        Text("Go Back")
                    }
                }
            }
        } else if (state.bill == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Bill not found", textAlign = TextAlign.Center, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }, shape = RoundedCornerShape(10.dp)) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            val bill = state.bill!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${bill.billNumber}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Blue227ed4
                                )
                                Text(
                                    text = if (bill.paymentStatus == "credit") "CREDIT" else "PAID",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .background(
                                            if (bill.paymentStatus == "credit") Color(0xFFE53935) else Color(0xFF43A047),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = DateUtils.formatDateTime(bill.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            if (bill.customerName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = bill.customerName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                            if (bill.customerMobile.isNotBlank()) {
                                Text(
                                    text = bill.customerMobile,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            if (bill.createdBy.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Generated by ${bill.createdBy}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Text("#", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.SemiBold, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                Text("Item", modifier = Modifier.weight(2f), fontWeight = FontWeight.SemiBold, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                Text("Qty", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.SemiBold, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                Text("Rate", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = TextSecondary, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                Text("Amt", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = TextSecondary, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                            }
                            Divider(color = Color(0xFFE5E7EB))
                            state.items.forEachIndexed { index, item ->
                                BillItemRow(index = index, item = item)
                            }
                            Divider(color = Color(0xFFE5E7EB))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "Total: ${Constants.CURRENCY_SYMBOL} ${bill.totalAmount.toLong()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Blue227ed4
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            viewModel.generatePdf(shopName, shopAddress, shopPhone)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue227ed4),
                        enabled = !isGenerating
                    ) {
                        if (isGenerating) {
                            Text("Generating PDF...")
                        } else {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Share PDF Invoice", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun BillItemRow(
    index: Int,
    item: BillItem
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text("${index + 1}", modifier = Modifier.weight(0.5f), color = TextPrimary)
        Text(item.itemName, modifier = Modifier.weight(2f), color = TextPrimary)
        Text("${item.quantity}", modifier = Modifier.weight(0.5f), color = TextPrimary)
        Text(
            text = "${Constants.CURRENCY_SYMBOL} ${item.unitPrice.toLong()}",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            color = TextPrimary
        )
        Text(
            text = "${Constants.CURRENCY_SYMBOL} ${item.subtotal.toLong()}",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}
