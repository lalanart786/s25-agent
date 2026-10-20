package com.agent.s25.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Autonomous Accessibility Service for Samsung Galaxy S25 Ultra.
 * Enables the on-device AI agent to inspect screen content, find UI elements,
 * click buttons, enter text, and navigate between apps.
 */
class AgentAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AgentAccessibility"

        @Volatile
        var instance: AgentAccessibilityService? = null
            private set

        val isAvailable: Boolean
            get() = instance != null

        fun getEnableIntent(): Intent {
            return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "AgentAccessibilityService connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Active window state changes can be monitored here
    }

    override fun onInterrupt() {
        Log.w(TAG, "AgentAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        Log.i(TAG, "AgentAccessibilityService destroyed.")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance == this) {
            instance = null
        }
        return super.onUnbind(intent)
    }

    /**
     * Finds and clicks a node containing or matching the given text.
     */
    fun clickByText(targetText: String, exactMatch: Boolean = false): Boolean {
        val root = rootInActiveWindow ?: return false
        val matchedNodes = root.findAccessibilityNodeInfosByText(targetText)
        if (matchedNodes.isNullOrEmpty()) {
            return findAndClickRecursive(root, targetText, exactMatch)
        }

        for (node in matchedNodes) {
            if (node == null) continue
            val nodeText = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
            val matches = if (exactMatch) {
                nodeText.equals(targetText, ignoreCase = true)
            } else {
                nodeText.contains(targetText, ignoreCase = true)
            }

            if (matches) {
                if (performClickOnNodeOrParent(node)) return true
            }
        }
        return false
    }

    /**
     * Finds and clicks a node by its view ID resource name (e.g. "com.whatsapp:id/send_button").
     */
    fun clickByViewId(viewId: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        if (!nodes.isNullOrEmpty()) {
            for (node in nodes) {
                if (node != null && performClickOnNodeOrParent(node)) {
                    return true
                }
            }
        }
        return false
    }

    private fun findAndClickRecursive(node: AccessibilityNodeInfo, targetText: String, exactMatch: Boolean): Boolean {
        val text = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
        val matches = if (exactMatch) {
            text.equals(targetText, ignoreCase = true)
        } else {
            text.contains(targetText, ignoreCase = true)
        }

        if (matches && performClickOnNodeOrParent(node)) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickRecursive(child, targetText, exactMatch)) {
                return true
            }
        }
        return false
    }

    private fun performClickOnNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }

        // If no clickable ancestor, dispatch a physical tap at the center of the node's bounds
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.width() > 0 && bounds.height() > 0) {
            return clickCoordinates(bounds.exactCenterX(), bounds.exactCenterY())
        }
        return false
    }

    /**
     * Dispatches a tap gesture at physical screen coordinates (x, y).
     */
    fun clickCoordinates(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 80)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    /**
     * Reads all visible text content on the current screen.
     */
    fun readScreenText(): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        val textItems = mutableListOf<String>()
        collectTextRecursive(root, textItems)
        return textItems
    }

    private fun collectTextRecursive(node: AccessibilityNodeInfo, list: MutableList<String>) {
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()

        if (!text.isNullOrEmpty()) {
            list.add(text)
        } else if (!desc.isNullOrEmpty()) {
            list.add("[$desc]")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectTextRecursive(child, list)
        }
    }

    /**
     * Scrolls the current active window or scrollable node.
     */
    fun scroll(direction: String = "forward"): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollableNode = findScrollableNode(root)

        val action = if (direction.equals("backward", ignoreCase = true) || direction.equals("up", ignoreCase = true)) {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }

        if (scrollableNode != null) {
            return scrollableNode.performAction(action)
        }

        // Fallback gesture swipe
        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2f
        val startY = if (direction.equals("backward", ignoreCase = true) || direction.equals("up", ignoreCase = true)) {
            displayMetrics.heightPixels * 0.3f
        } else {
            displayMetrics.heightPixels * 0.7f
        }
        val endY = if (direction.equals("backward", ignoreCase = true) || direction.equals("up", ignoreCase = true)) {
            displayMetrics.heightPixels * 0.7f
        } else {
            displayMetrics.heightPixels * 0.3f
        }

        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 300)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollableNode(child)
            if (found != null) return found
        }
        return null
    }

    /**
     * Sets text into the currently focused editable input field.
     */
    fun inputText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: findEditableNode(root) ?: return false

        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findEditableNode(child)
            if (found != null) return found
        }
        return null
    }

    /**
     * Performs standard Android global system navigation actions.
     */
    fun performGlobal(action: String): Boolean {
        val globalAction = when (action.lowercase()) {
            "back" -> GLOBAL_ACTION_BACK
            "home" -> GLOBAL_ACTION_HOME
            "recents" -> GLOBAL_ACTION_RECENTS
            "notifications" -> GLOBAL_ACTION_NOTIFICATIONS
            "quick_settings" -> GLOBAL_ACTION_QUICK_SETTINGS
            else -> GLOBAL_ACTION_BACK
        }
        return performGlobalAction(globalAction)
    }
}
