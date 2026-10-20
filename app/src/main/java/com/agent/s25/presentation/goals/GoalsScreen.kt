package com.agent.s25.presentation.goals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agent.s25.presentation.theme.*

import com.agent.s25.data.db.MemoryDatabase
import com.agent.s25.data.model.GoalEntity
import kotlinx.coroutines.launch

enum class GoalCategory(val label: String, val color: Color, val icon: ImageVector) {
    ALL("All", ElectricIndigo, Icons.Default.AllInclusive),
    SPATIAL("Spatial", SoftCyan, Icons.Default.Place),
    SCHEDULE("Schedule", ElectricIndigo, Icons.Default.Schedule),
    FINANCE("Finance", AmberIndicGold, Icons.Default.AccountBalanceWallet),
    SECURITY("Security", EmeraldSafeGreen, Icons.Default.Security)
}

data class GoalItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: GoalCategory,
    val triggerInfo: String,
    val isActive: Boolean = true,
    val lastTriggered: String = "Active • Ready",
    val priority: String = "HIGH"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(db: MemoryDatabase? = null) {
    val coroutineScope = rememberCoroutineScope()
    val dbGoalsState = db?.goalDao()?.getAllGoals()?.collectAsState(initial = emptyList())
    val dbGoals = dbGoalsState?.value ?: emptyList()

    var localGoals by remember {
        mutableStateOf(
            listOf(
                GoalItem(
                    id = "1",
                    title = "Surat Office Arrival Alert",
                    description = "When entering Surat Diamond Bourse office radius (200m), check pending client invoices & summarize on-screen.",
                    category = GoalCategory.SPATIAL,
                    triggerInfo = "Geofence: Surat Office • 21.1702°N, 72.8311°E",
                    isActive = true,
                    lastTriggered = "Armed • Background GPS Active",
                    priority = "CRITICAL"
                ),
                GoalItem(
                    id = "2",
                    title = "Daily Zoho Books Financial Audit",
                    description = "Pull today's unpaid invoices, vendor payouts, and daily credit notes. Send executive digest to Kishan's Telegram at 8:45 PM.",
                    category = GoalCategory.FINANCE,
                    triggerInfo = "Scheduled: Daily at 8:45 PM IST",
                    isActive = true,
                    lastTriggered = "Next trigger in 4 hrs 12 mins",
                    priority = "HIGH"
                ),
                GoalItem(
                    id = "3",
                    title = "Knox Memory Sync & Passphrase Rotation",
                    description = "Auto-re-index on-device episodic facts into SQLCipher hardware-backed Keystore with zero cloud leakage.",
                    category = GoalCategory.SECURITY,
                    triggerInfo = "System: On device idle & charging",
                    isActive = true,
                    lastTriggered = "Synced 38 mins ago",
                    priority = "HIGH"
                ),
                GoalItem(
                    id = "4",
                    title = "Low Battery Intelligence Safeguard",
                    description = "If battery drops below 20%, automatically pause heavy GGUF background model tasks and keep lightweight wake-word armed.",
                    category = GoalCategory.SCHEDULE,
                    triggerInfo = "Hardware: Battery < 20%",
                    isActive = false,
                    lastTriggered = "Paused by user",
                    priority = "MEDIUM"
                ),
                GoalItem(
                    id = "5",
                    title = "Lalan Art Morning Dispatch Briefing",
                    description = "Scan local WhatsApp order drafts & summarize daily shipping queues at 9:00 AM in native Gujarati.",
                    category = GoalCategory.FINANCE,
                    triggerInfo = "Schedule: Mon - Sat @ 9:00 AM IST",
                    isActive = true,
                    lastTriggered = "Completed today 9:00 AM",
                    priority = "NORMAL"
                )
            )
        )
    }

    // Use Room database goals if available, otherwise fallback to local goals
    val goals = if (db != null && dbGoals.isNotEmpty()) {
        dbGoals.map { entity ->
            val cat = try {
                GoalCategory.valueOf(entity.category)
            } catch (e: Exception) {
                GoalCategory.SPATIAL
            }
            GoalItem(
                id = entity.id.toString(),
                title = entity.title,
                description = entity.description,
                category = cat,
                triggerInfo = entity.triggerInfo,
                isActive = entity.isActive,
                lastTriggered = entity.lastTriggered,
                priority = entity.priority
            )
        }
    } else {
        localGoals
    }

    var selectedCategory by remember { mutableStateOf(GoalCategory.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredGoals = if (selectedCategory == GoalCategory.ALL) {
        goals
    } else {
        goals.filter { it.category == selectedCategory }
    }

    val activeCount = goals.count { it.isActive }

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
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Proactive Goals",
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
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$activeCount ACTIVE",
                            color = SoftCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                Text(
                    text = "Autonomous on-device triggers & scheduled agent missions",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Add Goal Button
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(ElectricIndigo, SoftCyan))
                    )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Goal",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Muse Telemetry Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricPill(
                title = "ARMED MISSIONS",
                value = "$activeCount/${goals.size}",
                accent = SoftCyan,
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                title = "NEXT RUN",
                value = "8:45 PM",
                accent = AmberIndicGold,
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                title = "GEOFENCES",
                value = "Surat (Armed)",
                accent = EmeraldSafeGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GoalCategory.values().forEach { cat ->
                val isSelected = selectedCategory == cat
                val chipBg by animateColorAsState(
                    targetValue = if (isSelected) AmoledSurfaceElevated else AmoledSurface,
                    label = "chipBg"
                )
                val chipBorder by animateColorAsState(
                    targetValue = if (isSelected) cat.color else AmoledGlassBorder,
                    label = "chipBorder"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(chipBg)
                        .border(1.dp, chipBorder, RoundedCornerShape(20.dp))
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (cat != GoalCategory.ALL) {
                            Icon(
                                cat.icon,
                                contentDescription = null,
                                tint = if (isSelected) cat.color else TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = cat.label,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Goals List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredGoals, key = { it.id }) { goal ->
                GoalCard(
                    goal = goal,
                    onToggle = { newState ->
                        if (db != null) {
                            coroutineScope.launch {
                                val longId = goal.id.toLongOrNull()
                                if (longId != null) {
                                    db.goalDao().toggleGoalActive(longId, newState)
                                }
                            }
                        } else {
                            localGoals = localGoals.map {
                                if (it.id == goal.id) it.copy(isActive = newState) else it
                            }
                        }
                    },
                    onDelete = {
                        if (db != null) {
                            coroutineScope.launch {
                                val longId = goal.id.toLongOrNull()
                                if (longId != null) {
                                    db.goalDao().deleteGoal(longId)
                                }
                            }
                        } else {
                            localGoals = localGoals.filterNot { it.id == goal.id }
                        }
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Add Goal Dialog
    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newDesc by remember { mutableStateOf("") }
        var newTrigger by remember { mutableStateOf("") }
        var newCategory by remember { mutableStateOf(GoalCategory.SPATIAL) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = AmoledSurface,
            title = {
                Text(
                    "New Proactive Goal",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Configure an autonomous trigger for the S25 Agent.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Goal Title", color = TextSecondary) },
                        placeholder = { Text("e.g., Surat Warehouse Arrival", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = ElectricIndigo,
                            unfocusedBorderColor = AmoledGlassBorder
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Instruction / Mission", color = TextSecondary) },
                        placeholder = { Text("What should the agent do automatically?", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = ElectricIndigo,
                            unfocusedBorderColor = AmoledGlassBorder
                        ),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newTrigger,
                        onValueChange = { newTrigger = it },
                        label = { Text("Trigger Rule", color = TextSecondary) },
                        placeholder = { Text("e.g. Daily 9:00 PM, Geofence Surat", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = ElectricIndigo,
                            unfocusedBorderColor = AmoledGlassBorder
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Select Category:", color = TextSecondary, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(GoalCategory.SPATIAL, GoalCategory.SCHEDULE, GoalCategory.FINANCE).forEach { cat ->
                            val isSel = newCategory == cat
                            FilterChip(
                                selected = isSel,
                                onClick = { newCategory = cat },
                                label = { Text(cat.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = cat.color.copy(alpha = 0.25f),
                                    selectedLabelColor = cat.color,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            if (db != null) {
                                coroutineScope.launch {
                                    db.goalDao().insertGoal(
                                        GoalEntity(
                                            title = newTitle.trim(),
                                            description = newDesc.trim().ifBlank { "Proactive agent automation" },
                                            category = newCategory.name,
                                            triggerInfo = newTrigger.trim().ifBlank { "Autonomous event trigger" },
                                            isActive = true,
                                            lastTriggered = "Armed • New Mission",
                                            priority = "HIGH"
                                        )
                                    )
                                }
                            } else {
                                localGoals = listOf(
                                    GoalItem(
                                        title = newTitle.trim(),
                                        description = newDesc.trim().ifBlank { "Proactive agent automation" },
                                        category = newCategory,
                                        triggerInfo = newTrigger.trim().ifBlank { "Autonomous event trigger" },
                                        isActive = true,
                                        lastTriggered = "Armed • New Mission",
                                        priority = "HIGH"
                                    )
                                ) + localGoals
                            }
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Text("Add Goal", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun GoalCard(
    goal: GoalItem,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (goal.isActive) goal.category.color.copy(alpha = 0.35f) else AmoledGlassBorder

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
                // Category Chip + Priority Tag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(goal.category.color.copy(alpha = 0.15f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                goal.category.icon,
                                contentDescription = null,
                                tint = goal.category.color,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = goal.category.label.uppercase(),
                                color = goal.category.color,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "• ${goal.priority}",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Switch and Delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = goal.isActive,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = goal.category.color,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = AmoledSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete Goal",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = goal.title,
                color = if (goal.isActive) TextPrimary else TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = goal.description,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Trigger & Live Status Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AmoledSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Outlined.Bolt,
                        contentDescription = null,
                        tint = if (goal.isActive) AmberIndicGold else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = goal.triggerInfo,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                Text(
                    text = goal.lastTriggered,
                    color = if (goal.isActive) EmeraldSafeGreen else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MetricPill(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AmoledSurface)
            .border(1.dp, AmoledGlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
