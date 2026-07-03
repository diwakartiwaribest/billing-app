package com.shop.billing.ui.screens.investment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.billing.ui.theme.Blue227ed4
import com.shop.billing.util.Constants

@Composable
fun ProductPickerDialog(
    products: List<ResolvedProduct>,
    onSelect: (ResolvedProduct) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(products, searchQuery) {
        if (searchQuery.isBlank()) products
        else products.filter {
            it.product.name.contains(searchQuery, ignoreCase = true) ||
            it.product.barcode.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Select Product", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search name or barcode") },
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
                Spacer(modifier = Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Text(
                        "No products found.",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.heightIn(max = 360.dp)
                    ) {
                        items(filtered, key = { it.product.id }) { resolved ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(resolved) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = resolved.product.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF111827)
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Sell: ${Constants.CURRENCY_SYMBOL}${resolved.product.sellingPrice.toLong()}",
                                                fontSize = 12.sp,
                                                color = Color(0xFF22C55E),
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (resolved.product.buyingPrice > 0) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Buy: ${Constants.CURRENCY_SYMBOL}${resolved.product.buyingPrice.toLong()}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF6B7280)
                                                )
                                            }
                                            if (resolved.lastPurchasePrice > 0) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Last buy: ${Constants.CURRENCY_SYMBOL}${resolved.lastPurchasePrice.toLong()}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFEAB308)
                                                )
                                            }
                                            if (resolved.product.barcode.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = resolved.product.barcode,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF9CA3AF)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Select",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Blue227ed4
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF6B7280))
            }
        }
    )
}
