package com.agent.s25.presentation.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agent.s25.data.model.ChatMessage
import com.agent.s25.data.model.MessageSender
import com.agent.s25.presentation.theme.*

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val loadedModelName by viewModel.loadedModelName.collectAsState()
    var inputText by remember { mutableStateOf("") }

    // Direct File Picker for .gguf model files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.selectModelUri(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBackground)
    ) {
        // Model Status & Selector Top Banner
        Surface(
            color = AmoledSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (loadedModelName != null) SafeGreen else WarningRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (loadedModelName != null) loadedModelName!! else "No Model Active",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                        Text(
                            text = if (loadedModelName != null) "Snapdragon 8 Elite • Ready" else "Tap Select to choose .gguf",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AmoledSurfaceVariant,
                            contentColor = IndicGold
                        )
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (loadedModelName == null) "Select Model" else "Switch", fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = { viewModel.refreshScan() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rescan", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Divider(color = AmoledSurfaceVariant, thickness = 1.dp)

        // Chat Message List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            reverseLayout = false
        ) {
            items(messages) { msg ->
                ChatBubble(msg = msg, onConfirmTool = { tool, args ->
                    viewModel.confirmToolExecution(msg.id, tool, args)
                })
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Suggested Indic Quick Prompts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionChip(
                onClick = { viewModel.sendMessage("મારી સુરત ઓફિસ પહોંચી ગયા?") },
                label = { Text("📍 સુરત ઓફિસ?", color = IndicGold, fontSize = 12.sp) }
            )
            SuggestionChip(
                onClick = { viewModel.sendMessage("આજનું શેડ્યુલ શું છે?") },
                label = { Text("📅 શેડ્યુલ", color = TextPrimary, fontSize = 12.sp) }
            )
            SuggestionChip(
                onClick = { viewModel.sendMessage("ફોનની બેટરી કેટલી છે?") },
                label = { Text("🔋 બેટરી", color = TextPrimary, fontSize = 12.sp) }
            )
        }

        // Input & Live Voice Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AmoledSurface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                placeholder = { Text("ગુજરાતી, હિન્દી કે English માં લખો...", color = TextSecondary, fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AmoledSurfaceVariant,
                    unfocusedContainerColor = AmoledSurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = PrimaryAccent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Mic Button (Sherpa-ONNX Native Voice)
            IconButton(
                onClick = {
                    if (isListening) viewModel.stopListeningAndSend()
                    else viewModel.startListening()
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(if (isListening) IndicGold else AmoledSurfaceVariant, CircleShape)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = if (isListening) AmoledBackground else TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Send Button
            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !isGenerating) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(PrimaryAccent, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = TextPrimary
                )
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage, onConfirmTool: (String, String) -> Unit) {
    val isUser = msg.sender == MessageSender.USER
    val isTool = msg.sender == MessageSender.TOOL_OUTPUT
    val isSystem = msg.sender == MessageSender.SYSTEM

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    when {
                        isUser -> PrimaryAccent
                        isTool -> AmoledSurfaceVariant
                        isSystem -> AmoledSurfaceVariant.copy(alpha = 0.8f)
                        else -> AmoledSurface
                    }
                )
                .padding(14.dp)
        ) {
            Text(
                text = msg.text,
                color = if (isSystem) IndicGold else TextPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }

        // Interactive Action Card for Tiered Tool Confirmations
        if (msg.isPendingConfirmation && msg.toolCallPayload != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = AmoledSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "⚠️ Confirmation Required for Action",
                        color = IndicGold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = msg.toolCallPayload,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onConfirmTool("execute", msg.toolCallPayload) },
                            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                        ) {
                            Text("Confirm & Run", color = Color.White)
                        }
                        OutlinedButton(onClick = { /* Reject */ }) {
                            Text("Reject", color = WarningRed)
                        }
                    }
                }
            }
        }
    }
}
