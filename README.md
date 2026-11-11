# Daldon's Winter Season Phenomena — v0.2.1 experimental

Minecraft Forge 1.20.1 / Java 17. Always test on a backup world.

## How it works

**Season** is global for the Overworld. **Event** is the currently active weather. Setting winter automatically starts snowfall, and setting freezing starts sleet. Snow cover accumulates gradually while snow/sleet/blizzard is active. A blizzard adds snow more quickly and more deeply; the extra depth settles slowly afterward. Hail damages exposed survival players. Thaw removes only the mod's temporary snow/ice.

Every chunk that loads, including newly explored chunks, is processed using the global season. Unloaded chunks are not force-generated. The world-wide event state is shared among players; particles are rendered around each camera (as in vanilla weather), not persisted server-side in unobserved chunks.

### Test changes v0.2.1
- New original 16x16 pixel textures for CRT case, monitor and keyboard, with a particle texture fallback.
- Smaller and better-spaced text on the live monitor.
- Snow cover appears in sparse per-column increments over time, instead of whitening hundreds of blocks instantly.
- Thaw and freezing are throttled. Chunk loading updates current season without instantly filling new regions.
- Client camera-relative overcast cloud transform repaired.
- Distant precipitation drawn as camera-relative world quads (30-102 blocks) plus close-up particles.
- Hail uses tiny textured ice pellets that bounce on the ground before disappearing.
- Atmospheric rendering has not been visually verified and may conflict with some shader mods.

### Newly added in v0.2.0

- Weather events include hail and thaw.
- Cloudy, snowy, and stormy weather get custom cloud-bank overlay and darker fog.
- Stronger sleet, faster and denser blizzard particles, wind-like vanilla ambience and weather audio.
- Retro forecast computer, crafted with iron, green stained glass pane and redstone. This has a real in-world CRT display showing season, current event, next event and countdown without a GUI.
- Explicit season switching releases a manually locked event and starts the season's normal weather.

### Important limitations

The audio uses existing vanilla sounds for now, not custom recordings. The sky rendering needs visual testing with different shaders and cloud settings. Thunderclouds currently also trigger vanilla rain. Melting intentionally does not remove natural snow, vanilla ice or player-placed snow. This is a test version, not yet verified in a dedicated modpack server.

## Commands

```
/daldon season set prewinter
/daldon season set freezing
/daldon season set winter
/daldon season lock true
/daldon season lock false
/daldon event set cloudy
/daldon event set thunderclouds
/daldon event set sleet
/daldon event set snow
/daldon event set blizzard
/daldon event set hail
/daldon event set thaw
/daldon clear
/daldon event auto
/daldon event timer
/daldon status
/daldon drizzle density low
/daldon drizzle density medium
/daldon drizzle density high
```

