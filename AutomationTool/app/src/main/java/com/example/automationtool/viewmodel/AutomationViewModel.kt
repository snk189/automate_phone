package com.example.automationtool.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.automationtool.automation.AccessibilityAutomationService
import com.example.automationtool.automation.AutomationExecutor
import com.example.automationtool.data.database.AutomationDatabase
import com.example.automationtool.data.entities.Action
import com.example.automationtool.data.entities.Automation
import com.example.automationtool.data.entities.AutomationStep
import com.example.automationtool.repository.AutomationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AutomationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AutomationRepository
    val allAutomations: Flow<List<Automation>>

    init {
        val dao = AutomationDatabase.getDatabase(application).automationDao()
        repository = AutomationRepository(dao)
        allAutomations = repository.allAutomations
    }

    private val _currentSteps = MutableStateFlow<List<AutomationStep>>(emptyList())
    val currentSteps: StateFlow<List<AutomationStep>> = _currentSteps

    private val _currentAutomation = MutableStateFlow<Automation?>(null)
    val currentAutomation: StateFlow<Automation?> = _currentAutomation

    private val executor = AutomationExecutor()

    private var loadStepsJob: Job? = null
    private var loadAutomationJob: Job? = null

    fun loadStepsForAutomation(automationId: Long) {
        loadStepsJob?.cancel()
        loadStepsJob = viewModelScope.launch {
            repository.getStepsForAutomation(automationId).collect { steps ->
                _currentSteps.value = steps
            }
        }
        
        loadAutomationJob?.cancel()
        loadAutomationJob = viewModelScope.launch(Dispatchers.IO) {
            val automation = repository.getAutomationById(automationId)
            _currentAutomation.value = automation
        }
    }
    
    fun createEmptyAutomation(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertAutomation(Automation(name = name))
            viewModelScope.launch(Dispatchers.Main) {
                onCreated(id)
            }
        }
    }

    fun deleteAutomation(automation: Automation) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAutomation(automation)
        }
    }
    
    fun renameAutomation(automation: Automation, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAutomation(automation.copy(name = newName))
        }
    }
    
    fun cloneAutomation(automation: Automation) {
        viewModelScope.launch(Dispatchers.IO) {
            val automations = repository.allAutomations.first()
            val baseName = automation.name.substringBeforeLast(" (").trim()
            val regex = Regex("""^${Regex.escape(baseName)}(?: \((\d+)\))?$""")
            var maxIndex = 0
            for (a in automations) {
                val match = regex.matchEntire(a.name)
                if (match != null) {
                    val num = match.groupValues[1].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
                    if (num >= maxIndex) {
                        maxIndex = num + 1
                    }
                }
            }
            val newName = if (maxIndex == 0) "$baseName (1)" else "$baseName ($maxIndex)"
            
            val newAutomationId = repository.insertAutomation(Automation(name = newName))
            val steps = repository.getStepsForAutomation(automation.id).first()
            for (step in steps) {
                repository.insertStep(step.copy(id = 0, automationId = newAutomationId))
            }
            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(getApplication(), "Automation cloned as $newName", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    fun addStep(automationId: Long) {
        val currentMaxOrder = _currentSteps.value.maxOfOrNull { it.stepOrder } ?: 0
        val newStep = AutomationStep(
            automationId = automationId,
            stepOrder = currentMaxOrder + 1,
            searchText = "",
            action = Action.CLICK
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertStep(newStep)
            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(getApplication(), "New step added!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateStep(step: AutomationStep) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStep(step)
        }
    }

    fun deleteStep(step: AutomationStep) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStep(step)
            // Rearrange step orders
            val remainingSteps = _currentSteps.value.filter { it.id != step.id }.sortedBy { it.stepOrder }
            remainingSteps.forEachIndexed { index, remainingStep ->
                if (remainingStep.stepOrder != index + 1) {
                    repository.updateStep(remainingStep.copy(stepOrder = index + 1))
                }
            }
        }
    }
    
    fun duplicateStep(step: AutomationStep) {
        viewModelScope.launch(Dispatchers.IO) {
            val newStep = step.copy(id = 0, stepOrder = step.stepOrder + 1)
            repository.insertStep(newStep)
        }
    }

    fun runAutomation(automationId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val service = AccessibilityAutomationService.instance.value
            if (service == null) {
                viewModelScope.launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Accessibility Service not running", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            // Fetch the current state of steps only once
            val steps = repository.getStepsForAutomation(automationId).first()
            if (steps.isNotEmpty()) {
                executor.executeSteps(steps, service)
                viewModelScope.launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Automation finished", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
