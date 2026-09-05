package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey val id: String,
    val name: String,
    val businessType: String, // General Store, Pharmacy, Restaurant, Auto Workshop, etc.
    val currency: String = "Rs.",
    val phone: String = "",
    val address: String = "",
    val nisabThreshold: Double = 150000.0, // Default in PKR
    val nisabStartDate: Long = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000), // ~6 months ago by default
    val cashInHand: Double = 25000.0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class StaffRole {
    OWNER,
    MANAGER,
    CASHIER
}

@Entity(
    tableName = "staff",
    indices = [Index(value = ["shopId"])]
)
data class StaffEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val name: String,
    val phone: String = "",
    val role: StaffRole = StaffRole.CASHIER,
    val pin: String = "1234", // 4-digit PIN
    val wageRate: Double = 25000.0, // Monthly or daily
    val wageType: String = "MONTHLY", // MONTHLY or DAILY
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "items",
    indices = [Index(value = ["shopId"]), Index(value = ["barcode"])]
)
data class ItemEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val name: String,
    val nameUrdu: String = "",
    val category: String = "General",
    val unit: String = "pcs", // pcs, kg, litre, box, hour
    val purchasePrice: Double,
    val salePrice: Double,
    val quantity: Double,
    val lowStockThreshold: Double = 5.0,
    val barcode: String = "",
    val isService: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customers",
    indices = [Index(value = ["shopId"]), Index(value = ["phone"])]
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val totalPurchases: Double = 0.0,
    val totalPaid: Double = 0.0,
    val currentDue: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customer_transactions",
    indices = [Index(value = ["shopId"]), Index(value = ["customerId"])]
)
data class CustomerTransactionEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val customerId: String,
    val type: String, // SALE_CREDIT, PAYMENT_RECEIVED, ADJUSTMENT
    val amount: Double,
    val oldBalance: Double,
    val newBalance: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val invoiceId: String? = null
)

@Entity(
    tableName = "suppliers",
    indices = [Index(value = ["shopId"])]
)
data class SupplierEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val name: String,
    val phone: String = "",
    val company: String = "",
    val totalBilled: Double = 0.0,
    val totalPaid: Double = 0.0,
    val currentBalanceOwed: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "supplier_transactions",
    indices = [Index(value = ["shopId"]), Index(value = ["supplierId"])]
)
data class SupplierTransactionEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val supplierId: String,
    val type: String, // PURCHASE_CREDIT, PAYMENT_MADE, ADJUSTMENT
    val amount: Double,
    val oldBalance: Double,
    val newBalance: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "invoices",
    indices = [Index(value = ["shopId"]), Index(value = ["invoiceNumber"])]
)
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val invoiceNumber: String,
    val customerId: String? = null,
    val customerName: String = "Cash Walk-In",
    val subtotal: Double,
    val discount: Double = 0.0,
    val grandTotal: Double,
    val paidAmount: Double,
    val creditAmount: Double = 0.0, // Remaining added to udhaar
    val paymentType: String = "CASH", // CASH, CREDIT, PARTIAL
    val staffId: String = "",
    val staffName: String = "",
    val totalProfit: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "invoice_items",
    indices = [Index(value = ["invoiceId"])]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: String,
    val itemId: String,
    val itemName: String,
    val unit: String = "pcs",
    val quantity: Double,
    val purchasePrice: Double,
    val salePrice: Double,
    val itemDiscount: Double = 0.0,
    val lineTotal: Double,
    val lineProfit: Double
)

@Entity(
    tableName = "expenses",
    indices = [Index(value = ["shopId"])]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val category: String, // Rent, Electricity, Salaries, Misc, etc.
    val amount: Double,
    val description: String = "",
    val staffName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance",
    indices = [Index(value = ["shopId"]), Index(value = ["staffId"]), Index(value = ["date"])]
)
data class AttendanceEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val staffId: String,
    val staffName: String,
    val date: String, // YYYY-MM-DD
    val status: String, // PRESENT, ABSENT, HALF_DAY, LEAVE
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "salary_payments",
    indices = [Index(value = ["shopId"]), Index(value = ["staffId"])]
)
data class SalaryPaymentEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val staffId: String,
    val staffName: String,
    val amount: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "activity_logs",
    indices = [Index(value = ["shopId"])]
)
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: String,
    val staffName: String,
    val actionType: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
