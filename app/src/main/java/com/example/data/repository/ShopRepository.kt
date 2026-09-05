package com.example.data.repository

import com.example.data.database.ShopDatabase
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

class ShopRepository(private val db: ShopDatabase) {

    // Shop
    fun getAllShops(): Flow<List<ShopEntity>> = db.shopDao().getAllShops()
    suspend fun getShopById(id: String): ShopEntity? = db.shopDao().getShopById(id)
    suspend fun insertShop(shop: ShopEntity) = db.shopDao().insertShop(shop)
    suspend fun updateShop(shop: ShopEntity) = db.shopDao().updateShop(shop)
    suspend fun deleteShop(shop: ShopEntity) = db.shopDao().deleteShop(shop)

    // Staff
    fun getStaffByShop(shopId: String): Flow<List<StaffEntity>> = db.staffDao().getStaffByShop(shopId)
    suspend fun getStaffByPin(shopId: String, pin: String): StaffEntity? = db.staffDao().getStaffByPin(shopId, pin)
    suspend fun insertStaff(staff: StaffEntity) = db.staffDao().insertStaff(staff)
    suspend fun updateStaff(staff: StaffEntity) = db.staffDao().updateStaff(staff)
    suspend fun deleteStaff(staff: StaffEntity) = db.staffDao().deleteStaff(staff)

    // Items
    fun getItemsByShop(shopId: String): Flow<List<ItemEntity>> = db.itemDao().getItemsByShop(shopId)
    fun getLowStockItems(shopId: String): Flow<List<ItemEntity>> = db.itemDao().getLowStockItems(shopId)
    suspend fun getItemById(itemId: String): ItemEntity? = db.itemDao().getItemById(itemId)
    suspend fun getItemByBarcode(shopId: String, barcode: String): ItemEntity? = db.itemDao().getItemByBarcode(shopId, barcode)
    suspend fun insertItem(item: ItemEntity) = db.itemDao().insertItem(item)
    suspend fun updateItem(item: ItemEntity) = db.itemDao().updateItem(item)
    suspend fun deleteItem(item: ItemEntity) = db.itemDao().deleteItem(item)
    suspend fun deductStock(itemId: String, qty: Double) = db.itemDao().deductStock(itemId, qty)

    // Customers
    fun getCustomersByShop(shopId: String): Flow<List<CustomerEntity>> = db.customerDao().getCustomersByShop(shopId)
    suspend fun getCustomerById(customerId: String): CustomerEntity? = db.customerDao().getCustomerById(customerId)
    suspend fun insertCustomer(customer: CustomerEntity) = db.customerDao().insertCustomer(customer)
    suspend fun updateCustomer(customer: CustomerEntity) = db.customerDao().updateCustomer(customer)
    suspend fun deleteCustomer(customer: CustomerEntity) = db.customerDao().deleteCustomer(customer)
    fun getCustomerTransactions(customerId: String): Flow<List<CustomerTransactionEntity>> = db.customerDao().getCustomerTransactions(customerId)
    suspend fun insertCustomerTransaction(tx: CustomerTransactionEntity) = db.customerDao().insertCustomerTransaction(tx)

    // Suppliers
    fun getSuppliersByShop(shopId: String): Flow<List<SupplierEntity>> = db.supplierDao().getSuppliersByShop(shopId)
    suspend fun insertSupplier(supplier: SupplierEntity) = db.supplierDao().insertSupplier(supplier)
    suspend fun updateSupplier(supplier: SupplierEntity) = db.supplierDao().updateSupplier(supplier)
    suspend fun deleteSupplier(supplier: SupplierEntity) = db.supplierDao().deleteSupplier(supplier)
    fun getSupplierTransactions(supplierId: String): Flow<List<SupplierTransactionEntity>> = db.supplierDao().getSupplierTransactions(supplierId)
    suspend fun insertSupplierTransaction(tx: SupplierTransactionEntity) = db.supplierDao().insertSupplierTransaction(tx)

    // Invoices
    fun getInvoicesByShop(shopId: String): Flow<List<InvoiceEntity>> = db.invoiceDao().getInvoicesByShop(shopId)
    suspend fun getInvoiceItems(invoiceId: String): List<InvoiceItemEntity> = db.invoiceDao().getInvoiceItems(invoiceId)
    suspend fun insertInvoice(invoice: InvoiceEntity, items: List<InvoiceItemEntity>) {
        db.invoiceDao().insertInvoice(invoice)
        db.invoiceDao().insertInvoiceItems(items)
        // Deduct inventory stock for non-service items
        for (item in items) {
            db.itemDao().deductStock(item.itemId, item.quantity)
        }
    }

    // Expenses
    fun getExpensesByShop(shopId: String): Flow<List<ExpenseEntity>> = db.expenseDao().getExpensesByShop(shopId)
    suspend fun insertExpense(expense: ExpenseEntity) = db.expenseDao().insertExpense(expense)
    suspend fun deleteExpense(expense: ExpenseEntity) = db.expenseDao().deleteExpense(expense)

    // Attendance & Payroll
    fun getAttendanceByShop(shopId: String): Flow<List<AttendanceEntity>> = db.attendanceDao().getAttendanceByShop(shopId)
    suspend fun insertAttendance(attendance: AttendanceEntity) = db.attendanceDao().insertAttendance(attendance)
    fun getSalaryPayments(shopId: String): Flow<List<SalaryPaymentEntity>> = db.attendanceDao().getSalaryPayments(shopId)
    suspend fun insertSalaryPayment(payment: SalaryPaymentEntity) = db.attendanceDao().insertSalaryPayment(payment)

    // Logs
    fun getLogsByShop(shopId: String): Flow<List<ActivityLogEntity>> = db.activityLogDao().getLogsByShop(shopId)
    suspend fun insertLog(log: ActivityLogEntity) = db.activityLogDao().insertLog(log)
}
