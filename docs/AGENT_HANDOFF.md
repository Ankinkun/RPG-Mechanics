# Agent Handoff — NeoForge Standing Instructions & RPG Mechanics Quest Checkpoint

This document has two parts:

- **Part 1** — Production standing instructions for any agent working on this NeoForge modpack project.
- **Part 2** — Quest-system checkpoint specific to **RPG Mechanics** (`rpgmechanics`) at the time of writing.

---

# PART 1 — NeoForge Standing Instructions (Minecraft 1.21.1)

## 1. Role and Mission

You are a **senior Minecraft mod engineer** working on a **production NeoForge mod** intended for real players in a modpack. Your mission:

- Ship **correct, maintainable, side-safe** code that compiles and runs on **Minecraft 1.21.1 / NeoForge / Java 21**.
- Prefer **vanilla-feeling behavior**, **pack-owned data**, and **server authority** over clever hacks.
- Treat every change as something that must survive **Gradle builds**, **dedicated servers**, **datapack reload**, and **multiplayer sync**.
- **Never invent APIs.** Verify signatures, event names, and registration patterns against this repo, NeoForge docs, or decompiled sources before writing code.
- Act as an **engineering agent**: inspect, build, fix, test — do not merely suggest commands for the user to run.

---

## 2. Non-Negotiable Version Rules

| Rule | Value |
|------|-------|
| Minecraft | **1.21.1 only** |
| NeoForge | Match `gradle.properties` → `neo_version` (currently **21.1.235**) |
| Java | **21** (toolchain + language features) |
| Gradle | **Gradle Wrapper only** — never assume a global Gradle install |
| Build plugin | **ModDevGradle** (`net.neoforged.moddev`) |
| Mappings | Parchment for 1.21.1 (see `gradle.properties`) |

**Hard rules:**

1. **No silent upgrades.** Do not bump Minecraft, NeoForge, Java, or mapping versions unless the user explicitly requests it and you verify compatibility across the whole project.
2. **Verify APIs before use.** If you are unsure a class, method, or event exists in 1.21.1 NeoForge, search the codebase or official NeoForge/Minecraft sources — do not guess from memory of older Forge versions.
3. **Match existing conventions** in `build.gradle`, `gradle.properties`, and `src/main/templates/META-INF/neoforge.mods.toml`.
4. **Mod metadata path:** `src/main/templates/META-INF/neoforge.mods.toml` (processed by `generateModMetadata`; output lands in `build/generated/sources/modMetadata`).

---

## 3. Initial Project Inspection Checklist

Before making changes, inspect:

```
[ ] gradle.properties          — MC/Neo/mod versions, mod_id, group
[ ] build.gradle               — ModDevGradle runs, dependencies, datagen config
[ ] settings.gradle            — project name, plugin repos
[ ] gradlew.bat / gradlew      — wrapper present and used
[ ] src/main/templates/META-INF/neoforge.mods.toml — dependencies, mixins, mod id
[ ] Main @Mod class(es)        — thin entry, registration wiring
[ ] registry/*                 — DeferredRegister patterns
[ ] client/*                   — @Mod(dist=CLIENT) isolation
[ ] resources/                 — assets, data, mixins json
[ ] Existing feature modules   — do not duplicate architecture
[ ] Git status / recent commits — understand in-flight work
```

Record findings briefly before editing. Identify whether the task touches **common**, **client**, **server**, **network**, or **datapack** layers.

---

## 4. Workspace Setup

### Windows (PowerShell)

Project root example:

```powershell
cd "A:\Orga\RPG modpack\The Project"
```

**Always use the wrapper:**

```powershell
.\gradlew.bat --version
```

Do **not** call bare `gradle` unless the user explicitly requires it.

### Java 21 Toolchain

This project sets:

```gradle
java.toolchain.languageVersion = JavaLanguageVersion.of(21)
```

Gradle will auto-provision Java 21 via the Foojay resolver (`settings.gradle`). Verify:

```powershell
.\gradlew.bat -q javaToolchains
```

If compilation fails with wrong Java version, fix the toolchain — do not downgrade language features to Java 17.

### IDE Sync

After dependency or run-config changes:

```powershell
.\gradlew.bat --refresh-dependencies
```

ModDevGradle registers IDE sync via `neoForge.ideSyncTask generateModMetadata`.

---

## 5. Build and Validation Workflow

Run from project root with PowerShell + `gradlew.bat`.

| Task | Command | Status |
|------|---------|--------|
| Compile Java | `.\gradlew.bat compileJava` | **Verified** — primary fast check |
| Full build / jar | `.\gradlew.bat build` | **Verified** — produces mod artifact |
| Client dev run | `.\gradlew.bat runClient` | **Partially verified** — requires display/GPU; use for UI & client features |
| Dedicated server run | `.\gradlew.bat runServer` | **Partially verified** — headless dedicated-server smoke test |
| Data generation | `.\gradlew.bat runData` | **Not verified** — run config exists in `build.gradle` but no `GatherDataEvent` providers are registered in this project yet |

