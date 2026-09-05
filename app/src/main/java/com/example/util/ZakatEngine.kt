package com.example.util

import kotlin.math.max

data class ZakatSummary(
    val inventoryCostValue: Double,
    val inventorySaleValue: Double,
    val cashInHand: Double,
    val customerReceivables: Double,
    val supplierPayables: Double,
    val grossAssets: Double,
    val netZakatableWealth: Double,
    val nisabThreshold: Double,
    val isSahibENisab: Boolean,
    val nisabStartDate: Long,
    val daysElapsed: Long,
    val daysRemaining: Long,
    val isHawlCompleted: Boolean,
    val zakatDue: Double
)

object ZakatEngine {

    fun calculate(
        inventoryCostValue: Double,
        inventorySaleValue: Double,
        cashInHand: Double,
        customerReceivables: Double,
        supplierPayables: Double,
        nisabThreshold: Double,
        nisabStartDate: Long,
        currentTime: Long = System.currentTimeMillis()
    ): ZakatSummary {
        val gross = inventoryCostValue + cashInHand + customerReceivables
        val net = max(0.0, gross - supplierPayables)
        val isSahib = net >= nisabThreshold

        val diffMillis = max(0L, currentTime - nisabStartDate)
        val daysElapsed = diffMillis / (1000L * 60 * 60 * 24)
        val hawlCycle = 354L // Islamic Lunar Year
        val daysRemaining = if (daysElapsed >= hawlCycle) 0L else (hawlCycle - daysElapsed)
        val hawlComplete = daysElapsed >= hawlCycle

        // Zakat is 2.5% of net zakatable assets
        val zakat = if (isSahib) net * 0.025 else 0.0

        return ZakatSummary(
            inventoryCostValue = inventoryCostValue,
            inventorySaleValue = inventorySaleValue,
            cashInHand = cashInHand,
            customerReceivables = customerReceivables,
            supplierPayables = supplierPayables,
            grossAssets = gross,
            netZakatableWealth = net,
            nisabThreshold = nisabThreshold,
            isSahibENisab = isSahib,
            nisabStartDate = nisabStartDate,
            daysElapsed = daysElapsed,
            daysRemaining = daysRemaining,
            isHawlCompleted = hawlComplete,
            zakatDue = zakat
        )
    }

    fun generateStatement(summary: ZakatSummary, shopName: String, lang: AppLanguage): String {
        return if (lang == AppLanguage.URDU) {
            """
            *زکوٰۃ مالِ تجارت گوشوارہ برائے: $shopName*
            ------------------------------------------
            📦 مال تجارت (قیمت خرید): Rs. ${String.format("%,.0f", summary.inventoryCostValue)}
            💵 دکان میں موجود نقد کیش: Rs. ${String.format("%,.0f", summary.cashInHand)}
            🤝 گاہکوں سے وصول طلب ادھار: Rs. ${String.format("%,.0f", summary.customerReceivables)}
            ------------------------------------------
            کل قابلِ زکوٰۃ اثاثے: Rs. ${String.format("%,.0f", summary.grossAssets)}
            منفی: سپلائرز کو واجب الادا قرض: Rs. ${String.format("%,.0f", summary.supplierPayables)}
            ==========================================
            خالص قابلِ زکوٰۃ مالیت: Rs. ${String.format("%,.0f", summary.netZakatableWealth)}
            مقررہ نصاب کی حد: Rs. ${String.format("%,.0f", summary.nisabThreshold)}
            حیثیت: ${if (summary.isSahibENisab) "صاحبِ نصاب (الحمدللہ)" else "نصاب سے کم"}
            سال (حول) کی تکمیل: ${summary.daysElapsed} دن گزرے (${summary.daysRemaining} دن باقی)
            ==========================================
            *واجب الادا زکوٰۃ (2.5٪): Rs. ${String.format("%,.0f", summary.zakatDue)}*
            """.trimIndent()
        } else {
            """
            *Zakat Commercial Wealth Statement — $shopName*
            ------------------------------------------
            📦 Commercial Inventory (Cost): Rs. ${String.format("%,.0f", summary.inventoryCostValue)}
            💵 Liquid Cash in Hand: Rs. ${String.format("%,.0f", summary.cashInHand)}
            🤝 Customer Receivables (Udhaar): Rs. ${String.format("%,.0f", summary.customerReceivables)}
            ------------------------------------------
            Total Gross Trading Assets: Rs. ${String.format("%,.0f", summary.grossAssets)}
            Less: Supplier Immediate Debts: Rs. ${String.format("%,.0f", summary.supplierPayables)}
            ==========================================
            Net Zakatable Wealth: Rs. ${String.format("%,.0f", summary.netZakatableWealth)}
            Nisab Threshold: Rs. ${String.format("%,.0f", summary.nisabThreshold)}
            Status: ${if (summary.isSahibENisab) "Sahib-e-Nisab (Eligible for Zakat)" else "Below Nisab"}
            Hawl Cycle (Lunar Year): ${summary.daysElapsed} days elapsed (${summary.daysRemaining} days left)
            ==========================================
            *Estimated Zakat Due (2.5%): Rs. ${String.format("%,.0f", summary.zakatDue)}*
            """.trimIndent()
        }
    }
}
