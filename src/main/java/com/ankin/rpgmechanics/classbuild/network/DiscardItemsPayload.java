package com.ankin.rpgmechanics.classbuild.network;

import java.util.List;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.ClassBuildManager;
import com.ankin.rpgmechanics.classbuild.StowedInventory;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DiscardItemsPayload(List<Integer> stowedIndices, List<Integer> gearSlotOrdinals)
        implements CustomPacketPayload {
    public static final Type<DiscardItemsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "discard_items"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Integer> VAR_INT_CODEC = StreamCodec.of(
            RegistryFriendlyByteBuf::writeVarInt,
            RegistryFriendlyByteBuf::readVarInt
    );
    private static final StreamCodec<RegistryFriendlyByteBuf, List<Integer>> INDEX_LIST_CODEC =
            VAR_INT_CODEC.apply(ByteBufCodecs.list(StowedInventory.MAX_DISCARD_BATCH));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiscardItemsPayload> STREAM_CODEC = StreamCodec.composite(
            INDEX_LIST_CODEC,
            DiscardItemsPayload::stowedIndices,
            INDEX_LIST_CODEC,
            DiscardItemsPayload::gearSlotOrdinals,
            DiscardItemsPayload::new
    );

    public DiscardItemsPayload {
        stowedIndices = List.copyOf(stowedIndices);
        gearSlotOrdinals = List.copyOf(gearSlotOrdinals);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DiscardItemsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !ClassBuildManager.hasActiveCharacter(player)) {
                return;
            }
            StowedInventory.discard(player, payload.stowedIndices(), payload.gearSlotOrdinals());
        });
    }
}
