# Zen Productivity App

A minimalist Android productivity app built with Kotlin: a pomodoro timer, a task list, and (in progress) a
calendar, stats and settings.

## Status

| Feature | State |
| --- | --- |
| Pomodoro timer | Working. Long breaks, daily goal, skip/reset, auto-start, alerts, notification actions; logs sessions |
| Tasks | Working. Categories, due dates and reminders, edit, drag to reorder, collapsible finished section |
| Calendar, Stats | Working. Session heatmaps, streaks and totals from the logged sessions |
| Settings | Working. Timer options, notifications, light/dark theme, export and delete data, about |
| Accounts | None by design: the app is local-first and opens straight into the timer |

## Building

Requirements: Android Studio (or JDK 17+) and the Android SDK with API 37.

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew installDebug         # install on a connected device or emulator
```

## Testing

```bash
./gradlew testDebugUnitTest            # JVM unit tests
./gradlew connectedDebugAndroidTest    # instrumented tests (needs a device or emulator)
./gradlew lintDebug                    # Android lint
```

CI (`.github/workflows/ci.yml`) runs lint, unit tests and a debug build, and the instrumented tests on an API 35
emulator, on every push to `main` and on pull requests.

The instrumented tests include Espresso screen tests (`androidTest/.../ui/`) for navigation, the task list and
the timer. The timer tests swap `PomodoroTimer.clock` for a fake clock, so finishing a 25 minute phase takes no
time. Every Espresso action also runs the Accessibility Test Framework checks (labels, touch targets, contrast).
Turn off animations on the test device (Developer options) to keep the tests stable.

Note: the instrumented tests clear the app's tasks and timer settings on the device they run on.

## Project structure

```
app/src/main/java/dev/samadali/zen/
├── ZenApp.kt              Application: creates the database, initialises the timer
├── MainActivity.kt        Bottom navigation host for the main tabs
├── pomodoro/              Timer state (PomodoroTimer), foreground service, UI
├── tasks/                 Task list, editor, adapter, ViewModel; reminders/ schedules notifications
├── data/                  Room entities, DAOs and database
├── stats/                 Streak and stats calculations, stats screen, heatmap views
├── calendar/ settings/    Tab screens
```

- UI uses XML layouts with View Binding, one fragment per tab inside `MainActivity`.
- `PomodoroTimer` is an app-wide singleton driven by an end timestamp; `PomodoroService` keeps it alive in the
  background and posts notifications.
- Tasks are read through `TaskDao` as `LiveData`, via `TaskViewModel`.
- The Room schema is exported to `app/schemas/`. When changing an entity, bump the database version, add a
  migration and commit the new schema file.
- Dependency versions live in `gradle/libs.versions.toml`.

Light and dark themes share the colour names in `res/values/colors.xml`; `res/values-night/colors.xml` swaps
their values (so "white" is the dark background in the dark theme). Use those names rather than raw colours.

## Data and backup

All data stays on the device: tasks in a Room database and timer settings in SharedPreferences. There are no
accounts and no server. Android Auto Backup (`allowBackup`, with the default rules) copies this data to the
user's own Google account when backup is on, and restores it on reinstall or a new phone, at no cost.

## Accessibility

Colours are chosen to meet WCAG 2.1 AA contrast; the ratios are noted in `res/values/colors.xml`. `green` and
`light_green` are for fills only, never for text or icons on white. Buttons, switches and titles get their
sizes and colours from the styles in `res/values/styles.xml`, and touch targets are at least 48dp. Both themes
are checked; the dark palette's ratios are in `res/values-night/colors.xml`.
