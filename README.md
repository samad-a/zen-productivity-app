# Zen Productivity App

A minimalist Android productivity app built with Kotlin: a pomodoro timer, a task list, and (in progress) a
calendar, stats and settings.

## Status

| Feature | State |
| --- | --- |
| Pomodoro timer | Working. Runs in a foreground service, alerts when a phase ends, remembers durations |
| Tasks | Working. Add, tick off, swipe to delete (with undo); stored in a Room database |
| Calendar, Stats | Designs only, not wired to data yet |
| Settings | Design only, apart from the backup note |
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
├── tasks/                 Task list, add task screen, adapter, ViewModel
├── data/                  Room entities, DAOs and database
├── calendar/ stats/ settings/   Tab screens
```

- UI uses XML layouts with View Binding, one fragment per tab inside `MainActivity`.
- `PomodoroTimer` is an app-wide singleton driven by an end timestamp; `PomodoroService` keeps it alive in the
  background and posts notifications.
- Tasks are read through `TaskDao` as `LiveData`, via `TaskViewModel`.
- The Room schema is exported to `app/schemas/`. When changing an entity, bump the database version, add a
  migration and commit the new schema file.
- Dependency versions live in `gradle/libs.versions.toml`.

The app uses a light-only theme: layouts use fixed light backgrounds and dark green text.

## Data and backup

All data stays on the device: tasks in a Room database and timer settings in SharedPreferences. There are no
accounts and no server. Android Auto Backup (`allowBackup`, with the default rules) copies this data to the
user's own Google account when backup is on, and restores it on reinstall or a new phone, at no cost.

## Accessibility

Colours are chosen to meet WCAG 2.1 AA contrast; the ratios are noted in `res/values/colors.xml`. `green` and
`light_green` are for fills only, never for text or icons on white. Buttons, switches and titles get their
sizes and colours from the styles in `res/values/styles.xml`, and touch targets are at least 48dp.
