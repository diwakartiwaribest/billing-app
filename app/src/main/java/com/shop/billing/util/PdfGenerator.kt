package com.shop.billing.util

import android.content.Context
import android.util.Base64
import android.util.Log
import com.itextpdf.html2pdf.ConverterProperties
import com.itextpdf.html2pdf.HtmlConverter
import com.shop.billing.data.model.Bill
import com.shop.billing.data.model.BillItem
import com.shop.billing.data.model.CustomerPayment
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

    fun generatePendingInvoicePdf(
        context: Context,
        creditBills: List<Pair<Bill, List<BillItem>>>,
        payments: List<CustomerPayment>,
        shopName: String,
        shopAddress: String,
        shopPhone: String,
        logoBase64: String? = null,
        invoiceMessage: String = "",
        customerName: String,
        customerMobile: String,
        creditAmount: Double = 0.0
    ): File {
        val safeName = customerName.replace(" ", "_").replace("/", "_").replace("\\", "_")
        val file = File(context.cacheDir, "invoices/DUES_INVOICE_${customerMobile}_$safeName.pdf")
        file.parentFile?.mkdirs()

        val logoFile = saveLogoToFile(context, logoBase64)
        val html = buildPendingInvoiceHtml(creditBills, payments, shopName, shopAddress, shopPhone, logoFile, invoiceMessage, customerName, customerMobile, creditAmount)

        val converterProperties = ConverterProperties()
            .setBaseUri("file:///android_asset/")

        FileOutputStream(file).use { os ->
            HtmlConverter.convertToPdf(html, os, converterProperties)
        }

        Log.d(TAG, "Pending invoice PDF generated: ${file.absolutePath}, size=${file.length()}")
        return file
    }

    fun buildPendingInvoiceHtml(
        creditBills: List<Pair<Bill, List<BillItem>>>,
        payments: List<CustomerPayment>,
        shopName: String,
        shopAddress: String,
        shopPhone: String,
        logoFile: File? = null,
        invoiceMessage: String = "",
        customerName: String,
        customerMobile: String,
        creditAmount: Double = 0.0
    ): String {
        val dateStr = DateUtils.formatDate(System.currentTimeMillis())
        val phoneHtml = if (shopPhone.isNotBlank()) escapeXml("Phone: $shopPhone") else ""

        val logoHtml = if (logoFile != null && logoFile.exists()) {
            """<div class="shop-logo"><img src="file://${logoFile.absolutePath}" /></div>"""
        } else ""

        var idx = 0
        val subHeaderHtml = """<tr class="sub-header">
    <th class="tc">#</th>
    <th class="tl">Invoice #</th>
    <th class="tl">Item Description</th>
    <th class="tc">Qty</th>
    <th class="tr">Rate</th>
    <th class="tr">Amount</th>
</tr>"""
        val itemsHtml = creditBills.flatMapIndexed { billIdx, (bill, items) ->
            val separator = if (billIdx > 0) listOf(subHeaderHtml) else emptyList()
            val rows = items.mapIndexed { itemIdx, item ->
                idx++
                val evenClass = if (idx % 2 == 1) "" else " even"
                val invNum = if (itemIdx == 0) escapeXml(bill.billNumber) else ""
                """<tr class="$evenClass">
    <td class="tc">$idx</td>
    <td class="invnum">$invNum</td>
    <td>${escapeXml(item.itemName)}</td>
    <td class="tc">${item.quantity}</td>
    <td class="tr">${String.format("%.0f", item.unitPrice)} ${Constants.CURRENCY_SYMBOL}</td>
    <td class="tr b">${String.format("%.0f", item.subtotal)} ${Constants.CURRENCY_SYMBOL}</td>
</tr>"""
            }
            separator + rows
        }.joinToString("\n")

        val itemsTotal = creditBills.flatMap { (_, items) -> items }.sumOf { it.subtotal }
        val paidTotal = payments.sumOf { it.amount }
        val balance = itemsTotal - paidTotal

        val paymentsHtml = payments.mapIndexed { i, p ->
            val evenClass = if (i % 2 == 1) "" else " even"
            """<tr class="$evenClass">
    <td class="tc">${i + 1}</td>
    <td>${escapeXml(DateUtils.formatDate(p.createdAt))}</td>
    <td>${escapeXml(p.note.ifEmpty { "Payment" })}</td>
    <td class="tr b">${String.format("%.0f", p.amount)} ${Constants.CURRENCY_SYMBOL}</td>
</tr>"""
        }.joinToString("\n")

        val msg = if (invoiceMessage.isNotBlank()) escapeXml(invoiceMessage) else "Thank you for your business!"
        val pendingBodyHtml = if (creditBills.isEmpty()) {
            val creditHtml = if (creditAmount > 0.0) {
                """<table class="credit-row">
        <tr>
          <td class="credit-label">Amount Credited</td>
          <td class="credit-amount">+${String.format("%.2f", creditAmount)} ${Constants.CURRENCY_SYMBOL}</td>
        </tr>
      </table>"""
            } else ""

            """<div class="settled-card">
      <div class="no-dues-box"><div class="settled-title">NO DUES</div></div>
      $creditHtml
    </div>"""
        } else {
            """<div class="section-title">Pending Invoices</div>
    <table class="items">
      <thead>
        <tr>
          <th style="width: 5%" class="tc">#</th>
          <th style="width: 17%" class="tl">Invoice #</th>
          <th style="width: 28%" class="tl">Item Description</th>
          <th style="width: 8%" class="tc">Qty</th>
          <th style="width: 18%" class="tr">Rate</th>
          <th style="width: 24%" class="tr">Amount</th>
        </tr>
      </thead>
      <tbody>
        $itemsHtml
      </tbody>
    </table>

    <hr class="divider-light" />

    <div class="section-title green">Amount Paid</div>
    <table class="payments">
      <thead>
        <tr>
          <th style="width: 8%" class="tc">#</th>
          <th style="width: 25%" class="tl">Date</th>
          <th style="width: 42%" class="tl">Note</th>
          <th style="width: 25%" class="tr">Amount</th>
        </tr>
      </thead>
      <tbody>
        $paymentsHtml
      </tbody>
    </table>

    <div class="totals">
      <table class="totals-table">
        <tr><td class="lbl">Subtotal (Invoices)</td><td class="val">${String.format("%.2f", itemsTotal)} ${Constants.CURRENCY_SYMBOL}</td></tr>
        <tr><td class="lbl">Payments</td><td class="val" style="color:#43a047;">-${String.format("%.2f", paidTotal)} ${Constants.CURRENCY_SYMBOL}</td></tr>
        <tr class="balance-row"><td class="lbl" style="color:#fff!important;">Balance Due</td><td class="val">${String.format("%.2f", balance)} ${Constants.CURRENCY_SYMBOL}</td></tr>
      </table>
    </div>"""
        }

        return """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <style>
    @page { size: A4; margin: 0 0 50px; @bottom-center { content: element(pageFooter); } }
    *, *::before, *::after { margin: 0; padding: 0; box-sizing: border-box; -webkit-print-color-adjust: exact; print-color-adjust: exact; color-adjust: exact; }
    body { font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif; color: #333; background: #fff; font-size: 13px; line-height: 1.6; padding: 20px; }
    .invoice-wrapper { max-width: 780px; margin: 0 auto; background: #fff; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); overflow: hidden; }
    .invoice-body { padding: 20px 32px 4px; }
    .header { background: #fff; color: #333; padding: 20px 28px; display: flex; align-items: center; gap: 20px; border-bottom: 2px solid #227ed4; }
    .shop-logo { width: 70px; height: 70px; flex-shrink: 0; }
    .shop-logo img { width: 100%; height: 100%; display: block; }
    .shop-details { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; text-align: center; }
    .shop-name { font-family: Georgia, 'Palatino Linotype', 'Book Antiqua', serif; font-size: 36px; font-weight: 700; color: #227ed4; letter-spacing: 2px; text-transform: uppercase; line-height: 1.2; }
    .shop-info { font-family: Georgia, 'Palatino Linotype', 'Book Antiqua', serif; color: #666; font-size: 14px; letter-spacing: 0.5px; }
    .info-bar { display: table; width: 100%; background: #f5f8fc; border-radius: 6px; overflow: hidden; margin-bottom: 20px; border: 1px solid #e4edf7; }
    .info-bar-cell { display: table-cell; padding: 14px 20px; vertical-align: middle; border-right: 1px solid #e4edf7; }
    .info-bar-cell:last-child { border-right: none; }
    .info-label { color: #7c8db5; font-size: 9px; text-transform: uppercase; letter-spacing: 1.5px; font-weight: 700; margin-bottom: 4px; }
    .info-value { color: #227ed4; font-weight: 700; font-size: 13px; letter-spacing: 0.5px; }
    .inv-badge { display: inline-block; background: #227ed4; color: #fff; font-size: 12px; font-weight: 700; padding: 4px 14px; border-radius: 4px; letter-spacing: 1px; }
    hr.divider { border: none; border-top: 2px solid #e4edf7; margin: 16px 0; }
    hr.divider-light { border: none; border-top: 1px solid #eceff1; margin: 12px 0; }
    table.items { width: 100%; border-collapse: collapse; border-radius: 6px; overflow: hidden; border: 1px solid #e4edf7; }
    table.items thead th { background: #227ed4 !important; color: #fff !important; padding: 6px 10px; font-family: Georgia, 'Palatino Linotype', 'Book Antiqua', serif; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 1.5px; border: none; }
    table.items tbody td { padding: 6px 10px; font-family: Georgia, 'Palatino Linotype', 'Book Antiqua', serif; font-size: 12px; border: none; border-bottom: 1px solid #f0f1f5; }
    table.items tbody tr:last-child td { border-bottom: none; }
    table.items tr.even td { background: #f5f8fc !important; }
    table.items tbody td:first-child { color: #333; font-weight: 600; text-align: center; }
    table.items tbody td:nth-child(2) { text-align: left; font-weight: 600; color: #227ed4; }
    table.items tbody tr.sub-header td,
    table.items tbody tr.sub-header th { background: #227ed4 !important; color: #fff !important; padding: 6px 10px; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 1.5px; border: none; border-bottom: 1px solid #1a66b0; }
    .section-title { font-family: Georgia, 'Palatino Linotype', 'Book Antiqua', serif; font-size: 14px; font-weight: 700; color: #227ed4; margin-bottom: 10px; letter-spacing: 1px; text-transform: uppercase; }
    .section-title.green { color: #43a047 !important; }
    table.payments { width: 100%; border-collapse: collapse; border-radius: 6px; overflow: hidden; border: 1px solid #c8e6c9; }
    table.payments thead th { background: #81c784 !important; color: #fff !important; padding: 6px 10px; font-family: Georgia, 'Palatino Linotype', 'Book Antiqua', serif; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 1.5px; border: none; }
    table.payments tbody td { padding: 6px 10px; font-family: Georgia, 'Palatino Linotype', 'Book Antiqua', serif; font-size: 12px; border: none; border-bottom: 1px solid #e8f5e9; }
    table.payments tbody tr:last-child td { border-bottom: none; }
    table.payments tr.even td { background: #c8e6c9 !important; }
    .settled-card { text-align: center; }
    .no-dues-box { border: 1px solid #d7e9fb; background: #f8fbff; border-radius: 10px; padding: 20px 22px; }
    .settled-title { font-family: Georgia, 'Palatino Linotype', 'Book Antiqua', serif; font-size: 36px; font-weight: 700; color: #227ed4; letter-spacing: 3px; line-height: 1; margin: 0; }
    table.credit-row { width: 100%; border-collapse: collapse; margin-top: 4px; background: #eaf5ff !important; border: 1px solid #b7dbff; border-radius: 6px; }
    table.credit-row td { padding: 8px 14px; font-family: Georgia, 'Palatino Linotype', 'Book Antiqua', serif; color: #1565c0; font-weight: 700; vertical-align: middle; line-height: 1.1; }
    table.credit-row .credit-label { text-align: left; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; }
    table.credit-row .credit-amount { text-align: right; font-size: 16px; white-space: nowrap; }
    .totals { text-align: right; }
    table.totals-table { border-collapse: collapse; margin-left: auto; width: 260px; border: 1px solid #e4edf7; border-top: none; border-radius: 0 0 6px 6px; overflow: hidden; }
    table.totals-table td { padding: 10px 16px; border: none; font-size: 12px; }
    table.totals-table tr:not(:last-child) td { border-bottom: 1px solid #f0f1f5; }
    table.totals-table .lbl { color: #7c8db5; text-align: left; font-weight: 500; }
    table.totals-table .val { text-align: right; font-weight: 700; color: #333; }
    table.totals-table .grand td { background: #227ed4 !important; color: #fff !important; font-size: 14px; font-weight: 700; padding: 12px 16px; letter-spacing: 1px; }
    .balance-row td { background: #227ed4 !important; color: #fff !important; font-size: 14px; font-weight: 700; padding: 12px 16px; }
    .balance-row td .lbl { color: #fff !important; }
    #pageFooter { position: running(pageFooter); text-align: center; padding: 6px 20px 4px; border-top: 2px solid #e4edf7; color: #227ed4; font-size: 11px; font-weight: 600; letter-spacing: 0.5px; }
    .sub { color: #b0bec5; font-size: 10px; font-weight: 400; letter-spacing: 0.3px; }
    #pageFooter { position: running(pageFooter); text-align: center; padding: 6px 20px 4px; border-top: 2px solid #e4edf7; color: #227ed4; font-size: 11px; font-weight: 600; letter-spacing: 0.5px; }
    .tc { text-align: center; } .tr { text-align: right; } .tl { text-align: left; } .b { font-weight: bold; }
  </style>
</head>
<body>
<div class="invoice-wrapper">
  <div class="header">
    $logoHtml
    <div class="shop-details">
      <div class="shop-name">${escapeXml(shopName)}</div>
      <div class="shop-info">${escapeXml(shopAddress)} &nbsp;|&nbsp; $phoneHtml</div>
    </div>
  </div>
  <div class="invoice-body">
    <div class="info-bar">
      <div class="info-bar-cell">
        <div class="info-label">Invoice</div>
        <div class="info-value"><span class="inv-badge">PENDING</span></div>
      </div>
      <div class="info-bar-cell">
        <div class="info-label">Date</div>
        <div class="info-value">$dateStr</div>
      </div>
      <div class="info-bar-cell">
        <div class="info-label">Customer Name</div>
        <div class="info-value">${escapeXml(customerName)}</div>
      </div>
      <div class="info-bar-cell">
        <div class="info-label">Mobile</div>
        <div class="info-value">${escapeXml(customerMobile)}</div>
      </div>
    </div>
    <hr class="divider" />

    $pendingBodyHtml

  </div>
</div>
<div id="pageFooter">
    $msg
    <div style="color: #b0bec5; font-size: 9px; font-weight: 400; letter-spacing: 0.3px;">This is a computer-generated invoice</div>
</div>
</body>
</html>"""
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
