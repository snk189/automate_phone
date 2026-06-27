package com.example.automationtool.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.automationtool.R
import com.example.automationtool.automation.AccessibilityAutomationService

class AutomationWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            prefs.edit().remove(PREF_PREFIX_KEY + appWidgetId).remove(PREF_PREFIX_NAME + appWidgetId).apply()
        }
    }

    companion object {
        const val PREFS_NAME = "com.example.automationtool.widget.AutomationWidget"
        const val PREF_PREFIX_KEY = "appwidget_"
        const val PREF_PREFIX_NAME = "appwidget_name_"

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            val automationId = prefs.getLong(PREF_PREFIX_KEY + appWidgetId, -1L)
            val automationName = prefs.getString(PREF_PREFIX_NAME + appWidgetId, "Run Script")

            val views = RemoteViews(context.packageName, R.layout.automation_widget)
            views.setTextViewText(R.id.widget_title, automationName)

            if (automationId != -1L) {
                val intent = Intent(context, AccessibilityAutomationService::class.java).apply {
                    action = "com.example.automationtool.RUN_AUTOMATION"
                    putExtra("automationId", automationId)
                }
                
                val pendingIntent = PendingIntent.getService(
                    context, 
                    appWidgetId, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
