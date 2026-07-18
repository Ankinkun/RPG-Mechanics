# Keybind System



Client-only keybind manager for RPG Mechanics (MC 1.21.1 / NeoForge).



## Player Controls screen



Replaces vanilla **Key Binds**. Each row is ~50% action name and ~50% four buttons:



`Primary | Secondary | Trigger | Reset`



- **Primary / Secondary** — click to bind; **Escape unbinds that slot entirely** (does not restore the default key). Either key fires the action.

- **Trigger** — cycles Press → Hold → Double Tap → Release (applies to both keys).

- **Hold** — keep the key down for **500 ms**, then one press fires; release earlier cancels.

- **Reset / Reset All** — restore pack/vanilla defaults.



Category headers use each binding’s category id as a lang key and resolve the label live from
vanilla/mod `assets/.../lang` files (via `I18n`). Custom pack categories fall back to their stored title.



## Authoring mode



Unlocked only when `config/rpgmechanics-client.toml` → `keybinds.keybindAuthoringMode=true`.



Authoring adds:



- **New Category** / **Reload**

- **Hide / Show** per binding (`visible` in JSON)

- **…** to cycle the binding’s category



Hidden bindings are omitted for players; authors still see them grayed with `[hidden]`.



## Persistence



`config/rpgmechanics/keybinds/`:



- `pack_defaults.json` — pack defaults; **seeded on first run** with every registered KeyMapping and its vanilla/mod category

- `player.json` — player edits (wins on merge)



New mods’ keybinds are merged into `pack_defaults.json` on later launches without overwriting existing entries.



## Other mods



- All `KeyMapping`s are discovered automatically (no per-mod deps).

- **Controlling** is declared as a NeoForge `discouraged` dependency: launching with it shows a warning. Remove Controlling to use the RPG Mechanics keybind menu without UI conflicts.

- Bindings present in the profile are managed by the input engine (including fully unbound empty chords).

- Mods that bypass `KeyMapping` (raw GLFW) are not intercepted.



## Example JSON



```json

{

  "categories": [

    {

      "id": "key.categories.movement",

      "title": "Movement",

      "entries": ["key.forward", "key.left", "key.back", "key.right", "key.jump", "key.sneak", "key.sprint"]

    }

  ],

  "bindings": {

    "key.jump": {

      "visible": true,

      "enabled": true,

      "customCategory": "key.categories.movement",

      "defaultKey": "key.keyboard.space",

      "chords": [

        { "key": "key.keyboard.space", "modifier": "none", "trigger": "press" }

      ]

    }

  }

}

```


