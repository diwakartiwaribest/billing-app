package com.shop.billing.ui.screens.newbill

import android.app.Activity
import android.content.Intent
import android.media.MediaActionSound
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.graphics.Bitmap
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import java.util.HashMap
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.shop.billing.R
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.shop.billing.data.repository.ProductRepository
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class Entry(val barcode: String, var quantity: Int)

@AndroidEntryPoint
class ContinuousScannerActivity : ComponentActivity() {

    @Inject lateinit var productRepository: ProductRepository

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var itemCountText: TextView
    private lateinit var barcodeImage: ImageView
    private lateinit var barcodeNumberText: TextView
    private lateinit var errorLabel: TextView
    private lateinit var productInfoLayout: RelativeLayout
    private lateinit var productNameLabel: TextView
    private lateinit var productPriceLabel: TextView
    private lateinit var productStockLabel: TextView
    private lateinit var productCategoryLabel: TextView
    private lateinit var qtyEditText: EditText
    private lateinit var navBackButton: Button
    private lateinit var navForwardButton: Button
    private lateinit var scanNextButton: Button
    private lateinit var rescanButton: Button
    private lateinit var doneButton: Button

    private val entries = mutableListOf<Entry>()
    private var currentIndex = -1
    private var isScanning = false
    private var replaceMode = false
    private var unknownScanned = false
    private var unknownBarcode = ""
    private var ignoreQtyChange = false
    private var knownBarcodes: Set<String> = emptySet()
    private var isSingleScan = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_continuous_scanner)

        knownBarcodes = intent?.getStringArrayListExtra("KNOWN_BARCODES")?.map { it.trim() }?.toSet() ?: emptySet()
        isSingleScan = intent?.getBooleanExtra("SINGLE_SCAN_MODE", false) ?: false

        barcodeView = findViewById(R.id.barcodeView)
        itemCountText = findViewById(R.id.itemCountText)
        barcodeImage = findViewById(R.id.barcodeImage)
        barcodeNumberText = findViewById(R.id.barcodeNumberText)
        errorLabel = findViewById(R.id.errorLabel)
        productInfoLayout = findViewById(R.id.productInfoLayout)
        productNameLabel = findViewById(R.id.productNameLabel)
        productPriceLabel = findViewById(R.id.productPriceLabel)
        productStockLabel = findViewById(R.id.productStockLabel)
        productCategoryLabel = findViewById(R.id.productCategoryLabel)
        qtyEditText = findViewById(R.id.qtyEditText)
        navBackButton = findViewById(R.id.navBackButton)
        navForwardButton = findViewById(R.id.navForwardButton)
        scanNextButton = findViewById(R.id.scanNextButton)
        rescanButton = findViewById(R.id.rescanButton)
        doneButton = findViewById(R.id.doneButton)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT < 30) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        findViewById<TextView>(R.id.closeButton).setOnClickListener { closeAndReturn() }

        findViewById<Button>(R.id.qtyMinusButton).setOnClickListener {
            val v = qtyEditText.text.toString().toIntOrNull() ?: 1
            if (v > 1) {
                ignoreQtyChange = true
                qtyEditText.setText("${v - 1}")
            }
        }
        findViewById<Button>(R.id.qtyPlusButton).setOnClickListener {
            val v = qtyEditText.text.toString().toIntOrNull() ?: 1
            ignoreQtyChange = true
            qtyEditText.setText("${v + 1}")
        }
        qtyEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (ignoreQtyChange) { ignoreQtyChange = false; return }
                val raw = s?.toString() ?: ""
                val digits = raw.filter { it.isDigit() }
                if (digits.isEmpty()) {
                    ignoreQtyChange = true
                    qtyEditText.setText("")
                } else if (digits.toIntOrNull() == 0) {
                    ignoreQtyChange = true
                    qtyEditText.setText("")
                } else if (digits != raw) {
                    ignoreQtyChange = true
                    qtyEditText.setText(digits)
                }
            }
        })

        navBackButton.setOnClickListener { navigateTo(currentIndex - 1) }
        navForwardButton.setOnClickListener { navigateTo(currentIndex + 1) }

        rescanButton.setOnClickListener {
            saveCurrentQty()
            replaceMode = true
            startSingleScan()
        }

        scanNextButton.setOnClickListener {
            when (scanNextButton.text) {
                "Add & Scan Next", "Scan Next" -> {
                    saveCurrentQty()
                    startSingleScan()
                }
                "Done" -> closeAndReturn()
            }
        }
        doneButton.setOnClickListener { closeAndReturn() }

        productInfoLayout.visibility = View.GONE
        startSingleScan()
    }

    private fun startSingleScan() {
        if (isScanning) return
        barcodeView.pause()
        barcodeView.resume()
        isScanning = true
        errorLabel.visibility = View.GONE
        scanNextButton.text = "Scanning..."
        scanNextButton.isEnabled = false
        rescanButton.visibility = View.GONE
        doneButton.visibility = View.VISIBLE

        barcodeView.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                val barcode = (result.text ?: return).trim()
                onItemScanned(barcode)
            }

            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {}
        })
    }

    private fun onItemScanned(barcode: String) {
        isScanning = false
        barcodeNumberText.text = barcode
        barcodeNumberText.setTextColor(android.graphics.Color.parseColor("#DDFFFFFF"))
        productInfoLayout.visibility = View.VISIBLE
        scanNextButton.text = "Add & Scan Next"
        scanNextButton.isEnabled = true
        rescanButton.visibility = if (isSingleScan) View.GONE else View.VISIBLE

        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) { generateBarcodeBitmap(barcode, 550, 180) }
            if (bitmap != null) {
                barcodeImage.setImageBitmap(bitmap)
            }
            val product = productRepository.getByBarcodeAnyShop(barcode)
            if (product != null) {
                if (isSingleScan) {
                    entries.add(Entry(barcode, 1))
                    currentIndex = entries.size - 1
                    updateItemCount()
                    updateNavButtons()
                    ignoreQtyChange = true
                    qtyEditText.setText("1")
                    playScanFeedback()
                    productNameLabel.text = product.name
                    productNameLabel.isSelected = true
                    productPriceLabel.text = "Sell: \u20B9${String.format("%.2f", product.sellingPrice)}  Buy: \u20B9${String.format("%.2f", product.buyingPrice)}"
                    val stockText = "Stock: ${product.stockQuantity}"
                    productStockLabel.text = if (product.stockQuantity <= 0)
                        "$stockText \u2014 Out of Stock" else stockText
                    productStockLabel.setTextColor(if (product.stockQuantity <= 0)
                        android.graphics.Color.parseColor("#EF4444") else android.graphics.Color.parseColor("#AAFFFFFF"))
                    productCategoryLabel.text = if (product.category.isNotBlank()) "Category: ${product.category}" else ""
                    errorLabel.visibility = View.GONE
                    scanNextButton.text = "Scan Next"
                    return@launch
                }
                errorLabel.visibility = View.GONE
                productNameLabel.text = product.name
                productNameLabel.isSelected = true
                productPriceLabel.text = "Sell: \u20B9${String.format("%.2f", product.sellingPrice)}  Buy: \u20B9${String.format("%.2f", product.buyingPrice)}"
                val stockText = "Stock: ${product.stockQuantity}"
                productStockLabel.text = if (product.stockQuantity <= 0)
                    "$stockText \u2014 Out of Stock" else stockText
                productStockLabel.setTextColor(if (product.stockQuantity <= 0)
                    android.graphics.Color.parseColor("#EF4444") else android.graphics.Color.parseColor("#AAFFFFFF"))
                productCategoryLabel.text = if (product.category.isNotBlank()) "Category: ${product.category}" else ""
                playScanFeedback()
            } else {
                if (isSingleScan) {
                    unknownScanned = true
                    unknownBarcode = barcode
                    closeAndReturn()
                    return@launch
                }
                productNameLabel.text = ""
                productPriceLabel.text = ""
                productStockLabel.text = ""
                productCategoryLabel.text = ""
                barcodeNumberText.setTextColor(android.graphics.Color.parseColor("#E65100"))
                errorLabel.text = "Unknown barcode \u2014 will verify from database"
                errorLabel.setTextColor(android.graphics.Color.parseColor("#E65100"))
                errorLabel.visibility = View.VISIBLE
            }
        }

        if (!isSingleScan) {
            val qty = 1
            if (replaceMode && currentIndex in entries.indices) {
                entries[currentIndex] = Entry(barcode, qty)
                replaceMode = false
            } else {
                entries.add(Entry(barcode, qty))
                currentIndex = entries.size - 1
            }
            updateNavButtons()
            updateItemCount()
        }
    }

    private fun navigateTo(index: Int) {
        if (index < 0 || index >= entries.size) return
        saveCurrentQty()
        currentIndex = index
        val entry = entries[index]
        barcodeNumberText.text = entry.barcode
        barcodeNumberText.setTextColor(android.graphics.Color.parseColor("#DDFFFFFF"))
        ignoreQtyChange = true
        qtyEditText.setText("${entry.quantity}")
        productInfoLayout.visibility = View.VISIBLE
        scanNextButton.text = "Add & Scan Next"
        scanNextButton.isEnabled = true
        rescanButton.visibility = if (isSingleScan) View.GONE else View.VISIBLE
        errorLabel.visibility = View.GONE

        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) { generateBarcodeBitmap(entry.barcode, 550, 180) }
            if (bitmap != null) {
                barcodeImage.setImageBitmap(bitmap)
            }
            val product = productRepository.getByBarcodeAnyShop(entry.barcode)
            if (product != null) {
                productNameLabel.text = product.name
                productNameLabel.isSelected = true
                productPriceLabel.text = "Sell: \u20B9${String.format("%.2f", product.sellingPrice)}  Buy: \u20B9${String.format("%.2f", product.buyingPrice)}"
                val stockText = "Stock: ${product.stockQuantity}"
                productStockLabel.text = if (product.stockQuantity <= 0)
                    "$stockText \u2014 Out of Stock" else stockText
                productStockLabel.setTextColor(if (product.stockQuantity <= 0)
                    android.graphics.Color.parseColor("#EF4444") else android.graphics.Color.parseColor("#AAFFFFFF"))
                productCategoryLabel.text = if (product.category.isNotBlank()) "Category: ${product.category}" else ""
            } else {
                barcodeNumberText.setTextColor(android.graphics.Color.parseColor("#E65100"))
                errorLabel.text = "Unknown barcode"
                errorLabel.setTextColor(android.graphics.Color.parseColor("#E65100"))
                errorLabel.visibility = View.VISIBLE
                productNameLabel.text = ""
                productPriceLabel.text = ""
                productStockLabel.text = ""
                productCategoryLabel.text = ""
            }
        }
        updateNavButtons()
    }

    private fun saveCurrentQty() {
        if (currentIndex < 0 || currentIndex >= entries.size) return
        val qty = qtyEditText.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
        entries[currentIndex].quantity = qty
    }

    private fun updateNavButtons() {
        navBackButton.visibility = if (currentIndex > 0 && entries.size > 1) View.VISIBLE else View.GONE
        navForwardButton.visibility = if (currentIndex >= 0 && currentIndex < entries.size - 1 && entries.size > 1) View.VISIBLE else View.GONE
    }

    private fun updateItemCount() {
        val total = entries.sumOf { it.quantity }
        itemCountText.text = "$total items (${entries.size})"
    }

    private val currentBarcode: String
        get() = if (currentIndex in entries.indices) entries[currentIndex].barcode else ""

    private fun generateBarcodeBitmap(barcode: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(barcode, BarcodeFormat.CODE_128, targetWidth, targetHeight)
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)
            for (x in 0 until targetWidth) {
                for (y in 0 until targetHeight) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun closeAndReturn() {
        if (currentIndex >= 0) saveCurrentQty()
        barcodeView.pause()
        val map = HashMap<String, Int>()
        entries.forEach { e -> map[e.barcode] = (map[e.barcode] ?: 0) + e.quantity }
        val result = Intent()
        if (isSingleScan) {
            if (unknownScanned && unknownBarcode.isNotBlank()) {
                result.putExtra("SINGLE_SCAN_BARCODE", unknownBarcode)
                result.putExtra("SINGLE_SCAN_QTY", 0) } else if (entries.isNotEmpty()) {
                result.putExtra("SINGLE_SCAN_BARCODE", entries[0].barcode)
                result.putExtra("SINGLE_SCAN_QTY", entries[0].quantity)
            }
            result.putExtra("SCANNED_ITEMS", map)
        } else {
            result.putExtra("SCANNED_ITEMS", map)
        }
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun playScanFeedback() {
        try {
            MediaActionSound().apply {
                play(MediaActionSound.SHUTTER_CLICK)
                release()
            }
        } catch (_: Exception) {}
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        barcodeView.pause()
    }
}
