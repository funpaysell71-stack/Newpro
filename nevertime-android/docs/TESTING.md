# Test report — 5.0

Verified source commit: `4da1e22a542074186c3743eebb32133789c8017f`.

[Successful GitHub Actions run](https://github.com/funpaysell71-stack/Newpro/actions/runs/40234115882).
[APK artifact](https://github.com/funpaysell71-stack/Newpro/actions/runs/40234115882/artifacts/12613629198).

- Main APK and instrumentation APK compiled successfully.
- HUD math: 11 regression assertions passed.
- Android 35 x86_64 emulator (Pixel 2 profile, landscape): 17 instrumentation assertions passed.
- Lint: 0 errors, 3 warnings (target SDK 35, string localization and intentional physical LEFT overlay coordinates).
- No FATAL EXCEPTION found by the device test log check.
- Screenshots inspected: home, floating menu and editor. Corrected initial floating N logo clipping with FIT_CENTER and repeated the full build/device run.
- APK SHA-256: `62d395b913dc25abd1000d54f8ad413b104e38cf572400e647657cc11c07d7f9`.
- APK contains only compatibility metadata under assets, no packs or native libraries.

Working status is limited to the tested Android UI/device-HUD scenarios. Persistence tests cover preferences/profile restoration and store recreation, not reboot/OEM process termination. Instrumentation invokes views programmatically; real finger input over Minecraft still requires a phone.

Known minor UI limits: selected overlay tab styling is initialized when opening the menu; after changing categories, the initial tab highlight may remain until reopening. Large HUD scales can overlap the default layout; reposition widgets in the editor. These do not affect the reported category content or saved settings.

Automated checks: HUD position/clamping/normalization tests; compatibility JSON parse; compile main/test APK; Android Lint; API 35 emulator instrumentation covering profiles, corrupt configuration atomicity, native-adapter absence, launcher navigation, N with zero widgets, ClickGUI categories, editor unlock/save lock, N drag persistence and service cleanup. Screenshots and logcat collected as Actions artifacts.

Not tested in Minecraft or on physical hardware. Game modules have no implementation and are not covered by synthetic game tests. In particular, no unsupported physics constants or slime references are represented as verified. Need physical Android 8/12/15+ testing, ARM phones/tablets, installed Minecraft launch/version detection, overlay permission denial/revocation, battery saver/OEM service termination, notification denial, cutouts, rotation, touch pass-through over actual gameplay and catalog network failures.
