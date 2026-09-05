package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    @Query("SELECT * FROM shops ORDER BY createdAt ASC")
    fun getAllShops(): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops WHERE id = :shopId LIMIT 1")
    suspend fun getShopById(shopId: String): ShopEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: ShopEntity)

    @Update
    suspend fun updateShop(shop: ShopEntity)

    @Delete
    suspend fun deleteShop(shop: ShopEntity)
}

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff WHERE shopId = :shopId AND active = 1 ORDER BY name ASC")
    fun getStaffByShop(shopId: String): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE shopId = :shopId AND pin = :pin AND active = 1 LIMIT 1")
    suspend fun getStaffByPin(shopId: String, pin: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE id = :staffId LIMIT 1")
    suspend fun getStaffById(staffId: String): StaffEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffEntity)

    @Update
    suspend fun updateStaff(staff: StaffEntity)

    @Delete
    suspend fun deleteStaff(staff: StaffEntity)
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE shopId = :shopId ORDER BY name ASC")
    fun getItemsByShop(shopId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE shopId = :shopId AND isService = 0 AND quantity <= lowStockThreshold ORDER BY quantity ASC")
    fun getLowStockItems(shopId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
    suspend fun getItemById(itemId: String): ItemEntity?

    @Query("SELECT * FROM items WHERE shopId = :shopId AND barcode = :barcode LIMIT 1")
    suspend fun getItemByBarcode(shopId: String, barcode: String): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>)

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("UPDATE items SET quantity = quantity - :qtyDeduction WHERE id = :itemId")
    suspend fun deductStock(itemId: String, qtyDeduction: Double)

    @Delete
    suspend fun deleteItem(item: ItemEntity)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE shopId = :shopId ORDER BY updatedAt DESC")
    fun getCustomersByShop(shopId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :customerId LIMIT 1")
    suspend fun getCustomerById(customerId: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM customer_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getCustomerTransactions(customerId: String): Flow<List<CustomerTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerTransaction(transaction: CustomerTransactionEntity)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers WHERE shopId = :shopId ORDER BY name ASC")
    fun getSuppliersByShop(shopId: String): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :supplierId LIMIT 1")
    suspend fun getSupplierById(supplierId: String): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity)

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Delete
    suspend fun deleteSupplier(supplier: SupplierEntity)

    @Query("SELECT * FROM supplier_transactions WHERE supplierId = :supplierId ORDER BY timestamp DESC")
    fun getSupplierTransactions(supplierId: String): Flow<List<SupplierTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplierTransaction(transaction: SupplierTransactionEntity)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices WHERE shopId = :shopId ORDER BY timestamp DESC")
    fun getInvoicesByShop(shopId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceItems(invoiceId: String): List<InvoiceItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE shopId = :shopId ORDER BY timestamp DESC")
    fun getExpensesByShop(shopId: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE shopId = :shopId ORDER BY date DESC, timestamp DESC")
    fun getAttendanceByShop(shopId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE shopId = :shopId AND staffId = :staffId ORDER BY date DESC")
    fun getAttendanceForStaff(shopId: String, staffId: String): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Query("SELECT * FROM salary_payments WHERE shopId = :shopId ORDER BY timestamp DESC")
    fun getSalaryPayments(shopId: String): Flow<List<SalaryPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalaryPayment(payment: SalaryPaymentEntity)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs WHERE shopId = :shopId ORDER BY timestamp DESC LIMIT 50")
    fun getLogsByShop(shopId: String): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity)
}
