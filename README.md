# Daldon's Winter Season Phenomena

An **independent weather and winter-phase mod** for Minecraft **Forge 1.20.1**, Java 17. Currently an experimental, work-in-progress prototype.

## Test build 01 — Drizzle (0.1.1)

Use `/daldon season set prewinter` followed by `/daldon event set drizzle` to freeze the event for visual testing. Stop it with `/daldon event set clear`. Set rain density with `/daldon drizzle density low` (default), `medium`, or `high`. Query with `/daldon drizzle status`. Each density choice is saved with the world and synchronized to multiplayer clients. This first test renders only vanilla-based falling water droplets and sparse ground splashes; it does **not** yet replace Minecraft's whole weather texture renderer or add any wind/rain looping audio.

All drizzle effects are client-side and do not cause snow/ice generation or new chunks to be loaded.

## First prototype

- Three saved, server-controlled phases: prewinter, freezing, winter.
- Weather event schedule with a **known next event and countdown**, saved to the world.
- Events: clear, drizzle, rain, cloudy, thunderclouds, sleet, snow and blizzard.
- Lightweight client particles for drizzle, sleet, snowfall, and cold ambience.
- Cooler fog in wintry weather and grass tint fading gradually between phases.
- Visible harmless lightning in thunderclouds without forcing rain.
- Bounded surface updates: seasonal snow accumulates and temporary water ice freezes; both thaw.
- **Temporary snow and ice have their own block IDs.** Natural snow and vanilla ice are not removed by thawing.
- Newly loaded chunks are queued for treatment; **unvisited chunks are never force-generated**.
- No overworld biome rewrites, static snow biome modifiers, or external mod dependencies.
- SMP: authoritative data in Overworld SavedData and client sync packets.

## Commands

All commands use the prefix /daldon. Changing states requires operator permission level 2.

| Command | Effect |
|---|---|
| /daldon status | Current phase, event, next event, and countdown |
| /daldon season set prewinter | Restart prewinter phase |
| /daldon season set freezing | Start freezing phase |
| /daldon season set winter | Start winter phase |
| /daldon season next | Advance one phase |
| /daldon season skip | Alias for next |
| /daldon season lock true | Prevent automatic phase advancement |
| /daldon season lock false | Resume automatic phase advancement |
| /daldon event set sleet | Force an event and lock weather |
| /daldon event set thunderclouds | Cloud cover and harmless lightning |
| /daldon event auto | Resume random events |
| /daldon event next | Start preselected next event |
| /daldon event timer | Show preselected next event and countdown |
| /daldon drizzle density low | Sparse drizzle (default) |
| /daldon drizzle density medium | Slightly denser drizzle |
| /daldon drizzle density high | Clearly visible test drizzle |
| /daldon drizzle status | Show the active drizzle density |

Available event IDs: clear, drizzle, rain, cloudy, thunderclouds, sleet, snow, blizzard.

Default phases: prewinter 3 Minecraft days; freezing 2 days; winter 7 days; then repeat. The phase timer uses world game time rather than real-world time. Each event lasts 6000–15999 game ticks (5–13.3 Minecraft minutes). Lock a season to keep an endless winter.

## Build

Requires Java 17 and Gradle 8.8 (or compatible Gradle). Run:

    gradle build

GitHub Actions builds and publishes the jar as a workflow artifact if the build succeeds. This is **not** a release-ready CurseForge build until it passes client, dedicated server, and modpack testing.

## Design and compatibility notes

- The first build uses simple client particles, not a full replacement of Minecraft's rain/snow renderer. Cloudy events alter the fog color but do not generate new 3D cloud geometry. Wind audio, improved lighting/sky rendering, config menus, a forecast block, and worldgen-first-frame coverage are planned.
- Snow and ice are applied by limited column updates to chunks loaded around players. A distant newly explored area may briefly look unchanged until its chunk job is processed. The mod never generates all chunks ahead of time.
- Saves store the phase/weather schedule. The next weather event is determined *before* the countdown starts.
- No files, source code, textures, or sounds from Eternal Winter or Expanded Ecosphere are included. Those mods were examined as **references only**, and the new implementation uses standard Forge/Minecraft APIs.

Author: Daldon. License: All Rights Reserved (original content). Third-party assets/licenses must be respected if ever integrated in future.
