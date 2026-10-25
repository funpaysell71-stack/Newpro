# Nevertime Client 5.0 Preview

Android launcher and device HUD, package `dev.nevertime.client`, Android 8.0+ (API 26), target/compile API 35. Free, no account. This is **not a native Minecraft integration**. No game adapter has been verified under the constraints of unmodified Minecraft, no root and no packs. Game modules are explicitly unavailable, without pretend toggles. The list of verified native Minecraft versions is empty.

## Install and use

1. Download the `Nevertime-Client-5.0-preview` artifact from a successful **Build Nevertime Client APK** GitHub Actions run. Sign in to GitHub if needed, open the ZIP and install `app-debug.apk` on Android. Allow installation from your browser/file manager when prompted.
2. If Android reports an incompatible signature from an earlier debug build, uninstall that build first (this deletes its local profiles). Debug signing keys may differ between CI runs.
3. Install your purchased Minecraft through Google Play separately. No Minecraft files are included or downloaded by Nevertime.
4. Open Nevertime, allow display over other apps for the N button, and optionally allow notifications. Return and press PLAY again. The compatibility dialog honestly reports no native adapter. Choose **Minecraft + меню N** to launch the ordinary installed game with device HUD.
5. Tap N to open/close HUD / World / Visual / Settings. Drag N to move it. In HUD Editor, move framed device widgets, then choose **Save HUD** to lock them. Locked widgets do not consume touches. N itself remains draggable.
6. Stop the overlay from its notification or Settings. It can remain over other apps: foreground-app detection is not implemented.

No add-on, behavior pack, resource pack or .mcpack is installed or required. `bedrock-android/` is legacy source from the prior approach; version 5.0 does not package or use it.

## Capabilities and honest status

| Capability | Status before device CI | Scope |
|---|---|---|
| Launcher, original vector N, adaptive dark UI | Built, Not Game-Tested | Java/native Android views; landscape sidebar and portrait navigation |
| Minecraft detection / PLAY | Built, Not Game-Tested | PackageManager version and ordinary Android launch Intent |
| Version Manager | Partial | HTTPS GitHub metadata, bounded/validated cached JSON; no native adapter or supported version |
| Floating N / ClickGUI | Built, Not Game-Tested | Independent Android overlay; saved position; small touch window |
| Device HUD / editor / profiles | Built, Not Game-Tested | Clock, battery, device RAM, service session, screen Hz (not game FPS); scale, opacity, color, snap, saved profiles |
| Performance modes | Built, Not Game-Tested | Device polling interval only: 3 / 1.5 / 0.75 seconds |
| Coordinates, Chunk Borders, Slime Chunks | Unsupported | No verified player data or world renderer under current constraints |
| Fullbright, Ender Pearl Trajectory, Custom Sky, Fireflies | Unsupported | No verified graphics/entity/physics integration |
| Native Core | Not Implemented | An empty NDK library would not provide game access |
| Client updates | Partial | Opens repository releases; no automatic APK installation |

See [research](docs/ARCHITECTURE.md), [test report](docs/TESTING.md) and [continuation](CONTINUE.md). “Unsupported” describes this architecture and verified evidence, not a claim that every possible authorized future integration is impossible.

## Build

JDK 17, Android SDK platform 35/build-tools 35.0.0, Gradle wrapper 8.9, Android Gradle Plugin 8.7.3.

```sh
cd nevertime-android
bash tests/run.sh
bash gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. CI additionally runs device instrumentation on an Android 35 emulator and uploads screenshots/logs. No native binary, Minecraft assets, or third-party client code is bundled. The app uses INTERNET only for explicit catalog refresh; settings and profiles remain local. Compatibility metadata cannot activate missing native code.
