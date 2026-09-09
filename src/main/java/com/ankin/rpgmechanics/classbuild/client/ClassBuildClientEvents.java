package com.ankin.rpgmechanics.classbuild.client;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.menu.RpgEquipmentMenu;
import com.ankin.rpgmechanics.classbuild.network.OpenRpgEquipmentPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = RpgMechanics.MOD_ID, value = Dist.CLIENT)
public final class ClassBuildClientEvents {
    private ClassBuildClientEvents() {
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getScreen() instanceof InventoryScreen)) {
            return;
        }
        if (!ClassBuildClientPayloadHandlers.cached().confirmed()) {
            event.setNewScreen(new ClassSelectScreen());
            return;
        }
        event.setCanceled(true);
        PacketDistributor.sendToServer(new OpenRpgEquipmentPayload());
    }

    @SubscribeEvent
    public static void onClientTickForceSelect(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        // Re-open select if still unconfirmed (payload may have arrived with screen closed).
        if (!ClassBuildClientPayloadHandlers.cached().confirmed()
                && ClassBuildClientPayloadHandlers.pendingSelect()) {
            ClassBuildClientPayloadHandlers.clearPendingSelect();
            minecraft.setScreen(new ClassSelectScreen());
        }
    }
}
