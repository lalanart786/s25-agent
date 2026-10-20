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
        f"🤖 *Jerry (My Agent) — Final Android APK Release*\n\n"
        f"📱 *App Name*: My Agent\n"
        f"🧠 *Agent*: Jerry\n"
        f"✨ *Release*: `{release_tag}`\n"
        f"🔒 *Knox Security*: StrongBox AES-256 Vault\n"
        f"🎙️ *Voiceprint*: Adaptive Speaker Training Armed\n"
        f"🛡️ *Safety*: Tactile & Verbal Confirmation Gate Active\n"
        f"🔓 *Lock Screen*: Ambient Voice-First Wake Enabled\n\n"
        f"📥 *Direct Download*: {release_url}\n\n"
        f"_Tap the file below to install directly on your Samsung Galaxy S25 Ultra!_"
    )

    doc_url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendDocument"

    if os.path.exists(apk_path):
        size_mb = os.path.getsize(apk_path) / (1024 * 1024)
        print(f"APK found: {apk_path} ({size_mb:.2f} MB)")

        for chat_id in CHAT_IDS:
            try:
                print(f"Uploading APK document to Telegram chat {chat_id}...")
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
                        timeout=180
                    )
                print(f"Telegram upload result: {res.status_code} - {res.text[:120]}")
            except Exception as e:
                print(f"Failed to upload document to {chat_id}: {e}")
                send_telegram_message(f"⚠️ Jerry APK is ready ({size_mb:.2f} MB), but Telegram direct upload timed out. Download here:\n{release_url}")
    else:
        print(f"APK file not found at {apk_path}. Sending release message.")
        send_telegram_message(caption)

if __name__ == "__main__":
    apk = sys.argv[1] if len(sys.argv) > 1 else r"C:\Users\ADMIN\.gemini\antigravity\scratch\s25-agent\app\build\outputs\apk\debug\app-debug.apk"
    tag = sys.argv[2] if len(sys.argv) > 2 else "release-14"
    rel_url = f"https://github.com/lalanart786/s25-agent/releases/tag/{tag}"
    send_telegram_apk(apk, tag, rel_url)
