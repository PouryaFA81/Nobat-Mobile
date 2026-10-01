# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [0.15.1] - 2026-10-01

### Added
- **Clinic code dual path** / **دو مسیر کد مطب**: pick **Relay code** / **کد رله** (hosted) or **Your own relay** / **رلهٔ خودتان** (advanced)
- Relay code path: paste → **Redeem** / **فعال‌سازی**; **Get a relay code** / **دریافت کد رله** opens admin-configured purchase URL (`relay_purchase_url`, default blank)
- Optional `nobatR1:` Relay-code wrapper (same JSON as `nobat1:`); redeem marks **Hosted** / **میزبانی‌شده** and hides real host
- Connected hosted: shareable Clinic code + Hosted chip; title **Hosted clinic notifications** / **اعلان‌های مطب میزبانی‌شده**
- Admin Relay advanced: editable purchase / contact URL (placeholder `https://example.com/relay`)

### Changed
- versionName **0.15.1**, versionCode **21**

### Notes
- Do not bake a real relay host into the APK; sellers mint `nobat1:` / `nobatR1:` offline

## [0.15.0] - 2026-10-01

### Added
- **Move / Reschedule** / **جابه‌جایی / تغییر زمان** from day view (Admin / Secretary only — same role gate as FAB book)
- Change date and/or time; Assign to and notes stay prefilled (editable)
- Notify on move: SMTP confirmation-style to assigned personnel email; Telegram when **Notify on book** is on; local in-app notification; Clinic relay publish with `event=move` + `personnelId`
- Cancel old 1h reminder WorkManager work; schedule new reminder for the new slot when reminders/SMTP ready

### Changed
- versionName **0.15.0**, versionCode **20**

## [0.14.0] - 2026-10-01

### Added
- **Clinic notifications** / **اعلان‌های مطب** (Phase 2): Admin configures **Relay** / **رله** (URL · topic · token) under **Clinic code** / **کد مطب**; shareable **Clinic code** (`nobat1:` + base64url JSON `{"u","t","k"}`); **Share code** / **اشتراک کد**; **Regenerate** / **بازتولید** (new topic, clear token)
- Staff: **Enter Clinic code** / **ورود کد مطب** → **Connect** / **اتصال** · **Connected** / **متصل** · **Disconnect** / **قطع اتصال**; in-app alerts for appointments assigned to linked personnel
- Publish on admin book/cancel (JSON body with `personnelId`, initials, day, time); staff subscribe via HTTP long-poll while process alive + WorkManager 15‑min poll; resume on boot
- EncryptedSharedPreferences for relay token/URL/topic; **Setup guide** / **راهنمای راه‌اندازی** → `docs/CLINIC-NOTIFICATIONS.md` (+ FA)
- UI never says the relay product name (docs may name ntfy once for operators)

### Changed
- Removed “Coming soon” from Clinic code row
- versionName **0.14.0**, versionCode **19**

### Notes
- Doze / killed app may delay listen-only HTTP; prefer always-on tablets or FCM upstream on your relay for pocket reliability

## [0.13.0] - 2026-10-01

### Added
- **Role** on Account: **Admin / Secretary** / **مدیر یا منشی** · **Staff** / **پرسنل** (Room migration v3→v4)
- **Link to personnel** / **پیوند به پرسنل** — pick from Personnel list (required for Staff My schedule)
- **My schedule** / **برنامهٔ من**: Admin tabs **Everyone** / **همه** + **My schedule**; Staff sees My schedule only (filter by linked personnel) · **no FAB**
- **Clinic code** / **کد مطب** row — Coming soon (Phase 2)
- **Local in-app notifications** on this device for book + cancel (NotificationCompat channel **Clinic notifications** / **اعلان‌های مطب**); POST_NOTIFICATIONS on API 33+

### Changed
- Day/month lists honor role + schedule filter; Staff cannot book

## [0.12.0] - 2026-10-01

### Added
- **Telegram** under Advanced Settings → Integrations: Bot token / Chat ID (EncryptedSharedPreferences), Test send via Bot API `sendMessage`, Notify on book, Connected when configured
- In-app **Telegram setup guide** / **راهنمای راه‌اندازی تلگرام** → `docs/TELEGRAM.md` (+ FA twin)
- On successful book, if Notify on book is on and token/chat set, posts appointment details + assigned staff name to the Integrations Chat ID (SMTP unchanged; skip Telegram if missing)

### Changed
- Removed “Coming soon” from Telegram row; Bale / Drive / Calendar stay Coming soon

## [0.11.0] - 2026-10-01

