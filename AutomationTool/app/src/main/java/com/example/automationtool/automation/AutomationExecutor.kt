package com.example.automationtool.automation

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.example.automationtool.data.entities.Action
import com.example.automationtool.data.entities.AutomationStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AutomationExecutor {
    
    suspend fun executeSteps(steps: List<AutomationStep>, service: AccessibilityService) {
        for (step in steps) {
            executeStep(step, service)
            delay(step.delayAfter)
        }
    }

    private suspend fun executeStep(step: AutomationStep, service: AccessibilityService) {
        Log.d("AutomationExecutor", "Executing step: ${step.action}")
        when (step.action) {
            Action.CLICK -> performNodeAction(step, service, AccessibilityNodeInfo.ACTION_CLICK)
            Action.LONG_CLICK -> performNodeAction(step, service, AccessibilityNodeInfo.ACTION_LONG_CLICK)
            Action.CLICK_AND_TYPE -> executeTypeAction(step, service, clickFirst = true, doubleClick = false)
            Action.TYPE_TEXT -> executeTypeAction(step, service, clickFirst = false, doubleClick = false)
            Action.DOUBLE_CLICK -> {
                val node = findNodeWithRetry(step.searchText, service)
                node?.let {
                    val clickable = NodeFinder.findClickableAncestor(it) ?: it
                    clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    delay(100)
                    clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
            Action.DOUBLE_CLICK_AND_TYPE -> executeTypeAction(step, service, clickFirst = true, doubleClick = true)
            Action.WAIT -> {
                // Wait is handled by the delayAfter of the step, but we can add extra if needed
            }
            Action.SWIPE -> {
                // Requires dispatchGesture, complex for a simple implementation.
                // Simplified: find scrollable and scroll forward
                val node = findNodeWithRetry(step.searchText, service)
                node?.let {
                    val scrollable = NodeFinder.findScrollableAncestor(it)
                    scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                }
            }
            Action.SCROLL_UNTIL_TEXT_FOUND -> {
                var found = false
                var retries = 0
                while (!found && retries < 10) {
                    val root = service.rootInActiveWindow
                    if (root != null && NodeFinder.findNodeByText(root, step.searchText) != null) {
                        found = true
                    } else {
                        // try to scroll
                        if (root != null) {
                            val scrollable = NodeFinder.findScrollableAncestor(root)
                            scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                        }
                        delay(500)
                        retries++
                    }
                }
            }
            Action.BACK -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            Action.HOME -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            Action.RECENT_APPS -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            Action.NOTIFICATION_ACTIONS -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            Action.OPEN_APP -> {
                val intent = service.packageManager.getLaunchIntentForPackage(step.searchText)
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent != null) {
                    service.startActivity(intent)
                }
            }
            Action.OPEN_URL_IN_CHROME -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(step.searchText))
                intent.setPackage("com.android.chrome")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    service.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to default browser if chrome is not installed
                    intent.setPackage(null)
                    service.startActivity(intent)
                }
            }
            Action.CONDITIONAL_BRANCH -> {
                // Placeholder for logic if (condition) jump to step
            }
            Action.LOOP -> {
                // Placeholder for loop construct
            }
        }
    }

    private suspend fun showToast(service: AccessibilityService, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(service, message, Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun executeTypeAction(step: AutomationStep, service: AccessibilityService, clickFirst: Boolean, doubleClick: Boolean) {
        val node = findNodeForTypingWithRetry(step.searchText, service) ?: findNodeWithRetry(step.searchText, service)
        if (node != null) {
            val clickable = NodeFinder.findClickableAncestor(node) ?: node
            showToast(service, "Typing in: ${node.className}")
            
            if (clickFirst) {
                clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (doubleClick) {
                    delay(100)
                    clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                delay(500)
            } else {
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                delay(200)
            }

            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, step.typeText)
            var success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            if (!success) {
                success = clickable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            }
            if (!success) {
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                clickable.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                val clipboard = service.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("automation", step.typeText))
                node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                clickable.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            }
            
            delay(1000) // Wait for UI update
            
            // Verify
            val root = service.rootInActiveWindow
            if (root != null) {
                val refreshedNode = NodeFinder.findNodeByText(root, step.searchText) ?: node
                val currentText = refreshedNode.text?.toString() ?: ""
                val currentClickableText = NodeFinder.findClickableAncestor(refreshedNode)?.text?.toString() ?: ""
                val isPass = refreshedNode.isPassword
                
                if (!(isPass || currentText.contains(step.typeText, ignoreCase = true) || currentClickableText.contains(step.typeText, ignoreCase = true) || (currentText != step.searchText && currentText.isNotEmpty()))) {
                    showToast(service, "Error: Text was not pasted successfully!")
                } else {
                    showToast(service, "Successfully typed!")
                }
            } else {
                showToast(service, "Error: Lost window focus after typing!")
            }
        } else {
            showToast(service, "Error: Could not find text '${step.searchText}'")
        }
    }

    private suspend fun clickNodeByGesture(node: AccessibilityNodeInfo, service: AccessibilityService, isLongClick: Boolean = false): Boolean = 
        suspendCoroutine { continuation ->
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            val rect = android.graphics.Rect()
            node.getBoundsInScreen(rect)
            if (rect.isEmpty || rect.left < 0 || rect.top < 0) {
                continuation.resume(false)
                return@suspendCoroutine
            }

            val x = rect.centerX().toFloat()
            val y = rect.centerY().toFloat()
            val path = android.graphics.Path().apply { moveTo(x, y) }
            val duration = if (isLongClick) 600L else 100L
            val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, duration)
            val gesture = android.accessibilityservice.GestureDescription.Builder().addStroke(stroke).build()

            val dispatched = service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    continuation.resume(true)
                }
                override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    continuation.resume(false)
                }
            }, null)
            
            if (!dispatched) {
                continuation.resume(false)
            }
        } else {
            continuation.resume(false)
        }
    }

    private suspend fun performNodeAction(step: AutomationStep, service: AccessibilityService, actionId: Int) {
        val node = findNodeWithRetry(step.searchText, service)
        if (node != null) {
            var success = false
            
            // 1. Try Gesture first (Most reliable for WebViews and tricky UI)
            if (actionId == AccessibilityNodeInfo.ACTION_CLICK || actionId == AccessibilityNodeInfo.ACTION_LONG_CLICK) {
                success = clickNodeByGesture(node, service, actionId == AccessibilityNodeInfo.ACTION_LONG_CLICK)
            }
            
            // 2. Try native node click
            if (!success) {
                success = node.performAction(actionId)
            }
            
            // 3. Try clickable ancestor
            if (!success) {
                val interactable = NodeFinder.findClickableAncestor(node)
                if (interactable != null && interactable != node) {
                    success = interactable.performAction(actionId)
                }
            }
            
            if (!success) {
                showToast(service, "Action failed on ${node.className}")
            }
        } else {
            showToast(service, "Could not find '${step.searchText}'")
        }
    }

    private suspend fun findNodeWithRetry(text: String, service: AccessibilityService): AccessibilityNodeInfo? {
        if (text.isEmpty()) return null
        
        var retries = 0
        while (retries < 3) { // Reduced to try fast and fail fast
            val root = service.rootInActiveWindow
            if (root != null) {
                val node = NodeFinder.findNodeByText(root, text)
                if (node != null) {
                    return node
                }
            }
            delay(200)
            retries++
        }
        return null
    }

    private suspend fun findNodeForTypingWithRetry(text: String, service: AccessibilityService): AccessibilityNodeInfo? {
        if (text.isEmpty()) return null
        var retries = 0
        while (retries < 3) { // Fast fail
            val root = service.rootInActiveWindow
            if (root != null) {
                // 1. Native search
                val nativeNodes = root.findAccessibilityNodeInfosByText(text)
                for (node in nativeNodes) {
                    if (NodeFinder.isNodeEditable(node)) return node
                }
                
                // 2. Deep search
                val nodes = mutableListOf<AccessibilityNodeInfo>()
                NodeFinder.findAllNodesByTextRecursive(root, text, nodes)
                
                for (node in nodes) {
                    if (NodeFinder.isNodeEditable(node)) return node
                }
                for (node in nodes) {
                    val parent = node.parent
                    if (parent != null) {
                        for (i in 0 until parent.childCount) {
                            val child = parent.getChild(i)
                            if (child != null && NodeFinder.isNodeEditable(child)) return child
                        }
                    }
                }
                for (node in nodes) {
                    val editableChild = NodeFinder.findEditableChild(node)
                    if (editableChild != null) return editableChild
                }
            }
            delay(200)
            retries++
        }
        return null
    }
}