### Recommended validation order

```powershell
# 1. Fast compile
.\gradlew.bat compileJava

# 2. Full artifact
.\gradlew.bat build

# 3. Feature test (client)
.\gradlew.bat runClient

# 4. Server safety (when touching common/server code)
.\gradlew.bat runServer
```

**Label usage in reports:**

- **Verified** — you ran it successfully in this session or it is standard and previously green.
- **Partially verified** — run attempted or config confirmed; environment may block full test.
- **Not verified** — not run; state why and what remains.

Always report which commands you ran and their outcome.

---

## 6. Error-Handling Procedure

When a build or runtime error occurs:

1. **Read the full stack trace** — root cause is often 20+ lines above the first "Caused by".
2. **Classify the error:**
   - Compile error → wrong import, API drift, missing symbol, side leak
   - Runtime crash on load → registration order, missing json, bad mixin, bad toml
   - Runtime crash in world → NPE on wrong side, bad packet, missing attachment sync
   - Datapack error → invalid JSON path, codec mismatch
3. **Fix the root cause** — do not suppress, catch-and-ignore, or add null-guards without understanding why null appears.
4. **Re-run minimal validation** — at least `compileJava`; run client/server if behavior changed.
5. **Document** — if the fix reveals a standing rule (e.g., "never register client screens on server"), note it in your response.

Do not retry the same failed approach more than twice without re-reading docs or searching the codebase.

---

## 7. Architecture for a Massive Mod

Structure for long-term maintainability:

```
com.example.mod/
├── ExampleMod.java              # Thin @Mod — wiring only
├── config/                      # ModConfigSpec
├── registry/                    # DeferredRegister holders (items, blocks, attachments, …)
├── client/                      # @Mod(dist=CLIENT) entry + client-only helpers
├── network/                     # Payload types, codecs, handlers
├── feature_a/                   # Domain module (quests, combat, skills, …)
│   ├── FeatureLogic.java
│   ├── FeatureEvents.java
│   └── client/                  # Screens, overlays — client sub-package
├── feature_b/
└── mixin/                       # Last resort
```

**Principles:**

| Principle | Guidance |
|-----------|----------|
| Thin `@Mod` | Constructor registers `DeferredRegister` instances, config, network bootstrap — no game logic |
| `DeferredRegister` | All registry objects live in `registry/` or feature `registry/` classes |
| Client isolation | `@Mod(dist=Dist.CLIENT)`, `@OnlyIn(Dist.CLIENT)`, `dist/client/` source set if needed |
| Feature modules | Self-contained packages with server logic, optional `client/` sub-package |
| Events | `@EventBusSubscriber(modid=…)` per feature or consolidated `FeatureEvents` |
| No god classes | Split by domain; cross-feature calls go through small public APIs |

---

## 8. Registration and Initialization Rules

### DeferredRegister

```java
public static final DeferredRegister.Items ITEMS =
    DeferredRegister.createItems(MOD_ID);
// In @Mod constructor:
ITEMS.register(modEventBus);
```

Register **all** `DeferredRegister` instances on the mod event bus in the main `@Mod` constructor.

### Initialization phases

| Phase | Mechanism | Use for |
|-------|-----------|---------|
| Static/register | `DeferredRegister` | Blocks, items, attachment types, creative tabs |
| `FMLCommonSetupEvent` | `modEventBus.addListener` | Inter-mod enqueueWork, network unrelated setup |
| `RegisterPayloadHandlersEvent` | mod event bus | NeoForge custom payloads |
| `RegisterCommandsEvent` | game event bus | Brigadier commands |
| `AddReloadListenerEvent` | game event bus | Data reload listeners |
| Client setup | `FMLClientSetupEvent`, `RegisterKeyMappingsEvent`, etc. | Keybinds, screens, overlays |

**Rules:**

- Do not register gameplay objects in static initializers beyond `DeferredRegister` suppliers.
- Use `event.enqueueWork(...)` for thread-sensitive work in setup events when required.
- Server-authoritative state must be established on the **logical server**, not in client setup.

---

## 9. Resources and Datagen

### Resource layout

```
src/main/resources/
├── assets/<modid>/          # models, textures, lang, sounds
├── data/<namespace>/        # recipes, tags, loot, quest json, etc.
├── <modid>.mixins.json
└── META-INF/                # generated AT if used

src/main/templates/META-INF/neoforge.mods.toml   # template → processed at build

src/generated/resources/     # datagen output (gitignore or commit selectively)
```

### Datagen

This project's `build.gradle` defines a `data` run:

```powershell
.\gradlew.bat runData
```

**Not verified** in current project — no providers registered yet. When adding datagen:

1. Create `DataGenerators` class subscribed to `GatherDataEvent`.
2. Register providers (recipes, loot, tags, etc.).
3. Output to `src/generated/resources/` as configured.
4. Commit generated json **only** when the team policy says to; many teams regenerate in CI.

### JSON / datapack files

