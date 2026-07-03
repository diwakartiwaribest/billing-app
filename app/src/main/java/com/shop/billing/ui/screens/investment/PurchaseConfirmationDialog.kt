package com.shop.billing.ui.screens.investment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.billing.ui.theme.Blue227ed4
import com.shop.billing.util.Constants

@Composable
fun PurchaseConfirmationDialog(
    items: List<PurchaseItem>,
    onSave: (List<PurchaseItem>) -> Unit,
    onDismiss: () -> Unit
) {
    val editableItems = remember { mutableStateListOf<PurchaseItem>().apply { addAll(items) } }

    val grandTotal = editableItems.sumOf { it.purchasePrice * it.quantity }
    val canSave = editableItems.isNotEmpty() && editableItems.all { it.purchasePrice > 0 && it.quantity > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Confirm Purchase", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "${editableItems.size} item${if (editableItems.size != 1) "s" else ""}",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                if (editableItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No items to record.", fontSize = 13.sp, color = Color(0xFF6B7280))
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        itemsIndexed(editableItems) { index, item ->
                            PurchaseRow(
                                index = index + 1,
                                item = item,
                                onUpdate = { updated ->
                                    val idx = editableItems.indexOf(item)
                                    if (idx >= 0) editableItems[idx] = updated
                                },
                                onRemove = { editableItems.remove(item) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Blue227ed4)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Grand Total",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${Constants.CURRENCY_SYMBOL}${grandTotal.toLong()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(editableItems.toList()) },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Blue227ed4,
                    disabledContainerColor = Color(0xFFD1D5DB)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Save",
                    fontWeight = FontWeight.SemiBold,
                    color = if (canSave) Color.White else Color(0xFF9CA3AF)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF6B7280))
            }
        }
    )
}

@Composable
private fun PurchaseRow(
    index: Int,
    item: PurchaseItem,
    onUpdate: (PurchaseItem) -> Unit,
    onRemove: () -> Unit
) {
    var qtyText by remember(item.productId) { mutableStateOf(item.quantity.toString()) }
    var priceText by remember(item.productId) {
        mutableStateOf(if (item.purchasePrice > 0) item.purchasePrice.toLong().toString() else "")
    }

    val hasPriceError = priceText.toDoubleOrNull() == null || (priceText.toDoubleOrNull() ?: 0.0) <= 0
    val subtotal = (priceText.toDoubleOrNull() ?: 0.0) * (qtyText.toIntOrNull()?.coerceAtLeast(1) ?: 0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$index",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Blue227ed4,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.productName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF111827),
                        maxLines = 2
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Sell: ${Constants.CURRENCY_SYMBOL}${item.sellingPrice.toLong()}",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${Constants.CURRENCY_SYMBOL}${item.quantity * item.sellingPrice.toLong()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF22C55E)
                        )
                    }
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.heightIn(min = 56.dp)
            ) {
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }
                        qtyText = filtered
                        val newQty = qtyText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        onUpdate(item.copy(quantity = newQty))
                    },
                    label = { Text("Qty", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    colors = fieldColors(),
                    modifier = Modifier.width(80.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        priceText = filtered
                        val newPrice = priceText.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(purchasePrice = newPrice))
                    },
                    label = { Text("Purchase Price", fontSize = 12.sp) },
                    placeholder = { Text("Enter price", fontSize = 13.sp, color = Color(0xFF9CA3AF)) },
                    isError = hasPriceError,
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    colors = fieldColors(
                        errorBorder = if (hasPriceError) Color(0xFFDC2626) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            if (hasPriceError) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        text = "Enter purchase price",
                        fontSize = 11.sp,
                        color = Color(0xFFDC2626)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = "Subtotal: ${Constants.CURRENCY_SYMBOL}${subtotal.toLong()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (subtotal > 0) Blue227ed4 else Color(0xFF9CA3AF)
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors(errorBorder: Color = Color(0xFFE2E8F0)) = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = errorBorder,
    focusedBorderColor = if (errorBorder == Color(0xFFDC2626)) Color(0xFFDC2626) else Blue227ed4,
    unfocusedContainerColor = Color(0xFFF9FAFB),
    focusedContainerColor = Color.White
)
