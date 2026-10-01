# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

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
