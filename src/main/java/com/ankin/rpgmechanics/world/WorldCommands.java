package com.ankin.rpgmechanics.world;

import com.ankin.rpgmechanics.world.border.BorderPolygon;
import com.ankin.rpgmechanics.world.border.WorldBorderCommands;
import com.ankin.rpgmechanics.world.border.WorldBorderDefinition;
import com.ankin.rpgmechanics.world.border.WorldBorderIO;
import com.ankin.rpgmechanics.world.border.WorldBorderState;
import com.ankin.rpgmechanics.world.border.network.WorldBorderSync;
import com.ankin.rpgmechanics.world.protection.WorldProtection;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Root {@code /rpgmechanics world …} commands (protection + border).
 */
public final class WorldCommands {
    private WorldCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("rpgmechanics")
                .then(Commands.literal("world")
                        .then(Commands.literal("protection")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.literal("status")
                                        .executes(WorldCommands::protectionStatus)))
                        .then(WorldBorderCommands.borderSubtree()
                                .then(Commands.literal("setbox")
                                        .then(Commands.argument("halfSize", DoubleArgumentType.doubleArg(8.0, 1_000_000.0))
                                                .executes(WorldCommands::setBoxCenteredOnPlayer)))));
    }

    private static int protectionStatus(CommandContext<CommandSourceStack> context) {
        boolean enabled = WorldProtection.isEnabled();
        boolean opsBypass = WorldProtection.opsBypass();
        String playerLine = "no player";
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            playerLine = "you canBuild=" + WorldProtection.isBuilder(player)
                    + " allowlisted=" + WorldProtection.isOnAllowlist(player)
                    + " op=" + player.hasPermissions(2);
        } catch (Exception ignored) {
            // console
        }
        String line = "protectionEnabled=" + enabled
                + " opsBypassProtection=" + opsBypass
                + " | " + playerLine
                + " | builders: config/rpgmechanics-server.toml → world.builderAllowlist";
        context.getSource().sendSuccess(() -> Component.literal(line), false);
        if (enabled && opsBypass) {
            context.getSource().sendSuccess(
                    () -> Component.literal("Note: opsBypassProtection=true — OP/cheats can break blocks."),
                    false
            );
        } else if (enabled) {
            context.getSource().sendSuccess(
                    () -> Component.literal("Terrain is locked. Add your name to world.builderAllowlist to build."),
                    false
            );
        }
        return 1;
    }

    /**
     * Quick test border: square centered on the player so fog is reachable on foot.
     */
    private static int setBoxCenteredOnPlayer(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            double half = DoubleArgumentType.getDouble(context, "halfSize");
            double cx = player.getX();
            double cz = player.getZ();
            ResourceLocation id = player.level().dimension().location();
            BorderPolygon polygon = new BorderPolygon(java.util.List.of(
                    new BorderPolygon.Vertex(cx - half, cz - half),
                    new BorderPolygon.Vertex(cx + half, cz - half),
                    new BorderPolygon.Vertex(cx + half, cz + half),
                    new BorderPolygon.Vertex(cx - half, cz + half)
            ));
            WorldBorderDefinition current = WorldBorderState.getOrDefault(id);
            WorldBorderDefinition next = new WorldBorderDefinition(
                    id,
                    true,
                    true,
                    polygon,
                    current.fogDepth(),
                    current.softMargin(),
                    current.maxDamagePerSecond()
            );
            WorldBorderIO.save(next);
            WorldBorderState.put(next);
            WorldBorderState.clearDraft(id);
            WorldBorderSync.syncDraft(context.getSource().getServer(), id);
            WorldBorderState.suppressVanillaBorder(player.serverLevel());
            WorldBorderSync.syncDefinition(context.getSource().getServer(), next);
            context.getSource().sendSuccess(
                    () -> Component.literal("Saved " + (int) (half * 2) + "×" + (int) (half * 2)
                            + " fog border centered on you. Walk ~" + (int) half + " blocks out to enter fog."),
                    true
            );
            return 1;
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal("setbox failed: " + exception.getMessage()));
            return 0;
        }
    }
}