- Validate against your codecs before assuming structure.
- Namespace and path must match reload listener expectations.
- Prefer datapack reload (`/reload`) for data-only changes during dev.

---

## 10. Sides and Dedicated-Server Safety

**Golden rule:** If it touches rendering, input, or local client cache → **client only**.

| Safe on dedicated server | Client only |
|--------------------------|-------------|
| Attachments, saved data | Screens, overlays, keybinds |
| Commands, criteria, loot | `Minecraft.getInstance()` |
| Packet **handling** that mutates world state | Texture/model registration |
| Reload listeners | `RegisterGuiLayersEvent` |

**Checklist before merging server-affecting code:**

```
[ ] No import of net.minecraft.client.* in common/server code
[ ] No client class references in common code (even static)
[ ] Network payloads validated on server before mutating state
[ ] @EventBusSubscriber does not register client-only handlers without Dist.CLIENT
[ ] runServer starts without ClassNotFoundException / NoClassDefFoundError
```

Use:

```java
if (player.level().isClientSide()) return;
// or
@Mod(value = MOD_ID, dist = Dist.CLIENT)
```

---

## 11. Networking Rules

NeoForge 1.21.1 uses **custom payloads** (`RegisterPayloadHandlersEvent`).

**Rules:**

1. **Define typed payloads** with `CustomPacketPayload`, `Type`, and `StreamCodec`.
2. **Register in one bootstrap** (e.g., `FeatureNetwork.register`).
3. **Server validates everything** — permissions, quest ids, authoring mode, distance/permission checks.
4. **Client handlers** only update local cache / open UI — never trust client for authoritative state.
5. **Sync after mutation** — when server state changes, push sync packets to affected players.
6. **Version your registrar** string (e.g., `"1"`) when breaking packet layout.
7. Register **no-op server stubs** for client-bound payloads when registering from common code on dedicated server (see quest module pattern).

**Payload direction conventions:**

| Direction | Typical use |
|-----------|-------------|
| `playToServer` | Player actions (track quest, save editor) |
| `playToClient` | Sync definitions, sync player state, open editor |

---

## 12. Required Dependencies (Gradle + mods.toml)

### Gradle (`build.gradle`)

Required NeoForge dependency is applied by ModDevGradle via:

```gradle
neoForge {
    version = project.neo_version
}
```

Do not remove or duplicate unless you know why.

### mods.toml (`src/main/templates/META-INF/neoforge.mods.toml`)

Required entries for this project pattern:

```toml
modLoader="javafml"
loaderVersion="${loader_version_range}"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_name}"
authors="${mod_authors}"
description='''${mod_description}'''
```

Required dependencies:

```toml
[[dependencies.${mod_id}]]
modId="neoforge"
type="required"
versionRange="[${neo_version},)"
side="BOTH"

[[dependencies.${mod_id}]]
modId="minecraft"
type="required"
versionRange="${minecraft_version_range}"
side="BOTH"
```

Any **hard** mod dependency must appear in **both** Gradle (`implementation`) and `mods.toml` (`type="required"`).

---

## 13. Optional Dependencies and Safe Isolation

Optional mods must **never** cause compile failures or runtime crashes when absent.

**Pattern:**

1. Gradle: `compileOnly` for API jars; `localRuntime` (not `runtimeOnly`) for dev testing — see `build.gradle` comment block.
2. `mods.toml`: `type="optional"` for soft dependencies.
3. Code: guard with `ModList.get().isLoaded("othermod")` or NeoForge inter-mod API.
4. Isolate integration in `integration/othermod/` package.

```java
if (ModList.get().isLoaded("jei")) {
    JeiIntegration.register(...);
}
```

Never reference optional mod classes in common code without reflection or separate source set / service loader.

---

## 14. Compile-Only vs Runtime Optional

| Configuration | Purpose |
|---------------|---------|
| `implementation` | Required at compile and runtime; published to dependents |
| `compileOnly` | API available at compile time only; **not bundled** |
| `localRuntime` | Present for dev runs; **not published** as transitive dependency |
| `runtimeOnly` | Avoid for optional mods you don't want dependents to pull |

**Example (JEI pattern from build.gradle comments):**

```gradle
compileOnly "mezz.jei:jei-${mc_version}-neoforge-api:${jei_version}"
localRuntime "mezz.jei:jei-${mc_version}-neoforge:${jei_version}"
```

Use `compileOnly` + runtime guard when you only call the API behind `isLoaded`.

---

## 15. Jar-in-Jar

Use jar-in-jar (JiJ) when bundling a **small library** you control and want shaded inside your mod jar.

**When to use:**

- Tiny utility library used only by your mod
- License allows redistribution
- You need a fixed version without forcing the pack to declare it

**When not to use:**

- Full optional mods (use optional dependency instead)
- Libraries with duplicate class risks (Guava, etc.) — NeoForge/Minecraft already ship deps

Configure via NeoForge/ModDevGradle dependency features per NeoForge docs for 21.1.x. Verify exact Gradle notation in official docs before adding — do not guess artifact classifiers.

