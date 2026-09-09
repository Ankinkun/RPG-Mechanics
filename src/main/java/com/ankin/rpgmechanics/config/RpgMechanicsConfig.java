package com.ankin.rpgmechanics.config;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class RpgMechanicsConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final Server SERVER;
    public static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        ModConfigSpec.Builder serverBuilder = new ModConfigSpec.Builder();
        SERVER = new Server(serverBuilder);
        SERVER_SPEC = serverBuilder.build();

        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        CLIENT = new Client(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();
    }

    private RpgMechanicsConfig() {
    }

    public static final class Server {
        public final ModConfigSpec.BooleanValue questAuthoringMode;
        public final ModConfigSpec.ConfigValue<List<? extends String>> questDevAllowlist;

        public final ModConfigSpec.BooleanValue protectionEnabled;
        public final ModConfigSpec.BooleanValue opsBypassProtection;
        public final ModConfigSpec.ConfigValue<List<? extends String>> builderAllowlist;
        public final ModConfigSpec.BooleanValue denyMessage;

        public final ModConfigSpec.BooleanValue borderEnabled;
        public final ModConfigSpec.DoubleValue borderFogDepth;
        public final ModConfigSpec.DoubleValue borderSoftMargin;
        public final ModConfigSpec.DoubleValue borderMaxDamagePerSecond;
        public final ModConfigSpec.DoubleValue borderHardKillDistance;

        public final ModConfigSpec.BooleanValue classbuildEnabled;
        public final ModConfigSpec.IntValue classbuildSpellLevel;
        public final ModConfigSpec.BooleanValue classbuildForceAdventure;

        private Server(ModConfigSpec.Builder builder) {
            builder.push("quests");
            questAuthoringMode = builder
                    .comment(
                            "When false (default), quest definitions load only from the mod JAR and datapacks.",
                            "The in-game Quest Editor cannot open or save.",
                            "When true, permitted users may export/edit quests under config/rpgmechanics/quest_export/",
                            "and that folder is overlaid onto the live registry for authoring iteration.",
                            "Keep false in the shipped modpack."
                    )
                    .define("questAuthoringMode", false);
            questDevAllowlist = builder
                    .comment(
                            "Player names or UUIDs allowed to use the Quest Editor when authoring mode is enabled.",
                            "Operators (permission level 2+) are always allowed when authoring mode is enabled."
                    )
                    .defineListAllowEmpty("questDevAllowlist", List.of(), () -> "", obj -> obj instanceof String);
            builder.pop();

            builder.push("world");
            protectionEnabled = builder
                    .comment(
                            "RPG-style terrain lock: players/mobs/explosions cannot break or place blocks.",
                            "Interactions (doors, chests, combat) still work.",
                            "Creative alone does not bypass — use builderAllowlist (and optionally opsBypassProtection)."
                    )
                    .define("protectionEnabled", true);
            opsBypassProtection = builder
                    .comment(
                            "When true, permission level 2+ (OP / singleplayer cheats) can modify blocks.",
                            "Default false so singleplayer-with-cheats still gets RPG terrain lock.",
                            "Put builder names/UUIDs in builderAllowlist instead."
                    )
                    .define("opsBypassProtection", false);
            builderAllowlist = builder
                    .comment(
                            "Player names or UUIDs allowed to modify blocks when protection is enabled."
                    )
                    .defineListAllowEmpty("builderAllowlist", List.of(), () -> "", obj -> obj instanceof String);
            denyMessage = builder
                    .comment("When true, show a rate-limited action-bar message when block modification is denied.")
                    .define("denyMessage", true);

            borderEnabled = builder
                    .comment(
                            "Custom polygonal fog border. When enabled, vanilla blue-stripe border is suppressed.",
                            "With no JSON override, uses a vanilla-mimic ~30M square with soft fog damage."
                    )
                    .define("borderEnabled", true);
            borderFogDepth = builder
                    .comment("How far past the border edge fog is visible (blocks).")
                    .defineInRange("borderFogDepth", 32.0, 1.0, 512.0);
            borderSoftMargin = builder
                    .comment("Distance past the border edge before damage starts (blocks).")
                    .defineInRange("borderSoftMargin", 8.0, 0.0, 256.0);
            borderMaxDamagePerSecond = builder
                    .comment("Maximum damage per second at full fog depth beyond softMargin.")
                    .defineInRange("borderMaxDamagePerSecond", 4.0, 0.0, 40.0);
            borderHardKillDistance = builder
                    .comment(
                            "Distance past the edge that applies lethal pressure (0 = disabled).",
                            "Players can still walk into fog; this is a safety net for extreme teleport distances."
                    )
                    .defineInRange("borderHardKillDistance", 0.0, 0.0, 1_000_000.0);
            builder.pop();

            builder.push("classbuild");
            classbuildEnabled = builder
                    .comment("Enable class select gate, custom equipment inventory, and managed Iron Spells loadouts.")
                    .define("enabled", true);
            classbuildSpellLevel = builder
                    .comment("Spell level written into the managed 4-slot spellbook.")
                    .defineInRange("spellLevel", 3, 1, 10);
            classbuildForceAdventure = builder
                    .comment("Set Adventure mode when a class build is confirmed.")
                    .define("forceAdventure", true);
            builder.pop();
        }
    }

    public static final class Client {
        public final ModConfigSpec.BooleanValue keybindAuthoringMode;
        public final ModConfigSpec.BooleanValue borderShaderFallbackWall;

        private Client(ModConfigSpec.Builder builder) {
            builder.push("keybinds");
            keybindAuthoringMode = builder
                    .comment(
                            "When false (default), the Controls keybinds screen only allows primary/secondary rebind,",
                            "trigger mode cycling, and reset. Category/hide taxonomy editing is hidden.",
                            "When true, authoring tools are shown (New Category, Hide/Show, cycle category).",
                            "Keep false in the shipped modpack."
                    )
                    .define("keybindAuthoringMode", false);
            builder.pop();

            builder.push("world");
            borderShaderFallbackWall = builder
                    .comment(
                            "Optional soft forcefield wall when Iris/Oculus has a shader pack active.",
                            "Default false — prefer distance-driven fogEnd (Photon patch in extras/shader-patches/photon).",
                            "Manual /rpgmechanics world border debugwall still works either way."
                    )
                    .define("borderShaderFallbackWall", false);
            builder.pop();
        }
    }
}
