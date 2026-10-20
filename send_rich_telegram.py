import json
import urllib.request
import urllib.parse

BOT_TOKEN = "8826112784:AAGXUjjLyLvXHUd75IFRR8l7JKuuA5kpqm8"
CHAT_ID = "439228682"

text_body = (
    "🛡️ *My Agent (Jerry) — Clean Build #17 (Play Protect Verified)*\n\n"
    "નમસ્તે કિશનભાઈ! 🙏\n"
    "તમારા ફોન માટે એકદમ ક્લીન APK તૈયાર થઈ ગયું છે.\n\n"
    "✅ *Google Play Protect Safe*:\n"
    "• Financial/Fraud રિસ્ક વાળી SMS પરમિશન્સ હટાવી દેવામાં આવી છે.\n"
    "• હવે Google Play Protect બ્લોક નહીં કરે અને સીધું ઇન્સ્ટોલ થઈ જશે!\n\n"
    "✨ *All Verified Jerry Features Intact*:\n"
    "• 🤖 Jerry AI Assistant (My Agent)\n"
    "• 🔮 Cybernetic Quantum Orb 3D Icon\n"
    "• 🎙️ Adaptive Voiceprint Recognition (Kishan Locked)\n"
    "• 🛡️ Lock Screen Voice Wake\n"
    "• ⚠️ Spoken Permission Gate: _\"કિશનભાઈ, શું હું આ બદલાવ કન્ફર્મ કરું?\"_\n"
    "• 🖥️ Live Screen Vision OCR\n"
    "• ☀️ Morning Executive Briefing\n"
    "• ⚡ Compound Workflows, WhatsApp, Maps, Calendar, Contacts, Alarms & Memory Vault\n\n"
    "📲 *Direct 1-Click Clean APK Download*:"
)

payload = {
    "chat_id": CHAT_ID,
    "text": text_body,
    "parse_mode": "Markdown",
    "reply_markup": {
        "inline_keyboard": [
            [{"text": "📥 Download Clean Jerry APK #17", "url": "https://github.com/lalanart786/s25-agent/releases/download/release-17/app-debug.apk"}],
            [{"text": "🌐 View GitHub Release v17", "url": "https://github.com/lalanart786/s25-agent/releases/tag/release-17"}]
        ]
    }
}

url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
data = json.dumps(payload).encode('utf-8')
req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'})

try:
    with urllib.request.urlopen(req) as resp:
        result = json.loads(resp.read().decode('utf-8'))
        if result.get("ok"):
            print("Telegram notification delivered successfully!")
        else:
            print(f"Telegram error: {result}")
except Exception as e:
    print(f"Failed to send Telegram message: {e}")
