# RPG Mechanics

A NeoForge mod for **Minecraft 1.21.1** that gives a pack an RPG layer: a quest journal, a custom controls screen, terrain protection, and a soft fog world border.

[![Latest release](https://img.shields.io/github/v/release/Ankinkun/RPG-Mechanics?label=latest)](https://github.com/Ankinkun/RPG-Mechanics/releases/latest)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.235+-orange)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://adoptium.net/)

**Latest download:** [rpgmechanics-0.1.5.jar](https://github.com/Ankinkun/RPG-Mechanics/releases/download/v0.1.5/rpgmechanics-0.1.5.jar) · [All versions](https://github.com/Ankinkun/RPG-Mechanics/releases)

Drop the jar into your instance `mods` folder. Requires **Java 21** and **NeoForge 21.1.235 or newer** on Minecraft **1.21.1**.

---

## Features

### Quest journal
Pack-owned quests load from datapacks. Players open a book with **J**, track one quest with right-click, and see the current objective on the HUD. Completing a step shows a vanilla-style toast. Progress uses Minecraft advancement criteria, not a custom poller.

### Custom keybinds
Replaces the vanilla Key Binds screen. Each action has a **primary** and **secondary** key, plus a trigger mode: Press, Hold (500 ms), Double Tap, or Release. Escape unbinds a slot. Pack defaults and player overrides live under `config/rpgmechanics/keybinds/`.

### Terrain protection
The world is locked by default. Breaking, placing, trampling, pistons, explosions, and mob grief are cancelled unless the player is on `world.builderAllowlist`. Operator / cheat mode does **not** unlock building unless you turn that on in config.

### Fog border
A polygonal soft fog edge replaces the vanilla striped world border. Unconfigured worlds use a vanilla-sized ~±30 million square (no fog near spawn). Walk past the edge and fog thickens; damage starts after a soft margin. With **Photon** (Iris/Oculus), apply the small shader patch so chunk-edge fog follows this border.

---

## Player guide

### Quests
1. Press **J** to open the quest book.
2. Left-click a quest for details. The book only shows the **current** objective.
3. Right-click a quest to **track** or untrack it. The tracked quest appears on the HUD.
4. Finish the objective in the world. A toast fires when the step (or the whole quest) completes.

Accepting a quest is currently an operator command (`/rpgmechanics quest accept <id>`). The bundled `welcome` quest is a short dirt → stick example.

### Controls
1. Open **Options → Controls → Key Binds**.
2. Click **Primary** or **Secondary**, then press a key. **Escape** clears that slot (it does not restore the default).
3. Click **Trigger** to cycle Press → Hold → Double Tap → Release.
4. **Reset** restores that row; **Reset All** restores the pack/vanilla defaults.

Remove the **Controlling** mod if it is in the pack — it fights this screen for the same menu.

### Building (protected worlds)
You cannot place or break blocks until your name (or UUID) is on the server allowlist:

```toml
# config/rpgmechanics-server.toml
[world]
builderAllowlist = ["YourMinecraftName"]
```

Check status in-game (OP 2+): `/rpgmechanics world protection status`

---

## Pack & operator tutorial

All operator commands need permission level **2**.

### 1. Install
1. Install Minecraft 1.21.1 with NeoForge **21.1.235+**.
2. Put `rpgmechanics-x.y.z.jar` in `mods`.
3. Boot once so config files generate.

### 2. Quest authoring (optional)
Authoring is **off** in a shipped pack.

```toml
# config/rpgmechanics-server.toml
[quests]
questAuthoringMode = true
```

Then, as OP:

| Command | What it does |
|---------|----------------|
| `/rpgmechanics quest editor` | Opens the in-game editor (or use the **Quest Editor** item) |
| `/rpgmechanics quest accept <id> [player]` | Gives a player the quest |
| `/rpgmechanics quest list` | Lists loaded definitions |
| `/rpgmechanics quest reload` | Reloads the export overlay and syncs clients |
| `/rpgmechanics quest export` | Prints the export folder path |

Saves land in `config/rpgmechanics/quest_export/` and overlay the datapack on reload. Custom icons can go in `config/rpgmechanics/quest_icons/`. Detection methods in the editor: biome, item, place block, kill entity.

### 3. Draw a playable fog border
The default border is vanilla-sized, so you will not see fog at spawn until you set a smaller one.

**Quick test square** (centered on you):

```
/rpgmechanics world border setbox 64
```

Walk ~64 blocks out to enter the fog.

**Polygon with the Border Wand** (creative tab **RPG Mechanics**, OP 2+):

1. Right-click the ground to place a vertex. Poles and edges preview for operators.
2. Sneak + right-click to undo the last point.
3. Place at least 3 points that do not cross.
4. `/rpgmechanics world border save`

| Command | What it does |
|---------|----------------|
| `/rpgmechanics world border info` | Current dimension border |
| `/rpgmechanics world border addvertex` | Add a vertex at your position (or with coords) |
| `/rpgmechanics world border undo` / `clear` | Edit the draft |
| `/rpgmechanics world border save` | Write JSON under `config/rpgmechanics/world/borders/` |
| `/rpgmechanics world border reset` | Back to the vanilla-sized mimic |
| `/rpgmechanics world border enable` / `disable` | Toggle that dimension |
| `/rpgmechanics world border reload` | Reload JSON from disk |
| `/rpgmechanics world border debugwall` | Client-only striped wall preview |

Crossing the edge is cosmetic fog first. Damage starts after `world.borderSoftMargin` (default 8 blocks).

### 4. Photon / Iris shaders
Vanilla fog planes are ignored by most shader packs. RPG Mechanics still sets `fogEnd` from **distance to the border**. Photon needs one file patched so its chunk-edge fog reads that value:

1. Backup your Photon zip.
2. Replace `shaders/include/fog/simple_fog.glsl` with [`extras/shader-patches/photon/simple_fog.glsl`](extras/shader-patches/photon/simple_fog.glsl).
3. Reload shaders.

Details: [`extras/shader-patches/photon/README.md`](extras/shader-patches/photon/README.md).

---

## Downloads

Jars are attached to [GitHub Releases](https://github.com/Ankinkun/RPG-Mechanics/releases). Use the latest unless you are matching an older pack pin.

| Version | Highlights | Jar |
|---------|------------|-----|
| **0.1.5** | Iris/Photon fog border via `fogEnd` | [Download](https://github.com/Ankinkun/RPG-Mechanics/releases/download/v0.1.5/rpgmechanics-0.1.5.jar) |
| 0.1.4 | Terrain protection + polygonal fog border | [Download](https://github.com/Ankinkun/RPG-Mechanics/releases/download/v0.1.4/rpgmechanics-0.1.4.jar) |
| 0.1.3 | Inventory hotbar keys + container RMB | [Download](https://github.com/Ankinkun/RPG-Mechanics/releases/download/v0.1.3/rpgmechanics-0.1.3.jar) |
| 0.1.2 | Keybind category order from profile JSON | [Download](https://github.com/Ankinkun/RPG-Mechanics/releases/download/v0.1.2/rpgmechanics-0.1.2.jar) |
| 0.1.1 | Live I18n keybind labels | [Download](https://github.com/Ankinkun/RPG-Mechanics/releases/download/v0.1.1/rpgmechanics-0.1.1.jar) |
| 0.1.0 | First public build | [Download](https://github.com/Ankinkun/RPG-Mechanics/releases/download/v0.1.0/rpgmechanics-0.1.0.jar) |

---

## Config cheat sheet

**Server** — `config/rpgmechanics-server.toml`

| Key | Default | Meaning |
|-----|---------|---------|
| `quests.questAuthoringMode` | `false` | In-game editor + export overlay |
| `world.protectionEnabled` | `true` | Lock terrain |
| `world.opsBypassProtection` | `false` | Let OP/cheats build |
| `world.builderAllowlist` | `[]` | Who may edit blocks |
| `world.borderEnabled` | `true` | Fog border system |
| `world.borderFogDepth` | `32` | Fog depth past the edge (blocks) |
| `world.borderSoftMargin` | `8` | Blocks past the edge before damage |
| `world.borderMaxDamagePerSecond` | `4` | Damage ramp cap |

**Client** — `config/rpgmechanics-client.toml`

| Key | Default | Meaning |
|-----|---------|---------|
| `keybinds.keybindAuthoringMode` | `false` | Category / hide tools in the keybind UI |
| `world.borderShaderFallbackWall` | `false` | Optional forcefield wall if shaders ignore fog |

---

## Build from source

```powershell
.\gradlew.bat build
# jar → build/libs/rpgmechanics-{version}.jar

.\gradlew.bat runClient
.\gradlew.bat runServer
```

Based on the [NeoForge 1.21.1 ModDevGradle MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle).

Contributor / agent contract: [`docs/AGENT_HANDOFF.md`](docs/AGENT_HANDOFF.md). Keybind internals: [`docs/KEYBINDS.md`](docs/KEYBINDS.md).

---

## License

All Rights Reserved.
