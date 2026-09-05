package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.entity.CustomerEntity
import com.example.data.entity.InvoiceEntity
import com.example.data.entity.InvoiceItemEntity
import com.example.data.entity.ShopEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptHelper {

    fun formatReceiptText(
        shop: ShopEntity,
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        customerOldDue: Double = 0.0,
        lang: AppLanguage
    ): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateStr = sdf.format(Date(invoice.timestamp))

        val builder = StringBuilder()
        builder.append("==============================\n")
        builder.append("       ${shop.name.uppercase()}\n")
        if (shop.phone.isNotBlank()) builder.append("       Tel: ${shop.phone}\n")
        if (shop.address.isNotBlank()) builder.append("       ${shop.address}\n")
        builder.append("==============================\n")
        builder.append("Bill #: ${invoice.invoiceNumber}\n")
        builder.append("Date: $dateStr\n")
        builder.append("Cashier: ${invoice.staffName}\n")
        builder.append("Customer: ${invoice.customerName}\n")
        builder.append("------------------------------\n")
        builder.append(String.format("%-14s %4s %6s %7s\n", "Item", "Qty", "Rate", "Total"))
        builder.append("------------------------------\n")

        for (item in items) {
            val nameTrunc = if (item.itemName.length > 14) item.itemName.take(13) + "." else item.itemName
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toLong().toString() else String.format("%.1f", item.quantity)
            builder.append(String.format("%-14s %4s %6.0f %7.0f\n", nameTrunc, qtyStr, item.salePrice, item.lineTotal))
        }

        builder.append("------------------------------\n")
        builder.append(String.format("%-20s %9.0f\n", "Subtotal:", invoice.subtotal))
        if (invoice.discount > 0) {
            builder.append(String.format("%-20s -%8.0f\n", "Discount:", invoice.discount))
        }
        builder.append(String.format("%-20s %9.0f\n", "GRAND TOTAL:", invoice.grandTotal))
        builder.append("------------------------------\n")
        builder.append(String.format("%-20s %9.0f\n", "Amount Paid:", invoice.paidAmount))

        if (invoice.creditAmount > 0) {
            val totalCustomerDue = customerOldDue + invoice.creditAmount
            builder.append(String.format("%-20s %9.0f\n", "Added to Udhaar:", invoice.creditAmount))
            builder.append(String.format("%-20s %9.0f\n", "Old Udhaar Due:", customerOldDue))
            builder.append("==============================\n")
            builder.append(String.format("%-20s %9.0f\n", "TOTAL DUE NOW:", totalCustomerDue))
        }

        builder.append("==============================\n")
        builder.append(if (lang == AppLanguage.URDU) "خریداری کا شکریہ! تشریف آوری کا ممنون۔\n" else "Thank you for shopping with us!\n")
        return builder.toString()
    }

    fun shareText(context: Context, text: String, title: String = "Share") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, title)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun sendWhatsAppMessage(context: Context, phone: String, message: String) {
        val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
        val formattedPhone = if (cleanPhone.startsWith("0")) {
            // Assume Pakistan +92 if starts with 0
            "92" + cleanPhone.substring(1)
        } else cleanPhone.replace("+", "")

        val uri = if (formattedPhone.isNotBlank()) {
            Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=" + Uri.encode(message))
        } else {
            Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(message))
        }

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to generic share
            shareText(context, message, "Send via WhatsApp")
        }
    }

    fun createCustomerReminderMessage(
        shopName: String,
        customer: CustomerEntity,
        lang: AppLanguage
    ): String {
        return if (lang == AppLanguage.URDU) {
            "محترم ${customer.name} صاحب! السلام علیکم۔ ${shopName} کی طرف سے یاد دہانی ہے کہ آپ کے ذمے بقایا رقم Rs. ${String.format("%,.0f", customer.currentDue)} ہے۔ برائے مہربانی سہولت سے ادا فرمائیں۔ جزاک اللہ خیراً۔"
        } else {
            "Assalam-o-Alaikum ${customer.name}! This is a gentle reminder from $shopName. Your current outstanding balance is Rs. ${String.format("%,.0f", customer.currentDue)}. Please arrange payment at your convenience. Thank you!"
        }
    }
}
