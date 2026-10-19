# GeekVPlayer

A lightweight native Android video player built for local videos and modest phones. Uses the supplied GeekVPlayer logo on the home screen and launcher.

## Features

- Android 6.0 (API 23) and newer; native Java and Android Views.
- Open one or multiple videos from phone storage or SD cards using Android's document picker.
- Recent history (up to 50 videos), resume playback, and remove/clear history without deleting files.
- Play/pause, seek bar, 10-second seeking, playback speed and embedded audio/subtitle track selection through Media3 controls. Tap the video to show controls; the gear opens speed/audio settings.
- External SRT and WebVTT subtitles: tap Subtitle, select a file, then select its format.
- Fit/fill, portrait/landscape rotation, audio focus, and pause when headphones disconnect.
- Open with GeekVPlayer from another app that shares a video content URI.
- Offline: no Internet permission, ads, analytics, login, or broad storage permission.

## Low-end device design

Low-memory mode is enabled by default. It uses an 8 MiB compressed sample buffer target, 3–10 second buffering window, and no back buffer. Standard mode uses a 24 MiB target and 10–30 second window. These are loading targets, **not total process-memory limits**: decoders, video frames, Android and UI allocate additional memory.

Playback uses Media3 with platform MediaCodec decoders and a SurfaceView. Hardware decoding is used when supported by the device and format; decoder fallback does not guarantee smooth playback for every codec. No bundled FFmpeg/native decoder, thumbnail scanning, preloading, database, or background playback service. The player releases decoder resources on pause; a recycled ListView keeps history UI inexpensive. Release builds enable R8 and resource shrinking.

For older phones, start with local H.264/AAC MP4 videos at 480p or 720p. Container and codec support depends on the phone; 4K, AV1, HEVC, unusual audio formats, and damaged videos may fail or stutter. Low-memory mode does not transcode or lower the resolution of local files. Cloud document providers must make their files available offline.

## Get an APK

Open this repository's **Actions → Android build → successful run → Artifacts → GeekVPlayer-debug**. Unzip the downloaded archive and install `app-debug.apk` on your phone. Android may ask you to allow installs from your browser/file manager. This is a development build.

`GeekVPlayer-release-unsigned` contains the smaller optimized APK, which must be signed with your own release key before installation/distribution. No signing keys are stored in the repository. For production, use Android Studio's **Generate Signed Bundle / APK** and retain your key securely. Debug artifacts from separate CI machines may use different debug keys; uninstall the previous debug app if Android rejects an update (this clears its history).

## Build locally

Open the project in Android Studio, or install JDK 17 and Android SDK platform 35/build-tools 35.0.0. Set `ANDROID_HOME` or use a local `local.properties` file with `sdk.dir`.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

On Windows use `gradlew.bat`. Outputs are in `app/build/outputs/apk/`. The Gradle wrapper pins Gradle 8.11.1 and verifies its distribution checksum; AGP is 8.9.2 and Media3 is 1.6.1. Network access is required for the first build.

## Verification

CI runs unit tests for resume/formatting edge cases, Android lint, and both APK builds. See [device test checklist](docs/DEVICE_TESTS.md) for runtime checks and memory measurements. Build success is not a claim of measured performance on low-end hardware.

History and positions stay in app-private preferences; backup is disabled. Selected videos are never copied into app storage. Android URI grants are released when their history entries are removed or evicted. External subtitles apply to the current playback session. No network streaming, PiP, background audio, folder scanning, or playlist auto-advance is included in this first version.

Implementation references: [Media3 player lifecycle](https://developer.android.com/media/media3/exoplayer/hello-world), [PlayerView and SurfaceView](https://developer.android.com/media/media3/ui/playerview), [AGP 8.9 compatibility](https://developer.android.com/build/releases/agp-8-9-0-release-notes).
