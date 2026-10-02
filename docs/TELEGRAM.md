# Telegram setup guide (Nobat Mobile)

Nobat Mobile can notify staff on **Telegram** using **your** bot. You paste the token and chat ID under **Account → Advanced Settings → Integrations → Telegram**. Messages go through Telegram’s API — not our servers.

Persian: [راهنمای راه‌اندازی تلگرام](TELEGRAM.fa.md)

## What you need

1. A Telegram account  
2. A bot (from [@BotFather](https://t.me/BotFather))  
3. The **chat ID** where messages should land (usually a private chat with the bot, or a group the bot is in)

Then in the app: **Bot token** · **Chat ID** → **Save** → **Test send**. Turn on **Notify on book** so the assigned personnel gets a message when you book (alongside SMTP if enabled).

---

## 1. Create a bot

1. Open Telegram and chat with [@BotFather](https://t.me/BotFather)
2. Send `/newbot` and follow the prompts (name + username ending in `bot`)
3. Copy the **HTTP API token** BotFather gives you (looks like `123456:ABC-DEF...`)
4. Paste it into Nobat Mobile as **Bot token** — keep it secret; anyone with it can control the bot

---

## 2. Get a chat ID

### Private chat (simplest)

1. Start a chat with **your new bot** and tap **Start** (or send any message)
2. Open this URL in a browser (replace `TOKEN` with your bot token):

   `https://api.telegram.org/botTOKEN/getUpdates`

3. Look for `"chat":{"id":` — that number is your **Chat ID** (e.g. `123456789`)
4. Paste it into the app as **Chat ID**

### Group

1. Add the bot to the group and send a message mentioning it (or disable privacy mode via BotFather `/setprivacy` → Disable if you need to see all messages)
2. Call `getUpdates` the same way and use the group’s `"chat":{"id":` (often a **negative** number)

---

## 3. In Nobat Mobile

| Field | What to enter |
|---|---|
| **Bot token** | From BotFather |
| **Chat ID** | From `getUpdates` |
| **Test send** | Sends a short test message |
| **Notify on book** | On = message when you book with **Assign to** |

Token and chat ID stay **on this phone**, for the local account you’re using.

---

## Tips

- If **Test send** fails, check the token, that you started the bot chat, and that the chat ID matches  
- For staff who don’t use Telegram yet, keep **SMTP** on as the main path  
- **Bale** uses the same Integrations form (from **0.17.0**)  

## Need help?

[Open an Issue](https://github.com/PouryaFA81/Nobat-Mobile/issues) from **Account → About**.
