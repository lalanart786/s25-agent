package com.agent.s25.presentation.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    val liveActionStep by viewModel.liveActionStep.collectAsState()
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
        // Meta Muse Header with Avatar & Model Switcher
        MetaMuseHeader(
            loadedModelName = loadedModelName,
            isGenerating = isGenerating,
            isListening = isListening,
            onSelectModel = { filePickerLauncher.launch(arrayOf("*/*")) },
            onRescan = { viewModel.refreshScan() }
        )

        // Live 3-Layer Telemetry Status Banner
        LiveThreeLayerStatusBanner(
            isGenerating = isGenerating,
            isListening = isListening,
            hasModel = loadedModelName != null,
            actionStep = liveActionStep
        )

        // Chat Message List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(
                    msg = msg,
                    onConfirmTool = { tool, args ->
                        viewModel.confirmToolExecution(msg.id, tool, args)
                    },
                    onRejectTool = {
                        viewModel.rejectToolExecution(msg.id)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Indic Suggestion Chips for quick taps
        IndicQuickSuggestionsBar(
            onSelectPrompt = { prompt ->
                viewModel.sendMessage(prompt)
            },
            onTriggerApprovalTest = {
                viewModel.triggerSampleApprovalCard()
            }
        )

        // Floating Meta Muse Input Bar with Waveform Mic
        FloatingInputBar(
            inputText = inputText,
            onTextChange = { inputText = it },
            isGenerating = isGenerating,
            isListening = isListening,
            onSend = {
                if (inputText.isNotBlank() && !isGenerating) {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                }
            },
            onMicClick = {
                if (isListening) viewModel.stopListeningAndSend()
                else viewModel.startListening()
            }
        )
    }
}