### Added
- **Reports & Print PDF** under Advanced Settings: day or month range (This day / Pick a day / This month / Pick a month); on-device `PdfDocument` with time · client · staff · note; Share via system sheet and PrintManager (Save as PDF)
- PDF layout (Art/Pen): orange title bar **Nobat Mobile** + range; column headers Time/ساعت · Client/مراجع · Staff/پرسنل · Note/یادداشت; FA RTL columns with LTR times; footer **Nobat Mobile**

### Changed
- Removed “Coming soon” from local Reports/print and Account hub Reports row; Drive-related Coming soon unchanged

## [0.10.0] - 2026-10-01

### Added
- **Local Backup & Restore** under Advanced Settings → Backup: export current account’s appointments, personnel, and non-secret notification prefs as JSON to Downloads (SAF fallback); restore via file picker with confirmation dialog before overwrite
- Restore confirm (Pen): **Restore backup?** / **بازیابی پشتیبان؟** — replaces this account’s data on the phone; **Restore** / **بازیابی** · **Cancel** / **انصراف**

### Changed
- Removed “Coming soon” from local backup/restore and from the Account hub Backup row; Google Drive backup/restore rows stay Coming soon

## [0.9.1] - 2026-10-01

### Added
- **Lock after** / **قفل پس از** under App lock: 5 minutes, 30 minutes, 1 hour, When phone locks (default) — persist preference; timer or `ACTION_SCREEN_OFF` re-requires PIN/biometric

### Changed
- **Account password** is create / change-password only; selecting a profile opens without password; unlocking the app is PIN / fingerprint
- Removed in-app forgot-password wipe / delete-account path (data goes away on uninstall)

## [0.9.0] - 2026-10-01

### Added
- **Personnel / پرسنل:** Room staff rows (name, email, optional phone) under Account Management; list + add/edit; empty state prompts Add personnel
- **Assign to** on book: pick who gets the slot; confirmation email + 1h SMTP reminder go to that person’s email (snapshot on appointment)
- If no personnel: block mail with **Add personnel first** / **اول پرسنل اضافه کنید** and open Personnel

### Changed
- Room DB v2 → v3: `personnel` table; `appointments.personnelId` + `personnelEmail`
- Reminder WorkManager carries recipient email from appointment (not global test recipient)

## [0.8.1] - 2026-10-01

### Changed
- **Account hub IA:** scrollable list so About is reachable; sections — Account Management / مدیریت حساب (Password, Switch account, Add account, App lock), Preferences / ترجیحات (Language, Appearance, Notifications), Advanced Settings / تنظیمات پیشرفته (Integrations, Backup, Reports & print), About / درباره

## [0.8.0] - 2026-10-01

### Added
- Integrations shell: **Google Calendar** / **تقویم گوگل** row (Coming soon)
- **Account hub:** Security (live) plus Integrations / Backup / Reports shells marked Coming soon
- **App lock (live):** PIN (PBKDF2 in EncryptedSharedPreferences) + Fingerprint (BiometricPrompt); enable/disable/change PIN; biometric disabled gracefully without hardware
- **Unlock gate:** PIN pad + biometric after account sign-in and on resume when lock enabled (Entry/Sign-in not blocked)
- **Shell screens:** Integrations (Telegram, Bale, Google Drive), Data Backup/Restore (Local/Drive), Reports & Print PDF — primary actions snackbar Coming soon / به‌زودی

## [0.7.0] - 2026-10-01

### Added
- **SMTP reminders (real):** when Reminders on + valid SMTP, schedules email **1 hour before** start via WorkManager (`OneTimeWorkRequest` + unique work `reminder_{id}`); reschedule/cancel on book/save settings/cancel; confirmation email to reminder recipient on book
- **Jalali calendar (FA):** month grid + day header use Shamsi; week starts Saturday (شنبه); EN keeps Gregorian Saturday-first layout
- **Appearance Dark / Light:** Account → Appearance; app-wide SharedPreferences; light surfaces + brand orange `#E95420`; wires `NobatTheme(darkTheme=…)`

### Changed
- Notifications: “test recipient” labeled as reminder recipient; Reminders switch enables scheduling (no “coming soon”)
- Removed Appearance/Theme “coming soon” stubs

## [0.6.0] - 2026-10-01

### Added
- Account → **Notifications** / **اعلان‌ها**: per-account SMTP settings (host, port, Use TLS, username, password, from) with Save + Test send
- SMS via device intent (`ACTION_SENDTO` / `smsto:`) — Test from Notifications + Share via SMS on appointment cards (no `SEND_SMS` permission)
- EncryptedSharedPreferences store keyed by `accountId` (password never logged); cleared on account reset
- SMTP via Android JavaMail (`com.sun.mail:android-mail` + `android-activation`)
- In-app **SMTP setup guide** / **راهنمای راه‌اندازی SMTP** (opens EN or FA docs by language)

