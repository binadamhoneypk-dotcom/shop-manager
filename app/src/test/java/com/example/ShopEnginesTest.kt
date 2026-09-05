package com.example

import com.example.util.CalculatorEngine
import com.example.util.ZakatEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopEnginesTest {

    @Test
    fun testCalculatorArithmetic() {
        val res1 = CalculatorEngine.evaluate("100 + 250 - 50")
        assertEquals(300.0, res1, 0.001)

        val res2 = CalculatorEngine.evaluate("10 * 5 + 20")
        assertEquals(70.0, res2, 0.001)

        val res3 = CalculatorEngine.evaluate("1000 / 4")
        assertEquals(250.0, res3, 0.001)
    }

    @Test
    fun testZakatCalculationEngine() {
        // Stock 500,000, Cash 100,000, Customer Receivables 50,000, Supplier Debt 150,000
        // Gross: 650,000, Net: 500,000
        // Nisab: 150,000
        // Expected Zakat (2.5%): 12,500
        val summary = ZakatEngine.calculate(
            inventoryCostValue = 500000.0,
            inventorySaleValue = 650000.0,
            cashInHand = 100000.0,
            customerReceivables = 50000.0,
            supplierPayables = 150000.0,
            nisabThreshold = 150000.0,
            nisabStartDate = System.currentTimeMillis() - (200L * 24 * 60 * 60 * 1000)
        )

        assertTrue(summary.isSahibENisab)
        assertEquals(500000.0, summary.netZakatableWealth, 0.01)
        assertEquals(12500.0, summary.zakatDue, 0.01)
        assertEquals(200L, summary.daysElapsed)
        assertEquals(154L, summary.daysRemaining)
    }
}
