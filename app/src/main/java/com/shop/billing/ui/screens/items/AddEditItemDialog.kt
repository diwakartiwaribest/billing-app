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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.billing.data.model.ShopItem
import com.shop.billing.ui.components.DialogCancelButton
import com.shop.billing.ui.components.DialogConfirmButton
import com.shop.billing.ui.components.DialogOverlay
import com.shop.billing.ui.theme.Blue227ed4

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemDialog(
    existingItem: ShopItem? = null,
    existingCategories: List<String> = emptyList(),
    isOwner: Boolean = true,
    barcode: String = "",
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, String, Int, Int, String) -> Unit
) {
    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var sellingPrice by remember { mutableStateOf(if (existingItem != null) existingItem.sellingPrice.toString() else "") }
    var buyingPrice by remember { mutableStateOf(if (existingItem != null) existingItem.buyingPrice.toString() else "") }
    var category by remember { mutableStateOf(existingItem?.category ?: "") }
    var stockQuantity by remember { mutableStateOf(if (existingItem != null) existingItem.stockQuantity.toString() else "0") }
    var lowStockThreshold by remember { mutableStateOf(if (existingItem != null) existingItem.lowStockThreshold.toString() else "10") }
    var error by remember { mutableStateOf(false) }

    val uniqueCategories = remember(existingCategories) {
        existingCategories.distinct().filter { it.isNotBlank() }
    }

    DialogOverlay(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Inventory2,
                contentDescription = null,
                tint = Blue227ed4,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                if (existingItem != null) "Edit Item" else "Add Item",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text("Fill in the product details below", fontSize = 13.sp, color = Color(0xFF6B7280))
        Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = sellingPrice,
                onValueChange = { sellingPrice = it.filter { c -> c.isDigit() || c == '.' }; error = false },
                label = { Text("Selling Price") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = Blue227ed4,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                modifier = Modifier.weight(1f),
                isError = error
            )
            OutlinedTextField(
                value = buyingPrice,
                onValueChange = { buyingPrice = it.filter { c -> c.isDigit() || c == '.' }; error = false },
                label = { Text("Buying Price") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = Blue227ed4,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                modifier = Modifier.weight(1f),
                isError = error
            )
        }

        if (error) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Please enter a valid name, selling price, buying price, and category",
                color = Color(0xFFE53935),
                fontSize = 11.sp
            )
        }

        if (barcode.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
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
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (uniqueCategories.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                uniqueCategories.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { if (category == cat) category = "" else category = cat },
                        label = { Text(cat, fontSize = 12.sp) },
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
            Spacer(modifier = Modifier.height(6.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isOwner) {
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
            }
            OutlinedTextField(
                value = stockQuantity,
                onValueChange = { stockQuantity = it.filter { c -> c.isDigit() } },
                label = { Text("Stock Qty") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = Blue227ed4,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = lowStockThreshold,
                onValueChange = { lowStockThreshold = it.filter { c -> c.isDigit() } },
                label = { Text("Low Alert") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = Blue227ed4,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DialogCancelButton(onClick = onDismiss, modifier = Modifier.weight(1f))
            DialogConfirmButton(
            text = "Save",
            modifier = Modifier.weight(1f),
            onClick = {
                val sellingPriceValue = sellingPrice.toDoubleOrNull()
                val buyingPriceValue = buyingPrice.toDoubleOrNull()
                val stockQtyValue = stockQuantity.toIntOrNull() ?: 0
                val thresholdValue = lowStockThreshold.toIntOrNull()?.coerceAtLeast(1) ?: 10
                if (name.isBlank() || sellingPriceValue == null || sellingPriceValue <= 0 || buyingPriceValue == null || buyingPriceValue < 0 || category.isBlank()) {
                    error = true
                } else {
                    onSave(name.trim(), sellingPriceValue, buyingPriceValue, category.trim(), stockQtyValue, thresholdValue, barcode)
                }
            }
        )
        }
    }
}
