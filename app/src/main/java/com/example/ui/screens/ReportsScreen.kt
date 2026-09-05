package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.StaffRole
import com.example.ui.ShopViewModel
import com.example.util.AppStrings
import com.example.util.ReceiptHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang by viewModel.language.collectAsState()
    val staff by viewModel.activeStaff.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val items by viewModel.items.collectAsState()

    var isUnlocked by remember { mutableStateOf(staff?.role != StaffRole.CASHIER) }

    // If staff is cashier and not yet unlocked
    if (!isUnlocked && staff?.role == StaffRole.CASHIER) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Access Restricted",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Cashier accounts cannot view profit & loss reports without Owner PIN.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            viewModel.requireRole(listOf(StaffRole.OWNER, StaffRole.MANAGER), "view financial reports") {
                                isUnlocked = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enter Owner / Manager PIN")
                    }
                }
            }
        }
        return
    }

    val totalSales = remember(invoices) { invoices.sumOf { it.grandTotal } }
    val totalDiscounts = remember(invoices) { invoices.sumOf { it.discount } }
    val totalGrossProfit = remember(invoices) { invoices.sumOf { it.totalProfit } }
    val totalExpenses = remember(expenses) { expenses.sumOf { it.amount } }
    val netProfit = remember(totalGrossProfit, totalExpenses) { totalGrossProfit - totalExpenses }

    val totalReceivables = remember(customers) { customers.sumOf { it.currentDue } }
    val totalPayables = remember(suppliers) { suppliers.sumOf { it.currentBalanceOwed } }

    Scaffold(
        modifier = modifier.testTag("reports_screen")
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = AppStrings.get("reports_analytics", lang),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Comprehensive Profit, Loss & Ledger Analytics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Export / Share Button
                    Button(
                        onClick = {
                            val csvData = viewModel.generateCsvExport()
                            ReceiptHelper.shareText(context, csvData, "Export Shop Data (CSV)")
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(AppStrings.get("export_data", lang), fontSize = 12.sp)
                    }
                }
            }

            // --- Net Profit Highlight Banner ---
            item {
                val isProfitable = netProfit >= 0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isProfitable) Color(0xFF006D5B) else MaterialTheme.colorScheme.error
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = AppStrings.get("net_profit", lang).uppercase(),
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Rs. ${String.format("%,.0f", netProfit)}",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Net Profit = Gross Profit (Rs. ${totalGrossProfit.toInt()}) - Expenses (Rs. ${totalExpenses.toInt()})",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // --- Key Metrics Grid ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = AppStrings.get("gross_sales", lang),
                        amount = totalSales,
                        subtitle = "${invoices.size} Invoices",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primary
                    )
                    MetricCard(
                        title = AppStrings.get("total_expenses", lang),
                        amount = totalExpenses,
                        subtitle = "${expenses.size} Entries",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = AppStrings.get("customer_receivables", lang),
                        amount = totalReceivables,
                        subtitle = "Udhaar Balance",
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFF97316)
                    )
                    MetricCard(
                        title = AppStrings.get("supplier_payables", lang),
                        amount = totalPayables,
                        subtitle = "Debts to Suppliers",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // --- Top Items by Stock & Value ---
            item {
                Text(
                    text = "Inventory Assets Valuation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val totalStockCost = items.filter { !it.isService }.sumOf { it.purchasePrice * it.quantity }
                        val totalStockSale = items.filter { !it.isService }.sumOf { it.salePrice * it.quantity }
                        val potentialMargin = totalStockSale - totalStockCost

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Stock Value (at Cost):", style = MaterialTheme.typography.bodyMedium)
                            Text("Rs. ${String.format("%,.0f", totalStockCost)}", fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Stock Value (at Retail Sale):", style = MaterialTheme.typography.bodyMedium)
                            Text("Rs. ${String.format("%,.0f", totalStockSale)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(Modifier.height(8.dp))
                        Divider()
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Expected Retail Profit Margin:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Rs. ${String.format("%,.0f", potentialMargin)}", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    amount: Double,
    subtitle: String,
    modifier: Modifier = Modifier,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Rs. ${String.format("%,.0f", amount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
