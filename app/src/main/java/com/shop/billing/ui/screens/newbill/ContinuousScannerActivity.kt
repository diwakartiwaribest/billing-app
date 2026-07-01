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
import android.widget.TextView
import java.util.HashMap
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.shop.billing.R

class ContinuousScannerActivity : Activity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var itemCountText: TextView
    private lateinit var scannedLabel: TextView
    private lateinit var errorLabel: TextView
    private lateinit var qtyEditText: EditText
    private lateinit var scanNextButton: Button
    private lateinit var doneButton: Button

    private val scannedItems = mutableMapOf<String, Int>()
    private var currentBarcode = ""
    private var isScanning = false
    private var ignoreQtyChange = false
    private var knownBarcodes: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_continuous_scanner)

        knownBarcodes = intent?.getStringArrayListExtra("KNOWN_BARCODES")?.map { it.trim() }?.toSet() ?: emptySet()

        barcodeView = findViewById(R.id.barcodeView)
        itemCountText = findViewById(R.id.itemCountText)
        scannedLabel = findViewById(R.id.scannedLabel)
        errorLabel = findViewById(R.id.errorLabel)
        qtyEditText = findViewById(R.id.qtyEditText)
        scanNextButton = findViewById(R.id.scanNextButton)
        doneButton = findViewById(R.id.doneButton)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT < 30) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        findViewById<TextView>(R.id.closeButton).setOnClickListener { finish() }

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
                if (digits.isEmpty() || digits.toIntOrNull() == 0) {
                    ignoreQtyChange = true
                    qtyEditText.setText("1")
                } else if (digits != raw) {
                    ignoreQtyChange = true
                    qtyEditText.setText(digits)
                }
            }
        })

        scanNextButton.setOnClickListener {
            if (scanNextButton.text == "Add & Scan Next") {
                addCurrentItem()
            }
            startSingleScan()
        }
        doneButton.setOnClickListener { finishWithResults() }

        startSingleScan()
    }

    private fun startSingleScan() {
        if (isScanning) return
        barcodeView.pause()
        barcodeView.resume()
        isScanning = true
        currentBarcode = ""
        scannedLabel.visibility = View.GONE
        errorLabel.visibility = View.GONE
        findViewById<View>(R.id.qtyRow).visibility = View.GONE
        scanNextButton.text = "Scanning..."
        scanNextButton.isEnabled = false

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
        currentBarcode = barcode
        scannedLabel.text = "Scanned: $barcode"
        scannedLabel.visibility = View.VISIBLE
        qtyEditText.setText("1")
        findViewById<View>(R.id.qtyRow).visibility = View.VISIBLE
        scanNextButton.text = "Add & Scan Next"
        scanNextButton.isEnabled = true
        if (barcode in knownBarcodes) {
            scannedLabel.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            errorLabel.text = "\u2713 Barcode found"
            errorLabel.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            errorLabel.visibility = View.VISIBLE
            playScanFeedback()
        } else {
            scannedLabel.setTextColor(android.graphics.Color.parseColor("#E65100"))
            errorLabel.text = "Unknown barcode \u2014 will verify from database"
            errorLabel.setTextColor(android.graphics.Color.parseColor("#E65100"))
            errorLabel.visibility = View.VISIBLE
        }
    }

    private fun addCurrentItem() {
        val qty = qtyEditText.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
        scannedItems[currentBarcode] = scannedItems.getOrDefault(currentBarcode, 0) + qty
        itemCountText.text = "${scannedItems.values.sum()} items"
        scannedLabel.visibility = View.GONE
        errorLabel.visibility = View.GONE
        findViewById<View>(R.id.qtyRow).visibility = View.GONE
    }

    private fun finishWithResults() {
        if (currentBarcode.isNotBlank() && scanNextButton.text == "Add & Scan Next") {
            addCurrentItem()
        }
        barcodeView.pause()
        val result = Intent().apply {
            putExtra("SCANNED_ITEMS", HashMap(scannedItems))
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
