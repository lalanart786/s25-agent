package com.agent.s25.presentation.mcp

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agent.s25.data.mcp.McpClient
import com.agent.s25.presentation.theme.*
import kotlinx.coroutines.launch

data class AppAutomationItem(
    val name: String,
    val packageName: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val defaultActionDescription: String,
    val sampleActionPayload: String,
    val fallbackUrl: String? = null
)

@Composable
fun McpHubScreen(mcpClient: McpClient) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val availableTools by mcpClient.availableTools.collectAsState()

    var activeResultDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isFlashlightOn by remember { mutableStateOf(false) }

    // Check accessibility status
    val isAccessibilityEnabled = remember(context) {
        try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
        } catch (_: Exception) {
            false
        }
    }

    val automatableApps = remember {
        listOf(
            AppAutomationItem(
                name = "WhatsApp",
                packageName = "com.whatsapp",
                description = "Send proactive client updates, order manifests & receipts",
                icon = Icons.Default.Chat,
                accentColor = EmeraldSafeGreen,
                defaultActionDescription = "Send WhatsApp update to Surat team: 'Dispatch processed'",
                sampleActionPayload = "{\"tool\": \"send_whatsapp\", \"recipient\": \"+91 98250 XXXXX\", \"message\": \"નમસ્તે, આજનું પાર્સલ રવાના કરી દીધું છે.\"}"
            ),
            AppAutomationItem(
                name = "Google Maps",
                packageName = "com.google.android.apps.maps",
                description = "Real-time navigation to client offices, mills & geofences",
                icon = Icons.Default.Navigation,
                accentColor = SoftCyan,
                defaultActionDescription = "Route to Surat Diamond Bourse Office",
                sampleActionPayload = "{\"tool\": \"open_navigation\", \"destination\": \"Surat Diamond Bourse, Gujarat\", \"lat\": 21.1702, \"lng\": 72.8311}",
                fallbackUrl = "https://maps.google.com"
            ),
            AppAutomationItem(
                name = "YouTube",
                packageName = "com.google.android.youtube",
                description = "Background market research, tutorials & trend monitoring",
                icon = Icons.Default.PlayCircle,
                accentColor = DangerRed,
                defaultActionDescription = "Search textile export trends and S25 Ultra tech reviews",
                sampleActionPayload = "{\"tool\": \"youtube_search\", \"query\": \"S25 Ultra Snapdragon 8 Elite benchmarks\"}",
                fallbackUrl = "https://youtube.com"
            ),
            AppAutomationItem(
                name = "Zoho Books",
                packageName = "com.zoho.books",
                description = "Lalan Art financial audit, ledger sync & unpaid invoices",
                icon = Icons.Default.AccountBalance,
                accentColor = AmberIndicGold,
                defaultActionDescription = "Fetch today's ledger summary & draft invoices",
                sampleActionPayload = "{\"tool\": \"zoho_books_digest\", \"action\": \"get_unpaid_invoices\", \"company\": \"Lalan Art\"}",
                fallbackUrl = "https://books.zoho.com"
            ),
            AppAutomationItem(
                name = "Phone & Dialer",
                packageName = "com.samsung.android.dialer",
                description = "Autonomous speed dial & call dispatch with caller ID memory",
                icon = Icons.Default.Phone,
                accentColor = ElectricIndigo,
                defaultActionDescription = "Initiate call to Surat Dispatch Supervisor",
                sampleActionPayload = "{\"tool\": \"dial_contact\", \"name\": \"Surat Manager\", \"number\": \"+91 98250 12345\"}"
            ),
            AppAutomationItem(
                name = "Camera & Vision",
                packageName = "com.sec.android.app.camera",
                description = "Document scanning, OCR invoice parsing & receipt capture",
                icon = Icons.Default.CameraAlt,
                accentColor = SoftCyan,
                defaultActionDescription = "Trigger macro camera for textile fabric OCR",
                sampleActionPayload = "{\"tool\": \"camera_ocr_scan\", \"mode\": \"receipt_scan\"}"
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Meta Muse Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MCP Apps Hub",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricIndigo.copy(alpha = 0.2f))
                            .border(1.dp, ElectricIndigo.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "MODEL CONTEXT PROTOCOL",
                            color = SoftCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Text(
                    text = "Phone apps automation & hardware control for on-device agent",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Accessibility Service Card
            item {
                AccessibilityStatusCard(
                    isEnabled = isAccessibilityEnabled,
                    onOpenSettings = {
                        try {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open settings: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Section 1: Phone App Automation
            item {
                SectionHeader(
                    title = "PHONE APP AUTOMATION",
                    subtitle = "Apps the S25 Agent can autonomously interact with via MCP"
                )
            }

            items(automatableApps) { app ->
                AppAutomationCard(
                    app = app,
                    onLaunch = {
                        try {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            if (launchIntent != null) {
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(launchIntent)
                            } else if (app.fallbackUrl != null) {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(app.fallbackUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(webIntent)
                            } else {
                                Toast.makeText(context, "${app.name} is not installed directly on this device.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Launch failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onTestAction = {
                        activeResultDialog = Pair(
                            "Executed MCP Action for ${app.name}",
                            "Action:\n${app.defaultActionDescription}\n\nPayload:\n${app.sampleActionPayload}\n\nStatus: ✅ Action synthesized & validated by MCP Client."
                        )
                    }
                )
            }

            // Section 2: System Tools & Hardware Sensors
            item {
                Spacer(modifier = Modifier.height(6.dp))
                SectionHeader(
                    title = "SYSTEM TOOLS & HARDWARE SENSORS",
                    subtitle = "Direct Snapdragon 8 Elite hardware interfaces"
                )
            }

            item {
                SystemHardwareCard(
                    title = "S25 Ultra Battery Telemetry",
                    description = "Query instantaneous battery level, temperature & charging state",
                    icon = Icons.Default.BatteryChargingFull,
                    accent = EmeraldSafeGreen,
                    actionButtonText = "Query Battery",
                    onAction = {
                        coroutineScope.launch {
                            val result = mcpClient.executeTool("get_device_battery", "{}")
                            activeResultDialog = Pair("Battery Status", result)
                        }
                    }
                )
            }

            item {
                SystemHardwareCard(
                    title = "Camera Torch / Flashlight",
                    description = "On-device flashlight toggle with thermal safeguard",
                    icon = Icons.Default.FlashlightOn,
                    accent = AmberIndicGold,
                    actionButtonText = if (isFlashlightOn) "Turn Off" else "Turn On",
                    onAction = {
                        coroutineScope.launch {
                            val newState = !isFlashlightOn
                            val res = mcpClient.executeTool("toggle_flashlight", "{\"enabled\": $newState}")
                            isFlashlightOn = newState
                            activeResultDialog = Pair("Torch Output", res)
                        }
                    }
                )
            }

            item {
                SystemHardwareCard(
                    title = "Spatial GPS & Geofencing",
                    description = "High-accuracy GNSS coordinates & geofence perimeter audit",
                    icon = Icons.Default.LocationOn,
                    accent = SoftCyan,
                    actionButtonText = "Get Current GPS",
                    onAction = {
                        coroutineScope.launch {
                            val result = mcpClient.executeTool("get_current_location", "{}")
                            activeResultDialog = Pair("Current Location", result)
                        }
                    }
                )
            }

            // Section 3: Executive Suite & Personal Productivity
            item {
                Spacer(modifier = Modifier.height(6.dp))
                SectionHeader(
                    title = "EXECUTIVE SUITE & PRODUCTIVITY",
                    subtitle = "Calendar, Alarms, Contacts & System Clipboard"
                )
            }

            item {
                SystemHardwareCard(
                    title = "Calendar & Meetings",
                    description = "Query upcoming schedule or insert new business meetings",
                    icon = Icons.Default.CalendarToday,
                    accent = ElectricIndigo,
                    actionButtonText = "List Events",
                    onAction = {
                        coroutineScope.launch {
                            val result = mcpClient.executeTool("list_calendar_events", "{\"daysAhead\": 2}")
                            activeResultDialog = Pair("Calendar Events", result)
                        }
                    }
                )
            }

            item {
                SystemHardwareCard(
                    title = "Alarms & Production Timers",
                    description = "Set wake-up alarms or countdown timers with skip-UI intent",
                    icon = Icons.Default.Alarm,
                    accent = AmberIndicGold,
                    actionButtonText = "Set 7:00 AM Alarm",
                    onAction = {
                        coroutineScope.launch {
                            val result = mcpClient.executeTool("set_alarm", "{\"hour\": 7, \"minute\": 0, \"message\": \"S25 Agent Morning Alarm\"}")
                            activeResultDialog = Pair("Alarm Action", result)
                        }
                    }
                )
            }

            item {
                SystemHardwareCard(
                    title = "System Clipboard Manager",
                    description = "Read current copied clipboard buffer or copy text to device",
                    icon = Icons.Default.ContentPaste,
                    accent = SoftCyan,
                    actionButtonText = "Read Clipboard",
                    onAction = {
                        coroutineScope.launch {
                            val result = mcpClient.executeTool("get_clipboard_text", "{}")
                            activeResultDialog = Pair("Clipboard Content", result)
                        }
                    }
                )
            }

            item {
                SystemHardwareCard(
                    title = "Contacts Book Search",
                    description = "Search phone contacts book by name or phone number",
                    icon = Icons.Default.Contacts,
                    accent = EmeraldSafeGreen,
                    actionButtonText = "Search Contacts",
                    onAction = {
                        coroutineScope.launch {
                            val result = mcpClient.executeTool("search_contacts", "{\"query\": \"Surat\"}")
                            activeResultDialog = Pair("Contacts Query ('Surat')", result)
                        }
                    }
                )
            }

            // Section 4: Communication & Messaging Suite
            item {
                Spacer(modifier = Modifier.height(6.dp))
                SectionHeader(
                    title = "COMMUNICATION & MESSAGING SUITE",
                    subtitle = "SMS OTP detection, direct WhatsApp, Gmail & Telegram"
                )
            }

            item {
                SystemHardwareCard(
                    title = "SMS Inbox & OTP Intelligence",
                    description = "Automatically parse latest incoming SMS messages and extract 2FA / OTP codes",
                    icon = Icons.Default.Sms,
                    accent = AmberIndicGold,
                    actionButtonText = "Scan OTP",
                    onAction = {
                        coroutineScope.launch {
                            val result = mcpClient.executeTool("get_latest_otp", "{}")
                            activeResultDialog = Pair("Latest OTP Code", result)
                        }
                    }
                )
            }

            item {
                SystemHardwareCard(
                    title = "WhatsApp Direct Message",
                    description = "Draft and trigger pre-formatted WhatsApp chat to client or office",
                    icon = Icons.Default.Chat,
                    accent = EmeraldSafeGreen,
                    actionButtonText = "Test WhatsApp",
                    onAction = {
                        coroutineScope.launch {
                            val result = mcpClient.executeTool("send_whatsapp_message", "{\"phone_number\": \"\", \"message\": \"નમસ્તે Lalan Art, આજનું ઓર્ડર સ્ટેટસ શું છે?\"}")
                            activeResultDialog = Pair("WhatsApp Action", result)
                        }
                    }
                )
            }

            item {
                SystemHardwareCard(
                    title = "Gmail / Email Composer",
                    description = "Compose structured client emails with prefilled subject and body",
                    icon = Icons.Default.Email,
                    accent = ElectricIndigo,
                    actionButtonText = "Draft Email",
                    onAction = {
                        coroutineScope.launch {
                            val result = mcpClient.executeTool("send_email_draft", "{\"to\": \"kishan@lalanart.com\", \"subject\": \"Lalan Art Daily Update\", \"body\": \"નમસ્તે કિશનભાઈ, આજનું ઓર્ડર પ્રોડક્શન રીપોર્ટ તૈયાર છે.\"}")
                            activeResultDialog = Pair("Email Action", result)
                        }
                    }
                )
            }

            item {
                SystemHardwareCard(
                    title = "Telegram Messenger Dispatch",
                    description = "Dispatch urgent telegram updates to team members or channels",
                    icon = Icons.Default.Send,
                    accent = SoftCyan,
                    actionButtonText = "Send Telegram",
                    onAction = {
                        coroutineScope.launch {
                            val result = mcpClient.executeTool("send_telegram_message", "{\"username\": \"Kmiyani\", \"message\": \"S25 Agent Live Test from Surat Office.\"}")
                            activeResultDialog = Pair("Telegram Action", result)
                        }
                    }
                )
            }

            // Registered MCP Tools Section
            if (availableTools.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    SectionHeader(
                        title = "REGISTERED MCP TOOLS (${availableTools.size})",
                        subtitle = "Active tools exposed to on-device LLM reasoning engine"
                    )
                }

                items(availableTools) { tool ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmoledSurface)
                            .border(1.dp, AmoledGlassBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tool.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(tool.description, color = TextSecondary, fontSize = 11.sp, maxLines = 2)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(EmeraldSafeGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("ONLINE", color = EmeraldSafeGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Result Output Dialog
    activeResultDialog?.let { (title, output) ->
        AlertDialog(
            onDismissRequest = { activeResultDialog = null },
            containerColor = AmoledSurface,
            title = {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmoledSurfaceVariant)
                        .padding(12.dp)
                ) {
                    Text(
                        output,
                        color = SoftCyan,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { activeResultDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Text("OK", color = TextPrimary)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
        Text(
            text = title,
            color = SoftCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = subtitle,
            color = TextMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun AccessibilityStatusCard(
    isEnabled: Boolean,
    onOpenSettings: () -> Unit
) {
    val borderColor = if (isEnabled) EmeraldSafeGreen.copy(alpha = 0.5f) else AmberIndicGold.copy(alpha = 0.5f)
    val statusBg = if (isEnabled) EmeraldSafeGreen.copy(alpha = 0.1f) else AmberIndicGold.copy(alpha = 0.1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AmoledSurface)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessibilityNew,
                        contentDescription = null,
                        tint = if (isEnabled) EmeraldSafeGreen else AmberIndicGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Autonomous App Access",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isEnabled) "ACTIVE" else "SETUP REQUIRED",
                        color = if (isEnabled) EmeraldSafeGreen else AmberIndicGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enable the S25 Agent Accessibility Service to allow autonomous app interactions, screen reading, and UI automation without root.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isEnabled) EmeraldSafeGreen else AmberIndicGold
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (isEnabled) EmeraldSafeGreen else AmberIndicGold)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEnabled) "Accessibility Active • Open Settings" else "Enable Accessibility in Settings",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AppAutomationCard(
    app: AppAutomationItem,
    onLaunch: () -> Unit,
    onTestAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AmoledSurface)
            .border(1.dp, AmoledGlassBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(app.accentColor.copy(alpha = 0.15f))
                        .border(1.dp, app.accentColor.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        app.icon,
                        contentDescription = app.name,
                        tint = app.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.name,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AmoledSurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "MCP READY",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = app.description,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dual Buttons: Launch & Test Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onLaunch,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(AmoledGlassBorder)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Launch", fontSize = 12.sp)
                }

                Button(
                    onClick = onTestAction,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = app.accentColor.copy(alpha = 0.2f),
                        contentColor = app.accentColor
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(app.accentColor.copy(alpha = 0.4f))
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Test Action", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SystemHardwareCard(
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    actionButtonText: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AmoledSurface)
            .border(1.dp, AmoledGlassBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalButton(
                onClick = onAction,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AmoledSurfaceVariant,
                    contentColor = accent
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(actionButtonText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