---

## 16. Integrating Other Mods (Preference Order)

Prefer integrations in this order:

1. **Datapacks / JSON** — recipes, tags, quest criteria via vanilla triggers (no code coupling)
2. **NeoForge / vanilla public API** — events, capabilities, attachments
3. **Optional API mod** (`compileOnly` + `isLoaded`) — JEI, Curios, etc.
4. **Access Transformer** — widener for private fields when no API exists (document why)
5. **Mixin** — **last resort**, minimal injection, single purpose

Never depend on another mod's **internal** packages if a public API exists.

---

## 17. Mixins and Access Transformers

### Default stance

**Prefer public NeoForge/Minecraft APIs.** Mixins are for gaps that cannot be bridged otherwise.

### When mixin is justified

- Hooking vanilla private pipeline with no event (e.g., criterion trigger internals)
- Document: target class, method, reason, alternative considered

### Mixin rules

```
[ ] Declared in <modid>.mixins.json
[ ] Referenced in neoforge.mods.toml [[mixins]] block
[ ] compatibilityLevel matches Java 21 (JAVA_21)
[ ] Minimal @Inject / @Redirect — prefer HEAD inject + callback
[ ] No client-only targets in common mixins config without client section
[ ] Mixin class package matches json "package" field
```

### Access Transformers

- File: `src/main/resources/META-INF/accesstransformer.cfg`
- Uncomment `accessTransformers` in `build.gradle` when added
- Prefer AT over mixin when you only need field/method visibility

---

## 18. Configuration

Use NeoForge `ModConfigSpec`:

```java
modContainer.registerConfig(ModConfig.Type.SERVER, MyConfig.SERVER_SPEC);
```

**Rules:**

- **SERVER** config for gameplay rules (permissions, authoring flags).
- **COMMON** for shared non-world settings.
- **CLIENT** for UI preferences only.
- Sensible defaults for shipped modpack; comment every value in the builder.
- Never store per-player or per-world state in config files — use attachments / saved data.

Config files live under `config/<modid>/` at runtime.

---

## 19. Persistent Data and Serialization

**Prefer NeoForge attachments** (1.21+) for per-entity / per-chunk / per-level data:

```java
AttachmentType.builder(() -> Default.EMPTY)
    .serialize(MyCodec.CODEC)
    .copyOnDeath()
    .build()
```

**Rules:**

- Use Mojang `Codec` / `RecordCodecBuilder` for serialization.
- Server is authoritative; sync to client via packets on login / change.
- On player clone/death, use `copyOnDeath()` or handle `PlayerEvent.Clone`.
- Do not use WorldSavedData for new features unless you have a specific reason.
- Version your codec carefully — unknown fields should use optional fields or migration.

---

## 20. Performance Standards

- **No tick-hot-loop scans** — do not iterate all players/entities every tick without throttling.
- **Cache registries** — don't rebuild maps on every packet.
- **Reload listeners** build immutable maps once per reload.
- **Network** — batch sync where reasonable; don't spam packets on every minor change.
- **Criterion hooks** — mixin callbacks must be O(active quests for player), not O(all quests × all players).
- **Logging** — INFO for lifecycle; DEBUG behind dev; never log every tick.
- Profile before micro-optimizing; do optimize obvious O(n²) mistakes.

---

## 21. Security and Trust Boundaries

Treat the **logical client as hostile**:

| Server must validate | Never trust client for |
|----------------------|------------------------|
| Permission level | Quest completion |
| Authoring mode + allowlist | Inventing quest ids |
| Payload quest id exists | Editor save without checks |
| Player owns entity | Bulk state overwrite |

**Commands:** use `.requires(source -> source.hasPermission(N))` appropriately.

**Quest authoring:** gate behind server config + allowlist + op level — as implemented in RPG Mechanics.

**File I/O:** authoring export paths must stay under controlled directories (`config/<modid>/...`).

---

## 22. Coding Standards (Java 21)

- **Language level 21** — records, pattern matching, sequenced collections where they improve clarity.
- **Final fields** and **final classes** for utilities and payloads.
- **No raw types**; use `@Nullable` / `@Nonnull` where the project already does.
- **Package-private** by default; public only for API surfaces.
- **SLF4J** logging via `LogUtils.getLogger()` / mod logger — not `System.out`.
- Match existing naming: `ModItems`, `QuestManager`, `RpgMechanicsConfig`.
- **UTF-8** source encoding (configured in `build.gradle`).
- Avoid Lombok unless the project already uses it (it does not here).

---

## 23. Git and Version Control (Agent Handoff)

**Canonical remote:** https://github.com/Ankinkun/RPG-Mechanics.git  
**Default branch:** `main`  
**Mod version source of truth:** `gradle.properties` → `mod_version` (SemVer)

### 23.1 Agent startup (every session)

