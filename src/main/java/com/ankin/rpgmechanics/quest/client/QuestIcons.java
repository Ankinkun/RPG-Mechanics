package com.ankin.rpgmechanics.quest.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.ankin.rpgmechanics.RpgMechanics;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Resolves quest icons from item ids, texture paths, or {@code config/rpgmechanics/quest_icons/}.
 */
public final class QuestIcons {
    private static final ResourceLocation FALLBACK_ITEM = ResourceLocation.withDefaultNamespace("writable_book");
    private static boolean customIconsLoaded;

    private QuestIcons() {
    }

    public static Path customIconDirectory() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve(RpgMechanics.MOD_ID).resolve("quest_icons");
        try {
            Files.createDirectories(dir);
        } catch (IOException exception) {
            RpgMechanics.LOGGER.error("Failed to create quest icon directory {}", dir, exception);
        }
        return dir;
    }

    /** Registers PNGs from the config icon folder as dynamic textures. */
    public static void ensureCustomIconsLoaded() {
        if (customIconsLoaded) {
            return;
        }
        customIconsLoaded = true;
        Path dir = customIconDirectory();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.png")) {
            for (Path path : stream) {
                String name = path.getFileName().toString();
                String base = name.substring(0, name.length() - 4);
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "quest_icon/" + sanitize(base));
                try (InputStream input = Files.newInputStream(path)) {
                    NativeImage image = NativeImage.read(input);
                    minecraft.getTextureManager().register(id, new DynamicTexture(image));
                } catch (Exception exception) {
                    RpgMechanics.LOGGER.error("Failed to load quest icon {}", path, exception);
                }
            }
        } catch (IOException exception) {
            RpgMechanics.LOGGER.error("Failed to list quest icons in {}", dir, exception);
        }
    }

    public static void reloadCustomIcons() {
        customIconsLoaded = false;
        ensureCustomIconsLoaded();
    }

    private static String sanitize(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
    }

    public static ResourceLocation customIconId(String fileBaseName) {
        return ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "quest_icon/" + sanitize(fileBaseName));
    }

    public static List<ResourceLocation> listCustomIcons() {
        ensureCustomIconsLoaded();
        List<ResourceLocation> icons = new ArrayList<>();
        Path dir = customIconDirectory();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.png")) {
            for (Path path : stream) {
                String name = path.getFileName().toString();
                icons.add(customIconId(name.substring(0, name.length() - 4)));
            }
        } catch (IOException ignored) {
        }
        icons.sort(Comparator.comparing(ResourceLocation::toString));
        return icons;
    }

    public static List<ResourceLocation> listItemIcons() {
        List<ResourceLocation> icons = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null && item != Items.AIR) {
                icons.add(id);
            }
        }
        icons.sort(Comparator.comparing(ResourceLocation::toString));
        return icons;
    }

    public static ItemStack resolveItemStack(Optional<ResourceLocation> icon) {
        return resolveItemStack(icon.orElse(null));
    }

    public static ItemStack resolveItemStack(ResourceLocation icon) {
        if (icon == null) {
            return new ItemStack(Items.WRITABLE_BOOK);
        }
        if (BuiltInRegistries.ITEM.containsKey(icon)) {
            return new ItemStack(BuiltInRegistries.ITEM.get(icon));
        }
        // Texture path that looks like textures/item/<name>.png → try matching item
        String path = icon.getPath();
        if (path.startsWith("textures/item/") && path.endsWith(".png")) {
            String itemPath = path.substring("textures/item/".length(), path.length() - 4);
            ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(icon.getNamespace(), itemPath);
            if (BuiltInRegistries.ITEM.containsKey(itemId)) {
                return new ItemStack(BuiltInRegistries.ITEM.get(itemId));
            }
        }
        if (BuiltInRegistries.ITEM.containsKey(FALLBACK_ITEM)) {
            return new ItemStack(BuiltInRegistries.ITEM.get(FALLBACK_ITEM));
        }
        return new ItemStack(Items.BOOK);
    }

    /**
     * Texture used for HUD / custom toast when the icon is a texture or dynamic quest icon.
     * Item ids map to {@code textures/item/...png}.
     */
    public static Optional<ResourceLocation> resolveTexture(ResourceLocation icon) {
        if (icon == null) {
            return Optional.empty();
        }
        ensureCustomIconsLoaded();
        if (icon.getNamespace().equals(RpgMechanics.MOD_ID) && icon.getPath().startsWith("quest_icon/")) {
            return Optional.of(icon);
        }
        if (icon.getPath().startsWith("textures/")) {
            return Optional.of(icon);
        }
        if (BuiltInRegistries.ITEM.containsKey(icon)) {
            return Optional.of(ResourceLocation.fromNamespaceAndPath(icon.getNamespace(), "textures/item/" + icon.getPath() + ".png"));
        }
        return Optional.of(icon);
    }
}
