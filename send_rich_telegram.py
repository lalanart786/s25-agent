import json
import urllib.request
import urllib.parse

BOT_TOKEN = "8826112784:AAGXUjjLyLvXHUd75IFRR8l7JKuuA5kpqm8"
CHAT_ID = "439228682"

text_body = (
    "🚀 *My Agent (Jerry) — Verified Build #18*\n\n"
    "નમસ્તે કિશનભાઈ! 🙏\n"
    "ચેટ અને કીબોર્ડના તમામ સુધારાઓ સાથે નવું *Build #18* તૈયાર છે!\n\n"
    "✨ *New Improvements & Bug Fixes*:\n"
    "• 🎯 *Dynamic Intent Fix*: હવે \"what you can do for me\" કે કોઈપણ સવાલ પર જૂનો રીપીટ રીપ્લાય નહીં આવે; Jerry પોતાના તમામ ફીચર્સ અને કામગીરીની સાચી માહિતી આપશે.\n"
    "• ☁️ *OpenRouter Multi-Model AI*: ઉપર 'API Key' બટનથી OpenRouter કી સેટ કરી Claude 3.5, DeepSeek R1, GPT-4o નો હાઈ-સ્પીડ બહુભાષી પાવર મેળવી શકશો.\n"
    "• ⌨️ *Floating Input Bar Above Keyboard*: કીબોર્ડ ખુલતા જ ઇનપુટ બાર આપમેળે કીબોર્ડની ઉપર ફ્લોટ થશે (`imePadding()`).\n"
    "• 📲 *Keyboard 'Send' Action (IME Send)*: હવે સેમસંગ કીબોર્ડના ખૂણામાં સીધું Send બટન છે — કીબોર્ડ બંધ કર્યા વગર ૧-ટેપમાં મેસેજ મોકલાશે!\n"
    "• 🛡️ *100% Play Protect Safe*: કોઈ બ્લોક વગર સરળ ઇન્સ્ટોલેશન.\n\n"
    "📲 *Direct 1-Click APK Download for Galaxy S25 Ultra*:"
)

payload = {
    "chat_id": CHAT_ID,
    "text": text_body,
    "parse_mode": "Markdown",
    "reply_markup": {
        "inline_keyboard": [
            [{"text": "📥 Download Jerry Build #18 (Direct APK)", "url": "https://github.com/lalanart786/s25-agent/releases/download/release-18/app-debug.apk"}],
            [{"text": "🌐 View GitHub Release v18", "url": "https://github.com/lalanart786/s25-agent/releases/tag/release-18"}]
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
