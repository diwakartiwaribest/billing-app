package com.shop.billing.ui.screens.investment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.billing.ui.components.DialogCancelButton
import com.shop.billing.ui.components.DialogConfirmButton
import com.shop.billing.ui.components.DialogOverlay

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddProductForPurchaseDialog(
    barcode: String,
    existingCategories: List<String>,
    initialSellingPrice: Double,
    initialQuantity: Int = 0,
    isAdmin: Boolean = false,
    onSave: (name: String, buyingPrice: Double, sellingPrice: Double, category: String, quantity: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(barcode) { mutableStateOf("") }
    var buyingPriceText by remember(barcode) { mutableStateOf("") }
    var sellingPriceText by remember(barcode) { mutableStateOf(if (initialSellingPrice > 0) initialSellingPrice.toLong().toString() else "") }
    var category by remember(barcode) { mutableStateOf("") }
    var categoryInput by remember(barcode) { mutableStateOf("") }
    var quantityText by remember(barcode) { mutableStateOf("") }
    var error by remember(barcode) { mutableStateOf<String?>(null) }
    val hideQuantity = initialQuantity > 0

    val uniqueCategories = remember(existingCategories) {
        existingCategories.distinct().filter { it.isNotBlank() }
    }

    DialogOverlay(onDismiss = { onDismiss() }) {
        Text(
            "New Product",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        if (barcode.isNotBlank()) {
            Text(
                "Barcode: $barcode",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        val scrollState = rememberScrollState()
        var containerHeight by remember { mutableIntStateOf(0) }
        val density = LocalDensity.current
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { containerHeight = it.height }
                .clipToBounds()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .padding(end = 8.dp)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (category.isBlank() && isAdmin) {
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = categoryInput.trim().isNotBlank(),
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Text("Add", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
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
                if (!hideQuantity) {
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
                }

                error?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, color = Color(0xFFE53935), fontSize = 12.sp)
                }
            }
            if (scrollState.maxValue > 0) {
                val cH = containerHeight.coerceAtLeast(1).toFloat()
                val totalContent = cH + scrollState.maxValue
                val thumbRatio = cH / totalContent
                val thumbH = (cH * thumbRatio).coerceAtLeast(32f)
                val maxOff = (cH - thumbH).coerceAtLeast(0f)
                val thumbOff = if (scrollState.maxValue > 0)
                    (scrollState.value.toFloat() / scrollState.maxValue) * maxOff else 0f
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(with(density) { thumbH.toDp() })
                            .offset(y = with(density) { thumbOff.toDp() })
                            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DialogCancelButton(onClick = { onDismiss() }, modifier = Modifier.weight(1f))
            DialogConfirmButton(
            text = "Add Item",
            modifier = Modifier.weight(1f),
            onClick = {
                val buyVal = buyingPriceText.toDoubleOrNull() ?: 0.0
                val sellVal = sellingPriceText.toDoubleOrNull() ?: 0.0
                val qtyVal = if (hideQuantity) initialQuantity else (quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1)
                val finalCategory = category.trim().ifBlank { categoryInput.trim() }
                when {
                    name.isBlank() -> error = "Please enter an item name"
                    finalCategory.isBlank() -> error = "Please select or enter a category"
                    buyVal <= 0 -> error = "Please enter a valid buying price"
                    sellVal <= 0 -> error = "Please enter a valid selling price"
                    else -> onSave(name.trim(), buyVal, sellVal, finalCategory, qtyVal)
                }
            }
        )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedContainerColor = MaterialTheme.colorScheme.surface
)
