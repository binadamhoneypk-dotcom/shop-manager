package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.ShopDatabase
import com.example.data.entity.*
import com.example.data.repository.ShopRepository
import com.example.util.AppLanguage
import com.example.util.CalculatorEngine
import com.example.util.ZakatEngine
import com.example.util.ZakatSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ShopRepository

    init {
        val db = ShopDatabase.getDatabase(application, viewModelScope)
        repository = ShopRepository(db)
    }

    // App Preferences
    val language = MutableStateFlow(AppLanguage.ENGLISH)
    val isDarkMode = MutableStateFlow(false)
    val currentTab = MutableStateFlow(ScreenTab.POS)

    // Shops
    val allShops: StateFlow<List<ShopEntity>> = repository.getAllShops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeShopId = MutableStateFlow<String?>("shop-general-01")

    val activeShop: StateFlow<ShopEntity?> = combine(allShops, activeShopId) { shops, id ->
        shops.find { it.id == id } ?: shops.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active Staff & Authentication
    val shopStaff: StateFlow<List<StaffEntity>> = activeShopId
        .flatMapLatest { id ->
            if (id != null) repository.getStaffByShop(id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeStaff = MutableStateFlow<StaffEntity?>(null)

    init {
        viewModelScope.launch {
            shopStaff.collect { list ->
                if (activeStaff.value == null || list.none { it.id == activeStaff.value?.id }) {
                    activeStaff.value = list.firstOrNull { it.role == StaffRole.OWNER } ?: list.firstOrNull()
                }
            }
        }
    }

    // Items
    val items: StateFlow<List<ItemEntity>> = activeShopId
        .flatMapLatest { id ->
            if (id != null) repository.getItemsByShop(id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockItems: StateFlow<List<ItemEntity>> = activeShopId
        .flatMapLatest { id ->
            if (id != null) repository.getLowStockItems(id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customers
    val customers: StateFlow<List<CustomerEntity>> = activeShopId
        .flatMapLatest { id ->
            if (id != null) repository.getCustomersByShop(id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Suppliers
    val suppliers: StateFlow<List<SupplierEntity>> = activeShopId
        .flatMapLatest { id ->
            if (id != null) repository.getSuppliersByShop(id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Invoices
    val invoices: StateFlow<List<InvoiceEntity>> = activeShopId
        .flatMapLatest { id ->
            if (id != null) repository.getInvoicesByShop(id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expenses
    val expenses: StateFlow<List<ExpenseEntity>> = activeShopId
        .flatMapLatest { id ->
            if (id != null) repository.getExpensesByShop(id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Attendance & Payroll
    val attendanceList: StateFlow<List<AttendanceEntity>> = activeShopId
        .flatMapLatest { id ->
            if (id != null) repository.getAttendanceByShop(id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salaryPayments: StateFlow<List<SalaryPaymentEntity>> = activeShopId
        .flatMapLatest { id ->
            if (id != null) repository.getSalaryPayments(id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Activity Logs
    val activityLogs: StateFlow<List<ActivityLogEntity>> = activeShopId
        .flatMapLatest { id ->
            if (id != null) repository.getLogsByShop(id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // POS / Cart State
    val cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val billDiscount = MutableStateFlow(0.0)
    val lastCompletedInvoice = MutableStateFlow<InvoiceEntity?>(null)
    val lastCompletedInvoiceItems = MutableStateFlow<List<InvoiceItemEntity>>(emptyList())
    val lastCompletedCustomerOldDue = MutableStateFlow(0.0)

    // Universal Calculator Overlay State
    val calculatorTarget = MutableStateFlow<CalculatorTarget?>(null)
    val isCalculatorOpen = MutableStateFlow(false)

    // PIN Protection Modal State
    val pinDialogState = MutableStateFlow<PinDialogState?>(null)

    // Zakat Summary State Flow
    val zakatSummary: StateFlow<ZakatSummary> = combine(
        activeShop,
        items,
        customers,
        suppliers
    ) { shop, itemList, custList, suppList ->
        val costValue = itemList.filter { !it.isService }.sumOf { it.purchasePrice * it.quantity }
        val saleValue = itemList.filter { !it.isService }.sumOf { it.salePrice * it.quantity }
        val cash = shop?.cashInHand ?: 0.0
        val receivables = custList.sumOf { it.currentDue }
        val payables = suppList.sumOf { it.currentBalanceOwed }
        val nisab = shop?.nisabThreshold ?: 150000.0
        val startDate = shop?.nisabStartDate ?: (System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000))

        ZakatEngine.calculate(
            inventoryCostValue = costValue,
            inventorySaleValue = saleValue,
            cashInHand = cash,
            customerReceivables = receivables,
            supplierPayables = payables,
            nisabThreshold = nisab,
            nisabStartDate = startDate
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ZakatEngine.calculate(0.0, 0.0, 0.0, 0.0, 0.0, 150000.0, System.currentTimeMillis())
    )

    // --- POS Functions ---

    fun addToCart(item: ItemEntity, quantity: Double = 1.0) {
        val current = cartItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.item.id == item.id }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            current[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            current.add(CartItem(item = item, quantity = quantity))
        }
        cartItems.value = current
    }

    fun updateCartItemQuantity(itemId: String, newQty: Double) {
        if (newQty <= 0) {
            removeFromCart(itemId)
        } else {
            cartItems.value = cartItems.value.map {
                if (it.item.id == itemId) it.copy(quantity = newQty) else it
            }
        }
    }

    fun updateCartItemDiscount(itemId: String, discount: Double) {
        cartItems.value = cartItems.value.map {
            if (it.item.id == itemId) it.copy(discount = discount) else it
        }
    }

    fun removeFromCart(itemId: String) {
        cartItems.value = cartItems.value.filter { it.item.id != itemId }
    }

    fun clearCart() {
        cartItems.value = emptyList()
        selectedCustomer.value = null
        billDiscount.value = 0.0
    }

    fun checkout(
        paymentType: String, // CASH, CREDIT, PARTIAL
        amountReceived: Double,
        onSuccess: (InvoiceEntity) -> Unit
    ) {
        val shop = activeShop.value ?: return
        val currentCart = cartItems.value
        if (currentCart.isEmpty()) return

        val subtotal = currentCart.sumOf { it.lineTotal }
        val grandTotal = (subtotal - billDiscount.value).coerceAtLeast(0.0)

        val paid = when (paymentType) {
            "CASH" -> grandTotal
            "CREDIT" -> 0.0
            else -> amountReceived.coerceIn(0.0, grandTotal)
        }
        val creditAmount = (grandTotal - paid).coerceAtLeast(0.0)
        val customer = selectedCustomer.value
        val oldDue = customer?.currentDue ?: 0.0

        val invoiceId = UUID.randomUUID().toString()
        val invoiceNumber = "INV-" + (System.currentTimeMillis() % 1000000).toString()

        val totalProfit = currentCart.sumOf { it.lineProfit } - billDiscount.value

        val staff = activeStaff.value

        val invoice = InvoiceEntity(
            id = invoiceId,
            shopId = shop.id,
            invoiceNumber = invoiceNumber,
            customerId = customer?.id,
            customerName = customer?.name ?: "Cash Walk-In",
            subtotal = subtotal,
            discount = billDiscount.value,
            grandTotal = grandTotal,
            paidAmount = paid,
            creditAmount = creditAmount,
            paymentType = paymentType,
            staffId = staff?.id ?: "",
            staffName = staff?.name ?: "Cashier",
            totalProfit = totalProfit,
            timestamp = System.currentTimeMillis()
        )

        val invoiceItems = currentCart.map { cItem ->
            InvoiceItemEntity(
                invoiceId = invoiceId,
                itemId = cItem.item.id,
                itemName = cItem.item.name,
                unit = cItem.item.unit,
                quantity = cItem.quantity,
                purchasePrice = cItem.item.purchasePrice,
                salePrice = cItem.customSalePrice,
                itemDiscount = cItem.discount,
                lineTotal = cItem.lineTotal,
                lineProfit = cItem.lineProfit
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertInvoice(invoice, invoiceItems)

            // Update shop cash balance with paid amount
            repository.updateShop(shop.copy(cashInHand = shop.cashInHand + paid))

            // If credit was extended to a customer, update their balance and record transaction
            if (creditAmount > 0 && customer != null) {
                val newDue = oldDue + creditAmount
                val updatedCustomer = customer.copy(
                    totalPurchases = customer.totalPurchases + grandTotal,
                    totalPaid = customer.totalPaid + paid,
                    currentDue = newDue,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateCustomer(updatedCustomer)

                repository.insertCustomerTransaction(
                    CustomerTransactionEntity(
                        id = UUID.randomUUID().toString(),
                        shopId = shop.id,
                        customerId = customer.id,
                        type = "SALE_CREDIT",
                        amount = creditAmount,
                        oldBalance = oldDue,
                        newBalance = newDue,
                        note = "Credit sale Invoice #$invoiceNumber",
                        invoiceId = invoiceId
                    )
                )
            } else if (customer != null && paid > 0) {
                // Cash sale to known customer
                repository.updateCustomer(
                    customer.copy(
                        totalPurchases = customer.totalPurchases + grandTotal,
                        totalPaid = customer.totalPaid + paid,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            // Log activity
            repository.insertLog(
                ActivityLogEntity(
                    shopId = shop.id,
                    staffName = staff?.name ?: "Cashier",
                    actionType = "SALE",
                    details = "Completed sale #$invoiceNumber for Rs. ${grandTotal.toInt()} (${invoiceItems.size} items)"
                )
            )

            lastCompletedInvoice.value = invoice
            lastCompletedInvoiceItems.value = invoiceItems
            lastCompletedCustomerOldDue.value = oldDue
            clearCart()
            onSuccess(invoice)
        }
    }

    // --- Customer Operations ---

    fun addCustomer(name: String, phone: String, address: String, initialDue: Double) {
        val shop = activeShop.value ?: return
        val id = UUID.randomUUID().toString()
        val customer = CustomerEntity(
            id = id,
            shopId = shop.id,
            name = name,
            phone = phone,
            address = address,
            totalPurchases = initialDue,
            totalPaid = 0.0,
            currentDue = initialDue
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCustomer(customer)
            if (initialDue > 0) {
                repository.insertCustomerTransaction(
                    CustomerTransactionEntity(
                        id = UUID.randomUUID().toString(),
                        shopId = shop.id,
                        customerId = id,
                        type = "INITIAL_DUE",
                        amount = initialDue,
                        oldBalance = 0.0,
                        newBalance = initialDue,
                        note = "Initial Opening Udhaar Balance"
                    )
                )
            }
        }
    }

    fun recordCustomerPayment(customer: CustomerEntity, amount: Double, note: String) {
        val shop = activeShop.value ?: return
        val oldDue = customer.currentDue
        val newDue = (oldDue - amount).coerceAtLeast(0.0)
        val updated = customer.copy(
            totalPaid = customer.totalPaid + amount,
            currentDue = newDue,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCustomer(updated)
            repository.insertCustomerTransaction(
                CustomerTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    shopId = shop.id,
                    customerId = customer.id,
                    type = "PAYMENT_RECEIVED",
                    amount = amount,
                    oldBalance = oldDue,
                    newBalance = newDue,
                    note = if (note.isBlank()) "Payment received from customer" else note
                )
            )
            // Add to cash in hand
            repository.updateShop(shop.copy(cashInHand = shop.cashInHand + amount))
            repository.insertLog(
                ActivityLogEntity(
                    shopId = shop.id,
                    staffName = activeStaff.value?.name ?: "Cashier",
                    actionType = "PAYMENT_RECEIVED",
                    details = "Received Rs. ${amount.toInt()} from ${customer.name}. New balance: Rs. ${newDue.toInt()}"
                )
            )
        }
    }

    fun addManualCredit(customer: CustomerEntity, amount: Double, note: String) {
        val shop = activeShop.value ?: return
        val oldDue = customer.currentDue
        val newDue = oldDue + amount
        val updated = customer.copy(
            totalPurchases = customer.totalPurchases + amount,
            currentDue = newDue,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCustomer(updated)
            repository.insertCustomerTransaction(
                CustomerTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    shopId = shop.id,
                    customerId = customer.id,
                    type = "MANUAL_CREDIT",
                    amount = amount,
                    oldBalance = oldDue,
                    newBalance = newDue,
                    note = if (note.isBlank()) "Manual Udhaar addition" else note
                )
            )
        }
    }

    // --- Supplier Operations ---

    fun addSupplier(name: String, phone: String, company: String, initialOwed: Double) {
        val shop = activeShop.value ?: return
        val id = UUID.randomUUID().toString()
        val supplier = SupplierEntity(
            id = id,
            shopId = shop.id,
            name = name,
            phone = phone,
            company = company,
            totalBilled = initialOwed,
            totalPaid = 0.0,
            currentBalanceOwed = initialOwed
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSupplier(supplier)
            if (initialOwed > 0) {
                repository.insertSupplierTransaction(
                    SupplierTransactionEntity(
                        id = UUID.randomUUID().toString(),
                        shopId = shop.id,
                        supplierId = id,
                        type = "INITIAL_BALANCE",
                        amount = initialOwed,
                        oldBalance = 0.0,
                        newBalance = initialOwed,
                        note = "Opening Supplier Payable Balance"
                    )
                )
            }
        }
    }

    fun recordSupplierPayment(supplier: SupplierEntity, amount: Double, note: String) {
        val shop = activeShop.value ?: return
        val oldBalance = supplier.currentBalanceOwed
        val newBalance = (oldBalance - amount).coerceAtLeast(0.0)
        val updated = supplier.copy(
            totalPaid = supplier.totalPaid + amount,
            currentBalanceOwed = newBalance
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSupplier(updated)
            repository.insertSupplierTransaction(
                SupplierTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    shopId = shop.id,
                    supplierId = supplier.id,
                    type = "PAYMENT_MADE",
                    amount = amount,
                    oldBalance = oldBalance,
                    newBalance = newBalance,
                    note = if (note.isBlank()) "Paid to supplier" else note
                )
            )
            // Deduct from cash
            repository.updateShop(shop.copy(cashInHand = (shop.cashInHand - amount).coerceAtLeast(0.0)))
        }
    }

    fun recordSupplierPurchase(supplier: SupplierEntity, amount: Double, note: String) {
        val shop = activeShop.value ?: return
        val oldBalance = supplier.currentBalanceOwed
        val newBalance = oldBalance + amount
        val updated = supplier.copy(
            totalBilled = supplier.totalBilled + amount,
            currentBalanceOwed = newBalance
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSupplier(updated)
            repository.insertSupplierTransaction(
                SupplierTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    shopId = shop.id,
                    supplierId = supplier.id,
                    type = "PURCHASE_CREDIT",
                    amount = amount,
                    oldBalance = oldBalance,
                    newBalance = newBalance,
                    note = if (note.isBlank()) "Stock purchased on credit" else note
                )
            )
        }
    }

    // --- Inventory Operations ---

    fun saveItem(item: ItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertItem(item)
            repository.insertLog(
                ActivityLogEntity(
                    shopId = item.shopId,
                    staffName = activeStaff.value?.name ?: "User",
                    actionType = "ITEM_SAVE",
                    details = "Saved item: ${item.name} (${item.unit})"
                )
            )
        }
    }

    fun deleteItem(item: ItemEntity) {
        requireRole(listOf(StaffRole.OWNER, StaffRole.MANAGER), "delete items") {
            viewModelScope.launch(Dispatchers.IO) {
                repository.deleteItem(item)
                repository.insertLog(
                    ActivityLogEntity(
                        shopId = item.shopId,
                        staffName = activeStaff.value?.name ?: "User",
                        actionType = "ITEM_DELETE",
                        details = "Deleted item: ${item.name}"
                    )
                )
            }
        }
    }

    // --- Expenses ---

    fun addExpense(category: String, amount: Double, description: String) {
        val shop = activeShop.value ?: return
        val expense = ExpenseEntity(
            id = UUID.randomUUID().toString(),
            shopId = shop.id,
            category = category,
            amount = amount,
            description = description,
            staffName = activeStaff.value?.name ?: "Staff",
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertExpense(expense)
            // Deduct expense from shop cash
            repository.updateShop(shop.copy(cashInHand = (shop.cashInHand - amount).coerceAtLeast(0.0)))
            repository.insertLog(
                ActivityLogEntity(
                    shopId = shop.id,
                    staffName = activeStaff.value?.name ?: "Staff",
                    actionType = "EXPENSE",
                    details = "Recorded expense: $category — Rs. ${amount.toInt()}"
                )
            )
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        requireRole(listOf(StaffRole.OWNER), "delete expenses") {
            viewModelScope.launch(Dispatchers.IO) {
                repository.deleteExpense(expense)
            }
        }
    }

    // --- Attendance & Payroll ---

    fun markAttendance(staffId: String, status: String, note: String = "") {
        val shop = activeShop.value ?: return
        val staff = shopStaff.value.find { it.id == staffId } ?: return
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        val attendance = AttendanceEntity(
            id = "$staffId-$todayStr",
            shopId = shop.id,
            staffId = staffId,
            staffName = staff.name,
            date = todayStr,
            status = status,
            note = note,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertAttendance(attendance)
            repository.insertLog(
                ActivityLogEntity(
                    shopId = shop.id,
                    staffName = activeStaff.value?.name ?: "Manager",
                    actionType = "ATTENDANCE",
                    details = "Marked attendance for ${staff.name}: $status"
                )
            )
        }
    }

    fun paySalary(staff: StaffEntity, amount: Double, note: String) {
        val shop = activeShop.value ?: return
        val payment = SalaryPaymentEntity(
            id = UUID.randomUUID().toString(),
            shopId = shop.id,
            staffId = staff.id,
            staffName = staff.name,
            amount = amount,
            note = note,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSalaryPayment(payment)
            // Deduct from cash
            repository.updateShop(shop.copy(cashInHand = (shop.cashInHand - amount).coerceAtLeast(0.0)))
            repository.insertLog(
                ActivityLogEntity(
                    shopId = shop.id,
                    staffName = activeStaff.value?.name ?: "Owner",
                    actionType = "SALARY_PAYMENT",
                    details = "Paid salary to ${staff.name}: Rs. ${amount.toInt()}"
                )
            )
        }
    }

    // --- Zakat & Nisab ---

    fun updateNisab(threshold: Double, startDate: Long) {
        requireRole(listOf(StaffRole.OWNER), "update Nisab configuration") {
            val shop = activeShop.value ?: return@requireRole
            val updated = shop.copy(nisabThreshold = threshold, nisabStartDate = startDate)
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateShop(updated)
            }
        }
    }

    fun recordZakatPayment(amount: Double, note: String) {
        val shop = activeShop.value ?: return
        val expense = ExpenseEntity(
            id = UUID.randomUUID().toString(),
            shopId = shop.id,
            category = "Zakat Distribution",
            amount = amount,
            description = if (note.isBlank()) "Zakat payment distributed" else note,
            staffName = activeStaff.value?.name ?: "Owner",
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertExpense(expense)
            repository.updateShop(shop.copy(cashInHand = (shop.cashInHand - amount).coerceAtLeast(0.0)))
            repository.insertLog(
                ActivityLogEntity(
                    shopId = shop.id,
                    staffName = activeStaff.value?.name ?: "Owner",
                    actionType = "ZAKAT_PAID",
                    details = "Distributed Zakat: Rs. ${amount.toInt()}"
                )
            )
        }
    }

    // --- Multi-Tenant Shop Management ---

    fun switchShop(shopId: String) {
        activeShopId.value = shopId
        clearCart()
    }

    fun addShop(name: String, businessType: String, phone: String, address: String, initialCash: Double) {
        val newId = "shop-" + UUID.randomUUID().toString().take(8)
        val newShop = ShopEntity(
            id = newId,
            name = name,
            businessType = businessType,
            phone = phone,
            address = address,
            cashInHand = initialCash
        )
        val defaultOwner = StaffEntity(
            id = "staff-" + UUID.randomUUID().toString().take(8),
            shopId = newId,
            name = "Owner",
            role = StaffRole.OWNER,
            pin = "1234"
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertShop(newShop)
            repository.insertStaff(defaultOwner)
            activeShopId.value = newId
        }
    }

    // --- Staff Switch & Permissions ---

    fun switchStaff(staffId: String, pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val staff = shopStaff.value.find { it.id == staffId }
        if (staff != null) {
            if (staff.pin == pin) {
                activeStaff.value = staff
                onSuccess()
            } else {
                onError("Incorrect PIN")
            }
        } else {
            onError("Staff not found")
        }
    }

    fun requireRole(allowedRoles: List<StaffRole>, actionDesc: String, onAuthorized: () -> Unit) {
        val current = activeStaff.value
        if (current != null && allowedRoles.contains(current.role)) {
            onAuthorized()
        } else {
            // Request Owner / Manager PIN
            pinDialogState.value = PinDialogState(
                title = "Authentication Required",
                description = "Enter Owner/Manager PIN to $actionDesc",
                onSuccess = {
                    pinDialogState.value = null
                    onAuthorized()
                }
            )
        }
    }

    // --- Universal Calculator Support ---

    fun openCalculator(target: CalculatorTarget) {
        calculatorTarget.value = target
        isCalculatorOpen.value = true
    }

    fun closeCalculator() {
        isCalculatorOpen.value = false
        calculatorTarget.value = null
    }

    fun submitCalculatorResult(result: String) {
        calculatorTarget.value?.onResult?.invoke(result)
        closeCalculator()
    }

    // --- Data Export & Backup Helper ---

    fun generateCsvExport(): String {
        val shop = activeShop.value ?: return ""
        val itemList = items.value
        val custList = customers.value
        val suppList = suppliers.value
        val invList = invoices.value
        val expList = expenses.value

        val sb = StringBuilder()
        sb.append("=== SHOP DATA EXPORT: ${shop.name} ===\n\n")

        sb.append("--- INVENTORY ITEMS ---\n")
        sb.append("Name,Category,Unit,CostPrice,SalePrice,Quantity,Barcode\n")
        for (i in itemList) {
            sb.append("\"${i.name}\",\"${i.category}\",\"${i.unit}\",${i.purchasePrice},${i.salePrice},${i.quantity},\"${i.barcode}\"\n")
        }

        sb.append("\n--- CUSTOMER UDHAAR LEDGER ---\n")
        sb.append("Name,Phone,TotalPurchases,TotalPaid,CurrentDue\n")
        for (c in custList) {
            sb.append("\"${c.name}\",\"${c.phone}\",${c.totalPurchases},${c.totalPaid},${c.currentDue}\n")
        }

        sb.append("\n--- SUPPLIERS ---\n")
        sb.append("Name,Company,Phone,TotalBilled,TotalPaid,BalanceOwed\n")
        for (s in suppList) {
            sb.append("\"${s.name}\",\"${s.company}\",\"${s.phone}\",${s.totalBilled},${s.totalPaid},${s.currentBalanceOwed}\n")
        }

        sb.append("\n--- SALES INVOICES ---\n")
        sb.append("InvoiceNumber,Customer,GrandTotal,Paid,Credit,Profit,Date\n")
        for (inv in invList) {
            val d = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(inv.timestamp))
            sb.append("\"${inv.invoiceNumber}\",\"${inv.customerName}\",${inv.grandTotal},${inv.paidAmount},${inv.creditAmount},${inv.totalProfit},\"$d\"\n")
        }

        sb.append("\n--- EXPENSES ---\n")
        sb.append("Category,Amount,Description,Date\n")
        for (e in expList) {
            val d = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(e.timestamp))
            sb.append("\"${e.category}\",${e.amount},\"${e.description}\",\"$d\"\n")
        }

        return sb.toString()
    }
}
