# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

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

[0.4.2]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.4.2
[0.4.1]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.4.1
[0.4.0]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.4.0
[0.3.0]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.3.0
[0.2.0]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.2.0
[0.1.0]: https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/v0.1.0