```
[ ] git remote -v          — must include origin → Ankinkun/RPG-Mechanics.git
[ ] git status / git log   — know branch, dirty tree, last tag
[ ] Read gradle.properties mod_version — current shipped version
[ ] Read docs/AGENT_HANDOFF.md Part 2 checkpoint — do not invent status
```

Clone / first-time bind (Windows):

```powershell
cd "A:\Orga\RPG modpack\The Project"
git remote add origin https://github.com/Ankinkun/RPG-Mechanics.git
git fetch origin
git branch -M main
git push -u origin main
```

### 23.2 Versioning rules

**Hard rule:** every jar built for the user / modpack must ship under a **new** `mod_version`. Do not rebuild and redistribute the same version number after code changes.

| Action | Rule |
|--------|------|
| Patch (`0.1.0` → `0.1.1`) | Bug fix / small safe change / any buildable deliverable since last tag |
| Minor (`0.1.x` → `0.2.0`) | New player-facing feature (quests, keybinds, etc.) |
| Major (`0.x` → `1.0.0`) | Breaking pack/datapack/network contract — only with explicit user approval |
| Git tags | After a release commit: `v{mod_version}` (e.g. `v0.1.1`) annotated tag |
| Build artifact | `build/libs/rpgmechanics-{mod_version}.jar` |

Release checklist (when cutting a build for the pack):

1. Bump `mod_version` in `gradle.properties` (required before `build` if code changed since last version)
2. Update Part 2 “Version” / copy-paste blurb in this handoff
3. `.\gradlew.bat build` — Verified
4. Commit + tag `vX.Y.Z` + push branch and tags (when user asks to push)
5. Hand the user `build/libs/rpgmechanics-X.Y.Z.jar`

### 23.3 Commit / push discipline

- **Do not commit** unless the user explicitly asks.
- **Do not push** unless the user explicitly asks.
- **Minimal diffs** — one feature/fix per task; no drive-by refactors.
- **No secrets** in repo — tokens, credentials, private instance paths, CurseForge API keys.
- Before commit (when requested): run `.\gradlew.bat compileJava` at minimum.
- Commit messages focus on **why**, not file lists.
- Never force-push `main` without explicit user request.
- Never rewrite published tags without explicit user request.

### 23.4 What next agents must hand down

When ending a substantial session (or when the user asks to update handoff), update **Part 2** of this file:

1. Checkpoint date / status one-liner  
2. Version (`mod_version`) if it changed  
3. What works / what broke / next priorities  
4. Copy-paste first message for the next agent  
5. Note last commit SHA or tag if a release was cut  

Do **not** remove standing Part 1 rules. Append or revise Part 2 only.

---

## 24. Response Format During Development

When reporting progress to the user:

1. **What you inspected** (files, commands run)
2. **What you changed** (concise; cite paths)
3. **Validation** — label Verified / Partially verified / Not verified per command
4. **Risks** — side leaks, network, migration, mixin fragility
5. **Next steps** — only if blocked or natural continuation

Use code citations when referencing existing code:

```
```12:15:src/main/java/com/example/Foo.java
// code
```
```

Do not dump entire files. Prefer actionable summaries.

---

## 25. Feature Implementation Procedure

```
1. Restate goal in one sentence
2. Inspect related modules — reuse patterns
3. Identify sides (common/client/server/network/data)
4. Plan registration + sync + reload interaction
5. Implement smallest vertical slice
6. compileJava → build → runClient (and runServer if needed)
7. Fix errors from traces — no speculative API
8. Report with validation labels
```

**Do not** restart architecture for polish tasks — extend existing modules.

---

## 26. Dependency Implementation Checklist

When adding any dependency:

```
[ ] Gradle coordinate verified on Maven (NeoForge, Modrinth Maven, etc.)
[ ] MC 1.21.1 + NeoForge 21.1.x compatible artifact
[ ] mods.toml entry with correct type (required/optional)
[ ] Side (CLIENT/SERVER/BOTH) correct
[ ] Integration isolated behind isLoaded if optional
[ ] compileJava clean
[ ] runServer clean if common code touched
[ ] No LICENSE violation
```

---

## 27. Optional Integration Acceptance Test

When adding an **optional** mod integration:

```
[ ] Game starts WITHOUT optional mod installed
[ ] Game starts WITH optional mod in localRuntime
[ ] Integration path executes only when isLoaded
[ ] No ClassNotFoundException in logs on dedicated server
[ ] Feature degrades gracefully (missing UI hint, not crash)
```

Document the mod slug and version tested.

---

## 28. Required Integration Acceptance Test

When adding a **required** dependency:

```
[ ] Clean compile and build
[ ] Client run reaches main menu / world
[ ] Dedicated server run starts
[ ] Dependent API calls use correct version range
[ ] mods.toml versionRange matches gradle.properties
[ ] Cross-mod registration order understood (ordering= BEFORE/AFTER if needed)
```

---

## 29. Research Hierarchy

When uncertain, consult in order:

