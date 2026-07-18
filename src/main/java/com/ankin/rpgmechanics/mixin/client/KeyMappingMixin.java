package com.ankin.rpgmechanics.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ankin.rpgmechanics.keybind.KeybindInputEngine;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    @Inject(method = "set(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V", at = @At("HEAD"), cancellable = true)
    private static void rpgmechanics$interceptSet(InputConstants.Key key, boolean held, CallbackInfo ci) {
        if (KeybindInputEngine.handleSet(key, held)) {
            ci.cancel();
        }
    }

    @Inject(method = "click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V", at = @At("HEAD"), cancellable = true)
    private static void rpgmechanics$interceptClick(InputConstants.Key key, CallbackInfo ci) {
        if (KeybindInputEngine.handleClick(key)) {
            ci.cancel();
        }
    }

    @Inject(method = "setAll()V", at = @At("HEAD"), cancellable = true)
    private static void rpgmechanics$interceptSetAll(CallbackInfo ci) {
        if (KeybindInputEngine.handleSetAll()) {
            ci.cancel();
        }
    }
}
