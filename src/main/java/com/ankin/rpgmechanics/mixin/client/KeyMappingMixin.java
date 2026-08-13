package com.ankin.rpgmechanics.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Inject(method = "releaseAll()V", at = @At("RETURN"))
    private static void rpgmechanics$afterReleaseAll(CallbackInfo ci) {
        KeybindInputEngine.onReleaseAll();
    }

    /**
     * Managed bindings are unbound on the vanilla {@code key} field so the input engine can claim
     * physical keys. GUI code still calls {@link KeyMapping#matches} (screenshot, fullscreen, etc.).
     */
    @Inject(method = "matches(II)Z", at = @At("HEAD"), cancellable = true)
    private void rpgmechanics$matchesManaged(int keysym, int scancode, CallbackInfoReturnable<Boolean> cir) {
        KeyMapping self = (KeyMapping) (Object) this;
        if (!KeybindInputEngine.shouldResolveGuiMatch(self.getName())) {
            return;
        }
        cir.setReturnValue(KeybindInputEngine.matchesManagedKeysym(self.getName(), keysym, scancode));
    }

    @Inject(method = "matchesMouse(I)Z", at = @At("HEAD"), cancellable = true)
    private void rpgmechanics$matchesMouseManaged(int mouseButton, CallbackInfoReturnable<Boolean> cir) {
        KeyMapping self = (KeyMapping) (Object) this;
        if (!KeybindInputEngine.shouldResolveGuiMatch(self.getName())) {
            return;
        }
        cir.setReturnValue(KeybindInputEngine.matchesManagedMouse(self.getName(), mouseButton));
    }
}
