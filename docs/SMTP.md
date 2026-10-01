# SMTP setup guide (Nobat Mobile)

Nobat Mobile sends reminder email **from your own mailbox**. You paste host, port, and login under **Account → Notifications → Email (SMTP)**. Nothing is sent through our servers.

Persian: [راهنمای راه‌اندازی SMTP](SMTP.fa.md)

## What you need

1. An email you control (Gmail, Outlook, Yahoo, clinic mail, etc.)
2. The **SMTP** settings for that provider
3. For Gmail/Outlook: usually an **app password** (not your normal login password)

Then in the app: fill **Host**, **Port**, **Username**, **Password**, **From** → turn on **Use TLS** if your provider needs it → **Save** → **Test send**.

---

## Gmail (most common)

Google no longer allows normal account passwords for SMTP. Use an **App Password**.

1. Open [Google Account → Security](https://myaccount.google.com/security)
2. Turn on **2-Step Verification** (required)
3. Search for **App passwords** → create one for “Mail” / “Other” (name it `Nobat Mobile`)
4. Copy the 16-character password (spaces don’t matter)

| Field in the app | Value |
|---|---|
| **Host** | `smtp.gmail.com` |
| **Port** | `587` |
| **Use TLS** | On |
| **Username** | your full Gmail address |
| **Password** | the **app password** (not your Google password) |
| **From** | the same Gmail address |

Tap **Test send** with a recipient you can check. If it fails, double-check 2-Step Verification and that you used the app password.

---

## Outlook / Microsoft 365

| Field | Value |
|---|---|
| **Host** | `smtp.office365.com` |
| **Port** | `587` |
| **Use TLS** | On |
| **Username** | your full Outlook / work email |
| **Password** | account password, or an [app password](https://account.microsoft.com/security) if you use 2FA |
| **From** | the same address |

Some work tenants block SMTP — ask your IT admin if test send fails.

---

## Yahoo Mail

| Field | Value |
|---|---|
| **Host** | `smtp.mail.yahoo.com` |
| **Port** | `587` |
| **Use TLS** | On |
| **Username** | your Yahoo email |
| **Password** | a Yahoo **app password** (generate under Account Security) |
| **From** | the same address |

---

## Other providers (clinic / custom mail)

Ask your provider for “SMTP outgoing mail” and map:

| App field | Usually means |
|---|---|
| **Host** | SMTP server name (e.g. `mail.yourclinic.com`) |
| **Port** | often `587` (TLS) or `465` (SSL) |
| **Use TLS** | On for 587; follow your provider’s note for 465 |
| **Username** | often the full email address |
| **Password** | mailbox password or app password |
| **From** | the address clients will see |

---

## Tips

- **Host / Port / From** stay left-to-right even in Persian UI — paste carefully.
- Prefer **Test send** before you rely on appointment reminders.
- Password is stored **on this phone**, for the account you’re signed into — not in the cloud.
- SMS is separate: **Send via device** opens your phone’s SMS app; no SMTP needed.

## Need help?

[Open an Issue](https://github.com/PouryaFA81/Nobat-Mobile/issues) on GitHub (Bug report / Support from **Account → About**).
