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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ItemEntity
import com.example.ui.CalculatorTarget
import com.example.ui.ShopViewModel
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.SaleReceiptDialog
import com.example.util.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.language.collectAsState()
    val shop by viewModel.activeShop.collectAsState()
    val allItems by viewModel.items.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val cart by viewModel.cartItems.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val billDiscount by viewModel.billDiscount.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }

    // Last completed invoice for viewing receipt
    val lastInvoice by viewModel.lastCompletedInvoice.collectAsState()
    val lastInvoiceItems by viewModel.lastCompletedInvoiceItems.collectAsState()
    val lastCustomerOldDue by viewModel.lastCompletedCustomerOldDue.collectAsState()

    val categories = remember(allItems) {
        listOf("All") + allItems.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredItems = remember(allItems, searchQuery, selectedCategory) {
        allItems.filter { item ->
            val matchesCategory = (selectedCategory == "All" || item.category == selectedCategory)
            val matchesSearch = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.nameUrdu.contains(searchQuery, ignoreCase = true) ||
                    item.barcode.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val subtotal = remember(cart) { cart.sumOf { it.lineTotal } }
    val grandTotal = remember(subtotal, billDiscount) { (subtotal - billDiscount).coerceAtLeast(0.0) }

    Scaffold(
        modifier = modifier.testTag("pos_screen"),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Search & Barcode Scan Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pos_search_input"),
                        placeholder = { Text(AppStrings.get("search_items", lang), fontSize = 14.sp) },
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

                    IconButton(
                        onClick = { showBarcodeScanner = true },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .testTag("pos_barcode_scan_btn")
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(if (cat == "All") AppStrings.get("all_categories", lang) else cat, fontSize = 12.sp) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (cart.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("pos_cart_bottom_bar")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${cart.size} item(s) • Total",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Rs. ${String.format("%,.0f", grandTotal)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.clearCart() },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(AppStrings.get("clear_cart", lang))
                                }

                                Button(
                                    onClick = { showCheckoutSheet = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("pos_proceed_btn")
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(AppStrings.get("complete_sale", lang), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Main Section: Items List
            LazyColumn(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${filteredItems.size} ${AppStrings.get("items_count", lang)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(filteredItems) { item ->
                    val cartQty = cart.find { it.item.id == item.id }?.quantity ?: 0.0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.addToCart(item, 1.0) }
                            .testTag("item_card_${item.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (cartQty > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (item.isService) {
                                        Spacer(Modifier.width(6.dp))
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("Service", fontSize = 10.sp) },
                                            modifier = Modifier.height(24.dp)
                                        )
                                    }
                                }
                                if (item.nameUrdu.isNotBlank()) {
                                    Text(
                                        text = item.nameUrdu,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Rs. ${item.salePrice.toInt()} / ${item.unit}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (!item.isService) {
                                        val isLow = item.quantity <= item.lowStockThreshold
                                        Text(
                                            text = "Stock: ${item.quantity.toInt()} ${item.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Quick Add Button or Quantity badge
                            if (cartQty > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (cartQty % 1.0 == 0.0) cartQty.toLong().toString() else String.format("%.1f", cartQty),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            } else {
                                IconButton(
                                    onClick = { viewModel.addToCart(item, 1.0) },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(80.dp))
                }
            }

            // Right Pane or Cart Drawer (For fast checkout review)
            if (cart.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 12.dp, top = 8.dp, bottom = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = AppStrings.get("cart", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(cart) { cItem ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = cItem.item.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "Rs. ${cItem.lineTotal.toInt()}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Spacer(Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "@ Rs. ${cItem.customSalePrice.toInt()}/${cItem.item.unit}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            // Quantity Adjuster
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { viewModel.updateCartItemQuantity(cItem.item.id, cItem.quantity - 1.0) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                                }

                                                Text(
                                                    text = if (cItem.quantity % 1.0 == 0.0) cItem.quantity.toLong().toString() else String.format("%.1f", cItem.quantity),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp)
                                                )

                                                IconButton(
                                                    onClick = { viewModel.updateCartItemQuantity(cItem.item.id, cItem.quantity + 1.0) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                                }

                                                // Universal Calculator for custom qty/rate
                                                IconButton(
                                                    onClick = {
                                                        viewModel.openCalculator(
                                                            CalculatorTarget(
                                                                fieldName = "${cItem.item.name} Qty",
                                                                initialValue = cItem.quantity.toString(),
                                                                onResult = { res ->
                                                                    res.toDoubleOrNull()?.let { qty ->
                                                                        viewModel.updateCartItemQuantity(cItem.item.id, qty)
                                                                    }
                                                                }
                                                            )
                                                        )
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Calculate,
                                                        contentDescription = "Calculate Qty",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Barcode Scanner Dialog ---
    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            onDismiss = { showBarcodeScanner = false },
            onBarcodeScanned = { barcode ->
                val matched = allItems.find { it.barcode.equals(barcode, ignoreCase = true) }
                if (matched != null) {
                    viewModel.addToCart(matched, 1.0)
                } else {
                    searchQuery = barcode
                }
            }
        )
    }

    // --- Checkout & Payment Modal Sheet ---
    if (showCheckoutSheet && cart.isNotEmpty()) {
        CheckoutModalSheet(
            viewModel = viewModel,
            grandTotal = grandTotal,
            subtotal = subtotal,
            onDismiss = { showCheckoutSheet = false },
            onComplete = {
                showCheckoutSheet = false
                showReceiptDialog = true
            }
        )
    }

    // --- Sale Receipt View Dialog ---
    if (showReceiptDialog && lastInvoice != null && shop != null) {
        SaleReceiptDialog(
            shop = shop!!,
            invoice = lastInvoice!!,
            items = lastInvoiceItems,
            customerOldDue = lastCustomerOldDue,
            lang = lang,
            onDismiss = { showReceiptDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutModalSheet(
    viewModel: ShopViewModel,
    grandTotal: Double,
    subtotal: Double,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val lang by viewModel.language.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    var paymentMethod by remember { mutableStateOf("CASH") } // CASH, CREDIT, PARTIAL
    var amountReceivedStr by remember { mutableStateOf(grandTotal.toInt().toString()) }
    var billDiscountStr by remember { mutableStateOf("0") }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    val discount = billDiscountStr.toDoubleOrNull() ?: 0.0
    val finalTotal = (subtotal - discount).coerceAtLeast(0.0)
    val amountPaid = if (paymentMethod == "CASH") finalTotal else if (paymentMethod == "CREDIT") 0.0 else (amountReceivedStr.toDoubleOrNull() ?: 0.0)
    val creditAdded = (finalTotal - amountPaid).coerceAtLeast(0.0)

    val oldCustomerDue = selectedCustomer?.currentDue ?: 0.0
    val newTotalCustomerDue = oldCustomerDue + creditAdded

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("checkout_modal_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = AppStrings.get("payment_method", lang),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(14.dp))

            // Customer Selector
            Text("Customer:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { customerDropdownExpanded = true }
                    .testTag("customer_picker_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = selectedCustomer?.name ?: AppStrings.get("walk_in", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedCustomer != null) {
                            Text(
                                text = "Current Udhaar: Rs. ${String.format("%,.0f", selectedCustomer!!.currentDue)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedCustomer!!.currentDue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }

            DropdownMenu(
                expanded = customerDropdownExpanded,
                onDismissRequest = { customerDropdownExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(AppStrings.get("walk_in", lang)) },
                    onClick = {
                        viewModel.selectedCustomer.value = null
                        customerDropdownExpanded = false
                    }
                )
                for (cust in customers) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(cust.name, fontWeight = FontWeight.Bold)
                                Text("Due: Rs. ${cust.currentDue.toInt()} • ${cust.phone}", fontSize = 12.sp)
                            }
                        },
                        onClick = {
                            viewModel.selectedCustomer.value = cust
                            customerDropdownExpanded = false
                        }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Payment Type Selector (Cash, Full Udhaar, Partial)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("CASH", AppStrings.get("pay_cash", lang), Icons.Default.Money),
                    Triple("CREDIT", AppStrings.get("pay_credit", lang), Icons.Default.CreditCard),
                    Triple("PARTIAL", AppStrings.get("pay_partial", lang), Icons.Default.Receipt)
                ).forEach { (type, label, icon) ->
                    val isSelected = paymentMethod == type
                    Button(
                        onClick = { paymentMethod = type },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Amount Received & Bill Discount with Universal Calculator Buttons!
            if (paymentMethod == "PARTIAL") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amountReceivedStr,
                        onValueChange = { amountReceivedStr = it },
                        label = { Text(AppStrings.get("amount_received", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            viewModel.openCalculator(
                                CalculatorTarget(
                                    fieldName = "Amount Received",
                                    initialValue = amountReceivedStr,
                                    onResult = { amountReceivedStr = it }
                                )
                            )
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = "Calculator", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            // Bill Level Discount Field with Calculator Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = billDiscountStr,
                    onValueChange = {
                        billDiscountStr = it
                        viewModel.billDiscount.value = it.toDoubleOrNull() ?: 0.0
                    },
                    label = { Text(AppStrings.get("bill_discount", lang)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        viewModel.openCalculator(
                            CalculatorTarget(
                                fieldName = "Bill Discount",
                                initialValue = billDiscountStr,
                                onResult = {
                                    billDiscountStr = it
                                    viewModel.billDiscount.value = it.toDoubleOrNull() ?: 0.0
                                }
                            )
                        )
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = "Calculator", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Breakdown card (combines old due + current items = total due)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(AppStrings.get("grand_total", lang))
                        Text("Rs. ${finalTotal.toInt()}", fontWeight = FontWeight.Bold)
                    }

                    if (creditAdded > 0) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(AppStrings.get("remaining_udhaar", lang))
                            Text("Rs. ${creditAdded.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }

                        if (selectedCustomer != null) {
                            Spacer(Modifier.height(4.dp))
                            Divider()
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${selectedCustomer!!.name} — Rs. ${oldCustomerDue.toInt()} old due + Rs. ${creditAdded.toInt()} current = Rs. ${newTotalCustomerDue.toInt()} total due",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Finalize Sale Button
            Button(
                onClick = {
                    viewModel.checkout(
                        paymentType = paymentMethod,
                        amountReceived = amountPaid,
                        onSuccess = { onComplete() }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_sale_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(AppStrings.get("complete_sale", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
