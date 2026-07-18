package com.ankin.rpgmechanics.quest;

import com.ankin.rpgmechanics.quest.network.OpenQuestEditorPayload;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class QuestCommands {
    private static final SuggestionProvider<CommandSourceStack> QUEST_IDS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    QuestRegistry.all().keySet().stream().map(ResourceLocation::toString),
                    builder
            );

    private QuestCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("rpgmechanics")
                .then(Commands.literal("quest")
                        .then(Commands.literal("accept")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("quest", ResourceLocationArgument.id())
                                        .suggests(QUEST_IDS)
                                        .executes(ctx -> accept(ctx, ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> accept(ctx, EntityArgument.getPlayer(ctx, "player"))))))
                        .then(Commands.literal("advance")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("quest", ResourceLocationArgument.id())
                                        .suggests(QUEST_IDS)
                                        .executes(ctx -> advance(ctx, ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> advance(ctx, EntityArgument.getPlayer(ctx, "player"))))))
                        .then(Commands.literal("reload")
                                .requires(source -> source.hasPermission(2))
                                .executes(QuestCommands::reload))
                        .then(Commands.literal("export")
                                .requires(source -> source.hasPermission(2))
                                .executes(QuestCommands::exportHint))
                        .then(Commands.literal("editor")
                                .requires(source -> source.hasPermission(2))
                                .executes(QuestCommands::openEditor))
                        .then(Commands.literal("list")
                                .executes(QuestCommands::list)));
    }

    private static int accept(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        ResourceLocation id = ResourceLocationArgument.getId(context, "quest");
        boolean ok = QuestManager.accept(player, id);
        context.getSource().sendSuccess(() -> Component.literal(ok ? "Accepted " + id : "Could not accept " + id), true);
        return ok ? 1 : 0;
    }

    private static int advance(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        ResourceLocation id = ResourceLocationArgument.getId(context, "quest");
        boolean ok = QuestManager.advance(player, id);
        context.getSource().sendSuccess(() -> Component.literal(ok ? "Advanced " + id : "Could not advance " + id), true);
        return ok ? 1 : 0;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        if (QuestPermissions.isAuthoringEnabled()) {
            QuestAuthoringIO.reloadOverlayFromDisk(context.getSource().getServer().registryAccess());
        } else {
            QuestRegistry.clearAuthoringOverlay();
        }
        QuestManager.purgeMissingDefinitionsForAll(context.getSource().getServer());
        QuestSync.syncDefinitionsToAll(context.getSource().getServer());
        context.getSource().sendSuccess(
                () -> Component.literal("Reloaded authoring overlay / synced " + QuestRegistry.all().size() + " quest(s). Use /reload for datapacks."),
                true
        );
        return 1;
    }

    private static int exportHint(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(
                () -> Component.literal("Authoring export folder: " + QuestAuthoringIO.exportDirectory()),
                false
        );
        return 1;
    }

    private static int openEditor(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!QuestPermissions.canUseEditor(player)) {
            context.getSource().sendFailure(Component.translatable("message.rpgmechanics.quest.authoring_denied"));
            return 0;
        }
        PacketDistributor.sendToPlayer(player, new OpenQuestEditorPayload());
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        if (QuestRegistry.all().isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No quests loaded."), false);
            return 0;
        }
        QuestRegistry.values().forEach(quest ->
                context.getSource().sendSuccess(
                        () -> Component.literal("- " + quest.id() + " | " + quest.title() + " (" + quest.steps().size() + " steps)"),
                        false
                )
        );
        return QuestRegistry.all().size();
    }
}
