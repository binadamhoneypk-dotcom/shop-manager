package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.ItemEntity
import com.example.data.entity.StaffRole
import com.example.ui.CalculatorTarget
import com.example.ui.ShopViewModel
import com.example.ui.components.BarcodeScannerDialog
import com.example.util.AppStrings
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.language.collectAsState()
    val shop by viewModel.activeShop.collectAsState()
    val staff by viewModel.activeStaff.collectAsState()
    val items by viewModel.items.collectAsState()
    val lowStockItems by viewModel.lowStockItems.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showOnlyLowStock by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ItemEntity?>(null) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    val categories = remember(items) {
        listOf("All") + items.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredItems = remember(items, searchQuery, selectedCategory, showOnlyLowStock) {
        items.filter { item ->
            val matchesCategory = (selectedCategory == "All" || item.category == selectedCategory)
            val matchesSearch = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.nameUrdu.contains(searchQuery, ignoreCase = true) ||
                    item.barcode.contains(searchQuery, ignoreCase = true)
            val matchesLowStock = !showOnlyLowStock || (!item.isService && item.quantity <= item.lowStockThreshold)
            matchesCategory && matchesSearch && matchesLowStock
        }
    }

    val totalCostValue = remember(items) {
        items.filter { !it.isService }.sumOf { it.purchasePrice * it.quantity }
    }

    Scaffold(
        modifier = modifier.testTag("inventory_screen"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingItem = null
                    showAddItemDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(AppStrings.get("add_item", lang)) },
                modifier = Modifier.testTag("fab_add_item")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))

                // Low Stock Alert Banner (if any item is low in stock)
                if (lowStockItems.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showOnlyLowStock = !showOnlyLowStock },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${lowStockItems.size} ${AppStrings.get("low_stock_warning", lang)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = if (showOnlyLowStock) "Showing only low stock items. Tap to reset." else "Items running below alert threshold. Tap to view.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                if (showOnlyLowStock) Icons.Default.FilterListOff else Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                }

                // Inventory Value Summary Card (Restricted to Owner/Manager if desired)
                if (staff?.role != StaffRole.CASHIER) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = AppStrings.get("stock_value_cost", lang),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Rs. ${String.format("%,.0f", totalCostValue)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AssistChip(
                                onClick = { showBarcodeScanner = true },
                                label = { Text("Scan") },
                                leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                }

                // Search & Filter
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inventory_search_input"),
                    placeholder = { Text(AppStrings.get("search_items", lang)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(Modifier.height(8.dp))

                // Categories
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(if (cat == "All") AppStrings.get("all_categories", lang) else cat) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            items(filteredItems) { item ->
                val isLow = !item.isService && item.quantity <= item.lowStockThreshold
                val canViewProfit = staff?.role != StaffRole.CASHIER
                val profitPerUnit = item.salePrice - item.purchasePrice

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editingItem = item
                            showAddItemDialog = true
                        }
                        .testTag("inv_item_${item.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLow) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (item.nameUrdu.isNotBlank()) {
                                    Text(
                                        text = item.nameUrdu,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${item.category} • Unit: ${item.unit}" + if (item.barcode.isNotBlank()) " • #${item.barcode}" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isLow) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Low Stock", fontSize = 11.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        labelColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Divider()
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Sale: Rs. ${item.salePrice.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (canViewProfit && !item.isService) {
                                    Text(
                                        text = "Cost: Rs. ${item.purchasePrice.toInt()} (Margin: Rs. ${profitPerUnit.toInt()})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!item.isService) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isLow) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = "${item.quantity.toInt()} ${item.unit}",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isLow) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        editingItem = item
                                        showAddItemDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }

                                IconButton(
                                    onClick = { viewModel.deleteItem(item) }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // Add or Edit Item Dialog
    if (showAddItemDialog) {
        ItemFormDialog(
            viewModel = viewModel,
            shopId = shop?.id ?: "",
            existingItem = editingItem,
            onDismiss = {
                showAddItemDialog = false
                editingItem = null
            },
            onSave = { savedItem ->
                viewModel.saveItem(savedItem)
                showAddItemDialog = false
                editingItem = null
            }
        )
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            onDismiss = { showBarcodeScanner = false },
            onBarcodeScanned = { code ->
                searchQuery = code
            }
        )
    }
}

@Composable
fun ItemFormDialog(
    viewModel: ShopViewModel,
    shopId: String,
    existingItem: ItemEntity?,
    onDismiss: () -> Unit,
    onSave: (ItemEntity) -> Unit
) {
    val lang by viewModel.language.collectAsState()

    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var nameUrdu by remember { mutableStateOf(existingItem?.nameUrdu ?: "") }
    var category by remember { mutableStateOf(existingItem?.category ?: "Grocery") }
    var unit by remember { mutableStateOf(existingItem?.unit ?: "pcs") }
    var purchasePriceStr by remember { mutableStateOf(existingItem?.purchasePrice?.toInt()?.toString() ?: "") }
    var salePriceStr by remember { mutableStateOf(existingItem?.salePrice?.toInt()?.toString() ?: "") }
    var quantityStr by remember { mutableStateOf(existingItem?.quantity?.toInt()?.toString() ?: "10") }
    var lowStockStr by remember { mutableStateOf(existingItem?.lowStockThreshold?.toInt()?.toString() ?: "5") }
    var barcode by remember { mutableStateOf(existingItem?.barcode ?: "") }
    var isService by remember { mutableStateOf(existingItem?.isService ?: false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("item_form_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingItem != null) AppStrings.get("edit_item", lang) else AppStrings.get("add_item", lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(AppStrings.get("item_name", lang) + " *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("item_name_input"),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = nameUrdu,
                            onValueChange = { nameUrdu = it },
                            label = { Text(AppStrings.get("item_name_urdu", lang)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it },
                                label = { Text(AppStrings.get("category", lang)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = unit,
                                onValueChange = { unit = it },
                                label = { Text(AppStrings.get("unit", lang) + " (kg/pcs/box)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { isService = !isService }
                        ) {
                            Checkbox(checked = isService, onCheckedChange = { isService = it })
                            Spacer(Modifier.width(6.dp))
                            Text(AppStrings.get("is_service", lang), style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    // Purchase Price with Universal Calculator
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = purchasePriceStr,
                                onValueChange = { purchasePriceStr = it },
                                label = { Text(AppStrings.get("purchase_price", lang) + " *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("item_cost_input"),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    viewModel.openCalculator(
                                        CalculatorTarget(
                                            fieldName = "Purchase Price",
                                            initialValue = purchasePriceStr,
                                            onResult = { purchasePriceStr = it }
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = "Calc Cost", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }

                    // Sale Price with Universal Calculator
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = salePriceStr,
                                onValueChange = { salePriceStr = it },
                                label = { Text(AppStrings.get("sale_price", lang) + " *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("item_sale_input"),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    viewModel.openCalculator(
                                        CalculatorTarget(
                                            fieldName = "Sale Price",
                                            initialValue = salePriceStr,
                                            onResult = { salePriceStr = it }
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = "Calc Sale", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }

                    // Stock Quantity & Low Stock Alert
                    if (!isService) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = quantityStr,
                                    onValueChange = { quantityStr = it },
                                    label = { Text(AppStrings.get("stock_qty", lang)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("item_qty_input"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = lowStockStr,
                                    onValueChange = { lowStockStr = it },
                                    label = { Text(AppStrings.get("low_stock_threshold", lang)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text(AppStrings.get("barcode", lang)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(AppStrings.get("cancel", lang))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val item = ItemEntity(
                                    id = existingItem?.id ?: UUID.randomUUID().toString(),
                                    shopId = shopId,
                                    name = name.trim(),
                                    nameUrdu = nameUrdu.trim(),
                                    category = category.trim().ifBlank { "General" },
                                    unit = unit.trim().ifBlank { "pcs" },
                                    purchasePrice = purchasePriceStr.toDoubleOrNull() ?: 0.0,
                                    salePrice = salePriceStr.toDoubleOrNull() ?: 0.0,
                                    quantity = if (isService) 999.0 else (quantityStr.toDoubleOrNull() ?: 0.0),
                                    lowStockThreshold = lowStockStr.toDoubleOrNull() ?: 5.0,
                                    barcode = barcode.trim(),
                                    isService = isService
                                )
                                onSave(item)
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_item_btn")
                    ) {
                        Text(AppStrings.get("save", lang))
                    }
                }
            }
        }
    }
}
