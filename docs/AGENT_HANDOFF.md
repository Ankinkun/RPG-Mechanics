# Agent Handoff — RPG Mechanics (NeoForge 1.21.1)

**Read this file fully before editing.** It is the standing contract between agents.

| Part | Contents |
|------|----------|
| **Part 0** | Day-one onboarding (new agent checklist) |
| **Part 1** | NeoForge standing skill set & engineering rules |
| **Part 2** | Project checkpoint — what exists, locks, next work |

Related docs: [`KEYBINDS.md`](KEYBINDS.md) · GitHub: https://github.com/Ankinkun/RPG-Mechanics

---

# PART 0 — Day-One Onboarding (New Agent)

Do this before writing code:

1. **Open the project** at `A:\Orga\RPG modpack\The Project` (Windows / PowerShell).
2. **Read Part 1 + Part 2** of this file (and `docs/KEYBINDS.md` if touching controls).
3. **Git**
   ```powershell
   git remote -v
   git status
   git log -5 --oneline
   git tag -l "v0.1.*"
   ```
   Remote must be `https://github.com/Ankinkun/RPG-Mechanics.git`, branch `main`.
4. **Versions** — confirm in `gradle.properties`:
   - `minecraft_version=1.21.1`
   - `neo_version=21.1.250`
   - `mod_version=0.1.7` (bump before every pack jar — see §23)
5. **Compile**
   ```powershell
   .\gradlew.bat compileJava
   ```
6. **Map the code** — feature domains:
   - `quest/` — pack-owned quests, book, editor, criterion bridge
   - `keybind/` — client keybind profile UI + input engine
   - `world/` — RPG terrain protection + polygonal fog border
   - `classbuild/` — character roster, Destiny gear bag, ISS + EF loadout
7. **Do not restart architecture.** Extend existing modules; ask before large redesigns.
8. **Do not commit/push** unless the user asks. Every user-facing build → new `mod_version` + jar `rpgmechanics-{version}.jar`.

Copy-paste kickoff for chat is at the **end of Part 2**.

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
| NeoForge | Match `gradle.properties` → `neo_version` (currently **21.1.250**) |
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

# PART 2 — Project Checkpoint (RPG Mechanics)

**Checkpoint date:** 2026-09-10  
**Mod version:** `0.1.7` (tag `v0.1.7`) — Destiny hub, stowed inventory, taxonomy v4, Map/EF/Quit polish  
**Status:** Quests + keybinds + world + **classbuild** (3 character slots, Darkness DD, ISS spellbook, LoL combat HUD, stowed bag, Map tab). Controlling is NeoForge-**discouraged**. **Do not restart architecture.**  
**Git:** `main` @ https://github.com/Ankinkun/RPG-Mechanics.git

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
| **NeoForge** | 21.1.250 (`gradle.properties` → `neo_version`) |
| **Java** | 21 |
| **Build** | ModDevGradle (`build.gradle`) |
| **Version** | `0.1.7` (`gradle.properties` → `mod_version`) |
| **Metadata** | `src/main/templates/META-INF/neoforge.mods.toml` |
| **Mixins** | `src/main/resources/rpgmechanics.mixins.json` |

```powershell
cd "A:\Orga\RPG modpack\The Project"
.\gradlew.bat compileJava
.\gradlew.bat build          # → build/libs/rpgmechanics-{mod_version}.jar
.\gradlew.bat runClient
```

If `createMinecraftArtifacts` fails with a locked jar, a leftover `runClient`/`runServer` Java process is holding `build/moddev/artifacts/neoforge-*.jar` — kill it, then rebuild.

**Modpack note:** Arcadias Legacy (and similar) should use NeoForge **≥ 21.1.250** to match this mod. Older NeoForge fails dependency resolution against this jar. Pack-side issues with other mods on newer NeoForge lines are **not** RPG Mechanics bugs.

---

## What Has Been Built (Summary)

### A. Quest system (server + client)

Pack-owned quest definitions (JAR datapack + optional authoring export). Per-player progress via attachment. Vanilla advancement **Criterion** JSON drives progress through a mixin bridge. In-game quest book (`J`), HUD for tracked quest, advancement-style toasts, OP/dev editor when authoring is on.

### B. Keybind system (client-only)

