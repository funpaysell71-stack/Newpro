> Updated investigation: [GAME_BRIDGE_AUDIT.md](GAME_BRIDGE_AUDIT.md). The earlier audit was incomplete: LeviLaunchroid provides a real in-process hosting architecture, but compatibility/compliance and live coordinates are not verified.

# Architecture audit — 5.0

Inspected public upstream source, not assumptions from promotional UI:

- [Launchly](https://github.com/flarialmc/Launchly): Kotlin Android version manager. `app/src/main/java/com/zeuroux/launchly/packageops/PackageCoordinator.kt` launches the installed Minecraft package using `getLaunchIntentForPackage`. Delivery/install code manages APK splits through Android installation mechanisms; this does not establish in-process game module loading. GPL-3.0; no code copied. Android min API 28 in inspected project; package compatibility depends on chosen game build/device ABI.
- [Flarial.Launcher](https://github.com/flarialmc/Flarial.Launcher): C#/Avalonia Windows launcher/MSIX workflow, GPL-3.0. It is not an Android native game adapter.
- [dll-oss](https://github.com/flarialmc/dll-oss): Windows C++ client, AGPL-3.0; CMake links Direct3D 11/12, DXGI, Direct2D and Windows libraries. Its Windows hooks and DLL cannot be loaded into Android Minecraft as an Android library.
- Supplied `Flarial 1.4.17.zip`: APK split archive, not licensed source. Previously inspected manifest reports package xyz.flarial.client, version 1.4.17, min API 32 / target 36, arm64 split. Archive metadata does not prove how gameplay integration works. No binary/assets copied.

## Requested integration questions

1. Public Android launcher: Kotlin package/version/download/install management is confirmed in Launchly.
2. Launch mechanism in inspected Launchly: standard Android package launch Intent.
3. Android client module loader: not established by inspected open source.
4. Android coordinates source: not established.
5. Android entity access: not established.
6. Native Android in-game renderer: not established; Windows renderer is not evidence of Android support.
7. Verified compatible Android Minecraft versions for Nevertime native integration: none.
8. Nevertime launcher: API 26+, Java bytecode/no ABI-specific libraries. Emulator verification targets API 35 x86_64; physical ARM devices need testing. Upstream requirements do not imply Nevertime compatibility.
9. Public projects use GPL/AGPL as above. This implementation uses independently written Android code; no copied source requires upstream license integration. No Minecraft files redistributed.

Android separates applications by UID, including native code: [official sandbox documentation](https://source.android.com/docs/security/app-sandbox). A separate NDK library does not expose another process's player/entity data. Under the user's constraints (no packs, root, unofficial APK modification or bypass), no verified integration was found. We therefore implement an independent launcher/overlay and explicitly keep gameplay modules unavailable. This is an engineering finding, not a legal opinion about alternative loaders.

## Implemented design

`MainActivity` / `Design`: adaptive native launcher, original vector logo, gradients, category navigation, settings and profile UI. `SettingsStore`: SharedPreferences with bounded profile restore. `OverlayService`: foreground specialUse service, device indicators and explicit locked/editor modes. `OverlayControls`: movable N and separate limited-area ClickGUI windows. `Compatibility`: HTTPS metadata only, max 64 KiB, schema validation, cached fallback. No remote executable code.

The overlay cannot provide true acrylic blur of Minecraft; translucent gradient panels approximate the visual style without claiming game renderer integration. HUD Editor controls device widgets only. Game coordinates, 3D chunk contours, projectile models and sky presets are not simulated.

## Audited upstream snapshots

- Launchly: `0d49046d568c3be5cedb4d00085566644d604098`
- Flarial.Launcher: `4d34c5b9f61a4bd3637f07153be70ac3e5f695d1`
- dll-oss: `40ad1870efd021138241192894cfbfacef1aefa5`

These are inspected snapshots, not a claim about all private or future Flarial components.
