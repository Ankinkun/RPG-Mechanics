package com.ankin.rpgmechanics.classbuild.network;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.ClassBuildManager;
import com.ankin.rpgmechanics.classbuild.GearSlot;
import com.ankin.rpgmechanics.classbuild.StowedInventory;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** RMB on an equipped doll/bag slot → move into stowed. */
public record UnequipSlotPayload(int gearSlotOrdinal) implements CustomPacketPayload {
    public static final Type<UnequipSlotPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "unequip_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnequipSlotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            UnequipSlotPayload::gearSlotOrdinal,
            UnequipSlotPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UnequipSlotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!ClassBuildManager.hasActiveCharacter(player)) {
                return;
            }
            GearSlot slot = GearSlot.fromOrdinalSafe(payload.gearSlotOrdinal());
            String error = StowedInventory.unequip(player, slot);
            if (error != null) {
                player.displayClientMessage(Component.literal(error), true);
            }
        });
    }
}
