package com.ankin.rpgmechanics.registry;

import java.util.function.Supplier;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.CharacterRoster;
import com.ankin.rpgmechanics.quest.PlayerQuestState;

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

    /**
     * Character roster (3 Destiny-style slots). Codec also accepts legacy single ClassBuildState NBT
     * previously stored under this attachment id.
     */
    public static final Supplier<AttachmentType<CharacterRoster>> PLAYER_CHARACTER_ROSTER = ATTACHMENT_TYPES.register(
            "player_class_build",
            () -> AttachmentType.builder(() -> CharacterRoster.EMPTY)
                    .serialize(CharacterRoster.CODEC)
                    .copyOnDeath()
                    .build()
    );

    private ModAttachments() {
    }
}
