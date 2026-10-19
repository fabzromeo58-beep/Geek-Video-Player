# Device verification checklist

Run on an Android 6 device/emulator, a low-RAM Android Go phone, and an Android 15+ device. Physical-device playback and profiling are required before claiming measured performance.

- Install the debug APK; confirm supplied logo on launcher and home. Check large font size, landscape and display cutouts.
- Open a local H.264/AAC MP4; check play/pause, seek, 0.5x/1x/2x, audio track selection, and subtitles.
- Open multiple documents; all appear in recents. Reopen a partly watched video and verify resume. Finish a video and verify it starts at the beginning next time.
- Rotate while playing and while paused; position and paused state survive. Repeat with external SRT and WebVTT subtitles.
- Switch apps, lock/unlock, return, and disconnect headphones. No background audio or retained decoder while paused. Incoming audio focus loss pauses/ducks appropriately.
- Cancel file/subtitle picker. Select a deleted, inaccessible, corrupt or unsupported video; verify readable error and working Back/Retry.
- Reboot and reopen a selected document; persistent grants should retain access for providers that support them. Remove history and verify original files remain.
- Open 60 files sequentially; history stays at 50 and old grants are released. Clear history and verify it remains empty after restart.
- Verify Open with from a file manager. Confirm no broad storage or network permissions in app settings.
- Check API 35+ navigation/status bar insets and rotation, including gesture navigation.

## Performance measurements (not yet recorded)

Use the **signed release build** for profiling. Record phone model, RAM, Android version, codec, resolution, bitrate and frame rate. Compare low-memory mode on/off with the same 10-minute local 480p/720p videos. Use Android Studio Profiler or `adb shell dumpsys meminfo com.geekvplayer.app` for baseline/playing/background memory. Record cold startup time and dropped frames via a debug-only Media3 AnalyticsListener if needed. Observe thermal throttling over 30 minutes. Buffer targets exclude decoder/frame memory; do not interpret them as the app's total RAM usage.
