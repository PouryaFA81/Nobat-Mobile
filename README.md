<p align="center">
  <img src="docs/icon-512.png" width="112" alt="Nobat Mobile">
</p>

<h1 align="center">Nobat Mobile</h1>

<p align="center">
  <strong>Clinic appointments on your phone — no server required.</strong><br>
  Local accounts · calendar · SMTP reminders · FA / EN
</p>

<p align="center">
  <em>Latest pre-release: <a href="https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.8.0">v0.8.0</a></em>
</p>

---

## Who it’s for

Small clinics, counselors, and front-desk teams who want a **simple appointment book on Android** — without renting a VPS or learning Docker.

If you already self-host the original [Nobat](https://github.com/PouryaFA81/Nobat) server, keep that for privacy-first hosting. **Nobat Mobile** is the sibling product for people who just want an app on the phone.

## What’s in the app (v0.8.0)

- **Local accounts** — Several profiles on one phone, each with a password; data stays on the device  
- **App lock** — PIN (auto-submit) and optional fingerprint  
- **Calendar** — Month grid → day list; book, cancel, empty-day CTA  
- **Jalali in Persian** — FA uses the solar calendar (week starts شنبه); English stays Gregorian  
- **Language** — فارسی (RTL) / English (LTR), including chrome and strings  
- **Theme** — Dark (default) or Light  
- **Email (SMTP)** — Save your mailbox, test send, confirmation on book, reminder **1 hour before**  
- **SMS** — Opens your phone’s SMS app with a prefilled message (you tap Send)  
- **About** — Version, check for updates, GitHub contact links  
- **Coming soon (shells)** — Integrations (Telegram, Bale, Google Drive, Google Calendar), Backup / Restore, Reports & Print PDF  

Next: wire those integrations and data tools for real.


## Nobat vs Nobat Mobile

| | **Nobat** (server) | **Nobat Mobile** (this app) |
|---|---|---|
| Where it runs | Your own server + browser/PWA | Android phone |
| Best for | Privacy-first self-hosting | Everyday use without IT |
| Data | Server + browser | On-device (Room), per local account |
| Notifications | Self-hosted options | Your SMTP + device SMS |
| Install | Docker + domain | APK / Play (when ready) |

Same idea — appointments for a small practice. Different home.

## Email (SMTP) setup

Reminders use **your** mailbox. Step-by-step for Gmail, Outlook, Yahoo, and custom hosts:

- English: **[SMTP setup guide](docs/SMTP.md)**
- فارسی: **[راهنمای راه‌اندازی SMTP](docs/SMTP.fa.md)**

In the app: **Account → Notifications → Email (SMTP)** → **SMTP setup guide**.

## Try it on your phone

### Option A — download the APK (recommended)

1. Open [Releases → v0.8.0](https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.8.0) (or the latest pre-release)
2. Download the `.apk`
3. On your phone, allow install from that source
4. Open **Nobat Mobile**, create or unlock a local account, then use the calendar

### Option B — run from a computer

You need a computer and a USB cable.

1. Install [Android Studio](https://developer.android.com/studio) (free).
2. Open this project: **File → Open** → the folder you cloned from GitHub (`Nobat-Mobile`).
3. On your phone: **Settings → About phone** → tap **Build number** seven times → go back → **Developer options** → turn on **USB debugging**.
4. Plug the phone into the computer and accept the debugging prompt on the phone.
5. In Android Studio, press **Run** ▶ and choose your phone.

The first sync can take a few minutes.

## Status

Pre-release dogfood builds. Booking, accounts, app lock, language, SMTP reminders, and themes are in **v0.8.0**. See [CHANGELOG](CHANGELOG.md) and [Releases](https://github.com/PouryaFA81/Nobat-Mobile/releases) for the full trail.

Want the self-hosted edition instead? → **[Nobat](https://github.com/PouryaFA81/Nobat)**

## License

[GNU Affero General Public License v3.0](LICENSE) — same family as [Nobat](https://github.com/PouryaFA81/Nobat).

---

<p align="center">
  <sub>Nobat Mobile · appointment scheduling for clinics, on Android</sub>
</p>
