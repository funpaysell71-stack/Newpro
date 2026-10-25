# Test report — 5.0

Build and emulator verification: pending first 5.0 CI run; see subsequent update in this file.

Automated checks: HUD position/clamping/normalization tests; compatibility JSON parse; compile main/test APK; Android Lint; API 35 emulator instrumentation covering profiles, corrupt configuration atomicity, native-adapter absence, launcher navigation, N with zero widgets, ClickGUI categories, editor unlock/save lock, N drag persistence and service cleanup. Screenshots and logcat collected as Actions artifacts.

Not tested in Minecraft or on physical hardware. Game modules have no implementation and are not covered by synthetic game tests. In particular, no unsupported physics constants or slime references are represented as verified. Need physical Android 8/12/15+ testing, ARM phones/tablets, installed Minecraft launch/version detection, overlay permission denial/revocation, battery saver/OEM service termination, notification denial, cutouts, rotation, touch pass-through over actual gameplay and catalog network failures.
