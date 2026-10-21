import os
import sys
import requests

BOT_TOKEN = "8826112784:AAGXUjjLyLvXHUd75IFRR8l7JKuuA5kpqm8"
CHAT_IDS = ["439228682"] # Kishan (@Kmiyani)

def send_telegram_message(text):
    url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
    for chat_id in CHAT_IDS:
        try:
            res = requests.post(url, json={
                "chat_id": chat_id,
                "text": text,
                "parse_mode": "Markdown"
            }, timeout=30)
            print(f"Message sent to {chat_id}: {res.status_code}")
        except Exception as e:
            print(f"Error sending message to {chat_id}: {e}")

def send_telegram_apk(apk_path, release_tag, release_url):
    caption = (
        f"🤖 *Jerry (My Agent) — Direct APK File Delivery*\n\n"
        f"નમસ્તે કિશનભાઈ! 🙏\n"
        f"આ APK ફાઇલ સીધી ટેલિગ્રામમાં નીચે આવી ગઈ છે. કોઈ બ્રાઉઝર કે ગિટહબ લિંક ખોલવાની જરૂર નથી!\n\n"
        f"📱 *Release*: `{release_tag}`\n"
        f"🧠 *AI Engine*: Snapdragon 8 Elite + Hermes 3 On-Device GGUF\n"
        f"⌨️ *Chat UI*: Keyboard open થતા ચેટ બોક્સ ક્લીન દેખાશે\n"
        f"📐 *Header*: 50% નાનું સ્લીક 38dp હેડર\n"
        f"🔇 *Speech*: માત્ર માઇકથી બોલો ત્યારે જ અવાજમાં જવાબ\n\n"
        f"💡 *ઇન્સ્ટોલેશન*: નીચે આપેલી `MyAgent-Jerry.apk` ફાઇલ પર ૧-ટેપ કરીને સીધું **Install** કરો!\n"
        f"_(જો જૂનું વર્ઝન ફોનમાં હોય તો એકવાર Uninstall કરી પછી આ ટેપ કરો)_"
    )

    doc_url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendDocument"

    if os.path.exists(apk_path):
        size_mb = os.path.getsize(apk_path) / (1024 * 1024)
        print(f"APK found: {apk_path} ({size_mb:.2f} MB)")

        if size_mb <= 49.5:
            for chat_id in CHAT_IDS:
                try:
                    print(f"Uploading APK document ({size_mb:.2f} MB) to Telegram chat {chat_id}...")
                    with open(apk_path, "rb") as f:
                        res = requests.post(
                            doc_url,
                            data={
                                "chat_id": chat_id,
                                "caption": caption,
                                "parse_mode": "Markdown"
                            },
                            files={
                                "document": ("MyAgent-Jerry.apk", f, "application/vnd.android.package-archive")
                            },
                            timeout=300
                        )
                    print(f"Telegram upload result: {res.status_code} - {res.text[:120]}")
                except Exception as e:
                    print(f"Failed to upload document to {chat_id}: {e}")
                    send_telegram_message(f"⚠️ Jerry APK is ready ({size_mb:.2f} MB):\n{release_url}")
        else:
            print(f"APK size ({size_mb:.2f} MB) exceeds Telegram bot 50MB limit. Sending link.")
            send_telegram_message(f"⚠️ Jerry APK ({size_mb:.2f} MB) is ready at:\n{release_url}")
    else:
        print(f"APK file not found at {apk_path}. Sending release message.")
        send_telegram_message(caption)

if __name__ == "__main__":
    apk = sys.argv[1] if len(sys.argv) > 1 else r"app\build\outputs\apk\debug\app-debug.apk"
    tag = sys.argv[2] if len(sys.argv) > 2 else "release-27"
    rel_url = f"https://github.com/lalanart786/s25-agent/releases/tag/{tag}"
    send_telegram_apk(apk, tag, rel_url)
