import json
import urllib.request
import urllib.parse

BOT_TOKEN = "8826112784:AAGXUjjLyLvXHUd75IFRR8l7JKuuA5kpqm8"
CHAT_ID = "439228682"

text_body = (
    "🚀 *My Agent (Jerry) — Critical Engine & UI/UX Upgrade (Release #26)*\n\n"
    "નમસ્તે કિશનભાઈ! 🙏\n"
    "તમે જણાવેલી બધી જ સમસ્યાઓ (મોડલ એકસરખો જવાબ આપતું હતું, કીબોર્ડ ખુલતા ચેટ બોક્સ ઢંકાઈ જતું હતું, હેડર અડધી સ્ક્રીન રોકતું હતું) નું સંપૂર્ણ સોલ્યુશન *Release #26* માં તૈયાર છે!\n\n"
    "🔍 *મુખ્ય સુધારાઓ (What Is Fixed in Release #26)*:\n\n"
    "1. 🧠 *AI મોડલ એકસરખો જવાબ કેમ આપતું હતું? (Root Cause Fixed)*:\n"
    "   - એન્ડ્રોઇડ ૧૫ / સેમસંગ വન UI ૭ માં `libggml-opencl.so` ને સિસ્ટમ `libOpenCL.so` ની જરૂર હતી જે સામાન્ય એપ્સ માટે લિંકર દ્વારા બ્લોક હતી. તેથી JNI એન્જિન લોડ ન થતાં ફોલબેક મેસેજ આવતો હતો.\n"
    "   - અમે એક કસ્ટમ ઓન-ડિવાઇસ OpenCL શિમ ડ્રાઇવર (`opencl_loader.c`) બનાવ્યો છે જે APK ની અંદર જ સમાયેલો છે. હવે Snapdragon 8 Elite નું Adreno 830 GPU અને Oryon CPU બંને સંપૂર્ણ સક્રિય થઈ ગયા છે અને Hermes 3 મોડલ ખરેખર ન્યુરલ ટોકન્સ જનરેટ કરશે!\n"
    "   - બોગસ સિમ્યુલેટેડ જવાબો સંપૂર્ણ ડિલીટ કરી દીધા છે.\n\n"
    "2. ⌨️ *કીબોર્ડ અને ચેટ બોક્સ UI સુધારો*:\n"
    "   - કીબોર્ડ ખુલતાની સાથે જ નીચેની નેવિગેશન બાર અને સજેશન ચિપ્સ આપોઆપ હાઇડ થઈ જશે, જેથી તમારું ચેટ બોક્સ અને પર્પલ સેન્ડ બટન સીધું કીબોર્ડની ઉપર ક્લીન દેખાશે. કીબોર્ડ બંધ કર્યા વગર જ મેસેજ સેન્ડ થઈ જશે!\n\n"
    "3. 📱 *હેડરની સાઈઝ ૫૦% થી વધુ નાની કરી*:\n"
    "   - ઉપરનું મોટું 'Jerry / MY AGENT' હેડર હવે એકદમ કોમ્પેક્ટ સ્લીક એક્ઝિક્યુટિવ બાર (માત્ર ૩૮dp) બની ગયું છે, જેથી સ્ક્રીન પર ચેટિંગ માટે પૂરી જગ્યા મળશે.\n\n"
    "4. 🔇 *વૉઇસ/સ્પીચ કંટ્રોલ*:\n"
    "   - જેરી હવે ફક્ત ત્યારે જ બોલશે જ્યારે તમે માઇકથી અવાજમાં પ્રશ્ન પૂછશો. ટાઇપ કરેલા ચેટમાં ચૂપચાપ ટેક્સ્ટમાં જ જવાબ આપશે.\n\n"
    "📲 *Direct 1-Click APK Download for Galaxy S25 Ultra*:"
)

payload = {
    "chat_id": CHAT_ID,
    "text": text_body,
    "parse_mode": "Markdown",
    "reply_markup": {
        "inline_keyboard": [
            [{"text": "📥 Download Jerry Release #26 (Direct APK)", "url": "https://github.com/lalanart786/s25-agent/releases/download/release-26/app-debug.apk"}],
            [{"text": "🌐 View GitHub Release v26", "url": "https://github.com/lalanart786/s25-agent/releases/tag/release-26"}]
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
            print("Telegram notification delivered successfully for Release #26!")
        else:
            print(f"Telegram error: {result}")
except Exception as e:
    print(f"Failed to send Telegram message: {e}")
