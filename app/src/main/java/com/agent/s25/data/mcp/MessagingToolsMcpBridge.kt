package com.agent.s25.data.mcp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * Communication & Messaging MCP Bridge:
 * Handles WhatsApp direct messaging, SMS inbox reading, OTP detection,
 * SMS drafting/sending, Gmail/Email drafting, and Telegram message dispatching.
 */
class MessagingToolsMcpBridge(private val context: Context) {

    private val gson = Gson()

    companion object {
        private const val TAG = "MessagingMcpBridge"
        private val OTP_REGEX = Pattern.compile("(?i)\\b(?:code|otp|passcode|pin|secret|verification)\\b[^0-9]{0,15}([0-9]{4,8})")
        private val GENERIC_NUM_REGEX = Pattern.compile("\\b[0-9]{4,8}\\b")
    }

    fun getToolDefinitions(): List<McpToolDefinition> {
        return listOf(
            McpToolDefinition(
                serverId = "embedded_android",
                name = "read_latest_sms",
                description = "Read the most recent incoming SMS messages from the phone inbox",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"limit\":{\"type\":\"integer\",\"description\":\"Number of messages to retrieve (default 5)\"}}}",
                isReadOnly = true
            ),
            McpToolDefinition(
                serverId = "embedded_android",
                name = "get_latest_otp",
                description = "Automatically find and extract the most recent one-time password (OTP) or 2FA code from SMS inbox",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{}}",
                isReadOnly = true
            ),
            McpToolDefinition(
                serverId = "embedded_android",
                name = "send_sms",
                description = "Draft or send an SMS text message to a specific phone number",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"recipient\":{\"type\":\"string\",\"description\":\"Phone number of recipient\"},\"message\":{\"type\":\"string\",\"description\":\"Message body text\"}},\"required\":[\"recipient\",\"message\"]}",
                isReadOnly = false
            ),
            McpToolDefinition(
                serverId = "embedded_android",
                name = "send_whatsapp_message",
                description = "Send or draft a direct WhatsApp message to a phone number or contact",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"phone_number\":{\"type\":\"string\",\"description\":\"Recipient phone number with country code (e.g. +919876543210)\"},\"message\":{\"type\":\"string\",\"description\":\"Message text content\"}},\"required\":[\"message\"]}",
                isReadOnly = false
            ),
            McpToolDefinition(
                serverId = "embedded_android",
                name = "send_email_draft",
                description = "Draft or compose an email with recipient address, subject line, and body content",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"to\":{\"type\":\"string\",\"description\":\"Recipient email address\"},\"subject\":{\"type\":\"string\",\"description\":\"Email subject line\"},\"body\":{\"type\":\"string\",\"description\":\"Email message body\"}},\"required\":[\"subject\",\"body\"]}",
                isReadOnly = false
            ),
            McpToolDefinition(
                serverId = "embedded_android",
                name = "send_telegram_message",
                description = "Draft or send a message via Telegram messenger",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\",\"description\":\"Message text to send via Telegram\"},\"username\":{\"type\":\"string\",\"description\":\"Optional Telegram username\"}},\"required\":[\"message\"]}",
                isReadOnly = false
            )
        )
    }

    suspend fun executeTool(name: String, arguments: JsonObject): String {
        return when (name) {
            "read_latest_sms" -> readLatestSms(arguments)
            "get_latest_otp" -> getLatestOtp()
            "send_sms" -> sendSms(arguments)
            "send_whatsapp_message" -> sendWhatsAppMessage(arguments)
            "send_email_draft" -> sendEmailDraft(arguments)
            "send_telegram_message" -> sendTelegramMessage(arguments)
            else -> "{\"error\":\"Unknown messaging tool: $name\"}"
        }
    }

    private fun readLatestSms(arguments: JsonObject): String {
        val limit = arguments.get("limit")?.asInt?.coerceIn(1, 20) ?: 5
        val result = JsonObject()
        val smsList = JsonArray()

        if (context.checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            result.addProperty("status", "permission_required")
            result.addProperty("message", "READ_SMS permission is not granted. Please allow SMS permission in Android settings.")
            return gson.toJson(result)
        }

        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val sortOrder = "${Telephony.Sms.DATE} DESC LIMIT $limit"

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, null, null, sortOrder)
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

            cursor?.let {
                val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    val item = JsonObject()
                    val sender = if (addressIdx >= 0) it.getString(addressIdx) ?: "Unknown" else "Unknown"
                    val body = if (bodyIdx >= 0) it.getString(bodyIdx) ?: "" else ""
                    val timestamp = if (dateIdx >= 0) it.getLong(dateIdx) else 0L

                    item.addProperty("sender", sender)
                    item.addProperty("body", body)
                    item.addProperty("time", if (timestamp > 0) sdf.format(Date(timestamp)) else "Unknown")
                    smsList.add(item)
                }
            }
            result.addProperty("status", "success")
            result.addProperty("count", smsList.size())
            result.add("messages", smsList)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading SMS inbox: ${e.message}", e)
            result.addProperty("status", "error")
            result.addProperty("error", e.message ?: "Failed to read SMS inbox")
        } finally {
            cursor?.close()
        }

        return gson.toJson(result)
    }

    private fun getLatestOtp(): String {
        val result = JsonObject()

        if (context.checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            result.addProperty("status", "permission_required")
            result.addProperty("message", "READ_SMS permission required to detect OTP codes.")
            return gson.toJson(result)
        }

        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val sortOrder = "${Telephony.Sms.DATE} DESC LIMIT 10"

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, null, null, sortOrder)
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())

            cursor?.let {
                val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    val sender = if (addressIdx >= 0) it.getString(addressIdx) ?: "" else ""
                    val body = if (bodyIdx >= 0) it.getString(bodyIdx) ?: "" else ""
                    val timestamp = if (dateIdx >= 0) it.getLong(dateIdx) else 0L

                    // Check for OTP pattern
                    val matcher = OTP_REGEX.matcher(body)
                    var detectedCode: String? = null
                    if (matcher.find()) {
                        detectedCode = matcher.group(1)
                    } else if (body.contains("OTP", ignoreCase = true) || body.contains("verification", ignoreCase = true)) {
                        val numMatcher = GENERIC_NUM_REGEX.matcher(body)
                        if (numMatcher.find()) {
                            detectedCode = numMatcher.group(0)
                        }
                    }

                    if (detectedCode != null) {
                        result.addProperty("status", "success")
                        result.addProperty("otp", detectedCode)
                        result.addProperty("sender", sender)
                        result.addProperty("time", if (timestamp > 0) sdf.format(Date(timestamp)) else "Recent")
                        result.addProperty("snippet", body.take(120))
                        return gson.toJson(result)
                    }
                }
            }
            result.addProperty("status", "not_found")
            result.addProperty("message", "No recent OTP found in the latest 10 SMS messages.")
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting OTP: ${e.message}", e)
            result.addProperty("status", "error")
            result.addProperty("error", e.message ?: "Failed to scan for OTP")
        } finally {
            cursor?.close()
        }

        return gson.toJson(result)
    }

    private fun sendSms(arguments: JsonObject): String {
        val recipient = arguments.get("recipient")?.asString?.trim() ?: ""
        val message = arguments.get("message")?.asString ?: ""
        val result = JsonObject()

        if (recipient.isEmpty()) {
            result.addProperty("status", "error")
            result.addProperty("error", "Recipient phone number is required.")
            return gson.toJson(result)
        }

        try {
            val sendIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$recipient")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(sendIntent)
            result.addProperty("status", "success")
            result.addProperty("action", "sms_drafted")
            result.addProperty("recipient", recipient)
            result.addProperty("message_preview", message.take(80))
        } catch (e: Exception) {
            Log.e(TAG, "Error launching SMS composer: ${e.message}", e)
            result.addProperty("status", "error")
            result.addProperty("error", e.message ?: "Failed to launch SMS composer")
        }

        return gson.toJson(result)
    }

    private fun sendWhatsAppMessage(arguments: JsonObject): String {
        val phoneNumber = arguments.get("phone_number")?.asString?.trim()?.replace(" ", "")?.replace("-", "") ?: ""
        val message = arguments.get("message")?.asString ?: ""
        val result = JsonObject()

        try {
            val encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
            val intent = if (phoneNumber.isNotEmpty()) {
                val cleanNumber = if (phoneNumber.startsWith("+")) phoneNumber.substring(1) else phoneNumber
                Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=$encodedMessage")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    `package` = "com.whatsapp"
                    putExtra(Intent.EXTRA_TEXT, message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            // Fallback check
            val resolved = intent.resolveActivity(context.packageManager)
            if (resolved != null) {
                context.startActivity(intent)
                result.addProperty("status", "success")
                result.addProperty("action", "whatsapp_dispatched")
                result.addProperty("recipient", if (phoneNumber.isNotEmpty()) phoneNumber else "WhatsApp Chooser")
                result.addProperty("message_preview", message.take(80))
            } else {
                // Try standard browser url
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                result.addProperty("status", "success")
                result.addProperty("action", "whatsapp_web_fallback")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching WhatsApp message: ${e.message}", e)
            result.addProperty("status", "error")
            result.addProperty("error", e.message ?: "Failed to dispatch WhatsApp")
        }

        return gson.toJson(result)
    }

    private fun sendEmailDraft(arguments: JsonObject): String {
        val to = arguments.get("to")?.asString?.trim() ?: ""
        val subject = arguments.get("subject")?.asString ?: ""
        val body = arguments.get("body")?.asString ?: ""
        val result = JsonObject()

        try {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                if (to.isNotEmpty()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
                }
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(emailIntent)
            result.addProperty("status", "success")
            result.addProperty("action", "email_drafted")
            result.addProperty("recipient", to.ifEmpty { "Default" })
            result.addProperty("subject", subject)
        } catch (e: Exception) {
            Log.e(TAG, "Error drafting email: ${e.message}", e)
            result.addProperty("status", "error")
            result.addProperty("error", e.message ?: "Failed to open email composer")
        }

        return gson.toJson(result)
    }

    private fun sendTelegramMessage(arguments: JsonObject): String {
        val message = arguments.get("message")?.asString ?: ""
        val username = arguments.get("username")?.asString?.trim()?.removePrefix("@") ?: ""
        val result = JsonObject()

        try {
            val encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
            val uri = if (username.isNotEmpty()) {
                Uri.parse("https://t.me/$username?text=$encodedMessage")
            } else {
                Uri.parse("tg://msg?text=$encodedMessage")
            }

            val telegramIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val resolved = telegramIntent.resolveActivity(context.packageManager)
            if (resolved != null) {
                context.startActivity(telegramIntent)
                result.addProperty("status", "success")
                result.addProperty("action", "telegram_dispatched")
                result.addProperty("username", username.ifEmpty { "Telegram Chooser" })
            } else {
                // Fallback web url
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/share/url?url=&text=$encodedMessage")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                result.addProperty("status", "success")
                result.addProperty("action", "telegram_web_dispatched")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching Telegram message: ${e.message}", e)
            result.addProperty("status", "error")
            result.addProperty("error", e.message ?: "Failed to launch Telegram")
        }

        return gson.toJson(result)
    }
}
