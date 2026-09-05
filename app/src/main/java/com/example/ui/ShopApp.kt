package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.StaffEntity
import com.example.data.entity.StaffRole
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.ZakatGold
import com.example.util.AppLanguage
import com.example.util.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopApp(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.language.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val activeShop by viewModel.activeShop.collectAsState()
    val allShops by viewModel.allShops.collectAsState()
    val activeStaff by viewModel.activeStaff.collectAsState()
    val shopStaff by viewModel.shopStaff.collectAsState()

    val isCalculatorOpen by viewModel.isCalculatorOpen.collectAsState()
    val calculatorTarget by viewModel.calculatorTarget.collectAsState()
    val pinDialogState by viewModel.pinDialogState.collectAsState()

    var showShopSwitcher by remember { mutableStateOf(false) }
    var showAddShopDialog by remember { mutableStateOf(false) }
    var showStaffSwitcher by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag("shop_app_root"),
        topBar = {
            TopAppBar(
                title = {
                    // Shop Switcher Chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clickable { showShopSwitcher = true }
                            .testTag("shop_switcher_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = activeShop?.name ?: "Select Shop",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1
                                )
                                Text(
                                    text = activeShop?.businessType ?: "General",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Switch",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Active Staff & Role Chip
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { showStaffSwitcher = true }
                            .padding(end = 4.dp)
                            .testTag("staff_profile_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = activeStaff?.name ?: "User",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Quick Universal Calculator Button
                    IconButton(
                        onClick = {
                            viewModel.openCalculator(
                                CalculatorTarget(
                                    fieldName = "Quick Calc",
                                    initialValue = "",
                                    onResult = {}
                                )
                            )
                        },
                        modifier = Modifier.testTag("topbar_calc_btn")
                    ) {
                        Icon(
                            Icons.Default.Calculate,
                            contentDescription = "Calculator",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Language Toggle (English <-> اردو)
                    IconButton(
                        onClick = {
                            viewModel.language.value = if (lang == AppLanguage.ENGLISH) AppLanguage.URDU else AppLanguage.ENGLISH
                        },
                        modifier = Modifier.testTag("language_toggle_btn")
                    ) {
                        Text(
                            text = if (lang == AppLanguage.ENGLISH) "اردو" else "EN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Dark Mode Toggle
                    IconButton(
                        onClick = { viewModel.isDarkMode.value = !isDark },
                        modifier = Modifier.testTag("dark_mode_toggle_btn")
                    ) {
                        Icon(
                            if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                // POS Billing
                NavigationBarItem(
                    selected = currentTab == ScreenTab.POS,
                    onClick = { viewModel.currentTab.value = ScreenTab.POS },
                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
                    label = { Text(AppStrings.get("pos_billing", lang), fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_pos")
                )

                // Inventory
                NavigationBarItem(
                    selected = currentTab == ScreenTab.INVENTORY,
                    onClick = { viewModel.currentTab.value = ScreenTab.INVENTORY },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                    label = { Text(AppStrings.get("inventory", lang), fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_inventory")
                )

                // Udhaar / Customers
                NavigationBarItem(
                    selected = currentTab == ScreenTab.CUSTOMERS,
                    onClick = { viewModel.currentTab.value = ScreenTab.CUSTOMERS },
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text(AppStrings.get("customer_ledger", lang), fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_customers")
                )

                // Zakat Tracker (Highlighted Gold)
                NavigationBarItem(
                    selected = currentTab == ScreenTab.ZAKAT,
                    onClick = { viewModel.currentTab.value = ScreenTab.ZAKAT },
                    icon = {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = null,
                            tint = if (currentTab == ScreenTab.ZAKAT) MaterialTheme.colorScheme.primary else ZakatGold
                        )
                    },
                    label = {
                        Text(
                            AppStrings.get("zakat_tracker", lang),
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == ScreenTab.ZAKAT) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("nav_zakat")
                )

                // More Menu Hub
                val isMoreSelected = currentTab in listOf(ScreenTab.SUPPLIERS, ScreenTab.STAFF, ScreenTab.EXPENSES, ScreenTab.REPORTS)
                NavigationBarItem(
                    selected = isMoreSelected,
                    onClick = { showMoreMenu = true },
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null) },
                    label = { Text(AppStrings.get("more", lang), fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_more")
                )
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentTab,
            modifier = Modifier.padding(innerPadding),
            label = "screen_transition"
        ) { tab ->
            when (tab) {
                ScreenTab.POS -> PosScreen(viewModel = viewModel)
                ScreenTab.INVENTORY -> InventoryScreen(viewModel = viewModel)
                ScreenTab.CUSTOMERS -> CustomerLedgerScreen(viewModel = viewModel)
                ScreenTab.ZAKAT -> ZakatScreen(viewModel = viewModel)
                ScreenTab.SUPPLIERS -> SupplierLedgerScreen(viewModel = viewModel)
                ScreenTab.STAFF -> StaffPayrollScreen(viewModel = viewModel)
                ScreenTab.EXPENSES -> ExpensesScreen(viewModel = viewModel)
                ScreenTab.REPORTS -> ReportsScreen(viewModel = viewModel)
            }
        }
    }

    // --- More Menu Sheet ---
    if (showMoreMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMoreMenu = false },
            modifier = Modifier.testTag("more_menu_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Shop Management Hub",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(14.dp))

                listOf(
                    Triple(ScreenTab.SUPPLIERS, AppStrings.get("supplier_ledger", lang), Icons.Default.LocalShipping),
                    Triple(ScreenTab.STAFF, AppStrings.get("staff_payroll", lang), Icons.Default.Badge),
                    Triple(ScreenTab.EXPENSES, AppStrings.get("expenses", lang), Icons.Default.MoneyOff),
                    Triple(ScreenTab.REPORTS, AppStrings.get("reports_analytics", lang), Icons.Default.Analytics)
                ).forEach { (targetTab, label, icon) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                viewModel.currentTab.value = targetTab
                                showMoreMenu = false
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(14.dp))
                            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // --- Switch Shop Dialog ---
    if (showShopSwitcher) {
        SwitchShopDialog(
            shops = allShops,
            activeShopId = activeShop?.id,
            onSelectShop = { newShopId -> viewModel.switchShop(newShopId) },
            onAddNewShop = { showAddShopDialog = true },
            onDismiss = { showShopSwitcher = false }
        )
    }

    // --- Add Shop Dialog ---
    if (showAddShopDialog) {
        AddShopDialog(
            onDismiss = { showAddShopDialog = false },
            onSave = { name, businessType, phone, address, cash ->
                viewModel.addShop(name, businessType, phone, address, cash)
            }
        )
    }

    // --- Switch Staff Account Dialog ---
    if (showStaffSwitcher) {
        StaffSwitchDialog(
            staffList = shopStaff,
            activeStaffId = activeStaff?.id,
            onDismiss = { showStaffSwitcher = false },
            onStaffSelected = { staff ->
                showStaffSwitcher = false
                viewModel.pinDialogState.value = PinDialogState(
                    title = "Switch to ${staff.name}",
                    description = "Enter PIN for ${staff.name} (${staff.role.name})",
                    onSuccess = {
                        viewModel.activeStaff.value = staff
                    }
                )
            }
        )
    }

    // --- Universal Calculator Dialog Overlay ---
    if (isCalculatorOpen) {
        UniversalCalculatorDialog(
            initialValue = calculatorTarget?.initialValue ?: "",
            fieldName = calculatorTarget?.fieldName ?: "",
            onDismiss = { viewModel.closeCalculator() },
            onInsertResult = { result ->
                viewModel.submitCalculatorResult(result)
            }
        )
    }

    // --- PIN Verification Dialog Overlay ---
    if (pinDialogState != null) {
        PinVerificationDialog(
            title = pinDialogState!!.title,
            description = pinDialogState!!.description,
            onDismiss = { viewModel.pinDialogState.value = null },
            onPinVerified = {
                pinDialogState?.onSuccess?.invoke()
            }
        )
    }
}

@Composable
fun StaffSwitchDialog(
    staffList: List<StaffEntity>,
    activeStaffId: String?,
    onDismiss: () -> Unit,
    onStaffSelected: (StaffEntity) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("staff_switch_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Switch Staff / Cashier",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(10.dp))

                for (staff in staffList) {
                    val isSelected = staff.id == activeStaffId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onStaffSelected(staff) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(staff.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Role: ${staff.role.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
