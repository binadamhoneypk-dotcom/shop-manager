package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        ShopEntity::class,
        StaffEntity::class,
        ItemEntity::class,
        CustomerEntity::class,
        CustomerTransactionEntity::class,
        SupplierEntity::class,
        SupplierTransactionEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        ExpenseEntity::class,
        AttendanceEntity::class,
        SalaryPaymentEntity::class,
        ActivityLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ShopDatabase : RoomDatabase() {
    abstract fun shopDao(): ShopDao
    abstract fun staffDao(): StaffDao
    abstract fun itemDao(): ItemDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: ShopDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): ShopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShopDatabase::class.java,
                    "shop_manager_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: ShopDatabase) {
            val shopDao = database.shopDao()
            val staffDao = database.staffDao()
            val itemDao = database.itemDao()
            val customerDao = database.customerDao()
            val supplierDao = database.supplierDao()
            val expenseDao = database.expenseDao()

            // 1. Al-Madina Superstore
            val shop1Id = "shop-general-01"
            val shop1 = ShopEntity(
                id = shop1Id,
                name = "Al-Madina Superstore",
                businessType = "General Store",
                currency = "Rs.",
                phone = "+92 300 1234567",
                address = "Main Commercial Market, Block B",
                nisabThreshold = 150000.0,
                nisabStartDate = System.currentTimeMillis() - (200L * 24 * 60 * 60 * 1000), // ~200 days elapsed
                cashInHand = 45000.0
            )
            shopDao.insertShop(shop1)

            // Staff for Shop 1
            val owner1 = StaffEntity(
                id = "staff-01",
                shopId = shop1Id,
                name = "Haji Usman",
                phone = "03001234567",
                role = StaffRole.OWNER,
                pin = "1234",
                wageRate = 0.0,
                wageType = "MONTHLY"
            )
            val cashier1 = StaffEntity(
                id = "staff-02",
                shopId = shop1Id,
                name = "Bilal Ahmed",
                phone = "03129876543",
                role = StaffRole.CASHIER,
                pin = "0000",
                wageRate = 28000.0,
                wageType = "MONTHLY"
            )
            val manager1 = StaffEntity(
                id = "staff-03",
                shopId = shop1Id,
                name = "Tariq Mahmood",
                phone = "03335554433",
                role = StaffRole.MANAGER,
                pin = "2222",
                wageRate = 45000.0,
                wageType = "MONTHLY"
            )
            staffDao.insertStaff(owner1)
            staffDao.insertStaff(cashier1)
            staffDao.insertStaff(manager1)

            // Items for Shop 1
            val itemsShop1 = listOf(
                ItemEntity(
                    id = UUID.randomUUID().toString(),
                    shopId = shop1Id,
                    name = "Basmati Rice Super Kernel",
                    nameUrdu = "سپر کرینل باسمتی چاول",
                    category = "Grocery",
                    unit = "kg",
                    purchasePrice = 280.0,
                    salePrice = 340.0,
                    quantity = 120.0,
                    lowStockThreshold = 20.0,
                    barcode = "8964001"
                ),
                ItemEntity(
                    id = UUID.randomUUID().toString(),
                    shopId = shop1Id,
                    name = "Dal Chana Special",
                    nameUrdu = "دال چنا اسپیشل",
                    category = "Grocery",
                    unit = "kg",
                    purchasePrice = 220.0,
                    salePrice = 270.0,
                    quantity = 4.0, // Low stock!
                    lowStockThreshold = 10.0,
                    barcode = "8964002"
                ),
                ItemEntity(
                    id = UUID.randomUUID().toString(),
                    shopId = shop1Id,
                    name = "Cooking Oil 5L Can",
                    nameUrdu = "کوکنگ آئل 5 لیٹر",
                    category = "Oil & Ghee",
                    unit = "can",
                    purchasePrice = 2300.0,
                    salePrice = 2650.0,
                    quantity = 25.0,
                    lowStockThreshold = 5.0,
                    barcode = "8964003"
                ),
                ItemEntity(
                    id = UUID.randomUUID().toString(),
                    shopId = shop1Id,
                    name = "Supreme Tea 950g",
                    nameUrdu = "چائے پتی 950 گرام",
                    category = "Beverages",
                    unit = "packet",
                    purchasePrice = 1450.0,
                    salePrice = 1680.0,
                    quantity = 30.0,
                    lowStockThreshold = 8.0,
                    barcode = "8964004"
                ),
                ItemEntity(
                    id = UUID.randomUUID().toString(),
                    shopId = shop1Id,
                    name = "White Sugar Premium",
                    nameUrdu = "صاف ستھری چینی",
                    category = "Grocery",
                    unit = "kg",
                    purchasePrice = 135.0,
                    salePrice = 150.0,
                    quantity = 150.0,
                    lowStockThreshold = 30.0,
                    barcode = "8964005"
                ),
                ItemEntity(
                    id = UUID.randomUUID().toString(),
                    shopId = shop1Id,
                    name = "Home Delivery Charge",
                    nameUrdu = "ہوم ڈیلیوری فیس",
                    category = "Services",
                    unit = "service",
                    purchasePrice = 0.0,
                    salePrice = 150.0,
                    quantity = 999.0,
                    isService = true
                )
            )
            itemDao.insertItems(itemsShop1)

            // Customers for Shop 1
            val cust1 = CustomerEntity(
                id = "cust-01",
                shopId = shop1Id,
                name = "Zaid Khan",
                phone = "0301-1122334",
                address = "House 12, Street 4",
                totalPurchases = 14500.0,
                totalPaid = 13000.0,
                currentDue = 1500.0
            )
            val cust2 = CustomerEntity(
                id = "cust-02",
                shopId = shop1Id,
                name = "Malik Arshad",
                phone = "0321-4455667",
                address = "Flat 302, Green Heights",
                totalPurchases = 28900.0,
                totalPaid = 25000.0,
                currentDue = 3900.0
            )
            val cust3 = CustomerEntity(
                id = "cust-03",
                shopId = shop1Id,
                name = "Dr. Farooq",
                phone = "0345-9988776",
                address = "Corner Clinic, Block C",
                totalPurchases = 8500.0,
                totalPaid = 8500.0,
                currentDue = 0.0
            )
            customerDao.insertCustomer(cust1)
            customerDao.insertCustomer(cust2)
            customerDao.insertCustomer(cust3)

            // Suppliers for Shop 1
            val supp1 = SupplierEntity(
                id = "supp-01",
                shopId = shop1Id,
                name = "Hassan Ghee Mills Ltd.",
                phone = "0300-8889900",
                company = "Hassan Distributors",
                totalBilled = 120000.0,
                totalPaid = 105000.0,
                currentBalanceOwed = 15000.0
            )
            val supp2 = SupplierEntity(
                id = "supp-02",
                shopId = shop1Id,
                name = "Lahore Wholesale Grain Market",
                phone = "0322-7776655",
                company = "Al-Rahim Traders",
                totalBilled = 85000.0,
                totalPaid = 75000.0,
                currentBalanceOwed = 10000.0
            )
            supplierDao.insertSupplier(supp1)
            supplierDao.insertSupplier(supp2)

            // Expenses for Shop 1
            expenseDao.insertExpense(
                ExpenseEntity(
                    id = UUID.randomUUID().toString(),
                    shopId = shop1Id,
                    category = "Electricity",
                    amount = 6800.0,
                    description = "Electricity bill for shop meter",
                    staffName = "Haji Usman"
                )
            )

            // 2. Al-Razi Medical Store (Pharmacy)
            val shop2Id = "shop-pharmacy-02"
            val shop2 = ShopEntity(
                id = shop2Id,
                name = "Al-Razi Pharmacy",
                businessType = "Pharmacy",
                currency = "Rs.",
                phone = "+92 313 7654321",
                address = "Near City Hospital Gate 2",
                nisabThreshold = 150000.0,
                nisabStartDate = System.currentTimeMillis() - (120L * 24 * 60 * 60 * 1000),
                cashInHand = 62000.0
            )
            shopDao.insertShop(shop2)

            staffDao.insertStaff(
                StaffEntity(
                    id = "staff-pharma-01",
                    shopId = shop2Id,
                    name = "Dr. Kamran (Pharm-D)",
                    phone = "03137654321",
                    role = StaffRole.OWNER,
                    pin = "1234"
                )
            )

            itemDao.insertItems(
                listOf(
                    ItemEntity(
                        id = UUID.randomUUID().toString(),
                        shopId = shop2Id,
                        name = "Panadol Extra 500mg (Box of 200)",
                        nameUrdu = "پیناڈول ایکسٹرا گولیاں",
                        category = "Analgesics",
                        unit = "box",
                        purchasePrice = 850.0,
                        salePrice = 980.0,
                        quantity = 40.0,
                        lowStockThreshold = 10.0,
                        barcode = "50001"
                    ),
                    ItemEntity(
                        id = UUID.randomUUID().toString(),
                        shopId = shop2Id,
                        name = "Augmentin 625mg Tablets",
                        nameUrdu = "اوگمنٹن 625 اینٹی بائیوٹک",
                        category = "Antibiotics",
                        unit = "pack",
                        purchasePrice = 420.0,
                        salePrice = 490.0,
                        quantity = 3.0, // Low stock
                        lowStockThreshold = 8.0,
                        barcode = "50002"
                    ),
                    ItemEntity(
                        id = UUID.randomUUID().toString(),
                        shopId = shop2Id,
                        name = "Digital BP Monitor Automatic",
                        nameUrdu = "ڈیجیٹل بلڈ پریشر مشین",
                        category = "Devices",
                        unit = "pcs",
                        purchasePrice = 3200.0,
                        salePrice = 4200.0,
                        quantity = 12.0,
                        lowStockThreshold = 3.0,
                        barcode = "50003"
                    )
                )
            )

            // 3. Falcon Auto Workshop
            val shop3Id = "shop-auto-03"
            val shop3 = ShopEntity(
                id = shop3Id,
                name = "Falcon Auto Workshop",
                businessType = "Auto Workshop",
                currency = "Rs.",
                phone = "+92 345 1122445",
                address = "Auto Market Plot 48",
                nisabThreshold = 150000.0,
                nisabStartDate = System.currentTimeMillis() - (300L * 24 * 60 * 60 * 1000),
                cashInHand = 38000.0
            )
            shopDao.insertShop(shop3)

            staffDao.insertStaff(
                StaffEntity(
                    id = "staff-auto-01",
                    shopId = shop3Id,
                    name = "Ustad Jameel",
                    phone = "03451122445",
                    role = StaffRole.OWNER,
                    pin = "1234"
                )
            )

            itemDao.insertItems(
                listOf(
                    ItemEntity(
                        id = UUID.randomUUID().toString(),
                        shopId = shop3Id,
                        name = "Engine Oil 20W-50 4L",
                        nameUrdu = "انجن آئل 4 لیٹر",
                        category = "Lubricants",
                        unit = "can",
                        purchasePrice = 3800.0,
                        salePrice = 4600.0,
                        quantity = 18.0,
                        barcode = "70001"
                    ),
                    ItemEntity(
                        id = UUID.randomUUID().toString(),
                        shopId = shop3Id,
                        name = "Complete Engine Tuning & Service",
                        nameUrdu = "کمپلیٹ انجن ٹیوننگ و سروس",
                        category = "Labor / Service",
                        unit = "service",
                        purchasePrice = 0.0,
                        salePrice = 2500.0,
                        quantity = 999.0,
                        isService = true
                    ),
                    ItemEntity(
                        id = UUID.randomUUID().toString(),
                        shopId = shop3Id,
                        name = "Front Brake Pads Set (Corolla)",
                        nameUrdu = "فرنٹ بریک پیڈ سیٹ",
                        category = "Spare Parts",
                        unit = "set",
                        purchasePrice = 2100.0,
                        salePrice = 2900.0,
                        quantity = 6.0,
                        barcode = "70002"
                    )
                )
            )
        }
    }
}
