# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test

```bash
./gradlew assembleDebug          # build debug APK
./gradlew test                   # run JVM unit tests
./gradlew connectedAndroidTest   # run instrumented tests (requires device/emulator)
```

JDK 21 required. Android SDK path is in `local.properties`.

## Architecture

Standard MVVM with manual DI (no Hilt/Koin). Repositories are created in `MainActivity.onCreate` and threaded through Composable parameters. Each ViewModel has a `Factory` inner class.

### Layer map

```
ui/       → Compose screens + ViewModels (per-feature folders: home, schedule, import_, settings, onboarding)
model/    → Domain data classes (Course, ScheduleSettings)
data/
  local/     → Room DB (AppDatabase), entity (CourseEntity), DAO (CourseDao)
  parser/    → QiangZhiParser: Jsoup-based HTML scraper for the 强智教务 system
  repository/→ CourseRepository (wraps DAO), SettingsRepository (wraps DataStore)
util/     → WeekUtils, QiangZhiUrlNormalizer, CourseColorMapper
```

### Navigation

`NavGraph` is the single navigation host with bottom bar (Home, Schedule, Settings) + Import + Onboarding. Route definitions are in the `Screen` sealed class. On first launch, onboarding state is checked via `SettingsRepository.onboardingCompleted` to decide whether to show `OnboardingScreen` or `Home`.

### Data conventions

- `CourseEntity.dayOfWeek`: 1=Monday .. 7=Sunday
- `CourseEntity.weekType`: 0=every week, 1=odd weeks only, 2=even weeks only
- Course colors are derived from `courseName.hashCode() % 10` mapped to a predefined palette in `ui/theme/Color.kt`
- `SettingsRepository.onboardingCompleted` is a derived flow: true if `ONBOARDING_COMPLETED` key is set, or falls back to checking whether URL + semester date are configured

### Import flow

1. `ImportScreen` hosts a `QiangZhiWebView` pointed at the configured 强智 URL
2. User navigates to the semester schedule page within the WebView
3. Tapping extract runs JS (`evaluateJavascript`) to get the page's `outerHTML`
4. `QiangZhiParser.parse()` uses Jsoup to find `#kbtable` or `div.kbcontent` elements and extracts course name, teacher, location, week range, period range, and day of week
5. Parsed courses are shown in a confirmation dialog, then saved via `CourseRepository.replaceAll()` (delete + insert)
