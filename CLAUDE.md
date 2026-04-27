# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Run a single unit test class
./gradlew testDebugUnitTest --tests "com.example.muse.ExampleUnitTest"

# Run lint
./gradlew lintDebug

# Build and install debug app
./gradlew installDebug
```

## Tech Stack

- **Language:** Kotlin 2.2.10
- **UI:** Jetpack Compose with Material3
- **Minimum SDK:** 32, Target SDK: 36
- **Architecture:** Single-module Android app
- **Build:** Gradle Kotlin DSL with version catalog (`gradle/libs.versions.toml`)

## Project Structure

```
app/src/
├── androidTest/         # Instrumented tests (Espresso + Compose)
│   └── java/com/example/muse/
├── main/
│   ├── java/com/example/muse/
│   │   ├── MainActivity.kt          # Single Activity entry point
│   │   └── ui/theme/                # Theme definitions
│   │       ├── Color.kt
│   │       ├── Theme.kt
│   │       └── Type.kt
│   ├── res/                          # Android resources
│   └── AndroidManifest.xml
└── test/                 # Unit tests (JUnit 4)
    └── java/com/example/muse/
```

## Architecture Notes

- Composable content is set directly in `MainActivity.kt` — no Navigation component or ViewModel setup yet
- Theme uses Material3 dynamic color on Android 12+ with fallback to custom purple scheme
- Edge-to-edge display is enabled (`enableEdgeToEdge()`)
- Version catalog at `gradle/libs.versions.toml` manages all dependency versions in one place
- All Gradle repositories include Aliyun mirrors as primary source for network stability in China
