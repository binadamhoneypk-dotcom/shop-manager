package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.CalculatorTarget
import com.example.ui.ShopViewModel
import com.example.ui.theme.TertiaryLight
import com.example.ui.theme.ZakatGold
import com.example.util.AppStrings
import com.example.util.ReceiptHelper
import com.example.util.ZakatEngine
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang by viewModel.language.collectAsState()
    val shop by viewModel.activeShop.collectAsState()
    val zakatSummary by viewModel.zakatSummary.collectAsState()

    var showNisabDialog by remember { mutableStateOf(false) }
    var showRecordPaymentDialog by remember { mutableStateOf(false) }
    var showRulesExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag("zakat_screen")
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))

                // Title & Subtitle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = AppStrings.get("zakat_tracker_title", lang),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = AppStrings.get("zakat_subtitle", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Share Statement Button
                    IconButton(
                        onClick = {
                            val text = ZakatEngine.generateStatement(zakatSummary, shop?.name ?: "Shop", lang)
                            ReceiptHelper.shareText(context, text, "Share Zakat Statement")
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // --- Sahib-e-Nisab Hero Banner ---
            item {
                val isSahib = zakatSummary.isSahibENisab
                val gradient = if (isSahib) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF006D5B),
                            Color(0xFF065F46),
                            Color(0xFF1E3A5F)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF4B5563),
                            Color(0xFF374151)
                        )
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sahib_e_nisab_banner"),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(gradient)
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (isSahib) ZakatGold else Color.Gray,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (isSahib) Icons.Default.Stars else Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (isSahib) "SAHIB-E-NISAB (صاحبِ نصاب)" else "BELOW NISAB THRESHOLD",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { showNisabDialog = true },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Nisab", tint = Color.White)
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Text(
                                text = AppStrings.get("net_zakatable_wealth", lang),
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Rs. ${String.format("%,.0f", zakatSummary.netZakatableWealth)}",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nisab: Rs. ${String.format("%,.0f", zakatSummary.nisabThreshold)}",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isSahib) "Zakat Due (2.5%): Rs. ${String.format("%,.0f", zakatSummary.zakatDue)}" else "Zakat Not Yet Due",
                                    color = if (isSahib) Color(0xFFFFE082) else Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // --- Hawl (Lunar Year Cycle) Progress Card ---
            item {
                val cycleDays = 354L
                val progress = (zakatSummary.daysElapsed.toFloat() / cycleDays).coerceIn(0f, 1f)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = AppStrings.get("hawl_status", lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(zakatSummary.nisabStartDate))
                            Text(
                                text = "Since: $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = if (zakatSummary.isHawlCompleted) ZakatGold else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${zakatSummary.daysElapsed} days elapsed",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (zakatSummary.isHawlCompleted) "Lunar Year Completed (Zakat Due!)" else "${zakatSummary.daysRemaining} days remaining in year",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (zakatSummary.isHawlCompleted) ZakatGold else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // --- Wealth Breakdown Table ---
            item {
                Text(
                    text = "Breakdown of Zakatable Assets & Liabilities",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Inventory at cost
                        AssetBreakdownRow(
                            icon = Icons.Default.Inventory,
                            title = AppStrings.get("asset_inventory", lang),
                            subtitle = "Trading inventory valued at wholesale cost",
                            amount = zakatSummary.inventoryCostValue,
                            isAddition = true
                        )

                        Divider(modifier = Modifier.padding(vertical = 10.dp))

                        // Cash in hand
                        AssetBreakdownRow(
                            icon = Icons.Default.Payments,
                            title = AppStrings.get("asset_cash", lang),
                            subtitle = "Liquid cash in shop counter & bank",
                            amount = zakatSummary.cashInHand,
                            isAddition = true
                        )

                        Divider(modifier = Modifier.padding(vertical = 10.dp))

                        // Customer Receivables (Udhaar)
                        AssetBreakdownRow(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = AppStrings.get("asset_receivables", lang),
                            subtitle = "Outstanding dues expected from customers",
                            amount = zakatSummary.customerReceivables,
                            isAddition = true
                        )

                        Divider(modifier = Modifier.padding(vertical = 10.dp))

                        // Less: Supplier Payables
                        AssetBreakdownRow(
                            icon = Icons.Default.MoneyOff,
                            title = AppStrings.get("liability_payables", lang),
                            subtitle = "Debts owed to suppliers (deductible)",
                            amount = zakatSummary.supplierPayables,
                            isAddition = false
                        )

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Total Net Result
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = AppStrings.get("net_zakatable_wealth", lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Total Assets - Immediate Liabilities",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Rs. ${String.format("%,.0f", zakatSummary.netZakatableWealth)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // --- Zakat Payment & WhatsApp Actions ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showRecordPaymentDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("record_zakat_paid_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(AppStrings.get("record_zakat_payment", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val text = ZakatEngine.generateStatement(zakatSummary, shop?.name ?: "Shop", lang)
                            ReceiptHelper.sendWhatsAppMessage(context, "", text)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("WhatsApp", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- Shariah Zakat Rules Guide Accordion ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRulesExpanded = !showRulesExpanded },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = AppStrings.get("zakat_guide", lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                if (showRulesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }

                        AnimatedVisibility(visible = showRulesExpanded) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text(
                                    text = AppStrings.get("zakat_guide_text", lang),
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 22.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "• Nisab Basis: Equivalent of 52.5 Tolas (612.36g) of Silver (or ~Rs. 150,000–180,000).\n• Valuation: Trading inventory is valued at current wholesale purchase cost, not selling price.\n• Deduction: Only immediate business operational debts due within the month/year are deducted.\n• Hawl: The Islamic lunar year is 354 days.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    // --- Configure Nisab Dialog ---
    if (showNisabDialog) {
        var thresholdStr by remember { mutableStateOf(zakatSummary.nisabThreshold.toInt().toString()) }
        var daysAgoStr by remember { mutableStateOf(zakatSummary.daysElapsed.toString()) }

        Dialog(onDismissRequest = { showNisabDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .testTag("nisab_config_dialog"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = AppStrings.get("edit_nisab", lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = thresholdStr,
                            onValueChange = { thresholdStr = it },
                            label = { Text("Nisab Threshold (Rs.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("nisab_threshold_input"),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                viewModel.openCalculator(
                                    CalculatorTarget(
                                        fieldName = "Nisab",
                                        initialValue = thresholdStr,
                                        onResult = { thresholdStr = it }
                                    )
                                )
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = "Calc", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = daysAgoStr,
                        onValueChange = { daysAgoStr = it },
                        label = { Text("Days Elapsed in Hawl Cycle (out of 354)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showNisabDialog = false }) {
                            Text(AppStrings.get("cancel", lang))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val t = thresholdStr.toDoubleOrNull() ?: 150000.0
                                val d = daysAgoStr.toLongOrNull() ?: 0L
                                val newStartDate = System.currentTimeMillis() - (d * 24 * 60 * 60 * 1000)
                                viewModel.updateNisab(t, newStartDate)
                                showNisabDialog = false
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(AppStrings.get("save", lang))
                        }
                    }
                }
            }
        }
    }

    // --- Record Zakat Distribution Dialog ---
    if (showRecordPaymentDialog) {
        var zakatPaidStr by remember { mutableStateOf(zakatSummary.zakatDue.toInt().toString()) }
        var zakatNote by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showRecordPaymentDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .testTag("record_zakat_dialog"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = AppStrings.get("record_zakat_payment", lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = zakatPaidStr,
                            onValueChange = { zakatPaidStr = it },
                            label = { Text("Zakat Amount Paid (Rs.) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                viewModel.openCalculator(
                                    CalculatorTarget(
                                        fieldName = "Zakat Paid",
                                        initialValue = zakatPaidStr,
                                        onResult = { zakatPaidStr = it }
                                    )
                                )
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = "Calc", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = zakatNote,
                        onValueChange = { zakatNote = it },
                        label = { Text("Beneficiary / Distribution Note") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showRecordPaymentDialog = false }) {
                            Text(AppStrings.get("cancel", lang))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amount = zakatPaidStr.toDoubleOrNull() ?: 0.0
                                if (amount > 0) {
                                    viewModel.recordZakatPayment(amount, zakatNote)
                                    showRecordPaymentDialog = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Confirm Payment")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssetBreakdownRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    amount: Double,
    isAddition: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAddition) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isAddition) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        Text(
            text = (if (isAddition) "+ " else "- ") + "Rs. ${String.format("%,.0f", amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isAddition) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
        )
    }
}