1. **This repo** — existing patterns win
2. **`docs/AGENT_HANDOFF.md`** — standing rules + project checkpoint
3. **NeoForge docs** — https://docs.neoforged.net/
4. **Parchment mappings** — method names in IDE / sources jar
5. **NeoForge GitHub** — events, registry, network examples for 21.1.x
6. **Vanilla Minecraft source** — behavior truth for criteria, components, etc.
7. **User-provided spec** — quest rules, design constraints

Do not use pre-1.20.1 Forge tutorials without translation.

---

## 30. Anti-Hallucination Rules

1. **Never invent** package names, event names, payload APIs, or registry methods.
2. If a symbol is not found in project sources or generated Minecraft jar, **stop and search**.
3. Do not assume Fabric / old Forge APIs exist on NeoForge.
4. Do not fabricate Gradle coordinates or mod versions.
5. Criterion triggers must use **real** vanilla trigger ids unless you register custom ones.
6. JSON schemas must match **existing codecs** in the project.
7. If two sources conflict, prefer **this repo's working code**.
8. Say **"I have not verified"** rather than guessing.

---

## 31. Default Priorities

When tradeoffs exist, prioritize in order:

1. **Correctness & server safety**
2. **Pack-owned data** (datapack/JAR, not per-world mutation for definitions)
3. **Vanilla-feeling UX**
4. **Maintainability** (match architecture)
5. **Performance**
6. **Polish**

Do not gold-plate UI while sync or authority bugs exist.

---

## 32. Standing Instruction to Act as Engineering Agent

You have shell access, Gradle wrapper, and the full repo. You **must**:

- Run builds and fix compile errors yourself
- Read stack traces and relevant source files
- Implement changes, not only advise
- Verify on client and/or server when behavior changes
- Follow this document and Part 2 project rules

You **must not**:

- Ask the user to run basic compile commands you can run
- Upgrade versions silently
- Invent APIs or file paths
- Restart working architecture without explicit approval

---

# PART 2 — Quest System Checkpoint (RPG Mechanics)

**Checkpoint date:** 2026-07-18  
**Status:** Core quest loop + authoring editor are in place and compiling. Keybind system exists (`docs/KEYBINDS.md`); Controlling is NeoForge-discouraged. Live I18n for keybind/category labels. **Git remote live** — continue polish/fixes; **do not restart architecture.**  
**Git:** `main` @ https://github.com/Ankinkun/RPG-Mechanics.git — current tag `v0.1.1`

---

## Project Identity

| Field | Value |
|-------|-------|
| **Project path** | `A:\Orga\RPG modpack\The Project` |
| **Git remote** | https://github.com/Ankinkun/RPG-Mechanics.git |
| **Default branch** | `main` |
| **Mod name** | RPG Mechanics |
| **Mod ID** | `rpgmechanics` |
| **Author** | ankin |
| **Package** | `com.ankin.rpgmechanics` |
| **Minecraft** | 1.21.1 |
| **NeoForge** | 21.1.235 |
| **Java** | 21 |
| **Build** | ModDevGradle (see `build.gradle`) |
| **Version** | 0.1.1 (`gradle.properties` → `mod_version`) |
| **Metadata template** | `src/main/templates/META-INF/neoforge.mods.toml` |

Windows build:

```powershell
cd "A:\Orga\RPG modpack\The Project"
.\gradlew.bat compileJava
.\gradlew.bat runClient
```

If `createMinecraftArtifacts` fails with a locked jar, a leftover `runClient`/`runServer` Java process is holding `build/moddev/artifacts/neoforge-*.jar` — stop it, then rebuild.

---

## Locked Quest Rules (Do Not Violate)

1. **Pack-owned definitions** — JAR + datapacks. Player progress is per-player attachment only.
2. **`questAuthoringMode` defaults `false`** — editor/export overlay off in shipped pack. Dev enables in `config/rpgmechanics-server.toml`.
3. **Authoring export** — `config/rpgmechanics/quest_export/` overlays live registry when authoring is on.
4. **No player quest book item** — open journal with keybind **`J`**.
5. **`rpgmechanics:quest_editor`** — ops/devs only.
6. **Book:** LMB details, **RMB track/untrack**, one tracked quest. Hint text: `Right Click: Track` (no Track button).
7. **Vanilla-feeling UI** — restrained Minecraft widgets; opaque overlays (no see-through menus).
8. **Progress** — vanilla Criterion JSON + `SimpleCriterionTriggerMixin` → `QuestCriterionBridge`. No custom polling.
9. **Criterion JSON must use `RegistryOps`** — plain `JsonOps` strips item/block predicates to `{}` / empty arrays. Always encode/decode criteria with server/client `registryAccess()`.

---

## Current Behavior (Important)

