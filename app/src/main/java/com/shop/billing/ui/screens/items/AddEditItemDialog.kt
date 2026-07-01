package com.shop.billing.ui.screens.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.shop.billing.data.model.ShopItem
import com.shop.billing.ui.theme.Blue227ed4

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemDialog(
    existingItem: ShopItem? = null,
    existingCategories: List<String> = emptyList(),
    isOwner: Boolean = true,
    barcode: String = "",
    onDismiss: () -> Unit,
    onSave: (String, Double, String, Int, Int, String) -> Unit
) {
    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var price by remember { mutableStateOf(if (existingItem != null) existingItem.price.toString() else "") }
    var category by remember { mutableStateOf(existingItem?.category ?: "") }
    var stockQuantity by remember { mutableStateOf(if (existingItem != null) existingItem.stockQuantity.toString() else "0") }
    var lowStockThreshold by remember { mutableStateOf(if (existingItem != null) existingItem.lowStockThreshold.toString() else "10") }
    var error by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }

    val uniqueCategories = remember(existingCategories) {
        existingCategories.distinct().filter { it.isNotBlank() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                if (existingItem != null) "Edit Item" else "Add Item",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = false },
                    label = { Text("Item Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedBorderColor = Blue227ed4,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    isError = error
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it; error = false },
                    label = { Text("Price") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedBorderColor = Blue227ed4,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    isError = error
                )
                if (error) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Please enter a valid name and price",
                        color = Color(0xFFE53935),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (barcode.isNotBlank()) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = {},
                        label = { Text("Barcode") },
                        readOnly = true,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = Blue227ed4,
                            unfocusedContainerColor = Color(0xFFF9FAFB),
                            focusedContainerColor = Color(0xFFF9FAFB)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "Category",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B7280)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (uniqueCategories.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uniqueCategories.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 13.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFFF3F4F6),
                                    selectedContainerColor = Blue227ed4,
                                    labelColor = Color(0xFF374151),
                                    selectedLabelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Color(0xFFE5E7EB),
                                    selectedBorderColor = Blue227ed4
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (isOwner) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = Blue227ed4,
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmed = newCategoryInput.trim()
                            if (trimmed.isNotBlank() && !uniqueCategories.contains(trimmed)) {
                                category = trimmed
                                newCategoryInput = ""
                            } else if (trimmed.isNotBlank() && uniqueCategories.contains(trimmed)) {
                                category = trimmed
                                newCategoryInput = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue227ed4,
                            disabledContainerColor = Color(0xFFD1D5DB)
                        ),
                        enabled = newCategoryInput.trim().isNotBlank(),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Text("Add", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = stockQuantity,
                    onValueChange = { stockQuantity = it.filter { c -> c.isDigit() } },
                    label = { Text("Stock Qty") },
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
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = lowStockThreshold,
                    onValueChange = { lowStockThreshold = it.filter { c -> c.isDigit() } },
                    label = { Text("Low Stock Alert At") },
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priceValue = price.toDoubleOrNull()
                    val stockQtyValue = stockQuantity.toIntOrNull() ?: 0
                    val thresholdValue = lowStockThreshold.toIntOrNull()?.coerceAtLeast(1) ?: 10
                    if (name.isBlank() || priceValue == null || priceValue <= 0) {
                        error = true
                    } else {
                        onSave(name.trim(), priceValue, category.trim(), stockQtyValue, thresholdValue, barcode)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Blue227ed4),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF6B7280))
            }
        }
    )
}
