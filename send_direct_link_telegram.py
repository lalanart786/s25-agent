import os
import sys
import json
import urllib.request

BOT_TOKEN = "8826112784:AAGXUjjLyLvXHUd75IFRR8l7JKuuA5kpqm8"
CHAT_ID = "439228682"

direct_url = sys.argv[1] if len(sys.argv) > 1 else ""
release_tag = sys.argv[2] if len(sys.argv) > 2 else "release"

text_body = (
    "🚀 *My Agent (Jerry) — Direct 1-Click APK Download Ready!*\n\n"
    "નમસ્તે કિશનભાઈ! 🙏\n"
    "ગિટહબ પ્રાઇવેટ હોવાને લીધે જે 404 / App not installed ની એરર આવતી હતી તે સોલ્વ કરી દીધી છે.\n"
    "હવે કોઈ લોગિન કે પાસવર્ડ વગર સીધું જ ૧-ક્લિકમાં APK ડાઉનલોડ થશે!\n\n"
    f"📱 *Release*: `{release_tag}`\n"
    "🧠 *Engine*: Snapdragon 8 Elite + Hermes 3 On-Device GGUF\n"
    "⌨️ *Chat UI*: કીબોર્ડ ખુલતા ચેટ બોક્સ અને સેન્ડ બટન ક્લીન દેખાશે\n"
    "📐 *Header*: 50% નાનું સ્લીક 38dp હેડર\n"
    "🔇 *Speech*: માત્ર માઇકથી બોલો ત્યારે જ અવાજમાં જવાબ\n\n"
    "💡 *ઇન્સ્ટોલેશન ગાઇડ*:\n"
    "1. જો જૂનું વર્ઝન ફોનમાં હોય, તો તેને *એકવાર Uninstall* કરી દો.\n"
    "2. નીચે આપેલા **'📥 Download Jerry APK'** બટન પર ટેપ કરો.\n"
    "3. ડાઉનલોડ પૂરું થતાં જ નોટિફિકેશન પર ટેપ કરીને **Install** કરો!"
)

payload = {
    "chat_id": CHAT_ID,
    "text": text_body,
    "parse_mode": "Markdown",
    "reply_markup": {
        "inline_keyboard": [
            [{"text": "📥 Download Jerry APK (Direct Fast Link)", "url": direct_url}],
            [{"text": "🌐 Alternate Download (Catbox)", "url": direct_url}]
        ]
    }
}

url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
data = json.dumps(payload).encode('utf-8')
req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'})

try:
    with urllib.request.urlopen(req) as resp:
        res = json.loads(resp.read().decode('utf-8'))
        if res.get("ok"):
            print("Direct Telegram link delivered successfully!")
        else:
            print("Telegram error:", res)
except Exception as e:
    print("Failed to deliver Telegram message:", e)
