package com.example.automationtool.repository

import com.example.automationtool.data.dao.AutomationDao
import com.example.automationtool.data.entities.Automation
import com.example.automationtool.data.entities.AutomationStep
import kotlinx.coroutines.flow.Flow

class AutomationRepository(private val dao: AutomationDao) {

    val allAutomations: Flow<List<Automation>> = dao.getAllAutomations()

    fun getStepsForAutomation(automationId: Long): Flow<List<AutomationStep>> {
        return dao.getStepsForAutomation(automationId)
    }

    suspend fun insertAutomation(automation: Automation): Long {
        return dao.insertAutomation(automation)
    }

    suspend fun updateAutomation(automation: Automation) {
        dao.updateAutomation(automation)
    }

    suspend fun deleteAutomation(automation: Automation) {
        dao.deleteStepsForAutomation(automation.id)
        dao.deleteAutomation(automation)
    }
    
    suspend fun getAutomationById(id: Long): Automation? {
        return dao.getAutomationById(id)
    }

    suspend fun insertStep(step: AutomationStep): Long {
        return dao.insertStep(step)
    }

    suspend fun updateStep(step: AutomationStep) {
        dao.updateStep(step)
    }

    suspend fun deleteStep(step: AutomationStep) {
        dao.deleteStep(step)
    }
}
