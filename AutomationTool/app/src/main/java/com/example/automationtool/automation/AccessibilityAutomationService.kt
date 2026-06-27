package com.example.automationtool.automation

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.automationtool.data.database.AutomationDatabase
import com.example.automationtool.repository.AutomationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccessibilityAutomationService : AccessibilityService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "com.example.automationtool.RUN_AUTOMATION") {
            val automationId = intent.getLongExtra("automationId", -1L)
            if (automationId != -1L) {
                executeAutomationFromService(automationId)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun executeAutomationFromService(automationId: Long) {
        val dao = AutomationDatabase.getDatabase(applicationContext).automationDao()
        val repo = AutomationRepository(dao)
        val executor = AutomationExecutor()
        
        CoroutineScope(Dispatchers.IO).launch {
            val steps = repo.getStepsForAutomation(automationId).first()
            if (steps.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Starting automation...", Toast.LENGTH_SHORT).show()
                }
                executor.executeSteps(steps, this@AccessibilityAutomationService)
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Automation finished", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _instance.value = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We handle events actively in AutomationExecutor when it's running
    }

    override fun onInterrupt() {
        // Required method
    }

    override fun onUnbind(intent: Intent?): Boolean {
        _instance.value = null
        return super.onUnbind(intent)
    }

    companion object {
        private val _instance = MutableStateFlow<AccessibilityAutomationService?>(null)
        val instance: StateFlow<AccessibilityAutomationService?> = _instance
    }
}
