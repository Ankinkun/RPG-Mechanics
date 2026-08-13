package com.ankin.rpgmechanics.world.border.client;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.world.border.WorldBorderDefinition;
import com.mojang.blaze3d.shaders.FogShape;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Fakes a world-fixed fog wall using vanilla radial fog.
 * <p>
 * Border-anchored near/far are <em>lerped</em> with the client's current RD fog planes
 * so there is no hard takeover when the edge first enters the fog horizon.
 */
@EventBusSubscriber(modid = RpgMechanics.MOD_ID, value = Dist.CLIENT)
public final class BorderFogRenderer {
    private BorderFogRenderer() {
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft minecraft = Minecraft.getInstance();
        float baseFar = minecraft.options.getEffectiveRenderDistance() * 16.0F;
        FogPlan plan = plan(baseFar, baseFar * 0.75F);
        if (plan == null || plan.outsideStrength() <= 0.0F || plan.influence() <= 0.0F) {
            return;
        }
        float t = plan.outsideStrength() * plan.influence() * 0.22F;
        event.setRed(lerp(event.getRed(), 0.78F, t));
        event.setGreen(lerp(event.getGreen(), 0.84F, t));
        event.setBlue(lerp(event.getBlue(), 0.92F, t));
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        float baseNear = event.getNearPlaneDistance();
        float baseFar = event.getFarPlaneDistance();
        FogPlan plan = plan(baseFar, baseNear);
        if (plan == null || plan.influence() <= 0.001F) {
            return;
        }

        float influence = plan.influence();
        float targetNear = plan.fogNear();
        float targetFar = plan.fogFar();

        // Always blend from the live vanilla planes — never snap-replace them.
        float near = lerp(baseNear, targetNear, influence);
        float far = lerp(baseFar, targetFar, influence);
        far = Math.max(near + 8.0F, far);
        far = Math.min(far, baseFar);
        near = Mth.clamp(near, 0.0F, far - 4.0F);

        event.setNearPlaneDistance(near);
        event.setFarPlaneDistance(far);
        event.setFogShape(FogShape.CYLINDER);
        event.setCanceled(true);
    }

    private static FogPlan plan(float baseFar, float baseNear) {
        if (!ClientWorldBorderCache.isFeatureEnabled()) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return null;
        }
        ResourceLocation dimension = minecraft.level.dimension().location();
        WorldBorderDefinition definition = ClientWorldBorderCache.get(dimension);
        if (definition == null || !definition.enabled() || !definition.polygon().isValid()) {
            return null;
        }

        double x = minecraft.player.getX();
        double z = minecraft.player.getZ();
        double fogDepth = Math.max(4.0, definition.fogDepth());
        double outside = definition.polygon().distanceOutside(x, z);

        float soft = Math.max((float) fogDepth * 1.5F, Math.min(72.0F, baseFar * 0.28F));
        // How far inside the RD fog horizon the border must be before we fully own the planes.
        float engage = Math.max(soft, baseFar * 0.22F);

        if (outside > 0.0) {
            float depth = (float) Math.min(1.0, outside / Math.max(fogDepth, 1.0));
            // Match inside-at-edge far (soft * 0.55) so crossing does not jump.
            float edgeFar = soft * 0.55F;
            float deepFar = Math.max(16.0F, soft * 0.35F);
            float fogNear = 0.0F;
            float fogFar = Mth.lerp(depth, edgeFar, deepFar);
            return new FogPlan(0.0F, fogNear, fogFar, depth, 1.0F);
        }

        float borderDist = (float) definition.polygon().distanceToEdge(x, z);
        float fogNear = Math.max(0.0F, borderDist - soft);
        float fogFar = borderDist + soft * 0.55F;

        // influence: 0 while border is at/beyond the fog horizon, →1 as it moves inward.
        float influence;
        if (borderDist >= baseFar) {
            influence = 0.0F;
        } else {
            float into = baseFar - borderDist;
            influence = smoothstep(Mth.clamp(into / engage, 0.0F, 1.0F));
        }
        if (influence <= 0.0F) {
            return null;
        }

        // Keep targets inside the current RD fog budget before blending.
        fogNear = Mth.clamp(fogNear, 0.0F, Math.max(0.0F, baseFar - 8.0F));
        fogFar = Mth.clamp(fogFar, fogNear + 8.0F, baseFar);

        return new FogPlan(borderDist, fogNear, fogFar, 0.0F, influence);
    }

    private static float smoothstep(float x) {
        return x * x * (3.0F - 2.0F * x);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private record FogPlan(
            float borderDistance,
            float fogNear,
            float fogFar,
            float outsideStrength,
            float influence
    ) {
    }
}
