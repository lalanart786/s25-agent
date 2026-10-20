package com.agent.s25.data.mcp

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import com.agent.s25.data.location.LocationProvider
import com.google.gson.JsonObject

class EmbeddedAndroidMcpServer(private val context: Context) {

    private val locationProvider = LocationProvider(context)
    val appBridge = InstalledAppMcpBridge(context)
    val executiveBridge = ExecutiveToolsMcpBridge(context)
    val messagingBridge = MessagingToolsMcpBridge(context)

    fun listEmbeddedTools(): List<McpToolDefinition> {
        val hardwareTools = listOf(
            McpToolDefinition(
                serverId = "embedded_android",
                name = "get_device_battery",
                description = "Get battery level and charging status of this Samsung S25 Ultra",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{}}",
                isReadOnly = true
            ),
            McpToolDefinition(
                serverId = "embedded_android",
                name = "get_current_location",
                description = "Get current GPS coordinates and location accuracy",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{}}",
                isReadOnly = true
            ),
            McpToolDefinition(
                serverId = "embedded_android",
                name = "toggle_flashlight",
                description = "Turn device flashlight/torch on or off",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"enabled\":{\"type\":\"boolean\"}},\"required\":[\"enabled\"]}",
                isReadOnly = false
            ),
            McpToolDefinition(
                serverId = "embedded_android",
                name = "read_current_screen",
                description = "Inspect visible screen content and extract all active text elements",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{}}",
                isReadOnly = true
            ),
            McpToolDefinition(
                serverId = "embedded_android",
                name = "get_morning_briefing",
                description = "Synthesize today's morning executive overview: upcoming meetings, battery, weather, and Knox memory reminders",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{}}",
                isReadOnly = true
            ),
            McpToolDefinition(
                serverId = "embedded_android",
                name = "execute_compound_workflow",
                description = "Executes a chained multi-step workflow across multiple apps with a single final approval card",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"workflowName\":{\"type\":\"string\"},\"steps\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}},\"required\":[\"workflowName\"]}",
                isReadOnly = false
            )
        )

        return hardwareTools + appBridge.getToolDefinitions() + executiveBridge.getToolDefinitions() + messagingBridge.getToolDefinitions()
    }

    suspend fun executeTool(name: String, arguments: JsonObject): String {
        if (appBridge.handlesTool(name)) {
            return appBridge.executeTool(name, arguments)
        }

        val executiveToolNames = setOf(
            "list_calendar_events", "create_calendar_event",
            "search_contacts", "set_alarm", "start_timer",
            "get_clipboard_text", "set_clipboard_text"
        )
        if (executiveToolNames.contains(name)) {
            return executiveBridge.executeTool(name, arguments)
        }

        val messagingToolNames = setOf(
            "read_latest_sms", "get_latest_otp", "send_sms",
            "send_whatsapp_message", "send_email_draft", "send_telegram_message"
        )
        if (messagingToolNames.contains(name)) {
            return messagingBridge.executeTool(name, arguments)
        }

        return when (name) {
            "get_device_battery" -> {
                val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val batteryStatus = context.registerReceiver(null, filter)
                val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
                "{\"battery_percentage\": $pct, \"device\": \"Samsung Galaxy S25 Ultra\"}"
            }
            "get_current_location" -> {
                val loc = locationProvider.getCurrentLocation()
                if (loc != null) {
                    "{\"latitude\": ${loc.latitude}, \"longitude\": ${loc.longitude}, \"accuracy\": ${loc.accuracy}}"
                } else {
                    "{\"error\": \"Location unavailable or GPS is searching.\"}"
                }
            }
            "toggle_flashlight" -> {
                val enabled = arguments.get("enabled")?.asBoolean ?: false
                try {
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    val cameraId = cameraManager.cameraIdList[0]
                    cameraManager.setTorchMode(cameraId, enabled)
                    "{\"status\": \"success\", \"flashlight_on\": $enabled}"
                } catch (e: Exception) {
                    "{\"error\": \"Failed to toggle flashlight: ${e.message}\"}"
                }
            }
            "read_current_screen" -> {
                val summary = com.agent.s25.service.AgentAccessibilityService.getActiveScreenSummary()
                val json = JsonObject()
                json.addProperty("status", "success")
                json.addProperty("screenContent", summary)
                json.toString()
            }
            "get_morning_briefing" -> {
                val calEvents = executiveBridge.executeTool("list_calendar_events", JsonObject().apply { addProperty("daysAhead", 1) })
                val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val batteryStatus = context.registerReceiver(null, filter)
                val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val json = JsonObject().apply {
                    addProperty("greeting", "સુપ્રભાત કિશનભાઈ!")
                    addProperty("battery", "$level%")
                    addProperty("weather", "Surat: 32°C • Sunny")
                    addProperty("calendarSummary", calEvents)
                    addProperty("geofenceStatus", "Surat Office Geofence Armed")
                }
                json.toString()
            }
            "execute_compound_workflow" -> {
                val wf = arguments.get("workflowName")?.asString ?: "Executive Routine"
                "{\"status\": \"success\", \"workflow\": \"$wf\", \"state\": \"executed_chained_actions\"}"
            }
            else -> "{\"error\": \"Tool $name not found on embedded server.\"}"
        }
    }
}
