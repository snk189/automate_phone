package com.example.automationtool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.content.Context
import android.content.Intent
import com.example.automationtool.data.entities.Action
import com.example.automationtool.data.entities.AutomationStep
import com.example.automationtool.viewmodel.AutomationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(val name: String, val packageName: String)

fun getInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val resolveInfos = pm.queryIntentActivities(intent, 0)
    return resolveInfos.map {
        AppInfo(
            name = it.loadLabel(pm).toString(),
            packageName = it.activityInfo.packageName
        )
    }.distinctBy { it.packageName }.sortedBy { it.name }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAutomationScreen(
    automationId: Long,
    viewModel: AutomationViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(automationId) {
        viewModel.loadStepsForAutomation(automationId)
    }

    val steps by viewModel.currentSteps.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Automation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = { viewModel.addStep(automationId) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Text("+ Add Step")
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(steps, key = { it.id }) { step ->
                StepCard(
                    step = step,
                    onUpdate = { viewModel.updateStep(it) },
                    onDelete = { viewModel.deleteStep(step) },
                    onDuplicate = { viewModel.duplicateStep(step) },
                    onMoveUp = {
                        // Simple reorder: swap with previous
                        val idx = steps.indexOf(step)
                        if (idx > 0) {
                            val prev = steps[idx - 1]
                            viewModel.updateStep(step.copy(stepOrder = prev.stepOrder))
                            viewModel.updateStep(prev.copy(stepOrder = step.stepOrder))
                        }
                    },
                    onMoveDown = {
                        // Simple reorder: swap with next
                        val idx = steps.indexOf(step)
                        if (idx < steps.size - 1) {
                            val next = steps[idx + 1]
                            viewModel.updateStep(step.copy(stepOrder = next.stepOrder))
                            viewModel.updateStep(next.copy(stepOrder = step.stepOrder))
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepCard(
    step: AutomationStep,
    onUpdate: (AutomationStep) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchTextState by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(step.searchText)) }
    var typeTextState by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(step.typeText)) }
    var delayTextState by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(step.delayAfter.toString())) }
    var isUnlockedLocally by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Step Order: ${step.stepOrder}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                
                IconButton(onClick = {
                    if (step.isLocked) {
                        if (!isUnlockedLocally && activity != null) {
                            promptBiometricUnlock(activity) {
                                isUnlockedLocally = true
                                onUpdate(step.copy(isLocked = false))
                            }
                        } else {
                            isUnlockedLocally = true
                            onUpdate(step.copy(isLocked = false))
                        }
                    } else {
                        isUnlockedLocally = false
                        onUpdate(step.copy(isLocked = true))
                    }
                }) {
                    Icon(
                        if (step.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock"
                    )
                }

                IconButton(onClick = onMoveUp) { Icon(Icons.Default.KeyboardArrowUp, "Up") }
                IconButton(onClick = onMoveDown) { Icon(Icons.Default.KeyboardArrowDown, "Down") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            if (step.action == Action.OPEN_APP) {
                var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
                var appExpanded by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        apps = getInstalledApps(context)
                    }
                }
                
                ExposedDropdownMenuBox(
                    expanded = appExpanded,
                    onExpandedChange = { appExpanded = !appExpanded }
                ) {
                    val currentApp = apps.find { it.packageName == step.searchText }?.name ?: step.searchText
                    OutlinedTextField(
                        value = currentApp,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select App to Open") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = appExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = appExpanded,
                        onDismissRequest = { appExpanded = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        apps.forEach { app ->
                            DropdownMenuItem(
                                text = { Text(app.name) },
                                onClick = {
                                    searchTextState = androidx.compose.ui.text.input.TextFieldValue(app.packageName)
                                    onUpdate(step.copy(searchText = app.packageName))
                                    appExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = searchTextState,
                    onValueChange = { 
                        searchTextState = it
                        onUpdate(step.copy(searchText = it.text)) 
                    },
                    label = { Text("Search Text or Package/URL") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = step.action.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Action") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    Action.values().forEach { actionOption ->
                        DropdownMenuItem(
                            text = { Text(actionOption.name) },
                            onClick = {
                                onUpdate(step.copy(action = actionOption))
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (step.action == Action.CLICK_AND_TYPE || step.action == Action.TYPE_TEXT || step.action == Action.DOUBLE_CLICK_AND_TYPE) {
                Spacer(modifier = Modifier.height(8.dp))
                val displayValue = if (step.isLocked && !isUnlockedLocally) {
                    androidx.compose.ui.text.input.TextFieldValue("****")
                } else {
                    typeTextState
                }
                
                OutlinedTextField(
                    value = displayValue,
                    onValueChange = { 
                        if (!step.isLocked || isUnlockedLocally) {
                            typeTextState = it
                            onUpdate(step.copy(typeText = it.text)) 
                        }
                    },
                    readOnly = step.isLocked && !isUnlockedLocally,
                    label = { Text("Text to Type") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (step.isLocked && !isUnlockedLocally) {
                    TextButton(onClick = {
                        if (activity != null) {
                            promptBiometricUnlock(activity) {
                                isUnlockedLocally = true
                            }
                        }
                    }) {
                        Text("Tap to unlock and view/edit")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = delayTextState,
                onValueChange = { 
                    delayTextState = it
                    val delay = it.text.toLongOrNull() ?: 0L
                    onUpdate(step.copy(delayAfter = delay))
                },
                label = { Text("Delay After Step (ms)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDuplicate) {
                    Text("Duplicate")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

fun promptBiometricUnlock(activity: androidx.fragment.app.FragmentActivity, onSuccess: () -> Unit) {
    val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
    val biometricPrompt = androidx.biometric.BiometricPrompt(activity, executor,
        object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
        })
    val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Step")
        .setSubtitle("Authenticate to view or edit this sensitive text")
        .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()
    biometricPrompt.authenticate(promptInfo)
}