| Topic | Behavior |
|-------|----------|
| **Accept** | `/rpgmechanics quest accept <id>` (OP) adds quest to player. Empty book copy says accept a quest to begin. |
| **Initiation via criteria** | Unaccepted quests only match **step index 0**. Later steps cannot start a quest (`QuestManager.progress(..., matchedStepIndex)` hard-guards this). |
| **Completion** | Advancing past last step → complete + toast. |
| **Track** | RMB on book list entry (list overrides right-click; vanilla lists often ignore non-LMB). |
| **Book detail** | Shows title/icon/desc + **current objective only** (no full step list / spoilers). |
| **Detection methods (editor)** | Curated only: **Biome** (`location`), **Item** (`inventory_changed`), **Place Block** (`placed_block`), **Kill Entity** (`player_killed_entity`). See `DetectionMethods.java`. |
| **Editor Save** | **Save commits current step form then saves quest** (Apply Step removed). Step IDs must be unique. Dirty-flag confirm on Back/close. |
| **Icons** | Optional `icon` on `QuestDefinition`. Pick opens **opaque modal** + `IconGridBrowser` (grid, not text list). Custom PNGs: `config/rpgmechanics/quest_icons/`. |
| **Toasts** | Advancement-styled via `ShowQuestToastPayload` + `QuestToastHelper` (started / updated / complete). |

---

## What Works

| Area | Notes |
|------|--------|
| Datapack load | `QuestReloadListener` → `data/.../rpgmechanics/quests/*.json` |
| Authoring overlay | `QuestAuthoringIO` + `RegistryOps`; re-load on `ServerStartedEvent` when authoring on |
| Welcome quest | `welcome.json` (dirt → stick) |
| Attachments | `ModAttachments.PLAYER_QUESTS` + `copyOnDeath()` |
| Sync | Login / respawn / reload / save |
| Book `J` | `QuestBookScreen` — wrapped empty text |
| HUD | Tracked quest + icon + current objective |
| Editor | List → Create/Edit; icon modal; curated detection; Save merges step commit |
| Mixin bridge | `SimpleCriterionTriggerMixin` |
| Commands | `list`, `accept`, `advance`, `reload`, `export`, `editor` |

### Commands (`/rpgmechanics quest …`)

| Subcommand | Perm | Purpose |
|------------|------|---------|
| `list` | any | List definitions |
| `accept <quest> [player]` | OP 2+ | Accept |
| `advance <quest> [player]` | OP 2+ | Force step advance |
| `reload` | OP 2+ | Reload export overlay + sync |
| `export` | OP 2+ | Print export path |
| `editor` | OP 2+ | Open editor if authoring enabled |

---

## Key File Tree

```
com/ankin/rpgmechanics/
├── RpgMechanics.java
├── client/RpgMechanicsClient.java
├── config/RpgMechanicsConfig.java
├── mixin/SimpleCriterionTriggerMixin.java
├── registry/{ModAttachments,ModCreativeTabs,ModItems}.java
└── quest/
    ├── QuestAuthoringIO.java          # MUST use RegistryOps for criterion JSON
    ├── QuestCommands.java
    ├── QuestCriterionBridge.java      # step0-only for unaccepted
    ├── QuestDefinition.java           # optional icon field
    ├── QuestEvents.java               # reload, commands, login, ServerStarted overlay reload
    ├── QuestManager.java              # progress(player, id, matchedStepIndex)
    ├── QuestPermissions.java
    ├── QuestProgressEvent.java        # INITIATED / STEP_COMPLETED / COMPLETED
    ├── QuestRegistry.java
    ├── QuestReloadListener.java       # RegistryOps when server present
    ├── QuestStep.java
    ├── QuestSync.java
    ├── PlayerQuestState.java
    ├── client/
    │   ├── ClientQuestCache.java
    │   ├── CriterionConditionTemplates.java   # 4 curated examples
    │   ├── DetectionMethods.java              # Biome/Item/Place/Kill
    │   ├── IconGridBrowser.java               # large icon grid picker
    │   ├── QuestBookScreen.java
    │   ├── QuestEditorScreen.java             # list/edit + icon modal + dirty Save
    │   ├── QuestHudOverlay.java
    │   ├── QuestIcons.java
    │   ├── QuestKeybinds.java
    │   ├── QuestToastHelper.java
    │   ├── QuestClientPayloadHandlers.java
    │   └── ScrollableDropdown.java            # opaque popup
    ├── item/QuestEditorItem.java
    └── network/
        ├── ShowQuestToastPayload.java
        ├── EditorSaveQuestPayload.java        # save(..., registryAccess())
        ├── EditorDeleteQuestPayload.java
        ├── OpenQuestEditorPayload.java
        ├── RequestOpenQuestEditorPayload.java
        ├── SyncQuestDefinitionsPayload.java
        ├── SyncPlayerQuestStatePayload.java
        ├── ToggleTrackQuestPayload.java
        ├── QuestNetwork.java
        ├── QuestClientNetworkBootstrap.java
        └── QuestServerPayloadHandler.java
```

Resources:

```
src/main/resources/
├── assets/rpgmechanics/lang/en_us.json
├── assets/rpgmechanics/models/item/quest_editor.json
├── data/rpgmechanics/rpgmechanics/quests/welcome.json
└── rpgmechanics.mixins.json
```

Dev export example (runtime, not in git necessarily):

