package com.ankin.rpgmechanics.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.ankin.rpgmechanics.keybind.KeybindInputEngine;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.extensions.IKeyMappingExtension;

/**
 * Inventory hotbar 1–9 (and drop / pick / close) use {@link IKeyMappingExtension#isActiveAndMatches}.
 * Managed bindings store {@link InputConstants#UNKNOWN} on the KeyMapping, so vanilla matching fails
 * unless we resolve against profile chords.
 */
@Mixin(IKeyMappingExtension.class)
public interface KeyMappingExtensionMixin {
    @Inject(method = "isActiveAndMatches", at = @At("HEAD"), cancellable = true)
    default void rpgmechanics$isActiveAndMatchesManaged(
            InputConstants.Key keyCode,
            CallbackInfoReturnable<Boolean> cir
    ) {
        KeyMapping self = (KeyMapping) this;
        if (!KeybindInputEngine.shouldResolveGuiMatch(self.getName())) {
            return;
        }
        boolean matched = KeybindInputEngine.matchesManaged(self.getName(), keyCode)
                && self.getKeyConflictContext().isActive();
        cir.setReturnValue(matched);
    }
}
