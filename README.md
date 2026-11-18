# HabitFlow – Smart Habit Tracker

HabitFlow is a native, offline-first Android habit tracking application built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, **Room**, **Hilt**, **WorkManager**, **DataStore**, and **Vico Charts**.

## Key Features

- **Guided Onboarding**: Animated welcome flow, personal goal selection, custom profile name, and preferred daily reminder setup.
- **Home Dashboard**: Time-of-day greeting, animated daily progress ring, today's habit counters (total, completed, pending), best streak badge, rotating motivational quotes, and instant habit completion/incremental logging.
- **Full Habit Management**: Create, edit, pause, resume, archive, and delete habits with 10 curated quick-fill suggestions, 16 icons, 10 accent colors, categories, daily/weekday/custom-interval frequencies, targets, and start dates.
- **Interactive Habit Calendar**: Monthly view respecting Monday/Sunday week start preference, color-coded full/partial/missed indicators, month navigation, and per-day completion toggling.
- **Analytics & Statistics**: Real-data Vico bar & line charts (7-Day, 4-Week, 6-Month), overall productivity score gauge, most & least consistent habit highlights, and habit-wise completion bars.
- **Smart Reminders**: WorkManager-powered local notifications with Android 13+ runtime permission handling, daily completion deduplication, and `BootCompletedReceiver` rescheduling after reboot/timezone changes.
- **Achievements System**: Automatic milestone badges (`First Step`, `3-Day`, `7-Day`, `14-Day`, `30-Day`, `100-Day`, `Perfect Day`, and `100 Habits Completed`).
- **Profile, Theme & Portability**: Light/Dark/System theme switching, JSON backup & restore via Android Storage Access Framework, and CSV habit history export.

## Architecture

```
com.example
├── data/
│   ├── dao/          # Room DAOs with Flow queries
│   ├── database/     # HabitFlowDatabase
│   ├── entity/       # HabitEntity, HabitCompletionEntity, AchievementEntity + mappers
│   ├── local/        # UserPreferencesRepository (DataStore)
│   └── repository/   # HabitRepository (transactional updates & backup restore)
├── domain/
│   ├── model/        # Domain models & enums
│   └── usecase/      # HabitScheduleHelper, StreakCalculator, AchievementEvaluator, BackupAndExportManager
├── di/               # Hilt DatabaseModule
├── navigation/       # Type-safe Navigation Compose graph + adaptive NavigationBar/NavigationRail
├── notifications/    # NotificationHelper, ReminderScheduler, HabitReminderWorker, BootCompletedReceiver
├── presentation/     # MVVM ViewModels & Jetpack Compose Screens
│   ├── onboarding/
│   ├── home/
│   ├── habits/
│   ├── calendar/
│   ├── statistics/
│   ├── achievements/
│   ├── profile/
│   └── components/
└── ui/theme/         # Material 3 Color, Typography (Outfit, Plus Jakarta Sans, JetBrains Mono), Shape, Theme
```

## Setup & Build Instructions

1. Open the project in Android Studio (Meerkat or newer) with JDK 17+ and Android SDK 36.
2. Sync Gradle (`gradle assembleDebug`). No API keys or backend configuration are required; all data is persisted locally.

### Running Unit & Robolectric Tests

```bash
gradle :app:testDebugUnitTest
```

### Building a Release APK / App Bundle

1. Configure your upload keystore environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) or update the `release` signing config in `app/build.gradle.kts`.
2. Build the release APK or AAB:

```bash
gradle :app:assembleRelease
# or for Google Play App Bundle:
gradle :app:bundleRelease
```

The signed artifacts are generated under `app/build/outputs/apk/release/` and `app/build/outputs/bundle/release/`.
