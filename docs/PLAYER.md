# Player guide

How to play with **RPG Mechanics** once the mod is installed. For pack setup, authoring, and configs, see [`PACK.md`](PACK.md).

---

## Characters

1. From the title screen, pick or create a character (v1: **Darkness Damage Dealer** only; Tank / Support Coming Soon).
2. The client loads the campaign save (`classbuild.campaignWorldName` in client config; empty = first available save).
3. **TAB** opens **Gear**. **Escape** opens **Overview**. Use the top bar to switch tabs.
4. **Quit** on the hub returns to character select (does not close Minecraft).

### Hub tabs

| Tab | What it does |
|-----|----------------|
| **Overview** | Character doll / status |
| **Gear** | Equipped doll + stowed bag |
| **Class** | School-themed kit UI (spellbooks, abilities) |
| **Quests** | Quest book |
| **Map** | Xaero world map with the same hub chrome |

There is **no Skills tab**. Epic Fight dodge / guard are granted automatically; you do not open the Epic Fight skill editor.

The world does **not** pause while the hub is open.

### Gear bag

- **LMB** on a stowed item → equip into the matching slot.
- **RMB** on equipped gear (doll or bag outline) → unequip to the bag.
- **F** (tap) → mark / unmark an item under the cursor.
- **Hold F** (~3 seconds) → destroy:
  - If nothing is marked → destroys the hovered item.
  - If items are marked → asks for confirmation with the count, then destroys the set.
- Hover items for vanilla tooltips.
- Pickups go into the **stowed bag**, never the hotbar.

### Class tab

- School themes (Blood / Eldritch / Ender) tint the chrome.
- Hover the main spellbook to fan out ultimates; the **selected** book stays in the center.
- Click a fan book to switch ultimate / theme (applies instantly).
- Hover ability slots for the Select Spell grid; click an option to apply.

### Movement

Moving forward **always sprints**. **Sneak** still crouches / walk-sneaks. There is no normal walk gait while standing.

---

## Quests

1. Press **J** to open the quest book (or use the **Quests** hub tab).
2. Left-click a quest for details. The book only shows the **current** objective.
3. Right-click a quest to **track** or untrack it. The tracked quest appears on the HUD.
4. Finish the objective in the world. A toast fires when the step (or the whole quest) completes.

Accepting a quest is currently an operator command (`/rpgmechanics quest accept <id>`). The bundled `welcome` quest is a short dirt → stick example.

---

## Controls

1. Open **Options → Controls → Key Binds** (RPG Mechanics screen).
2. Click **Primary** or **Secondary**, then press a key. **Escape** clears that slot (it does not restore the default).
3. Click **Trigger** to cycle Press → Hold → Double Tap → Release.
4. **Reset** restores that row; **Reset All** restores the pack/vanilla defaults.

Pack taxonomy groups bindings into **Movement / Combat / Inventory**. Remove the **Controlling** mod if it is in the pack — it fights this screen.

Default highlights: inventory **TAB**, map **M**, quest journal **J**.

---

## Building (protected worlds)

You cannot place or break blocks until your name (or UUID) is on the server allowlist. Operators do **not** bypass protection unless the pack enables that.

Ask a pack operator to add you, or see [`PACK.md`](PACK.md) for the allowlist config.

---

## Combat HUD

With an active character, a compact combat HUD shows health, abilities, and mana. Vanilla hotbar / food / XP chrome is hidden while that is on (client config `classbuild.combatHud`).
