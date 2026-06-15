package com.shop.billing.ui.screens.newbill

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shop.billing.ui.components.CategoryFilter
import com.shop.billing.ui.components.EmptyState
import com.shop.billing.ui.components.ItemCard
import com.shop.billing.ui.components.SearchBar
import com.shop.billing.ui.navigation.NavRoutes
import com.shop.billing.ui.theme.Blue227ed4
import com.shop.billing.ui.theme.SurfaceGray
import com.shop.billing.ui.theme.TextPrimary
import com.shop.billing.ui.theme.TextSecondary
import com.shop.billing.ui.theme.TealAccent
import com.shop.billing.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBillScreen(
    navController: NavController,
    viewModel: NewBillViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val totalAmount by viewModel.totalAmount.collectAsState()
    val filteredCustomers by viewModel.filteredCustomers.collectAsState()
    val paymentStatus by viewModel.paymentStatus.collectAsState()
    var customerName by remember { mutableStateOf("") }
    var customerMobile by remember { mutableStateOf("") }
    var showCustomerDropdown by remember { mutableStateOf(false) }

    val cartMap = remember(cartItems) {
        cartItems.associateBy { it.itemId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Bill", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
        containerColor = SurfaceGray
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp)
            ) {
                item {
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
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (items.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No items found",
                            subtitle = "Add items in the Items section first"
                        )
                    }
                } else {
                    items(items, key = { "shop_${it.id}" }) { item ->
                        val qty = cartMap[item.id]?.quantity ?: 0
                        ItemCard(
                            item = item,
                            quantity = qty,
                            onAddToCart = { viewModel.addToCart(item) },
                            onIncrease = { viewModel.increaseQuantity(item.id) },
                            onDecrease = { viewModel.decreaseQuantity(item.id) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            if (cartItems.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFE0F2FE), SurfaceGray)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Customer Details",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Box {
                                OutlinedTextField(
                                    value = customerName,
                                    onValueChange = {
                                        customerName = it
                                        viewModel.onCustomerSearchQueryChange(it)
                                        showCustomerDropdown = it.isNotBlank() && filteredCustomers.isNotEmpty()
                                    },
                                    label = { Text("Name") },
                                    placeholder = { Text("Search or type name") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color(0xFFE2E8F0),
                                        focusedBorderColor = Blue227ed4,
                                        unfocusedContainerColor = Color.White,
                                        focusedContainerColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (showCustomerDropdown && filteredCustomers.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 56.dp)
                                            .heightIn(max = 180.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                    ) {
                                        LazyColumn {
                                            items(filteredCustomers, key = { it.mobile }) { customer ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            customerName = customer.name
                                                            customerMobile = customer.mobile
                                                            showCustomerDropdown = false
                                                            viewModel.selectCustomer(customer)
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = Blue227ed4,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = customer.name.ifBlank { "Unknown" },
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = TextPrimary
                                                        )
                                                        Text(
                                                            text = customer.mobile,
                                                            fontSize = 11.sp,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customerMobile,
                                onValueChange = {
                                    customerMobile = it.filter { ch -> ch.isDigit() }.take(10)
                                    viewModel.onCustomerSearchQueryChange(it)
                                    showCustomerDropdown = it.isNotBlank() && filteredCustomers.isNotEmpty()
                                },
                                label = { Text("Mobile") },
                                placeholder = { Text("Search or type mobile") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedBorderColor = Blue227ed4,
                                    unfocusedContainerColor = Color.White,
                                    focusedContainerColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Payment Type",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = paymentStatus == "paid",
                                    onClick = { viewModel.onPaymentStatusChange("paid") },
                                    label = { Text("Paid", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color(0xFFF0F0F0),
                                        selectedContainerColor = Color(0xFF43A047),
                                        labelColor = TextSecondary,
                                        selectedLabelColor = Color.White
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = Color(0xFFE2E8F0),
                                        selectedBorderColor = Color(0xFF43A047)
                                    ),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                )
                                FilterChip(
                                    selected = paymentStatus == "credit",
                                    onClick = { viewModel.onPaymentStatusChange("credit") },
                                    label = { Text("Credit", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color(0xFFF0F0F0),
                                        selectedContainerColor = Color(0xFFE53935),
                                        labelColor = TextSecondary,
                                        selectedLabelColor = Color.White
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = Color(0xFFE2E8F0),
                                        selectedBorderColor = Color(0xFFE53935)
                                    ),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = Color(0xFFE2E8F0))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Amount",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${Constants.CURRENCY_SYMBOL} ${totalAmount.toLong()}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Blue227ed4
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    viewModel.generateBill(customerName, customerMobile) { billId ->
                                        navController.navigate(NavRoutes.BillDetail.createRoute(billId))
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Blue227ed4),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Generate Bill & PDF",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
