package com.example.automationtool.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automation_steps")
data class AutomationStep(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val automationId: Long,
    val stepOrder: Int,
    val searchText: String,
    val action: Action,
    val typeText: String = "",
    val delayAfter: Long = 500L,
    val isLocked: Boolean = false
)
