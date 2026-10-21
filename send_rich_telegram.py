import json
import urllib.request
import urllib.parse

BOT_TOKEN = "8826112784:AAGXUjjLyLvXHUd75IFRR8l7JKuuA5kpqm8"
CHAT_ID = "439228682"

text_body = (
    "🛠️ *My Agent (Jerry) — Installation Verified Build #24*\n\n"
    "નમસ્તે કિશનભાઈ! 🙏\n"
    "ફોનમાં 'App not installed' ની સમસ્યાનું મૂળ કારણ શોધીને સંપૂર્ણ ફિક્સ સાથે *Build #24* તૈયાર છે!\n\n"
    "🔧 *Why It Failed & What Is Fixed in Build #24*:\n"
    "1. 🚫 *Removed System Privilege Attributes*: મેનિફેસ્ટમાંથી `showForAllUsers` (જે ફક્ત સેમસંગ સિસ્ટમ એપ્સ માટે માન્ય છે) અને અનસપોર્ટેડ `specialUse` ફ્લેગ દૂર કર્યા છે, જેથી One UI 7 / Android 15 નું પેકેજ ઇન્સ્ટોલર બ્લોક ન કરે.\n"
    "2. 🔑 *Permanent Cryptographic Keystore*: કાયમી સત્તાવાર કીસ્ટોર (`s25-agent.keystore`) થી APK સાઇન કર્યું છે, જેથી દર વખતે સર્ટિફિકેટ બદલાશે નહીં.\n"
    "3. 📈 *Version Code Bumped (v24)*: વર્ઝન કોડ ૨૪ કર્યો છે.\n\n"
    "💡 *ઇન્સ્ટોલેશન ટિપ*: જો તમારા ફોનમાં અગાઉનું જૂનું બિલ્ડ ઇન્સ્ટોલ હોય, તો તેને *એકવાર Uninstall (Delete)* કરીને આ નવું Build #24 ઇન્સ્ટોલ કરો. ત્યારબાદ ભવિષ્યના તમામ અપડેટ્સ સીધા ૧-ક્લિકમાં અપડેટ થઈ જશે!\n\n"
    "📲 *Direct 1-Click APK Download for Galaxy S25 Ultra*:"
)

payload = {
    "chat_id": CHAT_ID,
    "text": text_body,
    "parse_mode": "Markdown",
    "reply_markup": {
        "inline_keyboard": [
            [{"text": "📥 Download Jerry Build #24 (Direct APK)", "url": "https://github.com/lalanart786/s25-agent/releases/download/release-24/app-debug.apk"}],
            [{"text": "🌐 View GitHub Release v24", "url": "https://github.com/lalanart786/s25-agent/releases/tag/release-24"}]
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