## [0.5.1] - 2026-10-01

### Added
- Account → **About** / **درباره**: Version, Check for updates (GitHub Releases API, includes prereleases), Contact (Bug report / Suggestions / Support → GitHub Issues)
- `INTERNET` permission for update check

## [0.5.0] - 2026-10-01

### Added
- Local multi-account (device-only): Room `Account` (displayName, PBKDF2-HMAC-SHA256 passwordHash + salt, createdAt)
- Entry screen: list local profiles → Sign in (password) → Month calendar; **+ Add account** / Create account
- Sign in / Create account screens with Pen bilingual strings + local-only note
- Account screen: Change password, Switch account, Add account, Language, Appearance/Theme stubs
- Session: unlocked `accountId` in memory; last profile id in SharedPreferences; cold start always requires unlock
- Appointments scoped by `accountId`; forgot-password reset deletes that account’s data

### Changed
- Room DB v1 → v2: `accounts` table + `appointments.accountId` (default 0 = orphan)
- **Migration choice:** on upgrade, existing appointments keep `accountId=0`. If orphans exist and no accounts, first launch forces Create account and attaches orphans to the new account. Empty DB shows Create account on Entry. Documented here (not auto-default-account).

## [0.4.2] - 2026-10-01

### Added
- Login / home entry screen: TopAppBar `brand_title` (FA نوبت موبایل · EN Nobat Mobile); Continue / ورود CTA → Month calendar (no auth yet)
- `brand_title` + `continue_cta` string resources; launcher `app_name` stays Latin Nobat Mobile in both locales

### Changed
- Header rule: brand only on entry; Calendar / Account section bars keep section titles only

## [0.4.1] - 2026-10-01

### Fixed
- Language switch actually reloads strings + RTL/LTR: MainActivity is now AppCompatActivity; `locales_config.xml` + `android:localeConfig`; AppLocalesMetadataHolderService autoStoreLocales
- Day/month chevrons: KeyboardArrowLeft (prev/start) + KeyboardArrowRight (next/end) with AutoMirrored — no hardcoded swap

### Added
- Account screen (Account icon in header) with Language picker + stub Appearance/Theme rows
- Month calendar scaffold (Gregorian, Sat-first): days with appointment counts; tap → day list

### Changed
- Language moved out of TopAppBar orphan action into Account
- Pen strings: nav_calendar, nav_add, account_title, appearance, theme, first_appointment, month hints (values + values-en)

## [0.4.0] - 2026-10-01

### Added
- In-app language picker (فارسی / English) on Home — SharedPreferences + AppCompatDelegate.setApplicationLocales

### Changed
- LayoutDirection follows app language (FA → RTL, EN → LTR) instead of hard-coded Rtl
- Time/date/duration numerals stay LTR (`TextDirection.Ltr`) in both languages
- Window/decor layout direction synced with app locale for dialogs and system chrome

## [0.3.0] - 2026-10-01

### Changed
- Booking form defaults: next :00/:30 time, 60 min duration; initials focused first
- Empty day: goal-gradient card («هنوز نوبتی نیست» / CTA «+ نوبت اول»)
- Cancel confirm: loss-aversion copy («این نوبت حذف می‌شود. ادامه می‌دهید؟»)
- Snackbar «نوبت ثبت شد» after save
- English string resources (`values-en`) — Pen Bot final copy

## [0.2.0] - 2026-10-01

### Added
- Book appointments (initials, time, duration, note) on a day
- Cancel appointment with confirm dialog
- Day prev / next / today navigation
- RTL layout for Persian UI
- Launcher icon B: charcoal matte + orange calendar (no gradient)

## [0.1.0] - 2026-10-01

First **pre-release** debug APK (scaffold only — not feature-complete).

### Added
- Kotlin + Jetpack Compose app shell (`app.nobat.mobile`)
- Dark-first One UI–leaning theme tokens (Yaru orange `#E95420`)
- Room database stub for appointments
- Launcher icons (Art Bot One UI mark)
- Home screen placeholder + FAB (booking not wired yet)

### Notes
- Book / cancel / SMTP / SMS come in later 0.x builds
- **1.0.0** reserved for the first build with a working booking loop

[0.11.0]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.11.0
[0.10.0]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.10.0
[0.9.0]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.9.0
[0.8.1]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.8.1
[0.8.0]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.8.0
[0.7.0]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.7.0
[0.6.0]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.6.0
[0.5.1]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.5.1
[0.5.0]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.5.0
[0.4.2]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.4.2
[0.4.1]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.4.1
[0.4.0]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.4.0
[0.3.0]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.3.0
[0.2.0]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.2.0
[0.1.0]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.1.0
