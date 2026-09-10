package com.ankin.rpgmechanics.classbuild.network;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.ClassBuildManager;
import com.ankin.rpgmechanics.classbuild.StowedInventory;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** LMB on a stowed bag index → equip into the resolved doll slot. */
public record EquipStowedPayload(int stowedIndex) implements CustomPacketPayload {
    public static final Type<EquipStowedPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "equip_stowed"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EquipStowedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            EquipStowedPayload::stowedIndex,
            EquipStowedPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EquipStowedPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!ClassBuildManager.hasActiveCharacter(player)) {
                return;
            }
            String error = StowedInventory.equipFromStowed(player, payload.stowedIndex());
            if (error != null) {
                player.displayClientMessage(Component.literal(error), true);
            }
        });
    }
}
