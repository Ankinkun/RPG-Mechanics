package com.ankin.rpgmechanics.world.protection;

import com.ankin.rpgmechanics.RpgMechanics;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;

@EventBusSubscriber(modid = RpgMechanics.MOD_ID)
public final class WorldProtectionEvents {
    private WorldProtectionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        Player player = event.getPlayer();
        if (!WorldProtection.canModifyBlocks(player, level)) {
            event.setCanceled(true);
            WorldProtection.notifyDenied(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        if (!WorldProtection.canModifyBlocks(entity, level)) {
            event.setCanceled(true);
            WorldProtection.notifyDenied(entity);
        }
    }

    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        if (!WorldProtection.canModifyBlocks(event.getEntity(), level)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        if (!WorldProtection.isEnabled()) {
            return;
        }
        // Lava/water turning cobble etc. still changes terrain — block globally when protection is on.
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onToolModify(BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated() || !(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        Player player = event.getPlayer();
        if (!WorldProtection.canModifyBlocks(player, level)) {
            event.setCanceled(true);
            WorldProtection.notifyDenied(player);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide() || !WorldProtection.isEnabled()) {
            return;
        }
        Entity source = event.getExplosion().getDirectSourceEntity();
        if (WorldProtection.canModifyBlocks(source, level)) {
            return;
        }
        // Keep entity damage; strip block destruction.
        event.getAffectedBlocks().clear();
    }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (!WorldProtection.isEnabled()) {
            return;
        }
        event.setCanGrief(false);
    }

    @SubscribeEvent
    public static void onLivingDestroyBlock(LivingDestroyBlockEvent event) {
        if (event.getEntity().level().isClientSide() || !WorldProtection.isEnabled()) {
            return;
        }
        if (!WorldProtection.canModifyBlocks(event.getEntity(), event.getEntity().level())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPiston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        if (!WorldProtection.isEnabled()) {
            return;
        }
        event.setCanceled(true);
    }
}
