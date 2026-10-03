# Beer30 Auto Miner (Minecraft 1.21.4 Fabric, client-side)

Flies loops inside a bedrock-walled area and mines whatever your crosshair is on. Beer30 themed menu.

- **Right Shift** opens the menu (if the Beer30 Client mod is also installed it already owns Right Shift, so this mod uses **Right Ctrl**). **Z** starts/stops it. Both rebindable in Controls > Beer30 Auto Miner.
- **Finds the area itself:** looks for the bedrock shell with one air block just inside it, seen at 2+ heights (natural bedrock floors and stray blocks are ignored). Unseen walls are estimated 24 blocks around you.
- **Flies loops** (circles/ovals, either direction) and picks a new loop about every 60 s (size, centre, direction). Re-routes if blocked.
- **Mines your crosshair:** it only turns left/right; your up/down aim is never touched. Attacks only a targeted block, never bedrock or anything outside the air gap, never mobs.
- **Stops itself** at 3 hearts, when your tool is almost broken, on world change, or when you press Z. Open menus pause it.
- Needs creative-style flight (otherwise it walks). Holds the height you started at.
- Only use it where automation is allowed.

Settings: `config/beer30-miner.json`. Tests for the planning logic: `test/MinerLogicTest.java` (plain Java, no Minecraft needed).

## Building on GitHub
Upload `beer30-automine-source.zip` (and `beer30-client-source.zip` if you want both) to your repo, use the workflow from the chat, then download the **beer30-mods** artifact.
