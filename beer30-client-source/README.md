# Beer30 Client (in-game mod, Minecraft 1.21.4 Fabric)

Busch Light themed HUD modules with a **Right Shift** menu to switch each one on/off, plus in-game Spotify.

| Module | What it does | Default |
|---|---|---|
| FPS / CPS / Ping | frame rate, clicks per second (left \| right), latency | on |
| Coordinates / Direction / Clock | XYZ, compass facing, real time | off |
| Keystrokes | WASD + mouse buttons overlay | on |
| Armor Status | armor icons with durability | on |
| Potion Status | active effects with timers | on |
| Toggle Sprint | sprints automatically while moving forward | off |
| Zoom | hold **C** to zoom (FOV 30) | on |
| Spotify | now-playing bar at the top, media keys `[` prev, `\` play/pause, `]` next | on |

The Auto Miner is a separate mod now (`beer30-automine`), so you can use either jar alone or both together.

Settings are saved to `config/beer30-client.json`. Keys can be rebound in Options > Controls > Beer30 Client.

These are my own implementations of common client features. I can not copy Lunar Client's mods (they are closed source), so
this is a core set in the same style, not a clone of their full list.

Spotify: the Beer30 launcher keeps your Spotify login and runs a localhost-only bridge (random port + one-time secret per game
launch). The mod only asks it "what's playing?" and sends play/pause/next/previous. It never sees a token.
It only works when the game is started from the Beer30 launcher with Spotify connected (Premium needed for control).

## Building (needs internet, so do it on GitHub - no installs)
1. Make a free GitHub repo and upload everything in this folder (including the hidden `.github` folder).
2. Open the **Actions** tab, wait for the `build` run to finish (about 3 minutes), open it and download the **beer30-client** artifact.
3. Unzip it and put `beer30-client-1.0.0.jar` in the launcher's data folder `%APPDATA%\Beer30-BuschLight-Launcher\beer30-mods\`
   (the launcher creates that folder on first launch). It is installed automatically into Fabric 1.21.4 instances.

Or locally with JDK 21: `./gradlew build` -> `build/libs/beer30-client-1.0.0.jar`.

If the build fails, copy the red error text and send it to me. If Gradle can not find the Yarn mappings, set `yarn_mappings` in
`gradle.properties` to the 1.21.4 value shown on https://fabricmc.net/develop .
