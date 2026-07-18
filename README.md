# RPG Mechanics

NeoForge mod for Minecraft Java Edition **1.21.1**.

**Repository:** https://github.com/Ankinkun/RPG-Mechanics

| Property | Value |
|----------|--------|
| Mod ID | `rpgmechanics` |
| Author | ankin |
| Minecraft | 1.21.1 |
| NeoForge | see `gradle.properties` (`neo_version`) |
| Java | 21 |
| Version | see `gradle.properties` (`mod_version`) |

## Agent handoff

**Start here:** [`docs/AGENT_HANDOFF.md`](docs/AGENT_HANDOFF.md)

- **Part 0** — day-one checklist for a new agent  
- **Part 1** — NeoForge standing skill set (MC 1.21.1 / Java 21 / ModDevGradle)  
- **Part 2** — project checkpoint (quests, keybinds, versioning)  

Keybinds detail: [`docs/KEYBINDS.md`](docs/KEYBINDS.md).

## Development

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

Based on the official [NeoForge 1.21.1 ModDevGradle MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle).

## Resources

- [NeoForged docs](https://docs.neoforged.net/)
- [NeoForged Discord](https://discord.neoforged.net/)
