package com.ankin.rpgmechanics.classbuild;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ClassBuildCommands {
    private ClassBuildCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> registerSubtree() {
        return Commands.literal("class")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ClassBuildManager.reset(player);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Class build reset — select a class again"),
                                    true
                            );
                            return 1;
                        }))
                .then(Commands.literal("apply")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ClassBuildManager.applyLoadout(player);
                            ctx.getSource().sendSuccess(() -> Component.literal("Class loadout reapplied"), true);
                            return 1;
                        }))
                .then(Commands.literal("info")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ClassBuildState state = ClassBuildManager.get(player);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("confirmed=" + state.confirmed()
                                            + " role=" + state.role()
                                            + " track=" + state.track()
                                            + " melee=" + state.melee()
                                            + " movement=" + state.movement()
                                            + " ranged=" + state.ranged()
                                            + " ultimate=" + state.ultimate()),
                                    false
                            );
                            return 1;
                        }));
    }
}
