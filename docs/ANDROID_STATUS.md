# Android port status

Updated: 2026-09-29

- [x] Android project structure created
- [x] ARM64 native library builds (`arm64-v8a`, NDK 27.2.12479018)
- [x] Debug and Release APKs build
- [x] APK launches on a connected Galaxy device
- [x] Vulkan instance and physical device discovery (Adreno 830)
- [x] Android Vulkan surface created; graphics queue and surface formats support presentation
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

Verified on 2026-09-29:

- `gradlew --no-daemon assembleDebug` — passed.
- `gradlew --no-daemon assembleRelease` — passed.
- `adb install -r ...app-debug.apk` — passed.
- Launched `com.nfsmw.android.debug/com.nfsmw.android.MainActivity` — passed.
- Logcat confirmed JNI startup, private directory setup, Vulkan loader API 1.1, and physical device `Adreno (TM) 830`; no `AndroidRuntime` crash was reported.
- Reinstalled and relaunched after adding `SurfaceView`; Logcat confirmed `Android surface presentation supported by Adreno (TM) 830`.
- APK inspection confirmed `lib/arm64-v8a/libnfsmw_android.so`.

## Blockers and findings

- Android SDK/NDK and ADB were installed but missing from PATH. JDK 21 from Android Studio was needed because the default Java 26 could not run Gradle 8.9. The build scripts now discover standard SDK/JDK locations.
- The base SDK platform header already detects Android and reuses POSIX/Linux code, but SDK CMake still selects X11/Wayland and GNU/Linux surfaces for Android. This must be addressed before the SDK runtime can build as an Android app.
- The requested Dante repository could not be cloned or downloaded: GitHub Git/API/codeload requests returned 404. No source from it was copied.
