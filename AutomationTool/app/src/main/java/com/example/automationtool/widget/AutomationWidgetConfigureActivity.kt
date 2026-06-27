package com.example.automationtool.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.automationtool.data.database.AutomationDatabase
import com.example.automationtool.data.entities.Automation
import com.example.automationtool.repository.AutomationRepository
import com.example.automationtool.theme.AutomationToolTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AutomationWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set the result to CANCELED. This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(Activity.RESULT_CANCELED)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Fetch automations for UI
        val dao = AutomationDatabase.getDatabase(this).automationDao()
        val repo = AutomationRepository(dao)
        val automations = runBlocking { repo.allAutomations.first() }

        enableEdgeToEdge()
        setContent {
            AutomationToolTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Select Automation") })
                    }
                ) { padding ->
                    if (automations.isEmpty()) {
                        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No automations available. Create one first!")
                        }
                    } else {
                        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                            items(automations) { automation ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .clickable { onAutomationSelected(automation) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text(
                                        text = automation.name,
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onAutomationSelected(automation: Automation) {
        val context = this@AutomationWidgetConfigureActivity
        
        // Save the selection
        val prefs = context.getSharedPreferences(AutomationWidgetProvider.PREFS_NAME, 0).edit()
        prefs.putLong(AutomationWidgetProvider.PREF_PREFIX_KEY + appWidgetId, automation.id)
        prefs.putString(AutomationWidgetProvider.PREF_PREFIX_NAME + appWidgetId, automation.name)
        prefs.apply()

        // Push widget update
        val appWidgetManager = AppWidgetManager.getInstance(context)
        AutomationWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)

        // Pass back original widgetId
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}
