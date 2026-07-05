package com.shop.billing.ui.screens.investment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.billing.ui.components.DialogCancelButton
import com.shop.billing.ui.components.DialogOverlay
import com.shop.billing.ui.theme.Blue227ed4
import com.shop.billing.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPickerDialog(
    products: List<ResolvedProduct>,
    categories: List<String>,
    onSelect: (List<ResolvedProduct>) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    val uniqueCategories = remember(categories) {
        categories.distinct().filter { it.isNotBlank() }.sorted()
    }

    val filtered = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                p.product.name.contains(searchQuery, ignoreCase = true) ||
                p.product.barcode.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory.isBlank() || p.product.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    DialogOverlay(onDismiss = onDismiss) {
        Text("Select Product", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
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
        if (uniqueCategories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategory.isEmpty(),
                    onClick = { selectedCategory = "" },
                    label = { Text("All", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Blue227ed4,
                        selectedLabelColor = Color.White
                    )
                )
                uniqueCategories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = if (selectedCategory == cat) "" else cat
                        },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Blue227ed4,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            Text("No products found.", fontSize = 13.sp, color = Color(0xFF6B7280), modifier = Modifier.padding(vertical = 24.dp))
        } else {
            val listState = rememberLazyListState()
            val density = LocalDensity.current
            Row(modifier = Modifier.weight(1f).heightIn(max = 360.dp)) {
                LazyColumn(modifier = Modifier.weight(1f).padding(end = 6.dp), state = listState, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filtered, key = { it.product.id }) { resolved ->
                        val isSelected = resolved.product.id in selectedIds
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedIds = if (isSelected) selectedIds - resolved.product.id
                                              else selectedIds + resolved.product.id
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF9FAFB)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(resolved.product.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF111827))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Sell: ${Constants.CURRENCY_SYMBOL}${resolved.product.sellingPrice.toLong()}", fontSize = 12.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Medium)
                                        if (resolved.product.buyingPrice > 0) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Buy: ${Constants.CURRENCY_SYMBOL}${resolved.product.buyingPrice.toLong()}", fontSize = 11.sp, color = Color(0xFF6B7280))
                                        }
                                        if (resolved.lastPurchasePrice > 0) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Last buy: ${Constants.CURRENCY_SYMBOL}${resolved.lastPurchasePrice.toLong()}", fontSize = 11.sp, color = Color(0xFFEAB308))
                                        }
                                        if (resolved.product.barcode.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(resolved.product.barcode, fontSize = 10.sp, color = Color(0xFF9CA3AF))
                                        }
                                    }
                                }
                                Text(
                                    if (isSelected) "✓" else "Select",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color(0xFF22C55E) else Blue227ed4
                                )
                            }
                        }
                    }
                }
                val totalItems = listState.layoutInfo.totalItemsCount
                if (totalItems > 0) {
                    val visibleItems = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
                    val firstVisible = listState.firstVisibleItemIndex
                    val scrollableSlots = (totalItems - visibleItems).coerceAtLeast(1)
                    val progress = firstVisible.toFloat() / scrollableSlots
                    val trackHeight = with(density) { listState.layoutInfo.viewportEndOffset.toDp() - listState.layoutInfo.viewportStartOffset.toDp() }
                    val thumbHeight = (trackHeight / totalItems * visibleItems).coerceAtLeast(20.dp)
                    val maxOffset = (trackHeight - thumbHeight).coerceAtLeast(0.dp)
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
                                .height(thumbHeight)
                                .offset(y = maxOffset * progress)
                                .background(Color(0xFFCBD5E1), RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            DialogCancelButton(onClick = onDismiss, modifier = Modifier.weight(1f))
            if (selectedIds.isNotEmpty()) {
                val selectedProducts = filtered.filter { it.product.id in selectedIds }
                androidx.compose.material3.Button(
                    onClick = {
                        try {
                            onSelect(selectedProducts)
                        } catch (e: Exception) {
                            android.util.Log.e("ProductPicker", "onSelect failed", e)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Blue227ed4)
                ) {
                    Text("Add Selected (${selectedIds.size})", fontSize = 13.sp)
                }
            } else {
                Text("Tap products to select", fontSize = 12.sp, color = Color(0xFF9CA3AF), modifier = Modifier.weight(1f))
            }
        }
    }
}
