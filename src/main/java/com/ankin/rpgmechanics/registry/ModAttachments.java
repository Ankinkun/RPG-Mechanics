package com.ankin.rpgmechanics.registry;

import java.util.function.Supplier;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.ClassBuildState;
import com.ankin.rpgmechanics.quest.PlayerQuestState;
import com.mojang.serialization.Codec;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, RpgMechanics.MOD_ID);

    public static final Supplier<AttachmentType<PlayerQuestState>> PLAYER_QUESTS = ATTACHMENT_TYPES.register(
            "player_quests",
            () -> AttachmentType.builder(() -> PlayerQuestState.EMPTY)
                    .serialize(PlayerQuestState.CODEC)
                    .copyOnDeath()
                    .build()
    );

    public static final Supplier<AttachmentType<ClassBuildState>> PLAYER_CLASS_BUILD = ATTACHMENT_TYPES.register(
            "player_class_build",
            () -> AttachmentType.builder(() -> ClassBuildState.EMPTY)
                    .serialize(ClassBuildState.CODEC)
                    .copyOnDeath()
                    .build()
    );

    private ModAttachments() {
    }
}
