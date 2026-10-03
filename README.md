# Intelza

**Understanding Beyond Answers**

Intelza is an Android app that lets primary school teachers check how well the class
understood a topic, right after teaching it.

Every student holds up a printed answer card. Turning the card so a letter (A, B, C or D)
is on top chooses an answer. The teacher sweeps the phone camera across the room and the
app reads every card at once, using [AprilTag](https://april.eecs.umich.edu/software/apriltag)
markers, then shows who understood and who needs help.

- [Teacher's guide](docs/USER_GUIDE.md): how to set up a class, print cards and run an evaluation
- [Requirements](docs/REQUIREMENTS.md): the full version 1.0 specification

## Privacy

- Everything is stored on the phone. There are no accounts.
- The app has no internet permission. Data leaves the phone only in files the teacher
  chooses to share (reports, card PDFs, backups).
- Camera frames are analysed in memory and never saved.

## Building

Requirements:

- Android Studio 2026.1 or newer (it bundles a suitable JDK)
- Android SDK platform 37, **NDK 30.0.16248370** and **CMake 4.1.2**
  (Android Studio → Settings → Languages & Frameworks → Android SDK → SDK Tools)

Open the project folder in Android Studio and run the `app` configuration, or build from
the command line:

```bash
./gradlew assembleDebug
```

Run the unit tests:

```bash
./gradlew testDebugUnitTest
```

Run the on-device tests (detector checks, needs a connected phone or emulator):

```bash
./gradlew connectedDebugAndroidTest
```

Every push to `main` is built by GitHub Actions; the debug APK is attached to each run.

## Release builds

Release builds are shrunk with R8 and need your own signing key, which must never be
committed.

1. Create a key once, e.g. in Android Studio: **Build → Generate Signed App Bundle or
   APK → Create new…**. Keep the `.jks` file and its passwords safe: every future update
   of the app must be signed with the same key.
2. Create `keystore.properties` in the project root (it is ignored by Git):

   ```properties
   storeFile=../keys/intelza-release.jks
   storePassword=…
   keyAlias=intelza
   keyPassword=…
   ```

   `storeFile` is relative to the project root.
3. Build:

   ```bash
   ./gradlew assembleRelease
   ```

   The signed APK is written to `app/build/outputs/apk/release/`. Without
   `keystore.properties` the release APK is built unsigned.

## Project layout

| Path | Contents |
|---|---|
| `app/src/main/cpp/` | JNI bridge (`intelza_jni.c`) and the vendored AprilTag 3 detector |
| `app/src/main/java/com/ht/intelza/scan/` | Camera analysis, card detection and answer decoding |
| `app/src/main/java/com/ht/intelza/data/` | Room database, repositories, backup |
| `app/src/main/java/com/ht/intelza/domain/` | Scoring and other app rules (unit tested) |
| `app/src/main/java/com/ht/intelza/ui/` | Jetpack Compose screens |
| `app/src/main/java/com/ht/intelza/cards/`, `export/` | Card PDFs, report PDFs and CSVs, sharing |
| `docs/` | Requirements and the teacher's guide |

## How answers are read

Each card number is an AprilTag ID from the tag36h11 family. The detector reports the
tag's corners in a fixed order relative to the printed card, so the app knows where the
card's top (the A edge) is. It compares that direction with gravity from the phone's
motion sensor, so answers are read correctly whichever way the teacher holds the phone.
A reading only counts once it has been stable for a few frames.

## Third-party code

- [AprilTag 3](https://github.com/AprilRobotics/apriltag) v3.4.5 — BSD 2-Clause License,
  copyright The Regents of The University of Michigan. See
  `app/src/main/cpp/apriltag/LICENSE.md`.
