package com.ankin.rpgmechanics.world.border.client;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import net.neoforged.fml.ModList;

/**
 * Soft detection for Iris / Oculus shader packs. No hard dependency —
 * uses ModList + reflective {@code IrisApi.isShaderPackInUse()}.
 */
public final class ShaderPackCompat {
    private static final boolean IRIS_LIKE_LOADED =
            ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus");

    @Nullable
    private static final Object IRIS_API;
    @Nullable
    private static final Method IS_SHADER_PACK_IN_USE;

    static {
        Object api = null;
        Method method = null;
        if (IRIS_LIKE_LOADED) {
            try {
                Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Method getInstance = apiClass.getMethod("getInstance");
                api = getInstance.invoke(null);
                method = apiClass.getMethod("isShaderPackInUse");
            } catch (ReflectiveOperationException ignored) {
                // Older Oculus / missing API — treat as "iris present but pack unknown".
            }
        }
        IRIS_API = api;
        IS_SHADER_PACK_IN_USE = method;
    }

    private ShaderPackCompat() {
    }

    public static boolean isIrisLikePresent() {
        return IRIS_LIKE_LOADED;
    }

    /**
     * True when a shader pack is actively driving rendering (Iris/Oculus).
     * Falls back to {@code false} if the API is unavailable.
     */
    public static boolean isShaderPackInUse() {
        if (IRIS_API == null || IS_SHADER_PACK_IN_USE == null) {
            return false;
        }
        try {
            Object result = IS_SHADER_PACK_IN_USE.invoke(IRIS_API);
            return result instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }
}
