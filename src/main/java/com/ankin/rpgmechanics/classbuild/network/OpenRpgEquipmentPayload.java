package com.ankin.rpgmechanics.classbuild.network;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.ClassBuildManager;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Legacy open-equipment packet. Gear is now a client {@code Screen}; this syncs roster only.
 */
public record OpenRpgEquipmentPayload() implements CustomPacketPayload {
    public static final Type<OpenRpgEquipmentPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "open_rpg_equipment"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenRpgEquipmentPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenRpgEquipmentPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenRpgEquipmentPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!ClassBuildManager.isFeatureEnabled() || !ClassBuildManager.get(player).confirmed()) {
                return;
            }
            ClassBuildManager.sync(player);
        });
    }
}
