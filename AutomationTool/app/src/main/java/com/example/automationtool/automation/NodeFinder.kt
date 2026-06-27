package com.example.automationtool.automation

import android.view.accessibility.AccessibilityNodeInfo

object NodeFinder {
    fun findAllNodesByTextRecursive(node: AccessibilityNodeInfo?, text: String, result: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        val nodeText = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val hintText = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            node.hintText?.toString() ?: ""
        } else ""

        if (nodeText.contains(text, ignoreCase = true) || 
            contentDesc.contains(text, ignoreCase = true) || 
            hintText.contains(text, ignoreCase = true)) {
            result.add(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findAllNodesByTextRecursive(child, text, result)
            }
        }
    }

    fun findNodeByText(root: AccessibilityNodeInfo, text: String, exactMatch: Boolean = false): AccessibilityNodeInfo? {
        // 1. Fast native search
        val nativeNodes = root.findAccessibilityNodeInfosByText(text)
        for (node in nativeNodes) {
            val nodeText = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
            if (exactMatch) {
                if (nodeText.equals(text, ignoreCase = true)) return node
            } else {
                return node
            }
        }
        
        // 2. Slow deep recursive search for inner html / hintText
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        findAllNodesByTextRecursive(root, text, nodes)
        
        for (node in nodes) {
            val nodeText = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
            val hintText = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                node.hintText?.toString() ?: ""
            } else ""

            if (exactMatch) {
                if (nodeText.equals(text, ignoreCase = true) || hintText.equals(text, ignoreCase = true)) {
                    return node
                }
            } else {
                return node
            }
        }
        return null
    }

    fun findClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current
            }
            current = current.parent
        }
        return null
    }
    
    fun findScrollableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isScrollable) {
                return current
            }
            current = current.parent
        }
        return null
    }

    fun isNodeEditable(node: AccessibilityNodeInfo): Boolean {
        val className = node.className?.toString() ?: ""
        return node.isEditable || className.contains("EditText") || className.contains("AutoCompleteTextView") || className.contains("android.webkit.WebView") && node.isFocused
    }

    fun findEditableChild(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (isNodeEditable(node)) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val editable = findEditableChild(child)
                if (editable != null) return editable
            }
        }
        return null
    }
}
