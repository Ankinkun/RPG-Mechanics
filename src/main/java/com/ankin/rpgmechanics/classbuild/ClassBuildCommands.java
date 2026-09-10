package com.ankin.rpgmechanics.classbuild;

import com.mojang.brigadier.arguments.IntegerArgumentType;
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
                                    () -> Component.literal("Character roster cleared — select a character"),
                                    true
                            );
                            return 1;
                        }))
                .then(Commands.literal("select")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ClassBuildManager.openCharacterSelect(player);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Opened character select"),
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
                .then(Commands.literal("delete")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(0, CharacterRoster.SLOT_COUNT - 1))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int slot = IntegerArgumentType.getInteger(ctx, "slot");
                                    String error = ClassBuildManager.deleteSlot(player, slot);
                                    if (error != null) {
                                        ctx.getSource().sendFailure(Component.literal(error));
                                        return 0;
                                    }
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("Deleted character slot " + slot),
                                            true
                                    );
                                    return 1;
                                })))
                .then(Commands.literal("info")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            CharacterRoster roster = ClassBuildManager.getRoster(player);
                            ClassBuildState state = roster.activeBuildOrEmpty();
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("activeSlot=" + roster.activeSlot()
                                            + " confirmed=" + state.confirmed()
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
