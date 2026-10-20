import json
import urllib.request
import urllib.parse

BOT_TOKEN = "8826112784:AAGXUjjLyLvXHUd75IFRR8l7JKuuA5kpqm8"
CHAT_ID = "439228682"

text_body = (
    "🚀 *My Agent (Jerry) — Verified Build #19*\n\n"
    "નમસ્તે કિશનભાઈ! 🙏\n"
    "તમારી જરૂરિયાત મુજબ નવી સ્પીચ પોલિસી અને ઇંગ્લિશ પ્રાઇમરી લેંગ્વેજ સાથે *Build #19* તૈયાર છે!\n\n"
    "✨ *New Updates in Build #19*:\n"
    "• 🤫 *Quiet Text Mode*: ટાઇપ કરીને મોકલેલા તમામ મેસેજમાં Jerry શાંત રહેશે (કોઈ સ્પીક આઉટ નહીં થાય).\n"
    "• 🎙️ *Speak ONLY on Voice / Permission*: Jerry ફક્ત ત્યારે જ બોલશે જ્યારે તમે માઇક/વૉઇસથી પૂછશો અથવા પરમિશન પૂછવાની હોય (_\"કિશનભાઈ, શું હું આ બદલાવ કન્ફર્મ કરું?\"_).\n"
    "• 🌐 *English as Primary*: મુખ્ય ડિફોલ્ટ ભાષા English છે, અને તમે ગુજરાતી કે હિન્દીમાં બોલશો તો આપમેળે તે જ ભાષામાં જવાબ આપશે.\n"
    "• ⌨️ *Floating IME Input Bar*: કીબોર્ડની બરાબર ઉપર ઇનપુટ બાર અને કીબોર્ડમાં જ 'Send' બટન.\n"
    "• ☁️ *OpenRouter Cloud AI*: 'API Key' બટનથી Claude 3.5, DeepSeek R1 કે GPT-4o કનેક્ટ કરી શકાશે.\n\n"
    "📲 *Direct 1-Click APK Download for Galaxy S25 Ultra*:"
)

payload = {
    "chat_id": CHAT_ID,
    "text": text_body,
    "parse_mode": "Markdown",
    "reply_markup": {
        "inline_keyboard": [
            [{"text": "📥 Download Jerry Build #19 (Direct APK)", "url": "https://github.com/lalanart786/s25-agent/releases/download/release-19/app-debug.apk"}],
            [{"text": "🌐 View GitHub Release v19", "url": "https://github.com/lalanart786/s25-agent/releases/tag/release-19"}]
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