Replaces vanilla Key Binds screen with `RpgKeybindsScreen`. Profile JSON under `config/rpgmechanics/keybinds/`. Primary + secondary chords, trigger modes (Press / Hold 500ms / Double Tap / Release), Escape = unbind slot, authoring taxonomy (categories / hide) gated by client config. Labels from live `I18n` (mod lang files). See `docs/KEYBINDS.md`.

### C. World system (server + client fog)

RPG terrain protection (break/place/grief cancelled; OP/allowlist builders). Soft polygonal fog border with vanilla-mimic ±30M default; custom JSON under `config/rpgmechanics/world/borders/`.

### D. Class / buildcrafting (server + client + ISS)

Destiny-style **3 character slots** (kit + equipped gear + **stowed bag** per slot). **Title-screen** character select → loads campaign world (`CLIENT classbuild.campaignWorldName`, empty = first save). Create = role/kit (v1 Darkness DD only; Tank/Support Coming Soon) + starter leather / stone sword / shield. Active character → Adventure + managed Curios spellbook (4 locked spells) + Epic Fight roll+guard + forced combat mode. **Inventory hub** (Overview doll / Gear Destiny bag / Class / Quests / **Map** (Xaero overlay) / Skills + Settings cog + Quit → character select); Esc → Overview (world does **not** pause); inventory key **TAB**. Pickups go straight to stowed (never hotbar). Soft `compileOnly` on Iron Spells + Curios + Epic Fight + Xaero World Map (+ `libs/xaerolib` for compile).

**TODO campaign:** later — per-character worlds / shared campaign state; new character → new world. Dev now: all characters load the same configured campaign save.

### E. Version control

GitHub repo live. Agents bump `mod_version` for every pack jar, tag `vX.Y.Z`, update this Part 2. Rules in Part 1 §23.

---

## Locked Rules (Do Not Violate)

### Quests

1. **Pack-owned definitions** — JAR + datapacks. Player progress is per-player attachment only.
2. **`questAuthoringMode` defaults `false`** — editor/export off in shipped pack (`config/rpgmechanics-server.toml`).
3. **Authoring export** — `config/rpgmechanics/quest_export/` overlays live registry when authoring is on.
4. **No player quest book item** — journal keybind **`J`**.
5. **`rpgmechanics:quest_editor`** — ops/devs only.
6. **Book:** LMB details, **RMB track/untrack**, one tracked quest. Hint: `Right Click: Track` (no Track button).
7. **Vanilla-feeling UI** — restrained widgets; opaque overlays.
8. **Progress** — vanilla Criterion JSON + `SimpleCriterionTriggerMixin` → `QuestCriterionBridge`. No custom polling loops.
9. **Criterion JSON must use `RegistryOps`** — plain `JsonOps` strips item/block predicates to `{}` / `[]`. Always encode/decode with `registryAccess()`.
10. **`repeatable`** on `QuestDefinition` (default `false` = permanent). Dismiss completed: permanent keeps history; repeatable can restart. See `PlayerQuestState` / book dismiss payload.

### Keybinds

1. **Authoring UI** only when `keybinds.keybindAuthoringMode=true` in **client** config — **not** auto-enabled in `runClient` / non-production.
2. **Controlling** is `discouraged` in `neoforge.mods.toml` — launch warning; remove Controlling for our menu to win cleanly (`NewKeyBindsScreen` also listens to `ScreenEvent.Opening`).
3. **Escape while binding** unbinds that slot (empty chords) — does **not** restore default. Reset restores `defaultKey`.
4. **Display names** — resolve live via `KeybindCatalog` / `I18n`; do not trust baked JSON `title` alone (early seed often wrote raw ids).
5. Seeded `pack_defaults.json` lists every `KeyMapping`; **bundled taxonomy** (`assets/rpgmechanics/keybinds/pack_taxonomy.json`, **v4** via `config/.../taxonomy.version`) rewrites categories to **Movement / Combat / Inventory** allowlist and **force-disables** the rest (`visible=false` + `enabled=false` + empty chords; ISS + hotbar prefixes). Defaults: inventory **TAB**, Xaero map **M**, EF skill GUI **K**. Profile entries with `enabled=true` are **managed** by `KeybindInputEngine` except `key.rpgmechanics.*` (vanilla consumeClick). Be careful changing `BindingOverride.isManaged()`.

### Process

1. **No silent MC/Neo/Java bumps.**
2. **Never invent APIs.**
3. **Every pack build → new `mod_version`.**
4. **Commit/push only when user asks.**

---

## Quest System Detail