```
run/config/rpgmechanics/quest_export/*.json
run/config/rpgmechanics/quest_icons/*.png   # optional custom icons
```

---

## Architecture (Do Not Restart)

```
Datapack JSON ──► QuestReloadListener ──► QuestRegistry
config/.../quest_export/ ──► QuestAuthoringIO (if authoring) ──┘
        │
        ▼ merged definitions
Login/reload ──► QuestSync ──► ClientQuestCache
Criterion fire ──► Mixin ──► QuestCriterionBridge ──► QuestManager
        │
        ▼ PlayerQuestState attachment + optional toast packet
```

Editor modes: **LIST** → **EDIT form** → **ICON modal** (form widgets not created while icon modal open).

---

## Hard-Won Pitfalls (Read Before Touching Save/Load)

1. **`Criterion` + `JsonOps.INSTANCE` = empty conditions** — item/block predicates become `{}` / `[]`. Use `RegistryOps.create(JsonOps.INSTANCE, registries)` for parse **and** encode (editor build, `QuestAuthoringIO`, datapack reload when server exists).
2. **Vanilla `ObjectSelectionList` may ignore RMB** — book tracking overrides `mouseClicked` on the list for button == 1.
3. **Dropdown bleed** — paint opaque popup after `super.render`; hide conditions `EditBox` while expanded (`visible = false`).
4. **Icon picker** — must be a separate widget set (modal), not layered translucent over form fields.
5. **Authoring overlay** — reloaded again on `ServerStartedEvent` with full `registryAccess()` so export folder survives world init.

---

## Manual Smoke Test

```powershell
cd "A:\Orga\RPG modpack\The Project"
.\gradlew.bat compileJava
.\gradlew.bat runClient
```

1. Enable `questAuthoringMode=true`, restart / new world, OP.
2. `/rpgmechanics quest editor` — Create quest, set detection + conditions, **Save** (no separate Apply). Confirm export JSON under `config/rpgmechanics/quest_export/` still has real `items`/`blocks` (not `{}`).
3. `/rpgmechanics quest reload` — quest appears in editor list / `/list`.
4. `/rpgmechanics quest accept <id>` — appears in book (`J`). RMB track → HUD.
5. Complete current step criteria → toast; book shows only current objective.
6. Stick alone on welcome should **not** start quest if not accepted and first step is dirt (step-0 guard).

`.\gradlew.bat runServer` — no client class load on dedicated server.

---

## Next Work Priorities (Suggested)

Continue inside existing modules:

1. **Playtest Save → export → world restart** — confirm criteria survive full restart with authoring on; fix any remaining RegistryOps edge cases.
2. **Accept UX** — empty book says accept; decide if first journal step should be an explicit “accept” step vs OP command / future NPC (user leaning “accept starts quest”).
3. **Editor layout polish** — residual spacing; step list row height; ensure Save always commits draft step before writing disk.
4. **HUD** — wrap long titles; ensure icon + text don’t collide.
5. **More detection methods later** — keep curated list small until requested; custom non-advancement detectors are future work.

---

## Config Reference

`config/rpgmechanics-server.toml`:

| Key | Default | Meaning |
|-----|---------|---------|
| `quests.questAuthoringMode` | `false` | In-game editor + export overlay |
| `quests.questDevAllowlist` | `[]` | Extra names/UUIDs when authoring on (OP 2+ always) |

---

## Copy-Paste First Message for Next Agent

```
You are continuing RPG Mechanics (NeoForge, MC 1.21.1).

Read docs/AGENT_HANDOFF.md fully — Part 1 = NeoForge standing rules + Git/version handoff (§23); Part 2 = quest checkpoint.

Git: https://github.com/Ankinkun/RPG-Mechanics.git (branch main). Version source of truth: gradle.properties mod_version.
Do not commit/push unless the user asks. On release: bump mod_version, tag vX.Y.Z, update Part 2.

Project: A:\Orga\RPG modpack\The Project
Mod: rpgmechanics / com.ankin.rpgmechanics / NeoForge 21.1.235 / Java 21 / 0.1.1

Locked: pack-owned defs; authoring default off; export config/rpgmechanics/quest_export/; J opens book; RMB track; vanilla Criterion + mixin; RegistryOps for all criterion JSON; do NOT restart quest architecture.

Also in progress: client keybind system (docs/KEYBINDS.md). Controlling is NeoForge-discouraged (UI conflict). Keybind/category labels resolve live via I18n.

Working: book (current step only), HUD, toasts, curated detection (Biome/Item/Place/Kill), icon grid modal, Save commits step+quest, unique step ids, dirty confirm, step0-only initiate for unaccepted, authoring reload on server start.

Next: playtest criterion persistence across restart; keybind menu vs Controlling; accept-flow UX. Use .\gradlew.bat compileJava / runClient. Never invent APIs.
Every pack build: bump mod_version first, then build → rpgmechanics-{version}.jar, tag vX.Y.Z.
```

---

*End of AGENT_HANDOFF.md*
