package com.example.automationtool.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.automationtool.data.entities.Automation
import com.example.automationtool.data.entities.AutomationStep
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automations ORDER BY createdAt DESC")
    fun getAllAutomations(): Flow<List<Automation>>

    @Insert
    suspend fun insertAutomation(automation: Automation): Long

    @Update
    suspend fun updateAutomation(automation: Automation): Int

    @Delete
    suspend fun deleteAutomation(automation: Automation): Int

    @Query("SELECT * FROM automation_steps WHERE automationId = :automationId ORDER BY stepOrder ASC")
    fun getStepsForAutomation(automationId: Long): Flow<List<AutomationStep>>

    @Insert
    suspend fun insertStep(step: AutomationStep): Long

    @Update
    suspend fun updateStep(step: AutomationStep): Int

    @Delete
    suspend fun deleteStep(step: AutomationStep): Int
    
    @Query("DELETE FROM automation_steps WHERE automationId = :automationId")
    suspend fun deleteStepsForAutomation(automationId: Long): Int
    
    @Query("SELECT * FROM automations WHERE id = :id")
    suspend fun getAutomationById(id: Long): Automation?
}
