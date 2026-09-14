# Pack & operator guide

Install, configure, and operate **RPG Mechanics** for a modpack. For day-to-day play, see [`PLAYER.md`](PLAYER.md).

All operator commands need permission level **2**.

---

## Install

1. Minecraft **1.21.1** with NeoForge **21.1.250+**.
2. Put `rpgmechanics-x.y.z.jar` in `mods`.
3. Soft integrations (recommended for full classbuild): **Iron’s Spellbooks**, **Curios**, **Epic Fight**, **Xaero’s World Map**.
4. Boot once so config files generate.
5. Remove **Controlling** if present — it conflicts with the custom keybinds screen.

Latest jar: [GitHub Releases](https://github.com/Ankinkun/RPG-Mechanics/releases/latest).

---

## Soft dependencies

| Mod | Used for |
|-----|----------|
| Iron’s Spellbooks + Curios | Managed spellbook, kit spells, mana |
| Epic Fight | Base dodge (roll) + guard; combat mode forced |
| Xaero’s World Map | Map hub tab |

Compile-time jars for CI / MDK live under `Required Dependencies/` in the repo.

---

## Config cheat sheet

### Server — `config/rpgmechanics-server.toml`

| Key | Default | Meaning |
|-----|---------|---------|
| `quests.questAuthoringMode` | `false` | In-game editor + export overlay |
| `classbuild.classbuildEnabled` | `true` | Character slots + inventory hub |
| `classbuild.baseMaxMana` | `650` | Iron’s Spellbooks max mana on kit apply |
| `classbuild.forceAdventure` | `true` | Adventure mode when a class is confirmed |
| `world.protectionEnabled` | `true` | Lock terrain |
| `world.opsBypassProtection` | `false` | Let OP/cheats build |
| `world.builderAllowlist` | `[]` | Who may edit blocks (name or UUID) |
| `world.borderEnabled` | `true` | Fog border system |
| `world.borderFogDepth` | `32` | Fog depth past the edge (blocks) |
| `world.borderSoftMargin` | `8` | Blocks past the edge before damage |
| `world.borderMaxDamagePerSecond` | `4` | Damage ramp cap |

### Client — `config/rpgmechanics-client.toml`

| Key | Default | Meaning |
|-----|---------|---------|
| `keybinds.keybindAuthoringMode` | `false` | Category / hide tools in the keybind UI |
| `world.borderShaderFallbackWall` | `false` | Optional forcefield wall if shaders ignore fog |
| `classbuild.alwaysSprintWhenMoving` | `true` | Sprint while moving forward (sneak wins) |
| `classbuild.combatHud` | `true` | LoL-style combat HUD; hides vanilla hotbar chrome |
| `classbuild.campaignWorldName` | `""` | Singleplayer save folder after title select (empty = first save) |

Builder allowlist example:

```toml
# config/rpgmechanics-server.toml
[world]
builderAllowlist = ["YourMinecraftName"]
```

Status: `/rpgmechanics world protection status`

---

## Quest authoring (optional)

Authoring is **off** in a shipped pack.

```toml
# config/rpgmechanics-server.toml
[quests]
questAuthoringMode = true
```

| Command | What it does |
|---------|----------------|
| `/rpgmechanics quest editor` | Opens the in-game editor (or use the **Quest Editor** item) |
| `/rpgmechanics quest accept <id> [player]` | Gives a player the quest |
| `/rpgmechanics quest list` | Lists loaded definitions |
| `/rpgmechanics quest reload` | Reloads the export overlay and syncs clients |
| `/rpgmechanics quest export` | Prints the export folder path |

Saves land in `config/rpgmechanics/quest_export/` and overlay the datapack on reload. Custom icons: `config/rpgmechanics/quest_icons/`. Editor detection methods: biome, item, place block, kill entity.

---

## Fog border

The default border is vanilla-sized (~±30M), so you will not see fog at spawn until you set a smaller one.

**Quick test square** (centered on you):

```
/rpgmechanics world border setbox 64
```

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

### Photon / Iris

Vanilla fog planes are ignored by most shader packs. RPG Mechanics sets `fogEnd` from **distance to the border**. Photon needs one file patched:

1. Backup your Photon zip.
2. Replace `shaders/include/fog/simple_fog.glsl` with [`../extras/shader-patches/photon/simple_fog.glsl`](../extras/shader-patches/photon/simple_fog.glsl).
3. Reload shaders.

Details: [`../extras/shader-patches/photon/README.md`](../extras/shader-patches/photon/README.md).

---

## Classbuild notes for pack authors

- Title character select → campaign world (`campaignWorldName`).
- Three slots: kit + gear + stowed bag per character.
- Kit apply learns Iron’s Spellbooks spells and sets base max mana.
- Epic Fight: roll + guard granted; combat mode forced; **no** skill GUI entry in the hub or pack taxonomy.
- Per-character worlds are a future TODO; today all characters share the configured campaign save.

---

## Build from source

```powershell
.\gradlew.bat build
# jar → build/libs/rpgmechanics-{version}.jar

.\gradlew.bat runClient
.\gradlew.bat runServer
```

Based on the [NeoForge 1.21.1 ModDevGradle MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle).

Developer / agent contract: [`AGENT_HANDOFF.md`](AGENT_HANDOFF.md). Keybind internals: [`KEYBINDS.md`](KEYBINDS.md).