### Current behavior

| Topic | Behavior |
|-------|----------|
| **Accept** | `/rpgmechanics quest accept <id>` (OP). Empty book says accept to begin. |
| **Initiation via criteria** | Unaccepted quests only match **step index 0**. |
| **Completion** | Past last step → complete + toast. |
| **Dismiss completed** | Removes from book UI; permanent keeps completion history; repeatable can re-accept. |
| **Track** | RMB on book list (list overrides right-click). |
| **Book detail** | Current objective only (no spoiler step list). |
| **Detection (editor)** | Curated: Biome / Item / Place Block / Kill Entity (`DetectionMethods`). |
| **Editor Save** | Commits current step form then saves quest. Unique step ids. Dirty confirm on Back/close. |
| **Icons** | Optional on definition; opaque modal + `IconGridBrowser`. Custom PNGs: `config/rpgmechanics/quest_icons/`. |
| **Toasts** | `ShowQuestToastPayload` + `QuestToastHelper`. |

### What works

| Area | Notes |
|------|--------|
| Datapack load | `QuestReloadListener` → `data/.../rpgmechanics/quests/*.json` |
| Authoring overlay | `QuestAuthoringIO` + `RegistryOps`; reload on `ServerStartedEvent` when authoring on |
| Welcome quest | `welcome.json` (dirt → stick) |
| Attachments | `ModAttachments.PLAYER_QUESTS` + `copyOnDeath()` |
| Sync | Login / respawn / reload / save |
| Book / HUD / Editor / Mixin / Commands | All in place |

### Commands (`/rpgmechanics quest …`)

| Subcommand | Perm | Purpose |
|------------|------|---------|
| `list` | any | List definitions |
| `accept <quest> [player]` | OP 2+ | Accept |
| `advance <quest> [player]` | OP 2+ | Force step advance |
| `reload` | OP 2+ | Reload export overlay + sync |
| `export` | OP 2+ | Print export path |
| `editor` | OP 2+ | Open editor if authoring enabled |

### Quest architecture (do not restart)

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

### Quest hard-won pitfalls

1. **`Criterion` + `JsonOps.INSTANCE` = empty conditions** — use `RegistryOps`.
2. **Vanilla lists may ignore RMB** — book overrides `mouseClicked` for button == 1.
3. **Dropdown bleed** — opaque popup after `super.render`; hide `EditBox` while expanded.
4. **Icon picker** — separate modal widget set.
5. **Authoring overlay** — reload on `ServerStartedEvent` with full `registryAccess()`.
6. **Missing quest defs** — purge from player state on reload/login so deleted quests do not linger.

---

## Keybind System Detail

Full player-facing doc: **`docs/KEYBINDS.md`**.

### Layout

`Primary | Secondary | Trigger | Reset` (~50% name / ~50% buttons).

| Control | Behavior |
|---------|----------|
| Primary / Secondary | Click to listen; Escape clears slot entirely |
| Trigger | Press → Hold (500 ms) → Double Tap → Release |
| Reset / Reset All | Restore pack/vanilla defaults |
| Authoring | New Category, Reload, Hide/Show, `…` cycle category |

### Config

| File | Key | Default |
|------|-----|---------|
| `rpgmechanics-client.toml` | `keybinds.keybindAuthoringMode` | `false` |

### Persistence

```
config/rpgmechanics/keybinds/
  pack_defaults.json   # seeded with all KeyMappings + categories
  player.json          # player overlay (wins on merge)
```

### Key packages / classes

```
keybind/
├── KeybindCatalog.java          # all KeyMappings + I18n display helpers
├── KeybindManager.java          # profile apply / rebind / bootstrap
├── KeybindProfileIO.java        # load/save/seed/merge
├── KeybindInputEngine.java      # claim keys + synthesize clicks/downs
├── KeybindPermissions.java      # authoring gate (config only)
├── KeybindClientEvents.java     # tick bootstrap + ScreenEvent → RpgKeybindsScreen
├── BindingOverride / CategoryDef / KeyChord / KeyTriggerMode / KeybindProfile
└── client/RpgKeybindsScreen.java
mixin/client/
├── KeyMappingMixin.java         # intercept set/click/setAll/releaseAll
├── KeyMappingExtensionMixin.java # isActiveAndMatches → profile chords
└── KeyMappingAccessor.java      # clickCount
```

### Keybind pitfalls

