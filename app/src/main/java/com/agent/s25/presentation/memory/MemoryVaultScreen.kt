package com.agent.s25.presentation.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agent.s25.data.db.MemoryDatabase
import com.agent.s25.presentation.theme.*
import kotlinx.coroutines.launch

@Composable
fun MemoryVaultScreen(db: MemoryDatabase) {
    val coroutineScope = rememberCoroutineScope()
    val allMemories by db.memoryDao().getAllMemories().collectAsState(initial = emptyList())
    var selectedCategory by remember { mutableStateOf("ALL") }

    val filtered = if (selectedCategory == "ALL") allMemories else allMemories.filter { it.category == selectedCategory }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .padding(16.dp)
    ) {
        // Vault Header with Knox Security Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Memory Vault",
                color = TextPrimary,
                fontSize = 22.sp
            )
            AssistChip(
                onClick = {},
                label = { Text("Knox Sealed (AES-256)", color = SafeGreen, fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SafeGreen) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "CORE_PROFILE", "PREFERENCE", "SPATIAL").forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.replace("_", " "), fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryAccent,
                        selectedLabelColor = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Memory Records List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered) { memory ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AmoledSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = memory.key,
                                color = IndicGold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = memory.value,
                                color = TextPrimary,
                                fontSize = 15.sp
                            )
                        }

                        IconButton(onClick = {
                            coroutineScope.launch {
                                db.memoryDao().deleteMemory(memory.id)
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Forget", tint = WarningRed)
                        }
                    }
                }
            }
        }
    }
}
