package com.agent.s25.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.agent.s25.data.db.MemoryDatabase
import com.agent.s25.domain.hardware.ThermalMonitor
import com.agent.s25.presentation.chat.ChatScreen
import com.agent.s25.presentation.chat.ChatViewModel
import com.agent.s25.presentation.location.SavedPlacesScreen
import com.agent.s25.presentation.mcp.McpHubScreen
import com.agent.s25.presentation.memory.MemoryVaultScreen
import com.agent.s25.presentation.theme.*
import com.agent.s25.service.AgentForegroundService

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()
    private var thermalMonitor: ThermalMonitor? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // After permissions granted, try starting background service
        tryStartBackgroundAgentService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Thermal monitoring (safe — no JNI calls)
        try {
            thermalMonitor = ThermalMonitor(this)
            thermalMonitor?.checkDeviceTemperature()
        } catch (e: Exception) {
            Log.w("MainActivity", "ThermalMonitor init skipped: ${e.message}")
        }

        requestRequiredPermissions()

        // Initialize encrypted database (safe — KeystoreManager has fallback)
        val db = try {
            MemoryDatabase.getInstance(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Database init failed: ${e.message}")
            null
        }

        setContent {
            S25AgentTheme {
                var selectedTab by remember { mutableStateOf(0) }

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = AmoledSurface,
                            contentColor = TextPrimary
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                                label = { Text("Chat") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryAccent,
                                    selectedTextColor = PrimaryAccent,
                                    indicatorColor = AmoledSurfaceVariant
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.Lock, contentDescription = "Vault") },
                                label = { Text("Vault") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryAccent,
                                    selectedTextColor = PrimaryAccent,
                                    indicatorColor = AmoledSurfaceVariant
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.Build, contentDescription = "MCP") },
                                label = { Text("MCP Hub") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryAccent,
                                    selectedTextColor = PrimaryAccent,
                                    indicatorColor = AmoledSurfaceVariant
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.Place, contentDescription = "Places") },
                                label = { Text("Places") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryAccent,
                                    selectedTextColor = PrimaryAccent,
                                    indicatorColor = AmoledSurfaceVariant
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(AmoledBackground)
                    ) {
                        when (selectedTab) {
                            0 -> ChatScreen(viewModel = chatViewModel)
                            1 -> if (db != null) MemoryVaultScreen(db = db) else PlaceholderScreen("Memory Vault initializing...")
                            2 -> McpHubScreen(mcpClient = chatViewModel.mcpClient)
                            3 -> if (db != null) SavedPlacesScreen(db = db) else PlaceholderScreen("Places initializing...")
                        }
                    }
                }
            }
        }

        // Defer foreground service start
        tryStartBackgroundAgentService()
    }

    @Composable
    private fun PlaceholderScreen(message: String) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AmoledBackground),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(text = message, color = TextSecondary)
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun tryStartBackgroundAgentService() {
        try {
            // Check POST_NOTIFICATIONS permission on Android 13+ before starting foreground service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                    Log.w("MainActivity", "POST_NOTIFICATIONS not granted yet, deferring foreground service.")
                    return
                }
            }
            val intent = Intent(this, AgentForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "Could not start foreground service: ${e.message}")
        }
    }
}
