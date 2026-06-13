package com.shop.billing.util

import android.content.Context
import android.util.Base64
import android.util.Log
import com.itextpdf.html2pdf.ConverterProperties
import com.itextpdf.html2pdf.HtmlConverter
import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.BillItem
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    private const val TAG = "PdfGenerator"

    fun generateVectorInvoice(
        context: Context,
        bill: Bill,
        items: List<BillItem>,
        shopName: String,
        shopAddress: String,
        shopPhone: String,
        logoBase64: String? = null,
        invoiceMessage: String = ""
    ): File {
        val file = File(context.cacheDir, "invoices/INVOICE_${bill.billNumber}.pdf")
        file.parentFile?.mkdirs()

        val logoFile = saveLogoToFile(context, logoBase64)
        val html = buildHtml(context, bill, items, shopName, shopAddress, shopPhone, logoFile, invoiceMessage)

        val converterProperties = ConverterProperties()
            .setBaseUri("file:///android_asset/")

        FileOutputStream(file).use { os ->
            HtmlConverter.convertToPdf(html, os, converterProperties)
        }

        Log.d(TAG, "Vector PDF generated: ${file.absolutePath}, size=${file.length()}")
        return file
    }

    private fun saveLogoToFile(context: Context, logoBase64: String?): File? {
        if (logoBase64 == null) return null
        return try {
            val dir = File(context.cacheDir, "logo")
            dir.mkdirs()
            dir.listFiles()?.forEach { it.delete() }
            val bytes = Base64.decode(logoBase64, Base64.NO_WRAP)
            val head = String(bytes, 0, minOf(bytes.size, 200)).lowercase()
            if ("<svg" in head) {
                val file = File(dir, "shop_logo.svg")
                file.writeBytes(bytes)
                Log.d(TAG, "SVG logo saved: ${file.absolutePath}, ${bytes.size} bytes")
                file
            } else {
                val file = File(dir, "shop_logo.png")
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
                val size = minOf(bitmap.width, bitmap.height)
                val x = (bitmap.width - size) / 2
                val y = (bitmap.height - size) / 2
                val cropped = android.graphics.Bitmap.createBitmap(bitmap, x, y, size, size)
                val circular = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(circular)
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
                paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(cropped, 0f, 0f, paint)
                file.outputStream().use { circular.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                file
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save logo to file", e)
            null
        }
    }

    fun buildHtml(
        context: Context,
        bill: Bill,
        items: List<BillItem>,
        shopName: String,
        shopAddress: String,
        shopPhone: String,
        logoFile: File? = null,
        invoiceMessage: String = ""
    ): String {
        val template = readUserTemplate(context)
            ?: context.assets.open("invoice_template.html").bufferedReader().readText()

        val dateStr = DateUtils.formatDate(bill.createdAt)

        val invoiceInfoHtml = if (bill.customerName.isNotBlank() && bill.customerMobile.isNotBlank()) {
            """<div class="info-bar">
                <div class="info-bar-cell">
                  <div class="info-label">Invoice</div>
                  <div class="info-value"><span class="inv-badge">#${escapeXml(bill.billNumber)}</span></div>
                </div>
                <div class="info-bar-cell">
                  <div class="info-label">Date</div>
                  <div class="info-value">$dateStr</div>
                </div>
                <div class="info-bar-cell">
                  <div class="info-label">Customer Name</div>
                  <div class="info-value">${escapeXml(bill.customerName)}</div>
                </div>
                <div class="info-bar-cell">
                  <div class="info-label">Mobile</div>
                  <div class="info-value">${escapeXml(bill.customerMobile)}</div>
                </div>
              </div>"""
        } else if (bill.customerName.isNotBlank()) {
            """<div class="info-bar">
                <div class="info-bar-cell">
                  <div class="info-label">Invoice</div>
                  <div class="info-value"><span class="inv-badge">#${escapeXml(bill.billNumber)}</span></div>
                </div>
                <div class="info-bar-cell">
                  <div class="info-label">Date</div>
                  <div class="info-value">$dateStr</div>
                </div>
                <div class="info-bar-cell">
                  <div class="info-label">Customer Name</div>
                  <div class="info-value">${escapeXml(bill.customerName)}</div>
                </div>
              </div>"""
        } else {
            """<div class="info-bar">
                <div class="info-bar-cell">
                  <div class="info-label">Invoice</div>
                  <div class="info-value"><span class="inv-badge">#${escapeXml(bill.billNumber)}</span></div>
                </div>
                <div class="info-bar-cell right-align">
                  <div class="info-label">Date</div>
                  <div class="info-value">$dateStr</div>
                </div>
              </div>"""
        }

        val logoHtml = if (logoFile != null && logoFile.exists()) {
            """<img src="file://${logoFile.absolutePath}" style="width:70px;height:70px;" />"""
        } else ""

        val itemsHtml = items.mapIndexed { i, item ->
            val evenClass = if (i % 2 == 1) " even" else ""
            """<tr class="$evenClass">
    <td class="tc">${i + 1}</td>
    <td>${escapeXml(item.itemName)}</td>
    <td class="tc">${item.quantity}</td>
    <td class="tr">${String.format("%.0f", item.unitPrice)} ${Constants.CURRENCY_SYMBOL}</td>
    <td class="tr b">${String.format("%.0f", item.subtotal)} ${Constants.CURRENCY_SYMBOL}</td>
</tr>"""
        }.joinToString("\n")

        val subtotal = items.sumOf { it.subtotal }

        return template
            .replace("{{SHOP_NAME}}", escapeXml(shopName))
            .replace("{{SHOP_ADDRESS}}", escapeXml(shopAddress))
            .replace("{{SHOP_PHONE}}", if (shopPhone.isNotBlank()) escapeXml("Phone: $shopPhone") else "")
            .replace("{{SHOP_LOGO}}", logoHtml)
            .replace("{{BILL_NUMBER}}", bill.billNumber)
            .replace("{{INVOICE_INFO_ROW}}", invoiceInfoHtml)
            .replace("{{ITEMS_ROWS}}", itemsHtml)
            .replace("{{CURRENCY}}", Constants.CURRENCY_SYMBOL)
            .replace("{{SUBTOTAL}}", String.format("%.2f", subtotal))
            .replace("{{TOTAL}}", String.format("%.2f", bill.totalAmount))
            .replace("{{INVOICE_MESSAGE}}", if (invoiceMessage.isNotBlank()) escapeXml(invoiceMessage) else "Thank you for your business!")
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    fun getTemplatesDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "ShopBilling")
        dir.mkdirs()
        return dir
    }

    private fun readUserTemplate(context: Context): String? {
        return try {
            val file = File(getTemplatesDir(context), "invoice_template.html")
            if (file.exists()) file.readText() else null
        } catch (_: Exception) { null }
    }

    fun ensureTemplateFilesExist(context: Context) {
        try {
            val dir = getTemplatesDir(context)
            dir.mkdirs()

            val htmlFile = File(dir, "invoice_template.html")
            val html = context.assets.open("invoice_template.html").bufferedReader().readText()
            htmlFile.writeText(html)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create template files", e)
        }
    }
}
