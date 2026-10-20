package com.agent.s25.service

import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.content.Context
import android.content.Intent
import com.agent.s25.presentation.MainActivity

class AgentAssistantSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return object : VoiceInteractionSession(this) {
            override fun onHandleAssist(data: Bundle?, structure: android.app.assist.AssistStructure?, content: android.app.assist.AssistContent?) {
                super.onHandleAssist(data, structure, content)
                // Triggered by S25 Ultra Side-Key long-press or diagonal corner swipe
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("TRIGGER_VOICE_IMMEDIATE", true)
                }
                context.startActivity(intent)
                hide()
            }
        }
    }
}
