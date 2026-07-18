package com.ankin.rpgmechanics.quest.network;

import java.io.IOException;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.quest.QuestAuthoringIO;
import com.ankin.rpgmechanics.quest.QuestManager;
import com.ankin.rpgmechanics.quest.QuestPermissions;
import com.ankin.rpgmechanics.quest.QuestSync;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EditorDeleteQuestPayload(ResourceLocation questId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<EditorDeleteQuestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "editor_delete_quest"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EditorDeleteQuestPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            EditorDeleteQuestPayload::questId,
            EditorDeleteQuestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EditorDeleteQuestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (!QuestPermissions.canUseEditor(serverPlayer)) {
                serverPlayer.sendSystemMessage(Component.translatable("message.rpgmechanics.quest.authoring_denied"));
                return;
            }
            try {
                QuestAuthoringIO.delete(payload.questId());
                QuestManager.purgeMissingDefinitionsForAll(serverPlayer.server);
                QuestSync.syncDefinitionsToAll(serverPlayer.server);
                serverPlayer.sendSystemMessage(Component.translatable("message.rpgmechanics.quest.deleted", payload.questId().toString()));
            } catch (IOException exception) {
                RpgMechanics.LOGGER.error("Failed to delete quest {}", payload.questId(), exception);
                serverPlayer.sendSystemMessage(Component.literal("Failed to delete quest: " + exception.getMessage()));
            }
        });
    }
}