@Composable
private fun MetaMuseHeader(
    loadedModelName: String?,
    isGenerating: Boolean,
    isListening: Boolean,
    onSelectModel: () -> Unit,
    onRescan: () -> Unit
) {
    Surface(
        color = AmoledSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Glowing Meta Muse Avatar + Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetaMuseAgentAvatar(
                    isActive = loadedModelName != null,
                    isBusy = isGenerating || isListening
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "S25 Muse Agent",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ElectricIndigo.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ALEX CORNELL SPEC",
                                color = SoftCyan,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Text(
                        text = if (loadedModelName != null) loadedModelName else "No GGUF Active • Tap Select",
                        color = if (loadedModelName != null) TextSecondary else AmberIndicGold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // Right: Model Switch Pill & Rescan
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(AmoledSurfaceVariant)
                        .border(1.dp, AmoledGlassBorder, RoundedCornerShape(16.dp))
                        .clickable(onClick = onSelectModel)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = SoftCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (loadedModelName == null) "Select" else "Switch",
                            color = SoftCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = onRescan,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Rescan",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaMuseAgentAvatar(isActive: Boolean, isBusy: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatarPulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isBusy) 1.35f else 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isBusy) 800 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isBusy) 0.6f else 0.35f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isBusy) 800 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(38.dp)
    ) {
        // Outer pulsing ring
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        if (isBusy) SoftCyan.copy(alpha = pulseAlpha) else ElectricIndigo.copy(alpha = pulseAlpha)
                    )
            )
        }

        // Inner avatar core
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ElectricIndigo,
                            Color(0xFF1E1B4B)
                        )
                    )
                )
                .border(1.dp, SoftCyan.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = "Muse Avatar",
                tint = if (isActive) TextPrimary else TextMuted,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun LiveThreeLayerStatusBanner(
    isGenerating: Boolean,
    isListening: Boolean,
    hasModel: Boolean,
    actionStep: String = ""
) {
    val layer1Text = when {
        isGenerating -> "Thinking..."
        isListening -> "Listening (Indic STT)..."
        hasModel -> "Active"
        else -> "Standby"
    }

    val layer1Color = when {
        isGenerating -> AmberIndicGold
        isListening -> SoftCyan
        hasModel -> EmeraldSafeGreen
        else -> TextMuted
    }

    val layer2Text = if (actionStep.isNotBlank()) {
        actionStep
    } else {
        when {
            isGenerating -> "Snapdragon 8 Elite • Synthesizing response"
            isListening -> "Sampling UnifiedAudioRouter stream in Gujarati/Hindi/English"
            hasModel -> "Ready for voice or text prompt"
            else -> "No model loaded. Tap 'Select' to load .gguf"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmoledSurfaceVariant)
            .border(
                width = 1.dp,
                color = AmoledGlassBorder,
                shape = RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Layer 1 & Layer 2
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Layer 1 Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(layer1Color.copy(alpha = 0.15f))
                        .border(1.dp, layer1Color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(layer1Color)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = layer1Text,
                            color = layer1Color,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Layer 2: Real-time action description
                Text(
                    text = layer2Text,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Layer 3: Telemetry tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AmoledSurface)
                    .border(1.dp, AmoledGlassBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Snapdragon 8 Elite • 6T • Vulkan",
                    color = SoftCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun ChatBubble(
    msg: ChatMessage,
    onConfirmTool: (String, String) -> Unit,
    onRejectTool: () -> Unit
) {
    val isUser = msg.sender == MessageSender.USER
    val isTool = msg.sender == MessageSender.TOOL_OUTPUT
    val isSystem = msg.sender == MessageSender.SYSTEM

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Bubble
        Box(
            modifier = Modifier
                .widthIn(max = 330.dp)
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
                        isUser -> Brush.linearGradient(
                            listOf(
                                ElectricIndigo.copy(alpha = 0.9f),
                                Color(0xFF4338CA)
                            )
                        )
                        isTool -> Brush.linearGradient(
                            listOf(
                                Color(0xFF0F172A),
                                AmoledSurfaceVariant
                            )
                        )
                        isSystem -> Brush.linearGradient(
                            listOf(
                                AmoledSurfaceVariant,
                                AmoledSurface
                            )
                        )
                        else -> Brush.linearGradient(
                            listOf(
                                AmoledSurface,
                                AmoledSurfaceElevated
                            )
                        )
                    }
                )
                .border(
                    width = 1.dp,
                    color = when {
                        isUser -> ElectricIndigo
                        isTool -> SoftCyan.copy(alpha = 0.4f)
                        isSystem -> AmberIndicGold.copy(alpha = 0.3f)
                        else -> AmoledGlassBorder
                    },
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column {
                if (isTool) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint = SoftCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "MCP TOOL EXECUTION OUTPUT",
                            color = SoftCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Text(
                    text = msg.text,
                    color = when {
                        isSystem -> AmberIndicGold
                        isTool -> SoftCyan
                        else -> TextPrimary
                    },
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    fontFamily = if (isTool) FontFamily.Monospace else FontFamily.Default
                )
            }
        }

        // Meta Muse Interactive Action Approval Card
        if (msg.isPendingConfirmation && msg.toolCallPayload != null) {
            MetaMuseApprovalCard(
                payload = msg.toolCallPayload,
                onConfirm = { onConfirmTool("execute", msg.toolCallPayload) },
                onReject = onRejectTool
            )
        }
    }
}

@Composable
private fun MetaMuseApprovalCard(
    payload: String,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(top = 10.dp),
        colors = CardDefaults.cardColors(containerColor = AmoledSurfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(AmberIndicGold.copy(alpha = 0.6f), ElectricIndigo.copy(alpha = 0.6f)))
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = AmberIndicGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Action Approval Required",
                        color = AmberIndicGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AmberIndicGold.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PRIVILEGED",
                        color = AmberIndicGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "The agent proposes to execute the following tool action on your device. Review the parameters before proceeding:",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Parameter Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF09090D))
                    .border(1.dp, AmoledGlassBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = payload,
                    color = SoftCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Confirm & Execute (Green) / Reject (Outlined)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Confirm & Execute Button
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafeGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Confirm & Execute",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Reject Button
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(0.7f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DangerRed.copy(alpha = 0.6f))
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reject",
                        color = DangerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun IndicQuickSuggestionsBar(
    onSelectPrompt: (String) -> Unit,
    onTriggerApprovalTest: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SuggestionPill(
            label = "📍 સુરત ઓફિસ?",
            accent = SoftCyan,
            onClick = { onSelectPrompt("મારી સુરત ઓફિસ નો રસ્તો બતાવ.") }
        )
        SuggestionPill(
            label = "📊 Zoho ઇન્વોઇસ",
            accent = AmberIndicGold,
            onClick = { onSelectPrompt("આજના પેન્ડિંગ Zoho Books ઇન્વોઇસ બતાવ.") }
        )
        SuggestionPill(
            label = "📅 આજની ઇવેન્ટ્સ",
            accent = ElectricIndigo,
            onClick = { onSelectPrompt("આજનું કેલેન્ડર શું છે?") }
        )
        SuggestionPill(
            label = "⏰ સવારે 7 નું એલાર્મ",
            accent = AmberIndicGold,
            onClick = { onSelectPrompt("સવારે 7:00 વાગ્યાનું એલાર્મ સેટ કર.") }
        )
        SuggestionPill(
            label = "📋 ક્લિપબોર્ડ",
            accent = SoftCyan,
            onClick = { onSelectPrompt("ક્લિપબોર્ડ માં શું કોપી થયેલું છે?") }
        )
        SuggestionPill(
            label = "🔋 બેટરી",
            accent = EmeraldSafeGreen,
            onClick = { onSelectPrompt("ફોનની બેટરી કેટલી છે?") }
        )
        SuggestionPill(
            label = "⚡ Approval Demo",
            accent = ElectricIndigo,
            onClick = onTriggerApprovalTest
        )
    }
}

@Composable
private fun SuggestionPill(
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AmoledSurface)
            .border(1.dp, AmoledGlassBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FloatingInputBar(
    inputText: String,
    onTextChange: (String) -> Unit,
    isGenerating: Boolean,
    isListening: Boolean,
    onSend: () -> Unit,
    onMicClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(AmoledSurface)
                .border(1.dp, AmoledGlassBorder, RoundedCornerShape(28.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text Field
                TextField(
                    value = inputText,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "ગુજરાતી, हिन्दी, English માં લખો...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = ElectricIndigo,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Waveform Mic Button
                WaveformMicButton(
                    isListening = isListening,
                    onClick = onMicClick
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Gradient Send Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank() && !isGenerating) MuseElectricGradient
                            else Brush.linearGradient(listOf(AmoledSurfaceVariant, AmoledSurfaceVariant))
                        )
                        .clickable(enabled = inputText.isNotBlank() && !isGenerating, onClick = onSend),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank() && !isGenerating) TextPrimary else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WaveformMicButton(
    isListening: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(280, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(310, easing = LinearEasing), RepeatMode.Reverse),
        label = "h4"
    )

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(
                if (isListening) AmberIndicGold.copy(alpha = 0.2f) else AmoledSurfaceVariant
            )
            .border(
                1.dp,
                if (isListening) AmberIndicGold else AmoledGlassBorder,
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isListening) {
            // Waveform animation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.5.dp)
            ) {
                WaveformBar(height = h1, color = AmberIndicGold)
                WaveformBar(height = h2, color = AmberIndicGold)
                WaveformBar(height = h3, color = AmberIndicGold)
                WaveformBar(height = h4, color = AmberIndicGold)
            }
        } else {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Input",
                tint = TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun WaveformBar(height: Float, color: Color) {
    Box(
        modifier = Modifier
            .width(2.5.dp)
            .height(height.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}
