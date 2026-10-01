<p align="center">
  <img src="docs/icon-512.png" width="112" alt="Nobat Mobile">
</p>

<h1 align="center">Nobat Mobile</h1>

<p align="center">
  <strong>Clinic appointments on your phone — no server required.</strong><br>
  Book sessions · remind colleagues · email or SMS from your own apps
</p>

<p align="center">
  <em><a href="https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.2.0">v0.2.0</a> — book &amp; cancel on your phone</em>
</p>

---

## Who it’s for

Small clinics, counselors, and front-desk teams who want a **simple appointment book on Android** — without renting a VPS or learning Docker.

If you already self-host the original [Nobat](https://github.com/PouryaFA81/Nobat) server, keep that for privacy-first hosting. **Nobat Mobile** is the sibling product for people who just want an app on the phone.

## What you can do (v0.2.0)

- **Calendar & day view** — see who’s booked and when  
- **Book, move, cancel** — client initials + time (+ optional note)  
- **Works offline** — data stays on the device  
- **Reminders (next)** — email (SMTP) or your **SMS app**  

Later releases can grow toward staff roles, wait lists, and more — without turning the first version into a server.

## Nobat vs Nobat Mobile

| | **Nobat** (server) | **Nobat Mobile** (this app) |
|---|---|---|
| Where it runs | Your own server + browser/PWA | Android phone |
| Best for | Privacy-first self-hosting | Everyday use without IT |
| Notifications | Self-hosted ntfy | Email / SMS (your apps) |
| Install | Docker + domain | APK / Play (when ready) |

Same idea — appointments for a small practice. Different home.

## Status

**v0.2.0** pre-release is out: charcoal icon + book/cancel on a day list.  
Download: [v0.2.0](https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.2.0)

Open the project in **Android Studio** if you prefer building from source.

Want the self-hosted edition instead? → **[Nobat](https://github.com/PouryaFA81/Nobat)**

## Try it on your phone

### Option A — download the APK (recommended)
1. Open [Releases → v0.2.0](https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.2.0)
2. Download the `.apk`
3. On your phone, allow install from that source
4. Open **Nobat Mobile** and try **+** to book, then cancel if needed

### Option B — run from a computer
You need a computer and a USB cable.

1. Install [Android Studio](https://developer.android.com/studio) (free).
2. Open this project: **File → Open** → the folder you cloned from GitHub (`Nobat-Mobile`).
3. On your phone: **Settings → About phone** → tap **Build number** seven times → go back → **Developer options** → turn on **USB debugging**.
4. Plug the phone into the computer and accept the debugging prompt on the phone.
5. In Android Studio, press **Run** ▶ and choose your phone.

The first sync can take a few minutes.

**Note:** Email/SMS reminders are not in 0.2.0 yet.

## Email (SMTP) setup

Reminders use **your** mailbox. Step-by-step for Gmail, Outlook, Yahoo, and custom hosts:

- English: **[SMTP setup guide](docs/SMTP.md)**
- فارسی: **[راهنمای راه‌اندازی SMTP](docs/SMTP.fa.md)**

In the app (from **0.6.0**): **Account → Notifications → Email (SMTP)** → **SMTP setup guide**.

## License

[GNU Affero General Public License v3.0](LICENSE) — same family as [Nobat](https://github.com/PouryaFA81/Nobat).

---

<p align="center">
  <sub>Nobat Mobile · appointment scheduling for clinics, on Android</sub>
</p>
