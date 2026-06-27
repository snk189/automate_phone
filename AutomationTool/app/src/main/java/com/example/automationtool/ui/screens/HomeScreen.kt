package com.example.automationtool.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.automationtool.automation.AccessibilityAutomationService
import com.example.automationtool.data.entities.Automation
import com.example.automationtool.viewmodel.AutomationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AutomationViewModel,
    onNavigateToCreate: (Long?) -> Unit
) {
    val automations by viewModel.allAutomations.collectAsState(initial = emptyList())
    val isServiceEnabled by AccessibilityAutomationService.instance.collectAsState()
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var newAutomationName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Automations") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Automation")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isServiceEnabled == null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).clickable {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Warning")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Accessibility Service disabled. Tap to enable.")
                    }
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(automations) { automation ->
                    AutomationCard(
                        automation = automation,
                        onRun = { viewModel.runAutomation(automation.id) },
                        onEdit = { onNavigateToCreate(automation.id) },
                        onDelete = { viewModel.deleteAutomation(automation) }
                    )
                }
            }
        }
        
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("New Automation") },
                text = {
                    OutlinedTextField(
                        value = newAutomationName,
                        onValueChange = { newAutomationName = it },
                        label = { Text("Name") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.createEmptyAutomation(newAutomationName) { id ->
                            showCreateDialog = false
                            onNavigateToCreate(id)
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun AutomationCard(
    automation: Automation,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(automation.name, style = MaterialTheme.typography.titleMedium)
            }
            IconButton(onClick = onRun) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Run")
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
