package com.ankin.rpgmechanics.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.KeyMapping;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("clickCount")
    int rpgmechanics$getClickCount();

    @Accessor("clickCount")
    void rpgmechanics$setClickCount(int clickCount);
}
