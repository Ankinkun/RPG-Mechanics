package com.ankin.rpgmechanics.world.border;

import com.ankin.rpgmechanics.world.border.network.ToggleBorderDebugWallPayload;
import com.ankin.rpgmechanics.world.border.network.WorldBorderSync;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.network.PacketDistributor;

public final class WorldBorderCommands {
    private WorldBorderCommands() {
    }

    /**
     * {@code border} literal with standard authoring commands (attach under {@code world}).
     */
    public static LiteralArgumentBuilder<CommandSourceStack> borderSubtree() {
        return Commands.literal("border")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(WorldBorderCommands::reload))
                .then(Commands.literal("info")
                        .executes(WorldBorderCommands::info))
                .then(Commands.literal("addvertex")
                        .executes(WorldBorderCommands::addVertexHere)
                        .then(Commands.argument("pos", Vec2Argument.vec2())
                                .executes(WorldBorderCommands::addVertexAt)))
                .then(Commands.literal("undo")
                        .executes(WorldBorderCommands::undo))
                .then(Commands.literal("clear")
                        .executes(WorldBorderCommands::clearDraft))
                .then(Commands.literal("save")
                        .executes(WorldBorderCommands::save))
                .then(Commands.literal("reset")
                        .executes(WorldBorderCommands::reset))
                .then(Commands.literal("enable")
                        .executes(ctx -> setEnabled(ctx, true)))
                .then(Commands.literal("disable")
                        .executes(ctx -> setEnabled(ctx, false)))
                .then(Commands.literal("debugwall")
                        .executes(WorldBorderCommands::debugWallHint));
    }

    private static int debugWallHint(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            PacketDistributor.sendToPlayer(player, new ToggleBorderDebugWallPayload());
            return 1;
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal("debugwall requires a player"));
            return 0;
        }
    }

    private static ResourceLocation dimensionId(CommandSourceStack source) {
        return source.getLevel().dimension().location();
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        WorldBorderState.reload(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.translatable("commands.rpgmechanics.world_border_reloaded"), true);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        ResourceLocation id = dimensionId(context.getSource());
        WorldBorderDefinition definition = WorldBorderState.getOrDefault(id);
        BorderPolygon draft = WorldBorderState.draft(id).orElse(null);
        String mode = definition.custom() ? "custom" : "vanilla-mimic (~30M — no fog near spawn)";
        int verts = definition.polygon().vertices().size();
        int draftVerts = draft == null ? 0 : draft.vertices().size();
        double outside = 0.0;
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            outside = definition.polygon().distanceOutside(player.getX(), player.getZ());
        } catch (Exception ignored) {
            // console
        }
        double finalOutside = outside;
        context.getSource().sendSuccess(
                () -> Component.literal(id + " [" + mode + "] enabled=" + definition.enabled()
                        + " vertices=" + verts + " draft=" + draftVerts
                        + " fogDepth=" + definition.fogDepth()
                        + " softMargin=" + definition.softMargin()
                        + " distOutside=" + String.format("%.1f", finalOutside)),
                false
        );
        if (!definition.custom()) {
            context.getSource().sendSuccess(
                    () -> Component.literal("Tip: /rpgmechanics world border setbox 64  — small test fog border around you"),
                    false
            );
        }
        if (draft != null && draft.vertices().size() >= 3) {
            draft.validationMessage().ifPresent(msg ->
                    context.getSource().sendSuccess(() -> Component.literal("Draft WARNING: " + msg), false)
            );
        }
        return 1;
    }

    private static int addVertexHere(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            Component message = WorldBorderAuthoring.addVertex(player, player.getX(), player.getZ());
            context.getSource().sendSuccess(() -> message, true);
            return 1;
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }
    }

    private static int addVertexAt(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            Vec2 pos = Vec2Argument.getVec2(context, "pos");
            Component message = WorldBorderAuthoring.addVertex(player, pos.x, pos.y);
            context.getSource().sendSuccess(() -> message, true);
            return 1;
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }
    }

    private static int undo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            Component message = WorldBorderAuthoring.undo(player);
            context.getSource().sendSuccess(() -> message, true);
            return 1;
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal("undo requires a player"));
            return 0;
        }
    }

    private static int clearDraft(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            Component message = WorldBorderAuthoring.clear(player);
            context.getSource().sendSuccess(() -> message, true);
            return 1;
        } catch (Exception exception) {
            ResourceLocation id = dimensionId(context.getSource());
            WorldBorderState.clearDraft(id);
            WorldBorderSync.syncDraft(context.getSource().getServer(), id);
            context.getSource().sendSuccess(() -> Component.literal("Cleared draft for " + id), true);
            return 1;
        }
    }

    private static int save(CommandContext<CommandSourceStack> context) {
        ResourceLocation id = dimensionId(context.getSource());
        BorderPolygon draft = WorldBorderState.draft(id).orElse(null);
        if (draft == null || !draft.isValid()) {
            context.getSource().sendFailure(Component.literal("Draft needs at least 3 vertices before save."));
            return 0;
        }
        var problem = draft.validationMessage();
        if (problem.isPresent()) {
            context.getSource().sendFailure(Component.literal("Cannot save: " + problem.get()));
            return 0;
        }
        WorldBorderDefinition current = WorldBorderState.getOrDefault(id);
        WorldBorderDefinition next = new WorldBorderDefinition(
                id,
                true,
                true,
                draft,
                current.fogDepth(),
                current.softMargin(),
                current.maxDamagePerSecond()
        );
        try {
            WorldBorderIO.save(next);
            WorldBorderState.put(next);
            WorldBorderState.clearDraft(id);
            WorldBorderSync.syncDefinition(context.getSource().getServer(), next);
            WorldBorderSync.syncDraft(context.getSource().getServer(), id);
            context.getSource().sendSuccess(() -> Component.literal("Saved custom border for " + id), true);
            return 1;
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal("Save failed: " + exception.getMessage()));
            return 0;
        }
    }

    private static int reset(CommandContext<CommandSourceStack> context) {
        ResourceLocation id = dimensionId(context.getSource());
        try {
            WorldBorderState.resetToVanillaMimic(id);
            ServerLevel level = context.getSource().getLevel();
            WorldBorderState.suppressVanillaBorder(level);
            WorldBorderSync.syncDefinition(context.getSource().getServer(), WorldBorderState.getOrDefault(id));
            WorldBorderSync.syncDraft(context.getSource().getServer(), id);
            context.getSource().sendSuccess(() -> Component.literal("Reset " + id + " to vanilla-mimic fog border"), true);
            return 1;
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal("Reset failed: " + exception.getMessage()));
            return 0;
        }
    }

    private static int setEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
        ResourceLocation id = dimensionId(context.getSource());
        WorldBorderDefinition current = WorldBorderState.getOrDefault(id);
        WorldBorderDefinition next = new WorldBorderDefinition(
                id,
                enabled,
                current.custom(),
                current.polygon(),
                current.fogDepth(),
                current.softMargin(),
                current.maxDamagePerSecond()
        );
        try {
            WorldBorderIO.save(next);
            WorldBorderState.put(next);
            WorldBorderSync.syncDefinition(context.getSource().getServer(), next);
            context.getSource().sendSuccess(
                    () -> Component.literal((enabled ? "Enabled" : "Disabled") + " border for " + id),
                    true
            );
            return 1;
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal("Failed: " + exception.getMessage()));
            return 0;
        }
    }
}
