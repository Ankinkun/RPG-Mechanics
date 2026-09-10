package com.ankin.rpgmechanics.classbuild.network;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.ClassBuildManager;
import com.ankin.rpgmechanics.classbuild.integration.IronSpellsSoft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client ability button → server quick-cast of managed book slot index 0–3. */
public record CastAbilityPayload(int spellIndex) implements CustomPacketPayload {
    public static final Type<CastAbilityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "cast_ability"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CastAbilityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CastAbilityPayload::spellIndex,
            CastAbilityPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CastAbilityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!ClassBuildManager.hasActiveCharacter(player)) {
                return;
            }
            int index = payload.spellIndex();
            if (index < 0 || index > 3) {
                return;
            }
            IronSpellsSoft.quickCast(player, index);
        });
    }
}
