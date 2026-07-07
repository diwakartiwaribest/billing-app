package com.shop.billing.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import com.shop.billing.R

class SummaryWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.e("SummaryWidget", "onUpdate called, ids=${appWidgetIds.toList()}")
        try {
            for (id in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_summary)
                views.setTextViewText(R.id.widget_header, "Today's Summary")
                views.setTextViewText(R.id.widget_daily_sales, "Sales: \u20B90")
                appWidgetManager.updateAppWidget(id, views)
                Log.e("SummaryWidget", "Updated widget id=$id")
            }
        } catch (e: Exception) {
            Log.e("SummaryWidget", "onUpdate failed", e)
        }
    }
}
