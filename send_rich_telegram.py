import requests

BOT_TOKEN = "8826112784:AAGXUjjLyLvXHUd75IFRR8l7JKuuA5kpqm8"
CHAT_ID = "439228682"

text_body = (
    "🚀 *Jerry (My Agent) — Proactive Suite Release #15*\n\n"
    "નમસ્તે કિશનભાઈ! 🙏\n"
    "Jerry માં નીચેના નવા પાવરફુલ એજન્ટિક ફીચર્સ સફળતાપૂર્વક ઉમેરી દીધા છે:\n\n"
    "✨ *New Advanced Capabilities*:\n"
    "• 🖥️ *Live Screen Vision & OCR*: Jerry તમારી સ્ક્રીન પર ખુલેલા કોઈપણ દસ્તાવેજ, ઇન્વોઇસ કે ચેટને સીધી વાંચીને સમજી શકે છે.\n"
    "• ☀️ *Morning Executive Briefing*: સુપ્રભાત કહેતા જ આજનું સુરત હવામાન, કેલેન્ડર મીટિંગ્સ, બેટરી અને જિયોફેન્સ સમરી આપશે.\n"
    "• ⚡ *Multi-Step Compound Workflows*: વોટ્સએપ મેસેજ ડ્રાફ્ટ + કેલેન્ડર ફોલો-અપ રીમાઇન્ડર એક સાથે સિંગલ અપ્રુવલ કાર્ડથી એક્ઝિક્યુટ થશે.\n"
    "• 🎙️ *Spoken Permission Safeguards*: દરેક બદલાવ પહેલાં Jerry પૂછશે: \"કિશનભાઈ, શું હું આ બદલાવ કન્ફર્મ કરું?\"\n\n"
    "📲 *Download & Install Build #15 on your S25 Ultra*:"
)

payload = {
    "chat_id": CHAT_ID,
    "text": text_body,
    "parse_mode": "Markdown",
    "reply_markup": {
        "inline_keyboard": [
            [{"text": "📥 Download Jerry Build #15 (Direct APK)", "url": "https://github.com/lalanart786/s25-agent/releases/download/release-15/app-debug.apk"}],
            [{"text": "🌐 View GitHub Release v15", "url": "https://github.com/lalanart786/s25-agent/releases/tag/release-15"}]
        ]
    }
}

res = requests.post(f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage", json=payload, timeout=20)
print(f"Telegram status: {res.status_code}")
