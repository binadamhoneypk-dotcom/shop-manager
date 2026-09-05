package com.example.ui

import com.example.data.entity.ItemEntity

enum class ScreenTab {
    POS,
    INVENTORY,
    CUSTOMERS,
    SUPPLIERS,
    ZAKAT,
    STAFF,
    EXPENSES,
    REPORTS
}

data class CartItem(
    val item: ItemEntity,
    val quantity: Double = 1.0,
    val customSalePrice: Double = item.salePrice,
    val discount: Double = 0.0 // flat discount on this line
) {
    val lineTotal: Double
        get() = ((customSalePrice * quantity) - discount).coerceAtLeast(0.0)

    val lineProfit: Double
        get() = (lineTotal - (item.purchasePrice * quantity))
}

data class CalculatorTarget(
    val fieldName: String,
    val initialValue: String,
    val onResult: (String) -> Unit
)

data class PinDialogState(
    val title: String,
    val description: String = "",
    val onSuccess: () -> Unit
)
