package com.agent.s25.data.mcp

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.agent.s25.service.AgentAccessibilityService
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * MCP Bridge providing full Android OS application launcher and accessibility automation.
 * Exposes device capabilities (WhatsApp deep-links, Maps navigation, YouTube search,
 * phone calls, app launching, and screen UI interaction) as MCP tools.
 */
class InstalledAppMcpBridge(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val gson = Gson()

    companion object {
        private const val TAG = "InstalledAppMcpBridge"
    }

    fun getToolDefinitions(): List<McpToolDefinition> {
        return listOf(
            McpToolDefinition(
                serverId = "embedded_android",
                name = "list_installed_apps",
                description = "List all applications installed on this phone with package names and labels",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"includeSystemApps\":{\"type\":\"boolean\",\"description\":\"Whether to include pre-installed system apps\"}}}",
                isReadOnly = true
            ),
            McpToolDefinition(
                serverId = "embedded_android",
                name = "launch_app",
                description = "Launch any app by name (e.g. 'WhatsApp', 'Maps', 'YouTube', 'Zoho Books', 'Phone', 'Camera', 'Gmail') or package name",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"appNameOrPackage\":{\"type\":\"string\",\"description\":\"App name or package identifier\"}},\"required\":[\"appNameOrPackage\"]}",
                isReadOnly = false
            ),
            McpToolDefinition(
                serverId = "embedded_android",
                name = "send_whatsapp_message",
                description = "Open WhatsApp directly to compose or send a message to a phone number or contact",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\",\"description\":\"The message text to send\"},\"phoneNumber\":{\"type\":\"string\",\"description\":\"Optional recipient phone number with country code (e.g. +919876543210)\"}},\"required\":[\"message\"]}",
                isReadOnly = false
            ),
            McpToolDefinition(
                serverId = "embedded_android",
                name = "navigate_maps",
                description = "Launch Google Maps navigation to specific coordinates or an address/location name",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"destination\":{\"type\":\"string\",\"description\":\"Destination name, address, or lat,lng coordinates (e.g. 'Surat Textile Market' or '21.1702,72.8311')\"}},\"required\":[\"destination\"]}",
                isReadOnly = false
            ),
            McpToolDefinition(
                serverId = "embedded_android",
                name = "search_youtube",
                description = "Search and open video results on the YouTube app",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\",\"description\":\"Search query string\"}},\"required\":[\"query\"]}",
                isReadOnly = false
            ),
            McpToolDefinition(
                serverId = "embedded_android",
                name = "make_phone_call",
                description = "Open the phone dialer with a specific contact or phone number",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"phoneNumber\":{\"type\":\"string\",\"description\":\"Phone number to dial\"}},\"required\":[\"phoneNumber\"]}",
                isReadOnly = false
            ),
            McpToolDefinition(
                serverId = "embedded_android",
                name = "screen_read_text",
                description = "Read all text currently visible on the active Android screen via Accessibility Service",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{}}",
                isReadOnly = true
            ),
            McpToolDefinition(
                serverId = "embedded_android",
                name = "screen_click",
                description = "Click an on-screen element by its visible text or view ID via Accessibility Service",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"targetText\":{\"type\":\"string\",\"description\":\"Text on the button or item to click\"},\"viewId\":{\"type\":\"string\",\"description\":\"Optional view ID resource name\"},\"exactMatch\":{\"type\":\"boolean\"}},\"required\":[\"targetText\"]}",
                isReadOnly = false
            ),
            McpToolDefinition(
                serverId = "embedded_android",
                name = "screen_scroll",
                description = "Scroll the active screen up, down, forward, or backward via Accessibility Service",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"direction\":{\"type\":\"string\",\"description\":\"'forward', 'backward', 'up', or 'down'\"}}}",
                isReadOnly = false
            )
        )
    }

    fun handlesTool(name: String): Boolean {
        return when (name) {
            "list_installed_apps",
            "launch_app",
            "send_whatsapp_message",
            "navigate_maps",
            "search_youtube",
            "make_phone_call",
            "screen_read_text",
            "screen_click",
            "screen_scroll" -> true
            else -> false
        }
    }

    suspend fun executeTool(name: String, arguments: JsonObject): String {
        return try {
            when (name) {
                "list_installed_apps" -> listInstalledApps(arguments)
                "launch_app" -> launchApp(arguments)
                "send_whatsapp_message" -> sendWhatsAppMessage(arguments)
                "navigate_maps" -> navigateMaps(arguments)
                "search_youtube" -> searchYouTube(arguments)
                "make_phone_call" -> makePhoneCall(arguments)
                "screen_read_text" -> screenReadText()
                "screen_click" -> screenClick(arguments)
                "screen_scroll" -> screenScroll(arguments)
                else -> "{\"error\": \"Tool $name not handled by InstalledAppMcpBridge.\"}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Tool execution error for $name: ${e.message}", e)
            "{\"error\": \"Exception executing $name: ${e.message}\"}"
        }
    }

    private fun listInstalledApps(arguments: JsonObject): String {
        val includeSystem = arguments.get("includeSystemApps")?.asBoolean ?: false
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        val array = JsonArray()
        for (app in packages) {
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!includeSystem && isSystem) continue

            val label = packageManager.getApplicationLabel(app).toString()
            val item = JsonObject().apply {
                addProperty("label", label)
                addProperty("package", app.packageName)
                addProperty("is_system", isSystem)
            }
            array.add(item)
        }

        val result = JsonObject().apply {
            addProperty("total_count", array.size())
            add("apps", array)
        }
        return gson.toJson(result)
    }

    private fun launchApp(arguments: JsonObject): String {
        val target = arguments.get("appNameOrPackage")?.asString?.trim()
            ?: return "{\"error\": \"appNameOrPackage is required.\"}"

        // 1. Resolve known shortcuts
        val lower = target.lowercase()
        when (lower) {
            "phone", "dialer", "call" -> {
                val intent = Intent(Intent.ACTION_DIAL).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(intent)
                return "{\"status\": \"launched\", \"app\": \"Phone Dialer\"}"
            }
            "camera" -> {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(intent)
                return "{\"status\": \"launched\", \"app\": \"Camera\"}"
            }
        }

        val knownPackages = mapOf(
            "whatsapp" to "com.whatsapp",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "youtube" to "com.google.android.youtube",
            "zoho" to "com.zoho.books",
            "zoho books" to "com.zoho.books",
            "gmail" to "com.google.android.gm",
            "email" to "com.google.android.gm",
            "chrome" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "settings" to "com.android.settings"
        )

        var resolvedPackage = knownPackages[lower]

        // 2. Direct package name or search
        if (resolvedPackage == null) {
            if (target.contains(".")) {
                resolvedPackage = target
            } else {
                // Search installed apps by label
                val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                val matched = apps.firstOrNull { app ->
                    val label = packageManager.getApplicationLabel(app).toString()
                    label.contains(target, ignoreCase = true)
                }
                resolvedPackage = matched?.packageName
            }
        }

        if (resolvedPackage.isNullOrEmpty()) {
            return "{\"error\": \"Could not resolve application matching '$target'.\"}"
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(resolvedPackage)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            "{\"status\": \"launched\", \"package\": \"$resolvedPackage\"}"
        } else {
            "{\"error\": \"App '$resolvedPackage' is not installed or cannot be launched directly.\"}"
        }
    }

    private fun sendWhatsAppMessage(arguments: JsonObject): String {
        val message = arguments.get("message")?.asString
            ?: return "{\"error\": \"Message text is required.\"}"
        val phoneRaw = arguments.get("phoneNumber")?.asString?.trim()

        return if (!phoneRaw.isNullOrEmpty()) {
            // Clean phone digits
            var digitsOnly = phoneRaw.replace(Regex("[^0-9]"), "")
            if (digitsOnly.length == 10) {
                // Prepend Indian country code (+91) for 10-digit mobile numbers
                digitsOnly = "91$digitsOnly"
            }

            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$digitsOnly&text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(intent)
                "{\"status\": \"opened_chat\", \"recipient\": \"$digitsOnly\", \"message\": \"$message\"}"
            } catch (e: Exception) {
                // Fallback without restricting package
                val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                "{\"status\": \"opened_chat_fallback\", \"recipient\": \"$digitsOnly\"}"
            }
        } else {
            // General share to WhatsApp
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                "{\"status\": \"opened_share\", \"message\": \"$message\"}"
            } catch (e: Exception) {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                "{\"status\": \"opened_share_fallback\"}"
            }
        }
    }

    private fun navigateMaps(arguments: JsonObject): String {
        val destination = arguments.get("destination")?.asString
            ?: return "{\"error\": \"Destination is required.\"}"

        // Primary: Google Navigation deep link
        val navUri = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(mapIntent)
            "{\"status\": \"navigating\", \"destination\": \"$destination\"}"
        } catch (e: Exception) {
            // Fallback: Web / geo view
            val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(destination)}")
            val fallbackIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
            "{\"status\": \"navigating_fallback\", \"destination\": \"$destination\"}"
        }
    }

    private fun searchYouTube(arguments: JsonObject): String {
        val query = arguments.get("query")?.asString
            ?: return "{\"error\": \"Query is required.\"}"

        val intent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra("query", query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            "{\"status\": \"searching_youtube\", \"query\": \"$query\"}"
        } catch (e: Exception) {
            val webUri = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
            val fallbackIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
            "{\"status\": \"searching_youtube_web\", \"query\": \"$query\"}"
        }
    }

    private fun makePhoneCall(arguments: JsonObject): String {
        val number = arguments.get("phoneNumber")?.asString
            ?: return "{\"error\": \"Phone number is required.\"}"

        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "{\"status\": \"dialer_opened\", \"phoneNumber\": \"$number\"}"
    }

    private fun screenReadText(): String {
        val service = AgentAccessibilityService.instance
        if (service == null) {
            return "{\"status\": \"accessibility_disabled\", \"message\": \"Agent Accessibility Service is not active. Please enable it in Settings > Accessibility > S25 Autonomous Screen Automation.\"}"
        }
        val items = service.readScreenText()
        val result = JsonObject().apply {
            addProperty("elements_count", items.size)
            add("screen_text", gson.toJsonTree(items))
        }
        return gson.toJson(result)
    }

    private fun screenClick(arguments: JsonObject): String {
        val service = AgentAccessibilityService.instance
        if (service == null) {
            return "{\"status\": \"accessibility_disabled\", \"message\": \"Agent Accessibility Service is not active. Please enable it in Settings > Accessibility.\"}"
        }

        val text = arguments.get("targetText")?.asString ?: ""
        val viewId = arguments.get("viewId")?.asString
        val exact = arguments.get("exactMatch")?.asBoolean ?: false

        val success = if (!viewId.isNullOrEmpty()) {
            service.clickByViewId(viewId) || service.clickByText(text, exact)
        } else {
            service.clickByText(text, exact)
        }

        return if (success) {
            "{\"status\": \"clicked\", \"target\": \"${text.ifEmpty { viewId }}\"}"
        } else {
            "{\"status\": \"not_found\", \"target\": \"${text.ifEmpty { viewId }}\", \"message\": \"Element not visible on current screen.\"}"
        }
    }

    private fun screenScroll(arguments: JsonObject): String {
        val service = AgentAccessibilityService.instance
        if (service == null) {
            return "{\"status\": \"accessibility_disabled\", \"message\": \"Agent Accessibility Service is not active.\"}"
        }

        val direction = arguments.get("direction")?.asString ?: "forward"
        val success = service.scroll(direction)
        return "{\"status\": \"scrolled\", \"direction\": \"$direction\", \"success\": $success}"
    }
}
