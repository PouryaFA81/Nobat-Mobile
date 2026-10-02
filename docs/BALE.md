# Bale setup guide (Nobat Mobile)

Nobat Mobile can notify staff on **Bale** using **your** bot. You paste the token and chat ID under **Account → Advanced Settings → Integrations → Bale**. Messages go through Bale’s official Bot API — not our servers.

Persian: [راهنمای راه‌اندازی بله](BALE.fa.md)

## What you need

1. A Bale account  
2. A bot (from [@Botfather](https://ble.ir/botfather) in Bale)  
3. The **chat ID** where messages should land (usually a private chat with the bot, or a group the bot is in)

Then in the app: **Bot token** · **Chat ID** → **Save** → **Test send**. Turn on **Notify on book** so the assigned personnel gets a message when you book (alongside SMTP / Telegram if enabled).

---

## 1. Create a bot

1. Open Bale and chat with [@Botfather](https://ble.ir/botfather)
2. Send `/newbot` and follow the prompts (name + username ending in `bot`)
3. Copy the **API token** Botfather gives you (looks like `123456789:ABCD...`)
4. Paste it into Nobat Mobile as **Bot token** — keep it secret; anyone with it can control the bot

---

## 2. Get a chat ID

### Private chat (simplest)

1. Start a chat with **your new bot** and tap **Start** (or send any message)
2. Open this URL in a browser (replace `TOKEN` with your bot token):

   `https://tapi.bale.ai/botTOKEN/getUpdates`

3. Look for `"chat":{"id":` — that number is your **Chat ID** (e.g. `123456789`)
4. Paste it into the app as **Chat ID**

### Group

1. Add the bot to the group and send a message mentioning it
2. Call `getUpdates` the same way and use the group’s `"chat":{"id":` (often a **negative** number)

---

## 3. In Nobat Mobile

| Field | What to enter |
|---|---|
| **Bot token** | From Botfather in Bale |
| **Chat ID** | From `getUpdates` |
| **Test send** | Sends a short test message |
| **Notify on book** | On = message when you book with **Assign to** |

Token and chat ID stay **on this phone**, for the local account you’re using.

API used by the app: `https://tapi.bale.ai/bot<token>/sendMessage` ([official docs](https://docs.bale.ai/)).

---

## Tips

- If **Test send** fails, check the token, that you started the bot chat, and that the chat ID matches  
- For staff who don’t use Bale yet, keep **SMTP** or **Telegram** on as the main path  
- Evening digest also uses Bale when **Notify on book** is on and the bot is configured  

## Need help?

[Open an Issue](https://github.com/PouryaFA81/Nobat-Mobile/issues) from **Account → About**.
