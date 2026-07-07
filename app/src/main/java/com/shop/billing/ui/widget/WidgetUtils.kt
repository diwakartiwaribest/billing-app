package com.shop.billing.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Room
import com.shop.billing.R
import com.shop.billing.data.local.AppDatabase
import com.shop.billing.data.local.dao.InvoiceDao
import com.shop.billing.data.local.dao.InvestmentDao
import com.shop.billing.data.local.dao.ProductDao
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class WidgetData(
    val today: Double, val week: Double, val month: Double,
    val totalSales: Double, val totalInvested: Double,
    val low: Int, val out: Int, val value: Double, val date: String
)

object WidgetUtils {
    private val fmt = NumberFormat.getNumberInstance(Locale.US)

    fun formatCurrency(amount: Double): String = "${Constants.CURRENCY_SYMBOL}${fmt.format(amount.toLong())}"

    suspend fun getShopCode(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val prefs = context.applicationContext.dataStore.data.first()
            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] ?: ""
        } catch (e: Exception) {
            Log.e("WidgetUtils", "Failed to read shopCode", e)
            ""
        }
    }

    suspend fun refreshAllWidgets(context: Context, db: AppDatabase? = null) = withContext(NonCancellable) {
        Log.e("WidgetUtils", "refresh START db=${db != null}")
        val ctx = context.applicationContext
        val shopCode = getShopCode(ctx)
        if (shopCode.isBlank()) { Log.e("WidgetUtils", "No shopCode - aborting"); return@withContext }
        Log.e("WidgetUtils", "shopCode=$shopCode")

        val data = if (db != null) {
            queryData(shopCode, db.invoiceDao(), db.productDao(), db.investmentDao())
        } else {
            withDatabase(ctx) { database ->
                queryData(shopCode, database.invoiceDao(), database.productDao(), database.investmentDao())
            }
        }

        if (data == null) { Log.e("WidgetUtils", "queryData returned null"); return@withContext }

        Handler(Looper.getMainLooper()).post {
            try {
                val salesIds = updateSalesWidget(ctx, data.date, data.today, data.week, data.month)
                val plIds = updatePLWidget(ctx, data.date, data.totalSales, data.totalInvested)
                val stockIds = updateStockWidget(ctx, data.date, data.low, data.out, data.value)
                Log.e("WidgetUtils", "refresh DONE salesIds=${salesIds.toList()} plIds=${plIds.toList()} stockIds=${stockIds.toList()}")
                // Force re-render via broadcast (Vivo Funtouch OS ignores direct updateAppWidget).
                // Only from ViewModel path (db != null) to avoid recursion:
                // broadcast -> onUpdate -> refreshAllWidgets(db=null) -> post -> (no broadcast)
                if (db != null) {
                    forceWidgetUpdate(ctx, SalesWidget::class.java, salesIds)
                    forceWidgetUpdate(ctx, ProfitLossWidget::class.java, plIds)
                    forceWidgetUpdate(ctx, StockWidget::class.java, stockIds)
                }
            } catch (e: Exception) {
                Log.e("WidgetUtils", "Handler widget update failed", e)
            }
        }
    }

    private fun forceWidgetUpdate(ctx: Context, cls: Class<*>, ids: IntArray) {
        if (ids.isEmpty()) return
        try {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            intent.component = ComponentName(ctx.packageName, cls.name)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            ctx.sendBroadcast(intent)
            Log.e("WidgetUtils", "Broadcast sent for ${cls.simpleName} ids=${ids.toList()}")
        } catch (e: Exception) {
            Log.e("WidgetUtils", "Broadcast failed for ${cls.simpleName}", e)
        }
    }

    private suspend fun queryData(shopCode: String, invoiceDao: InvoiceDao, productDao: ProductDao, investmentDao: InvestmentDao): WidgetData? {
        return try {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
            val dayEnd = cal.timeInMillis

            val today = withContext(Dispatchers.IO) { invoiceDao.observeDailySales(shopCode, dayStart, dayEnd).first() }
            Log.e("WidgetUtils", "today=$today")

            cal.timeInMillis = dayStart; cal.add(Calendar.DAY_OF_YEAR, -6)
            val week = withContext(Dispatchers.IO) { invoiceDao.observeDailySales(shopCode, cal.timeInMillis, dayEnd).first() }
            Log.e("WidgetUtils", "week=$week")

            cal.timeInMillis = dayStart; cal.add(Calendar.DAY_OF_YEAR, -29)
            val month = withContext(Dispatchers.IO) { invoiceDao.observeDailySales(shopCode, cal.timeInMillis, dayEnd).first() }
            Log.e("WidgetUtils", "month=$month")

            val totalSales = withContext(Dispatchers.IO) { invoiceDao.observeTotalSales(shopCode).first() }
            Log.e("WidgetUtils", "totalSales=$totalSales")

            val totalInvested = withContext(Dispatchers.IO) { investmentDao.observeTotal(shopCode).first() }
            Log.e("WidgetUtils", "totalInvested=$totalInvested")

            val low = withContext(Dispatchers.IO) { productDao.observeLowStockCount(shopCode).first() }
            val out = withContext(Dispatchers.IO) { productDao.observeOutOfStockCount(shopCode).first() }
            val value = withContext(Dispatchers.IO) { productDao.observeTotalStockValue(shopCode).first() }
            Log.e("WidgetUtils", "low=$low out=$out value=$value")

            val date = SimpleDateFormat("d MMM", Locale.getDefault()).format(Calendar.getInstance().time)
            WidgetData(today, week, month, totalSales, totalInvested, low, out, value, date)
        } catch (e: Exception) {
            Log.e("WidgetUtils", "queryData failed", e)
            null
        }
    }

    private suspend fun <T> withDatabase(context: Context, block: suspend (AppDatabase) -> T): T? = withContext(Dispatchers.IO) {
        try {
            Log.e("WidgetUtils", "withDatabase: building new instance")
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "billing_room.db"
            )
                .addMigrations(
                    AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3,
                    AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5,
                    AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7,
                    AppDatabase.MIGRATION_7_8
                )
                .build()
            Log.e("WidgetUtils", "withDatabase: built successfully")
            try {
                block(db)
            } finally {
                db.close()
                Log.e("WidgetUtils", "withDatabase: db closed")
            }
        } catch (e: Exception) {
            Log.e("WidgetUtils", "withDatabase error", e)
            null
        }
    }

    private fun updateSalesWidget(ctx: Context, date: String, today: Double, week: Double, month: Double): IntArray {
        return try {
            val views = RemoteViews(ctx.packageName, R.layout.widget_sales)
            views.setInt(R.id.widget_sales_root, "setBackgroundResource", R.drawable.widget_bg)
            views.setInt(R.id.widget_sales_header, "setBackgroundResource", R.drawable.widget_header_bg)
            views.setTextViewText(R.id.widget_sales_date, date)
            views.setTextViewText(R.id.widget_sales_today, formatCurrency(today))
            views.setTextViewText(R.id.widget_sales_week, formatCurrency(week))
            views.setTextViewText(R.id.widget_sales_month, formatCurrency(month))
            val ids = AppWidgetManager.getInstance(ctx).getAppWidgetIds(ComponentName(ctx, SalesWidget::class.java))
            if (ids.isNotEmpty()) {
                AppWidgetManager.getInstance(ctx).updateAppWidget(ids, views)
                Log.e("WidgetUtils", "Sales widget updateAppWidget called for ids=${ids.toList()}")
            } else {
                Log.e("WidgetUtils", "Sales widget IDs empty")
            }
            ids
        } catch (e: Exception) {
            Log.e("WidgetUtils", "Sales widget update failed", e)
            IntArray(0)
        }
    }

    private fun updatePLWidget(ctx: Context, date: String, totalSales: Double, totalInvested: Double): IntArray {
        return try {
            val netPL = totalSales - totalInvested
            val views = RemoteViews(ctx.packageName, R.layout.widget_pl)
            views.setInt(R.id.widget_pl_root, "setBackgroundResource", R.drawable.widget_bg)
            views.setInt(R.id.widget_pl_header, "setBackgroundResource", R.drawable.widget_header_bg)
            views.setTextViewText(R.id.widget_pl_date, date)
            views.setTextViewText(R.id.widget_pl_sales, formatCurrency(totalSales))
            views.setTextViewText(R.id.widget_pl_invested, formatCurrency(totalInvested))
            val netStr = formatCurrency(kotlin.math.abs(netPL))
            views.setTextViewText(R.id.widget_pl_net, "${if (netPL >= 0) "+" else "-"}$netStr")
            views.setTextColor(R.id.widget_pl_net, if (netPL >= 0) 0xFF16A34A.toInt() else 0xFFDC2626.toInt())
            val ids = AppWidgetManager.getInstance(ctx).getAppWidgetIds(ComponentName(ctx, ProfitLossWidget::class.java))
            if (ids.isNotEmpty()) {
                AppWidgetManager.getInstance(ctx).updateAppWidget(ids, views)
                Log.e("WidgetUtils", "PL widget updateAppWidget called for ids=${ids.toList()}")
            } else {
                Log.e("WidgetUtils", "PL widget IDs empty")
            }
            ids
        } catch (e: Exception) {
            Log.e("WidgetUtils", "P&L widget update failed", e)
            IntArray(0)
        }
    }

    private fun updateStockWidget(ctx: Context, date: String, low: Int, out: Int, value: Double): IntArray {
        return try {
            val views = RemoteViews(ctx.packageName, R.layout.widget_stock)
            views.setInt(R.id.widget_stock_root, "setBackgroundResource", R.drawable.widget_bg)
            views.setInt(R.id.widget_stock_header, "setBackgroundResource", R.drawable.widget_header_bg)
            views.setTextViewText(R.id.widget_stock_date, date)
            views.setTextViewText(R.id.widget_stock_low, low.toString())
            views.setTextViewText(R.id.widget_stock_out, out.toString())
            views.setTextViewText(R.id.widget_stock_value, formatCurrency(value))
            val ids = AppWidgetManager.getInstance(ctx).getAppWidgetIds(ComponentName(ctx, StockWidget::class.java))
            if (ids.isNotEmpty()) {
                AppWidgetManager.getInstance(ctx).updateAppWidget(ids, views)
                Log.e("WidgetUtils", "Stock widget updateAppWidget called for ids=${ids.toList()}")
            } else {
                Log.e("WidgetUtils", "Stock widget IDs empty")
            }
            ids
        } catch (e: Exception) {
            Log.e("WidgetUtils", "Stock widget update failed", e)
            IntArray(0)
        }
    }
}
