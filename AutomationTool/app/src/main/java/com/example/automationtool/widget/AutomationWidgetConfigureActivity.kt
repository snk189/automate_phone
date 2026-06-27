package com.example.automationtool.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import java.io.File
import java.io.FileOutputStream

class AutomationWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        val dao = AutomationDatabase.getDatabase(this).automationDao()
        val repo = AutomationRepository(dao)
        val automations = runBlocking { repo.allAutomations.first() }

        enableEdgeToEdge()
        setContent {
            AutomationToolTheme {
                var selectedAutomation by remember { mutableStateOf<Automation?>(null) }

                Scaffold(
                    topBar = {
                        TopAppBar(title = { 
                            Text(if (selectedAutomation == null) "Select Automation" else "Select Icon") 
                        })
                    }
                ) { padding ->
                    if (selectedAutomation == null) {
                        // STEP 1: Select Automation
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
                                            .clickable { selectedAutomation = automation },
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
                    } else {
                        // STEP 2: Select Icon
                        
                        val launcher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent()
                        ) { uri: Uri? ->
                            if (uri != null) {
                                onImageSelected(selectedAutomation!!, uri)
                            }
                        }

                        Column(
                            modifier = Modifier.padding(padding).fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = { onDefaultIconSelected(selectedAutomation!!) },
                                modifier = Modifier.fillMaxWidth(0.8f).padding(8.dp)
                            ) {
                                Text("Use Default App Logo")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { launcher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(0.8f).padding(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload Custom Image")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onDefaultIconSelected(automation: Automation) {
        saveAndFinish(automation, null)
    }

    private fun onImageSelected(automation: Automation, uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(filesDir, "widget_icon_${appWidgetId}.png")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            saveAndFinish(automation, file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback
            saveAndFinish(automation, null)
        }
    }

    private fun saveAndFinish(automation: Automation, customImagePath: String?) {
        val context = this@AutomationWidgetConfigureActivity
        
        val prefs = context.getSharedPreferences(AutomationWidgetProvider.PREFS_NAME, 0).edit()
        prefs.putLong(AutomationWidgetProvider.PREF_PREFIX_KEY + appWidgetId, automation.id)
        prefs.putString(AutomationWidgetProvider.PREF_PREFIX_NAME + appWidgetId, automation.name)
        if (customImagePath != null) {
            prefs.putString(AutomationWidgetProvider.PREF_PREFIX_ICON + appWidgetId, customImagePath)
        } else {
            prefs.remove(AutomationWidgetProvider.PREF_PREFIX_ICON + appWidgetId)
        }
        prefs.apply()

        val appWidgetManager = AppWidgetManager.getInstance(context)
        AutomationWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)

        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}
