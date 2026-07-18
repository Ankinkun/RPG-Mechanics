package com.ankin.rpgmechanics.quest.network;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.quest.QuestDefinition;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EditorSaveQuestPayload(QuestDefinition quest) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<EditorSaveQuestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "editor_save_quest"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EditorSaveQuestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodecWithRegistries(QuestDefinition.CODEC),
            EditorSaveQuestPayload::quest,
            EditorSaveQuestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EditorSaveQuestPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
                return;
            }
            if (!com.ankin.rpgmechanics.quest.QuestPermissions.canUseEditor(serverPlayer)) {
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.rpgmechanics.quest.authoring_denied"));
                return;
            }
            QuestDefinition quest = payload.quest();
            if (quest.id() == null || quest.title() == null || quest.title().isBlank() || quest.steps() == null || quest.steps().isEmpty()) {
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("Invalid quest definition"));
                return;
            }
            try {
                com.ankin.rpgmechanics.quest.QuestAuthoringIO.save(quest, serverPlayer.registryAccess());
                com.ankin.rpgmechanics.quest.QuestSync.syncDefinitionsToAll(serverPlayer.server);
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.rpgmechanics.quest.saved", quest.id().toString()));
            } catch (java.io.IOException exception) {
                RpgMechanics.LOGGER.error("Failed to save quest {}", quest.id(), exception);
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("Failed to save quest: " + exception.getMessage()));
            }
        });
    }
}
