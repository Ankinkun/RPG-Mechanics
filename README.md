# RPG Mechanics

A NeoForge mod for **Minecraft 1.21.1** that adds an RPG layer: Destiny-style character slots and inventory hub, pack quests, custom controls, terrain protection, and a soft fog world border.

[![Latest release](https://img.shields.io/github/v/release/Ankinkun/RPG-Mechanics?label=latest)](https://github.com/Ankinkun/RPG-Mechanics/releases/latest)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.250+-orange)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://adoptium.net/)

**Latest download:** [rpgmechanics-0.1.8.jar](https://github.com/Ankinkun/RPG-Mechanics/releases/download/v0.1.8/rpgmechanics-0.1.8.jar) · [All versions](https://github.com/Ankinkun/RPG-Mechanics/releases)

---

## Install

1. Minecraft **1.21.1** + NeoForge **21.1.250+** + **Java 21**.
2. Drop the jar into `mods`.
3. Soft integrations for full classbuild: Iron’s Spellbooks, Curios, Epic Fight, Xaero’s World Map.
4. Boot once so configs generate. Remove **Controlling** if it fights the keybinds screen.

---

## What you get

- **Characters & hub** — 3 slots, stowed gear bag, Class / Quests / Map tabs, always-sprint, F-mark destroy on Gear
- **Quests** — datapack journal (**J**), track one objective on the HUD
- **Keybinds** — primary + secondary chords, trigger modes, pack taxonomy
- **World** — terrain protection + polygonal fog border (Photon patch available)

---

## Docs

| Guide | For |
|-------|-----|
| [**Player guide**](docs/PLAYER.md) | Playing — hub, gear, quests, controls |
| [**Pack & operator guide**](docs/PACK.md) | Install, config, borders, quest authoring |
| [Documentation index](docs/README.md) | All docs |
| [Keybind internals](docs/KEYBINDS.md) | Controls system details |
| [Agent / contributor handoff](docs/AGENT_HANDOFF.md) | Architecture contract for developers |

---

## Downloads

| Version | Highlights | Jar |
|---------|------------|-----|
| **0.1.8** | Class UI polish, Skills tab removed, always-sprint, gear F-destroy | [Download](https://github.com/Ankinkun/RPG-Mechanics/releases/download/v0.1.8/rpgmechanics-0.1.8.jar) |
| 0.1.7 | Destiny hub, stowed inventory, taxonomy v4 | [Download](https://github.com/Ankinkun/RPG-Mechanics/releases/download/v0.1.7/rpgmechanics-0.1.7.jar) |
| 0.1.6 | Class buildcrafting v1 + NeoForge 21.1.250 | [Download](https://github.com/Ankinkun/RPG-Mechanics/releases/download/v0.1.6/rpgmechanics-0.1.6.jar) |

Older builds: [all releases](https://github.com/Ankinkun/RPG-Mechanics/releases).

---

## Build from source

```powershell
.\gradlew.bat build
# → build/libs/rpgmechanics-{version}.jar
```

Compile-only dependency jars live under `Required Dependencies/`. Based on the [NeoForge 1.21.1 ModDevGradle MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle).

---

## License

All Rights Reserved.
