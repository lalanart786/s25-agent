import json
import urllib.request
import urllib.parse

BOT_TOKEN = "8826112784:AAGXUjjLyLvXHUd75IFRR8l7JKuuA5kpqm8"
CHAT_ID = "439228682"

text_body = (
    "👑 *My Agent (Jerry) — Full Personal Agent Build #22*\n\n"
    "નમસ્તે કિશનભાઈ! 🙏\n"
    "તમારી જરૂરિયાત મુજબ તમામ જેનરિક/કૅન્ડ ટેમ્પ્લેટ્સ સંપૂર્ણપણે હટાવીને 100% પ્યોર ઓન-ડિવાઇસ ન્યુરલ પર્સનલ એજન્ટ સાથે *Build #22* તૈયાર છે!\n\n"
    "🧠 *What is New & Enhanced in Build #22*:\n"
    "• 🎯 *No Generic Canned Responses*: તમામ હાર્ડકોડેડ ફાસ્ટ-પાથ ટેમ્પ્લેટ્સ રદ કર્યા છે. હવે Jerry દર વખતે તમારા સવાલને વાસ્તવિક ન્યુરલ નેટવર્ક (Hermes-3 / Llama-3.1 GGUF) દ્વારા પ્રોસેસ કરીને તાજો, ડાયનેમિક અને વ્યક્તિગત જવાબ આપશે.\n"
    "• 👔 *Kishan's Executive Chief of Staff*: Jerry હવે ફક્ત જેનરિક બોટ નથી, પરંતુ તમારો સમર્પિત ચીફ ઓફ સ્ટાફ છે. તે લાલન આર્ટ (સુરત, લક્ઝરી સાડીઓ, કાનજીભાઈ, ડિસ્પેચ) અને તમારા Knox મેમરી વોલ્ટને સંપૂર્ણપણે ઓળખે છે.\n"
    "• 🎲 *Dynamic Non-Repetitive Sampler*: `llama_sampler_init_penalties` (repetition penalty 1.15) અને ટાઇમ-બેઝ્ડ રેન્ડમ સીડથી સજ્જ — રિપીટ થવાનું નામ જ નહીં!\n"
    "• 🛡️ *Strict Approval Gate*: કોઈ પણ મેસેજ મોકલતા, ફોન કરતા કે ડેટા બદલતા પહેલા Jerry હંમેશા તમારી સ્પષ્ટ મંજૂરી (Action Approval Card) માંગશે.\n"
    "• ⚡ *1-Tap Model Setup Card*: જો મોડેલ ફાઇલ અનમાઉન્ટ થયેલી હોય તો ચેટમાં જ 1-ક્લિક 'Select .gguf File' બટન ઉપલબ્ધ છે.\n\n"
    "📲 *Direct 1-Click APK Download for Galaxy S25 Ultra*:"
)

payload = {
    "chat_id": CHAT_ID,
    "text": text_body,
    "parse_mode": "Markdown",
    "reply_markup": {
        "inline_keyboard": [
            [{"text": "📥 Download Jerry Build #22 (Direct APK)", "url": "https://github.com/lalanart786/s25-agent/releases/download/release-22/app-debug.apk"}],
            [{"text": "🌐 View GitHub Release v22", "url": "https://github.com/lalanart786/s25-agent/releases/tag/release-22"}]
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
