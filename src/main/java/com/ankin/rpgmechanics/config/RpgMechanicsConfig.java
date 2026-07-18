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
        }
    }

    public static final class Client {
        public final ModConfigSpec.BooleanValue keybindAuthoringMode;

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
        }
    }
}
