package com.shop.billing.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import com.shop.billing.R
import kotlinx.coroutines.runBlocking

class SalesWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.e("SalesWidget", "onUpdate called ids=${appWidgetIds.toList()}")
        try {
            runBlocking { WidgetUtils.refreshAllWidgets(context) }
        } catch (e: Exception) {
            Log.e("SalesWidget", "onUpdate failed", e)
        }
    }
}
