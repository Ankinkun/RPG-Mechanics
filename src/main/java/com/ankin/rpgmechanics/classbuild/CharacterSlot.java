package com.ankin.rpgmechanics.classbuild;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * One Destiny-style character: kit + equipped gear + stowed bag.
 */
public record CharacterSlot(
        boolean occupied,
        String name,
        ClassBuildState build,
        ItemStack helmet,
        ItemStack chest,
        ItemStack legs,
        ItemStack boots,
        ItemStack weapon,
        ItemStack offhand,
        List<ItemStack> stowed
) {
    public static final CharacterSlot EMPTY = new CharacterSlot(
            false,
            "",
            ClassBuildState.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            List.of()
    );

    public static final Codec<CharacterSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("occupied", false).forGetter(CharacterSlot::occupied),
            Codec.STRING.optionalFieldOf("name", "").forGetter(CharacterSlot::name),
            ClassBuildState.CODEC.optionalFieldOf("build", ClassBuildState.EMPTY).forGetter(CharacterSlot::build),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("helmet", ItemStack.EMPTY).forGetter(CharacterSlot::helmet),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("chest", ItemStack.EMPTY).forGetter(CharacterSlot::chest),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("legs", ItemStack.EMPTY).forGetter(CharacterSlot::legs),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("boots", ItemStack.EMPTY).forGetter(CharacterSlot::boots),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("weapon", ItemStack.EMPTY).forGetter(CharacterSlot::weapon),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("offhand", ItemStack.EMPTY).forGetter(CharacterSlot::offhand),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("stowed", List.of()).forGetter(CharacterSlot::stowed)
    ).apply(instance, CharacterSlot::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CharacterSlot> STREAM_CODEC = StreamCodec.of(
            (buf, slot) -> {
                buf.writeBoolean(slot.occupied());
                ByteBufCodecs.STRING_UTF8.encode(buf, slot.name());
                ClassBuildState.STREAM_CODEC.encode(buf, slot.build());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, slot.helmet());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, slot.chest());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, slot.legs());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, slot.boots());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, slot.weapon());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, slot.offhand());
                ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, slot.stowed());
            },
            buf -> new CharacterSlot(
                    buf.readBoolean(),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ClassBuildState.STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf)
            )
    );

    public CharacterSlot {
        stowed = copyStowed(stowed);
    }

    public static CharacterSlot create(String name, ClassBuildState build) {
        return new CharacterSlot(
                true,
                name == null || name.isBlank() ? defaultName(build) : name,
                build.confirmedCopy(),
                new ItemStack(Items.LEATHER_HELMET),
                new ItemStack(Items.LEATHER_CHESTPLATE),
                new ItemStack(Items.LEATHER_LEGGINGS),
                new ItemStack(Items.LEATHER_BOOTS),
                new ItemStack(Items.STONE_SWORD),
                new ItemStack(Items.SHIELD),
                List.of()
        );
    }

    public CharacterSlot withBuild(ClassBuildState next) {
        return new CharacterSlot(true, name, next.confirmedCopy(), helmet, chest, legs, boots, weapon, offhand, stowed);
    }

    public CharacterSlot withEquipment(
            ItemStack helmet,
            ItemStack chest,
            ItemStack legs,
            ItemStack boots,
            ItemStack weapon,
            ItemStack offhand
    ) {
        return new CharacterSlot(
                occupied,
                name,
                build,
                helmet.copy(),
                chest.copy(),
                legs.copy(),
                boots.copy(),
                weapon.copy(),
                offhand.copy(),
                stowed
        );
    }

    public CharacterSlot withStowed(List<ItemStack> nextStowed) {
        return new CharacterSlot(occupied, name, build, helmet, chest, legs, boots, weapon, offhand, nextStowed);
    }

    public ItemStack get(GearSlot slot) {
        return switch (slot) {
            case HELMET -> helmet;
            case CHEST -> chest;
            case LEGS -> legs;
            case BOOTS -> boots;
            case WEAPON -> weapon;
            case OFFHAND -> offhand;
        };
    }

    public CharacterSlot withGear(GearSlot slot, ItemStack stack) {
        ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
        return switch (slot) {
            case HELMET -> new CharacterSlot(occupied, name, build, copy, chest, legs, boots, weapon, offhand, stowed);
            case CHEST -> new CharacterSlot(occupied, name, build, helmet, copy, legs, boots, weapon, offhand, stowed);
            case LEGS -> new CharacterSlot(occupied, name, build, helmet, chest, copy, boots, weapon, offhand, stowed);
            case BOOTS -> new CharacterSlot(occupied, name, build, helmet, chest, legs, copy, weapon, offhand, stowed);
            case WEAPON -> new CharacterSlot(occupied, name, build, helmet, chest, legs, boots, copy, offhand, stowed);
            case OFFHAND -> new CharacterSlot(occupied, name, build, helmet, chest, legs, boots, weapon, copy, stowed);
        };
    }

    private static List<ItemStack> copyStowed(List<ItemStack> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        List<ItemStack> copy = new ArrayList<>(source.size());
        for (ItemStack stack : source) {
            if (stack != null && !stack.isEmpty()) {
                copy.add(stack.copy());
            }
        }
        return Collections.unmodifiableList(copy);
    }

    private static String defaultName(ClassBuildState build) {
        return "Darkness " + build.role().getSerializedName().replace('_', ' ');
    }
}
