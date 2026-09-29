# Android port status

Updated: 2026-09-29

- [x] Android project structure created
- [x] ARM64 native library builds (`arm64-v8a`, NDK 28.2.13676358)
- [x] Debug and Release APKs build
- [x] APK launches on a connected Galaxy device
- [x] Vulkan instance and physical device discovery (Adreno 830)
- [x] Android Vulkan surface created; graphics queue and surface formats support presentation
- [x] Extracted game filesystem copied to app-private storage and root markers recognized
- [ ] ReXGlue runtime initialized in the Android process
- [ ] Guest memory initialized
- [ ] `default.xex` loaded
- [ ] First NFSMW guest code executed
- [ ] First frame
- [ ] Menu
- [ ] Audio
- [ ] Gameplay
- [ ] Save/load
- [ ] Touch controls

## Current implementation

The Android launcher and JNI library are a bootstrap only. JNI startup logs to Logcat, creates app-private `game_root` and `cache` directories, creates an Android `VkSurfaceKHR`, and confirms the Adreno 830 has a graphics queue and surface formats for presentation. It does not yet create a swapchain or render a frame, initialize ReXGlue, or load game data.

The bootstrap includes the SDK's `rex/platform.h` and has a compile-time assertion that the NDK build is simultaneously Android, POSIX/Linux-compatible, and ARM64. This validates the SDK's existing platform macro path without claiming that the full SDK runtime is linked.

The launcher has an SAF folder picker and background importer that targets `files/nfsmw/game_root`, checks for `default.xex`, `NFS`, and `Movies`, and swaps a new copy into place only after validation. Debug and Release share the application ID `com.nfsmw.android`, so the game directory remains in the same app-private storage across variants. The provided extracted folder was copied from the workspace to the connected phone and then into `/data/user/0/com.nfsmw.android/files/nfsmw/game_root`. The private copy is 6.5 GiB with 49 files; the three root markers were checked. The temporary `/sdcard/Download/NFSMW_Source` transfer copy was removed. The SAF picker/import path compiles but still needs a direct UI run; this initial copy was completed locally on-device after the phone's in-call screen covered the picker.

Verified on 2026-09-29:

- `gradlew --no-daemon assembleDebug` — passed.
- `gradlew --no-daemon assembleRelease` — passed.
- `adb install -r ...app-debug.apk` — passed.
- Launched `com.nfsmw.android.debug/com.nfsmw.android.MainActivity` — passed.
- Logcat confirmed JNI startup, private directory setup, Vulkan loader API 1.1, and physical device `Adreno (TM) 830`; no `AndroidRuntime` crash was reported.
- Reinstalled and relaunched after adding `SurfaceView`; Logcat confirmed `Android surface presentation supported by Adreno (TM) 830`.
- APK inspection confirmed `lib/arm64-v8a/libnfsmw_android.so`.
- Installed the final package ID `com.nfsmw.android` and verified the copied game's root markers and 49-file count under its private `files/nfsmw/game_root` directory.
- The initial NDK 27 APK triggered the device's 16 KiB page-size compatibility warning. The build now uses NDK 28.2.13676358 and links the native library with 16 KiB load alignment; `llvm-readelf` confirmed `p_align=0x4000` for its load segments.
- Confirmed the internal game copy is 6.5 GiB and contains 49 files; no game data was added to the repository or uploaded.

## Blockers and findings

- JDK 21 from Android Studio was needed because the default Java 26 could not run Gradle 8.9. The build scripts now discover standard SDK/JDK locations.
- The base SDK platform header already detects Android and reuses POSIX/Linux code, but SDK CMake still selects X11/Wayland and GNU/Linux surfaces for Android. This must be addressed before the SDK runtime can build as an Android app.
- SDK CMake now gives Android its own `android-arm64` target branch, avoids desktop host code generators in the Android cross-build, and no longer requests X11/Wayland packages. The SDL window path now wraps its Android native window for Vulkan presentation. The SDK has not yet been configured or compiled with the NDK, so this adaptation still needs a build check.
- The requested Dante repository could not be cloned or downloaded: GitHub Git/API/codeload requests returned 404. No source from it was copied.
