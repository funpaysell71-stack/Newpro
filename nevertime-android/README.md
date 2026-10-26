# Nevertime Client Android

Independent Minecraft Bedrock companion app, not affiliated with Flarial. Android 12L+.

Includes native dark UI, floating draggable HUD with live device clock, permission controls, Minecraft launcher.
Includes an experimental LSPosed connection probe for Minecraft's main ARM64 process.
It hooks Application.attach, loads its own native library, finds the already loaded
libminecraftpe.so using dl_iterate_phdr, and compares its SHA-256 with the supplied sample.
This is NOT an implementation of gameplay hooks. KillAura, FullBright, AutoTotem,
CrystalAura, target selection, animated skies, game FPS and coordinates are unimplemented.

Build: GitHub Actions → Build Nevertime Client APK → Run workflow → download Nevertime-Client-APK artifact.
The workflow pins Gradle 8.9 and NDK 27.2, and runs JVM tests, compilation and Android lint.
Device verification remains required. No device test has been performed by the author.

## Phone test
1. Install the debug APK. If an existing installation uses a different signing key,
   Android will reject the update; preserve its settings before deciding to uninstall it.
2. Enable Nevertime in LSPosed and select only com.mojang.minecraftpe.
3. Fully restart Minecraft, let the main menu load, then return to Nevertime.
4. Open “Проверка подключения”. Copy the report if it contains ERROR, a mismatch,
   or the engine timeout. If no report arrives, copy the Nevertime lines in LSPosed's log.

LIBRARY_MATCHED proves only the probe found a matching engine file in the game process.
It does not prove an in-game feature is hooked or operational. Heartbeats older than
15 seconds are marked stale; background app suspension can make a healthy probe stale.
No report is sent over the network. The provider accepts reports only from the Minecraft
UID, records the caller PID itself, and does not accept game commands.

## Sample identity
The user identified the game as v26.50 from the menu screenshot. The binary's identity is:
- Architecture: ELF64 little-endian AArch64.
- Uncompressed size: 328419680 bytes.
- SHA-256: c3ff36e845a7b9d200d01bbed81188784438989153a6a972cb003d92f8e1ceda.

The proprietary game library is not included. No function offsets or signatures have been
validated. Native loading/IPC can fail on particular LSPosed or Android builds; failures
must be diagnosed from the report before implementing gameplay adapters.