1. **Controlling** also replaces Key Binds via `ScreenEvent.Opening` — remove it or expect UI fights (discouraged warning on launch).
2. **Early seed** baked raw lang ids into `title` — UI must use `KeybindCatalog.displayCategoryString` / `displayNameString` (live `I18n`).
3. **Managing all seeded bindings** — `isManaged()` ≈ `enabled`; empty chords = unbound but owned. Changing this affects whether vanilla `KeyMapping` path works.
4. **Hold** needs `ClientTickEvent` (`KeybindInputEngine.tick`).
5. **Modifiers** — `KeyModifier.NONE` must still fire while sneak/sprint held (`modifiersMatch`).
6. **GUI match vs unbound keys** — managed bindings set vanilla `KeyMapping.key` to `UNKNOWN` so the engine can claim physical keys. Inventory hotbar 1–9 / drop / pick / close use `isActiveAndMatches` (and some paths use `matches` / `matchesMouse`), which compare the bound key — **not** `consumeClick`. Resolve via `KeybindInputEngine.matchesManaged*` + mixins on `IKeyMappingExtension` / `KeyMapping`; do not re-bind into `MAP` or in-game synthesis double-fires.
7. **Mouse hold across GUIs** — opening a container with right-click (`key.use`) runs `KeyMapping.releaseAll()`, then the GUI eats the mouse-up so `KeyMapping.set(mouse, false)` never fires. Do **not** restore mouse mappings in `setAll` (vanilla only resyncs KEYSYM). Drop mouse `physicalDown` / press-holds in `onReleaseAll` or RMB stays down and `startUseItem` repeats every 4 ticks.

---

## Top-Level File Tree

```
com/ankin/rpgmechanics/
├── RpgMechanics.java                 # thin @Mod
├── client/RpgMechanicsClient.java
├── config/RpgMechanicsConfig.java    # SERVER quests+world + CLIENT keybinds
├── keybind/                          # see above
├── world/                            # protection + fog border
├── classbuild/                       # roster, Destiny gear, ISS + EF loadout
├── mixin/
│   ├── SimpleCriterionTriggerMixin.java
│   └── client/KeyMapping{Mixin,Accessor,ExtensionMixin}.java
├── registry/{ModAttachments,ModCreativeTabs,ModItems}.java
└── quest/                            # see Key File Tree historically; full tree in repo
```

### World module (v1)

| Area | Behavior |
|------|----------|
| **Protection** | SERVER `world.protectionEnabled` (default true). Cancels break/place/trample/tool-modify/fluid-place/piston/mob-grief; explosions clear block list. Bypass: `builderAllowlist` only by default (`opsBypassProtection=false` so singleplayer cheats do not unlock building). |
| **Border** | SERVER `world.borderEnabled` (default true). Soft polygonal fog; unconfigured → vanilla-mimic square (~±29,999,984) — **no fog near spawn**. Use `setbox` or JSON for a playable border. Vanilla border size pushed to max + zero damage. |
| **Commands** | `/rpgmechanics world protection status`; `/rpgmechanics world border reload\|info\|setbox\|addvertex\|…` (OP 2+) |
| **Client** | Soft RD-anchored fog (`BorderFogRenderer`) from distance-to-edge → `fogStart`/`fogEnd`. Photon chunk-edge fog follows via `extras/shader-patches/photon`. Optional forcefield wall off by default (`CLIENT world.borderShaderFallbackWall`). Draft preview for OP. Debug wall: `/rpgmechanics world border debugwall`. |
| **Authoring** | Border Wand item + `addvertex`/`undo`/`clear`/`save`. Draft syncs to clients. Save rejects self-intersecting / zero-area polygons. |

```
world/
├── WorldEvents.java
├── protection/{WorldProtection,WorldProtectionEvents}
└── border/
    ├── BorderPolygon / WorldBorderDefinition / WorldBorderIO / WorldBorderState
    ├── WorldBorderEngine / WorldBorderCommands
    ├── network/{WorldNetwork,SyncWorldBorderPayload,WorldBorderSync,WorldClientNetworkBootstrap}
    └── client/{ClientWorldBorderCache,BorderFogRenderer,BorderDraftRenderer,BorderDebugWallRenderer,BorderWallPainter,BorderShaderFallbackRenderer,ShaderPackCompat}
```

Resources:

```
src/main/resources/
├── assets/rpgmechanics/lang/en_us.json
├── assets/rpgmechanics/models/item/quest_editor.json
├── data/rpgmechanics/rpgmechanics/quests/welcome.json
└── rpgmechanics.mixins.json

src/main/templates/META-INF/neoforge.mods.toml   # Controlling = discouraged
```

---

## Config Reference

**Server** `config/rpgmechanics-server.toml`:

| Key | Default | Meaning |
|-----|---------|---------|
| `quests.questAuthoringMode` | `false` | In-game editor + export overlay |
| `quests.questDevAllowlist` | `[]` | Extra names/UUIDs when authoring on (OP 2+ always) |
| `world.protectionEnabled` | `true` | RPG terrain lock |
| `world.opsBypassProtection` | `false` | When true, OP/cheats can build (off by default for RPG/SP) |
| `world.builderAllowlist` | `[]` | Block-edit bypass names/UUIDs |
| `world.denyMessage` | `true` | Action-bar deny feedback |
| `world.borderEnabled` | `true` | Fog border system |
| `world.borderFogDepth` | `32` | Fog visibility depth past edge |
| `world.borderSoftMargin` | `8` | Distance before damage |
| `world.borderMaxDamagePerSecond` | `4` | Damage ramp cap |
| `world.borderHardKillDistance` | `0` | Optional lethal distance (0=off) |
| CLIENT `world.borderShaderFallbackWall` | `false` | Optional forcefield wall with shaders (fog preferred) |
| CLIENT `classbuild.combatHud` | `true` | LoL HUD; hides vanilla + ISS mana/spell chrome when character active |
| CLIENT `classbuild.campaignWorldName` | `""` | Save folder after title select; empty = first save |
| `classbuild.enabled` | `true` | Character select + equipment + managed spellbooks |
| `classbuild.spellLevel` | `3` | Level written into managed book slots |
| `classbuild.forceAdventure` | `true` | Adventure mode on confirm |

**Client** `config/rpgmechanics-client.toml`:

| Key | Default | Meaning |
|-----|---------|---------|
| `keybinds.keybindAuthoringMode` | `false` | Category / hide taxonomy tools |
| `classbuild.combatHud` | `true` | Combat HUD + hide vanilla/ISS chrome |
| `classbuild.campaignWorldName` | `""` | Campaign SP world folder name |

---

## Class build (v1) locks

1. Do not restart — extend `classbuild/`.
2. Roster attachment (`CharacterRoster`, 3 slots) is source of truth in-world; title UI uses `config/rpgmechanics/characters/roster.json` until join; active slot owns kit + equipped + **stowed**; **world pickups / `Inventory.add` go straight to stowed** (vanilla hotbar 1–8 + storage unused; weapon stays hotbar 0). No auto-equip on pickup (Destiny-style manual equip; optional auto-equip / force-always-equipped later via settings).
3. Title-screen character select loads campaign world; login still clears active then client re-applies pending title selection (suppresses in-world select flash).
4. Inventory hub: Esc → Overview (unpaused); top bar tabs + Settings cog + **Quit → disconnect → Character Select** (not `Minecraft.stop()`).
5. Tank/Support + other element tracks + passives are out of scope until designed.
6. Iron Spells / Curios via `IronSpellsSoft` / `IronSpellsClientSoft`; Epic Fight via `EpicFightSoft` / `EpicFightClientSoft` reflection (no hard crash if jars missing).
7. ISS `addSpellAtIndex(spell, level, index, locked)` — level before index.
8. OP helpers: `/rpgmechanics class reset|select|apply|delete|info`
9. Gear: LMB equip from stowed, RMB unequip to stowed; Overview shows 6-slot doll; bag = stowed ∪ equipped (outlined).
10. **TODO campaign:** per-character saves / new-character world — not implemented yet.

---

## Manual Smoke Tests

### Class build

1. Boot → Character Select (title). Create/Select → loads campaign world (`campaignWorldName` or first save).
2. Create empty slot → DD kit → Confirm → Adventure + Curios spellbook with **4** spells + leather/sword/shield; EF roll+guard when Epic Fight present.
3. **TAB** → Gear Destiny bag (doll + owned grid); Esc → Overview doll (world unpaused); cog → Options; **Quit → world unloads → Character Select**.
4. Class / Quests / **Map** (Xaero + hub bar overlay) / Skills tabs; J opens Quests; **M** opens Map.
5. Combat HUD: HP / QERF abilities with CDs / mana; vanilla hotbar + ISS mana/spell bar hidden; **no hotbar scroll** (selected pinned to 0); creative keeps vanilla inventory.
6. Controls shows **Movement / Combat / Inventory** only (taxonomy **v4**; unused binds unbound).
7. Gear LMB/RMB equip rules; ground pickups → stowed (not hotbar); Inscription Table / Curios unequip cannot change loadout.
8. Without ISS/EF/Xaero jars: game still boots; class UI works; book/HUD mana / Skills / Map no-op with hint.

