<p align="center">
  <img src="docs/icon-512.png" width="112" alt="Nobat Mobile">
</p>

<h1 align="center">Nobat Mobile</h1>

<p align="center">
  <strong>Clinic appointments on your phone — no server required.</strong><br>
  Book sessions · remind colleagues · email or SMS from your own apps
</p>

<p align="center">
  <em>Kotlin scaffold on <code>main</code> — open in Android Studio to run</em>
</p>

---

## Who it’s for

Small clinics, counselors, and front-desk teams who want a **simple appointment book on Android** — without renting a VPS or learning Docker.

If you already self-host the original [Nobat](https://github.com/PouryaFA81/Nobat) server, keep that for privacy-first hosting. **Nobat Mobile** is the sibling product for people who just want an app on the phone.

## What you can do (planned v0)

- **Calendar & day view** — see who’s booked and when  
- **Book, move, cancel** — client initials + time (+ optional note)  
- **Reminders** — send via **email (SMTP)** or open your **SMS app** to message from your own number  
- **Works offline** — data stays on the device  

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

**Kotlin + Jetpack Compose scaffold** is on `main` (local Room DB stub, dark One UI–leaning theme, launcher icon). Booking / SMTP / SMS intents come next.

### Run locally
1. Install [Android Studio](https://developer.android.com/studio)
2. Open this repo folder
3. Let Gradle sync, then Run on an emulator or phone

Check [Releases](https://github.com/PouryaFA81/Nobat-Mobile/releases) for the first APK when it lands.

Want the self-hosted edition instead? → **[Nobat](https://github.com/PouryaFA81/Nobat)**

## Try it on your phone

There is **no downloadable APK yet**. Two ways to see the app:

### Option A — wait for a release
When a debug build is ready, it will appear under [Releases](https://github.com/PouryaFA81/Nobat-Mobile/releases). Download the `.apk`, allow install from that source on your phone, and open it.

### Option B — run from a computer (today)
You need a computer and a USB cable.

1. Install [Android Studio](https://developer.android.com/studio) (free).
2. Open this project: **File → Open** → the folder you cloned from GitHub (`Nobat-Mobile`).
3. On your phone: **Settings → About phone** → tap **Build number** seven times → go back → **Developer options** → turn on **USB debugging**.
4. Plug the phone into the computer and accept the debugging prompt on the phone.
5. In Android Studio, press **Run** ▶ and choose your phone.

The first sync can take a few minutes. You should see the Nobat Mobile home screen when it finishes.

**Note:** This is an early scaffold — booking and email/SMS come in the next updates.

## License

[GNU Affero General Public License v3.0](LICENSE) — same family as [Nobat](https://github.com/PouryaFA81/Nobat).

---

<p align="center">
  <sub>Nobat Mobile · appointment scheduling for clinics, on Android</sub>
</p>
