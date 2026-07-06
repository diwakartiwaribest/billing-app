package com.shop.billing.ui.screens.investment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.shop.billing.ui.components.DialogCancelButton
import com.shop.billing.ui.components.DialogOverlay
import com.shop.billing.ui.theme.Blue227ed4
import com.shop.billing.util.Constants

private val SuccessGreen = Color(0xFF22C55E)
private val Amber500 = Color(0xFFEAB308)

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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Select Product", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search name or barcode", fontSize = 14.sp) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (uniqueCategories.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategory.isEmpty(),
                    onClick = { selectedCategory = "" },
                    label = { Text("All", fontSize = 12.sp) },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                uniqueCategories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) "" else cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                Text("No products found.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.product.id }) { resolved ->
                    val isSelected = resolved.product.id in selectedIds
                    val badgeColors = if (isSelected) listOf(MaterialTheme.colorScheme.primary, Color(0xFF2563EB))
                        else listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.surfaceVariant)
                    val badgeIconTint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    Card(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).let { mod ->
                            if (isSelected) mod.border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            else mod.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        }.clickable {
                            selectedIds = if (isSelected) selectedIds - resolved.product.id
                                          else selectedIds + resolved.product.id
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp)) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(badgeColors)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = badgeIconTint, modifier = Modifier.size(21.dp))
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .align(Alignment.BottomEnd)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(resolved.product.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Spacer(Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val parts = mutableListOf<@Composable () -> Unit>()
                                    if (resolved.product.buyingPrice > 0) {
                                        parts.add { Text("Buy ${Constants.CURRENCY_SYMBOL}${resolved.product.buyingPrice.toLong()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    }
                                    if (resolved.lastPurchasePrice > 0) {
                                        parts.add { Text("Last ${Constants.CURRENCY_SYMBOL}${resolved.lastPurchasePrice.toLong()}", fontSize = 12.sp, color = Color(0xFFD97706)) }
                                    }
                                    if (resolved.product.barcode.isNotBlank()) {
                                        parts.add { Text(resolved.product.barcode, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    }
                                    parts.forEachIndexed { index, part ->
                                        if (index > 0) { Text("  ·  ", fontSize = 10.sp, color = MaterialTheme.colorScheme.outlineVariant) }
                                        part()
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("${Constants.CURRENCY_SYMBOL}${resolved.product.sellingPrice.toLong()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
            Spacer(Modifier.height(12.dp))
        if (selectedIds.isEmpty()) {
            DialogCancelButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                DialogCancelButton(onClick = onDismiss, modifier = Modifier.weight(1f))
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
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Selected (${selectedIds.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
