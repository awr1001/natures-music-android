# Nature's Music

Android app that loops and mixes nature sounds (ocean, rain, forest, storm, city, and extras) with an optional alarm. Port of Aaron Roberts's iOS app **Nature's Music** (Sound Generator 2, v1.8). No ads, accounts, backend, or Google Services.

- **applicationId:** `com.aaronroberts.naturesmusic`
- **versionName:** 1.8 · **versionCode:** 1
- Kotlin, Jetpack Compose, version catalog (`gradle/libs.versions.toml`)
- Android Gradle Plugin **8.9.2** (do not bump to 8.13 / 9.x)
- compileSdk 36, targetSdk 36, minSdk 26

## License

- **Source code** (Kotlin, Gradle, scripts, and related non-asset files): [Apache License 2.0](LICENSE) — Copyright 2026 Aaron Roberts
- **Sounds and images** (`app/src/main/res/raw/`, drawable help screenshots and artwork, launcher icons): [CC BY 4.0](LICENSE-ASSETS) — Attribution: Aaron Roberts / App-O-Matic — https://creativecommons.org/licenses/by/4.0/

## Build

### Android Studio

1. Install Android Studio (Meerkat / 2024.3.1 or later is enough for API 36).
2. **File → Open** this folder.
3. Let Gradle sync. Studio will write `local.properties` with your SDK path.
4. Start an emulator (API 26+) or plug in a phone with USB debugging.
5. Click **Run**.

### Command line

JDK 17+ is required. AGP 8.9.2 expects **Gradle 8.11.1** (already pinned in the wrapper).

```bash
./gradlew :app:assembleRelease
```

Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk` (or signed, if you configure signing).

Debug build:

```bash
./gradlew :app:assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

This repository is a single FOSS release build (no proprietary product flavor). Suitable for [F-Droid](https://f-droid.org/) once the source is hosted on a public Git forge and an fdroiddata merge request is opened.

## How it works

| Screen | What it does |
| --- | --- |
| Home | Title, **?** help, then Ocean, Rain, Forest, Storm, City, Extra Sounds, Alarm |
| Category | That sound set: Play / Stop and volume per loop |
| Extra Sounds | Birds, seagulls, wind, highway, industrial |
| Alarm | Time + sound. Notification permission is requested **here only**, not on launch |
| Help | Mixing and alarm, in brief |

More than one loop can play at once (ocean + rain, extras on top, and so on). Loops keep going if you leave a screen or background the app. Playback runs in a `mediaPlayback` foreground service so a sleep session is not killed. The mixer uses `MediaPlayer` on a dedicated audio thread.

The alarm uses `AlarmManager.setAlarmClock`, a foreground service, and a full-screen activity so it can ring on Android 8+ with the app closed.

## Audio assets

Loops live in `app/src/main/res/raw`. Android raw names must stay lowercase `[a-z0-9_]`. `.wav`, `.mp3`, and `.ogg` all work.

To regenerate placeholder tones (not the shipped recordings):

```bash
python3 scripts/generate_assets.py
```

## Icon

Adaptive launcher (wave + leaf, light ground, no app name in the artwork). High-res PNG: `play/icon-1024.png`. F-Droid metadata icon: `fastlane/metadata/android/en-US/images/icon.png`.

## Project layout

```
app/src/main/java/com/aaronroberts/naturesmusic/
  playback/     MixerEngine + MixerPlaybackService
  alarm/        scheduler, receivers, ringing UI
  ui/           Compose screens (home, category, extras, alarm, help)
  data/         SoundCatalog
fastlane/metadata/android/en-US/   F-Droid / Fastlane store listing
```

## F-Droid

Store listing text and icon live under `fastlane/metadata/android/en-US/`. After this repo is on a public GitHub or GitLab URL, add the app to [fdroiddata](https://gitlab.com/fdroid/fdroiddata) with a recipe that builds `./gradlew :app:assembleRelease` (or the Antifeature-free FOSS build) from that tag.