### Quests

```powershell
.\gradlew.bat compileJava
.\gradlew.bat runClient
```

1. `questAuthoringMode=true`, OP, `/rpgmechanics quest editor` — Create, Save; export JSON must keep real `items`/`blocks`.
2. `/rpgmechanics quest reload` → accept → book `J` → RMB track → HUD.
3. Complete step → toast; book shows current objective only.
4. Stick alone must **not** start welcome if step 0 is dirt (step-0 guard).
5. `.\gradlew.bat runServer` — no client class load.

### Keybinds

1. Without Controlling: Options → Controls → Key Binds → RPG Mechanics screen.
2. Rebind primary; Escape → `---`; Reset restores default.
3. Labels readable (not raw `key.categories.*` when lang exists).
4. With `keybindAuthoringMode=true`: Hide/Show + New Category appear; with `false`: they do not (even in `runClient`).

---

## Next Work Priorities (Suggested)

1. **Keybind UI vs Controlling** — pack should remove Controlling, or raise our `ScreenEvent` priority / replace `NewKeyBindsScreen` by class name.
2. **Quest Save → export → world restart** — RegistryOps persistence playtest.
3. **Accept UX** — explicit accept in book vs OP-only today.
4. **Editor/HUD polish** — spacing, long title wrap.
5. **World-space depth fog** — `BorderWorldFogRenderer` disabled; revisit only with solid unprojection.

---

## Copy-Paste First Message for Next Agent

```
You are continuing RPG Mechanics (NeoForge, MC 1.21.1).

Read docs/AGENT_HANDOFF.md fully:
- Part 0 = day-one checklist
- Part 1 = NeoForge standing skill set + Git/version (§23)
- Part 2 = project checkpoint (quests + keybinds + world)
Also read docs/KEYBINDS.md if touching controls.

Git: https://github.com/Ankinkun/RPG-Mechanics.git (branch main).
Version source of truth: gradle.properties mod_version (currently 0.1.7 / tag v0.1.7).
Every pack jar: bump mod_version → build → rpgmechanics-{version}.jar → tag vX.Y.Z.
Do not commit/push unless the user asks. Never invent APIs. Do not restart architecture.

Project: A:\Orga\RPG modpack\The Project
Mod: rpgmechanics / com.ankin.rpgmechanics / NeoForge 21.1.250 / Java 21

Quests (locked): pack-owned defs; authoring default off; export config/rpgmechanics/quest_export/;
J opens book; RMB track; Criterion + mixin; RegistryOps for criteria; step0-only initiate;
repeatable flag; dismiss completed; do NOT restart quest architecture.

Keybinds (client): RpgKeybindsScreen; pack_defaults+player JSON; primary/secondary/trigger/reset;
Escape unbinds; authoring only if keybindAuthoringMode=true; live I18n labels;
Controlling is NeoForge-discouraged — remove it for clean menu. See docs/KEYBINDS.md.
Inventory hotbar 1–9 + RMB container open fixed in 0.1.3.

World (v1+): protection + RD-anchored fog border; Photon fogEnd patch in extras/shader-patches/photon.
CLIENT world.borderShaderFallbackWall default false.

Classbuild (v1+): title Character Select → campaign world; 3 slots; Darkness DD kit;
managed 4-slot ISS spellbook; Destiny inventory hub (Esc=Overview, unpaused, TAB inventory);
Map tab overlays Xaero; LoL combat HUD; key taxonomy v4; stowed pickups + Quit→select;
EF combat mode forced; TODO campaign per-char worlds.
Extend classbuild/ — do not restart.

Working: quests; keybinds taxonomy v4; world; classbuild hub+stowed+map (0.1.7).

Next (pick with user): playtest; settings overhaul (auto-equip / force-equipped); Tank/Support later.
Use .\gradlew.bat compileJava / build / runClient.
```

---

*End of AGENT_HANDOFF.md*
