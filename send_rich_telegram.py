import requests

BOT_TOKEN = "8826112784:AAGXUjjLyLvXHUd75IFRR8l7JKuuA5kpqm8"
CHAT_ID = "439228682"

text_body = (
    "🚀 *Jerry (My Agent) — Final Verified APK Build #14*\n\n"
    "નમસ્તે કિશનભાઈ! 🙏\n"
    "તમારો પર્સનલાઇઝ્ડ ઓન-ડિવાઇસ એજન્ટ *Jerry* સફળતાપૂર્વક તૈયાર છે.\n\n"
    "✨ *New Identity & Features*:\n"
    "• *App Name*: My Agent\n"
    "• *Agent Identity*: Jerry\n"
    "• *App Icon*: Cybernetic Quantum Orb (3D Holographic Sphere)\n"
    "• *Voice Security*: Adaptive Speaker Training (Kishan Voiceprint Locked)\n"
    "• *Lock Screen Wake*: Ambient Voice-First Wake above Keyguard\n"
    "• *Final-Stage Safety*: Tactile Card + Spoken Confirmation Prompt: \n"
    "  _\"કિશનભાઈ, શું હું આ બદલાવ કન્ફર્મ કરું?\"_\n"
    "• *Ecosystem*: WhatsApp, SMS OTP, Maps, Calendar, Contacts, Alarms, YouTube, Zoho Books\n\n"
    "📲 *Download & Install APK Directly on your S25 Ultra*:"
)

payload = {
    "chat_id": CHAT_ID,
    "text": text_body,
    "parse_mode": "Markdown",
    "reply_markup": {
        "inline_keyboard": [
            [{"text": "📥 Download My Agent (Jerry) APK (46.5 MB)", "url": "https://github.com/lalanart786/s25-agent/releases/download/release-14/app-debug.apk"}],
            [{"text": "🌐 View GitHub Release v14", "url": "https://github.com/lalanart786/s25-agent/releases/tag/release-14"}]
        ]
    }
}

res = requests.post(f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage", json=payload, timeout=20)
print(f"Telegram status: {res.status_code}")
