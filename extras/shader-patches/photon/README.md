# Photon border fog → RPG Mechanics

Photon’s built-in **Border Fog** (`border_fog` in `shaders/include/fog/simple_fog.glsl`) fades terrain using Iris `far` (render distance). That ignores our polygonal border.

## What we change

Drive that fade with Iris `fogEnd` instead (still capped by `far` / DH distance):

```glsl
float fog_radius = max(min(far, fogEnd), 1.0);
float fog = cubic_length(scene_pos.xz) / fog_radius;
```

RPG Mechanics aims `fogEnd` at the nearest border edge when you look toward it (`BorderFogRenderer`). Unpatched Photon still ignores this.

## Apply to a Photon zip

1. Backup your shaderpack zip.
2. Replace `shaders/include/fog/simple_fog.glsl` inside the zip with the copy next to this README (or merge the `border_fog` function from it).
3. Reload shaders in-game (or toggle the pack).

Dev `run/shaderpacks/photon_v1.1.zip` is already patched for local testing. Original backup: `photon_v1.1.zip.bak-pre-rpgborder`.

## Pack shipping

Ship the patched Photon (or this single-file overlay) with the modpack. The Java mod alone cannot rewrite an active shader pack.
