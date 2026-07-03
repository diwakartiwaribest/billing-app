package com.shop.billing.ui.screens.investment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.billing.ui.theme.Blue227ed4

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddProductForPurchaseDialog(
    barcode: String,
    existingCategories: List<String>,
    initialSellingPrice: Double,
    onSave: (name: String, buyingPrice: Double, sellingPrice: Double, category: String, quantity: Int) -> Unit,
    onDismiss: (barcode: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var buyingPriceText by remember { mutableStateOf("") }
    var sellingPriceText by remember { mutableStateOf(initialSellingPrice.toLong().toString()) }
    var category by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val uniqueCategories = remember(existingCategories) {
        existingCategories.distinct().filter { it.isNotBlank() }
    }

    AlertDialog(
        onDismissRequest = { onDismiss(barcode) },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    "New Product",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (barcode.isNotBlank()) {
                    Text(
                        "Barcode: $barcode",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Item Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Category",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B7280)
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (uniqueCategories.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uniqueCategories.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = {
                                    if (category == cat) category = "" else category = cat
                                    if (categoryInput.isNotBlank()) categoryInput = ""
                                },
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
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = categoryInput,
                        onValueChange = {
                            categoryInput = it
                            if (it.isNotBlank() && category.isNotBlank()) category = ""
                        },
                        label = { Text("New category") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = fieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmed = categoryInput.trim()
                            if (trimmed.isNotBlank()) {
                                category = trimmed
                                categoryInput = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue227ed4),
                        enabled = categoryInput.trim().isNotBlank(),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Text("Add", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = buyingPriceText,
                    onValueChange = { input ->
                        buyingPriceText = input.filter { it.isDigit() || it == '.' }
                        error = null
                    },
                    label = { Text("Buying Price") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = sellingPriceText,
                    onValueChange = { input ->
                        sellingPriceText = input.filter { it.isDigit() || it == '.' }
                        error = null
                    },
                    label = { Text("Selling Price") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { input ->
                        quantityText = input.filter { it.isDigit() }
                    },
                    label = { Text("Quantity") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, color = Color(0xFFE53935), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val buyVal = buyingPriceText.toDoubleOrNull() ?: 0.0
                    val sellVal = sellingPriceText.toDoubleOrNull() ?: 0.0
                    val qtyVal = quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    val finalCategory = category.trim().ifBlank { categoryInput.trim() }
                    when {
                        name.isBlank() -> error = "Please enter an item name"
                        buyVal <= 0 -> error = "Please enter a valid buying price"
                        sellVal <= 0 -> error = "Please enter a valid selling price"
                        else -> onSave(name.trim(), buyVal, sellVal, finalCategory, qtyVal)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Blue227ed4),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Add Item")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss(barcode) }) {
                Text("Cancel", color = Color(0xFF6B7280))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = Color(0xFFE2E8F0),
    focusedBorderColor = Blue227ed4,
    unfocusedContainerColor = Color.White,
    focusedContainerColor = Color.White
)
