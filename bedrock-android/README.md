# NOVA Bedrock — Android utility add-on

An **independent** Flarial-inspired in-game utility menu for Minecraft Bedrock Android. Not an official Flarial client, not an APK, and not a native injected overlay.

## Target
- Target game: Minecraft Bedrock Android **26.52**.
- Uses stable `@minecraft/server` 2.0.0 and `@minecraft/server-ui` 2.0.0.
- Minimum declared engine: 1.21.90. Actual compatibility on 26.52 **requires device testing**.

## Features
- Compass opens the in-game menu.
- Alternative command: `/scriptevent nova:menu`.
- Toggle coordinates, world time, horizontal movement speed in the action bar.
- Toggle night vision and decorative particles.
- No root, no patched APK, no account credentials.
- Settings persist for the current game session only.

## Build / install on Android
1. Download the repo ZIP from GitHub or run the `Build Bedrock Android pack` GitHub Action.
2. From the `bedrock-android/behavior_pack` directory, ZIP the **contents** (manifest.json and scripts directory, not the enclosing behavior_pack directory).
3. Rename the ZIP to `NOVA-Bedrock-Android.mcpack`.
4. Open the .mcpack file with Minecraft on Android.
5. Enable the imported behavior pack in the world's Behavior Packs settings.
6. Enter the world and use a compass, or type `/scriptevent nova:menu`.

## Limitations
Bedrock's official add-on API cannot reproduce native Flarial features such as injected ClickGUI, FPS hooks, or rendering overlays. HUD is implemented using Minecraft's action bar; settings are session-local. Multiplayer server policies may restrict add-ons.
