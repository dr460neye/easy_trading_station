# Configuration guide

Easy Trading Station is built for modpack makers. Everything that decides **which NPC needs which
workstation** and **how the NPC Catcher behaves** is plain config, no scripting and no code.

- [Where the files are](#where-the-files-are)
- [Quick start: assign workstations to your NPCs](#quick-start-assign-workstations-to-your-npcs)
- [How a station decides what an NPC needs](#how-a-station-decides-what-an-npc-needs)
- [`requiredWorkstations` in detail](#requiredworkstations-in-detail)
- [Fallback options](#fallback-options)
- [NPC Catcher modes](#npc-catcher-modes)
- [Extra station variants (modded woods, any block)](#extra-station-variants-modded-woods-any-block)
- [Common config](#common-config)
- [Shipping the config with your modpack](#shipping-the-config-with-your-modpack)
- [Recipes for modpack makers](#recipes-for-modpack-makers)
- [Troubleshooting](#troubleshooting)

---

## Where the files are

| File | Type | Purpose |
|------|------|---------|
| `easy_npc_trading_station-server.toml` | server config | Catcher modes, required workstations (the file you will edit most). |
| `easy_npc_trading_station-common.toml` | common config | Recipe switch for generated station variants. |
| `config/easy_npc_trading_station-extra-variants.txt` | plain text | Extra station variants (created on first start). |

The server config belongs to the world: in a normal setup it is `<world>/serverconfig/`, on a
server it is in the world folder too. Some modpacks move server configs into `config/` (the
FTB Evolution 2 dev instance does). Start the game once, then look for the file by name.

> The mod id (`easy_npc_trading_station`) is kept for file names and ids so existing worlds and
> configs keep working. The display name of the mod is **Easy Trading Station**.

---

## Quick start: assign workstations to your NPCs

You want the *Wayfarer* to need a Waystone and the *Beastmaster* to need a Bestiary Lectern.
Open `easy_npc_trading_station-server.toml` and set:

```toml
[station]
  requiredWorkstations = [
    "wayfarer=waystones:*waystone;Waystone",
    "beastmaster=iceandfire:lectern;Bestiary Lectern"
  ]
```

Done. In the game:

1. The player puts the NPC into a station (right-click with a filled NPC Catcher, or
   Shift + right-click and use the NPC slot).
2. Until the workstation is in the **workstation slot** the NPC stands in the station with a
   strike sign in front of it ("ON STRIKE! Not working until my equipment (Waystone) is back in
   order.") and clicking the station sends the chat hint
   *There is no workstation for this EasyNPC available. Please provide a "Waystone".*
   Nothing of the NPC's logic runs (no shop, no dialog, no KubeJS click handler).
3. As soon as a matching block is in the workstation slot the sign disappears, the block is shown
   in front of the NPC and the NPC works normally.

An NPC without an entry still needs a workstation: the **fallback** (a Crafting Table by default).

---

## How a station decides what an NPC needs

For the NPC in a station the first rule that matches wins:

1. `requiredWorkstations` (most flexible, see below)
2. `workstationByName`
3. `workstationDefaults` (by entity type)
4. `defaultWorkstation` (one block for everybody, default `minecraft:crafting_table`)

The item in the workstation slot must match the rule, otherwise the NPC counts as "on strike".
A station without an NPC just shows a Crafting Table as decoration.

---

## `requiredWorkstations` in detail

```toml
[station]
  requiredWorkstations = ["<match>=<accepted>[|<accepted>...][;<Label>]", ...]
```

### `<match>`: which NPC

| Form | Meaning | Example |
|------|---------|---------|
| contains a `:` | exact **entity type id** | `easy_npc:villager`, `easy_npc:humanoid_slim` |
| plain text | **name** match: the text must appear in the NPC's name, case-insensitive. Translation keys are matched too, so `wayfarer` matches a name stored as `ftb.shopkeeper.wayfarer`. | `wayfarer`, `beastmaster` |

Use the type id to give *all* NPCs of a type the same workstation, use the name for individual
NPCs (typical for named shopkeepers). Entries are checked from top to bottom; put specific ones
first.

### `<accepted>`: which blocks count

Separate several options with `|`.

| Form | Example | Matches |
|------|---------|---------|
| item id | `iceandfire:lectern` | exactly that block item |
| id with `*` wildcards | `waystones:*waystone` | every id that fits, here all Waystone variants |
| item tag (`#`) | `#minecraft:logs` | any item in the tag |

Only the **item id** of the block item is compared, no NBT or amounts.

### `;<Label>` (optional): the name in the hint and on the sign

`...;Waystone` shows *"Waystone"*. Without a label the display name of the first concrete item is
used (for wildcard and tag entries you should always give a label).

### Examples

```toml
requiredWorkstations = [
  # all Easy NPC villagers need a lectern
  "easy_npc:villager=minecraft:lectern;Lectern",
  # a named NPC: any smithing table or anvil works
  "blacksmith=minecraft:smithing_table|minecraft:anvil;Smithing Table or Anvil",
  # any chest-like block from a tag
  "banker=#c:chests;Chest",
  # a modded workstation, all colour variants
  "jewler=mymod:*_station;Jewelcrafting Station"
]
```

---

## Fallback options

```toml
[station]
  # one block for every NPC without a more specific entry
  defaultWorkstation = "minecraft:crafting_table"

  # per entity type: "entity_type_id=block_id"
  workstationDefaults = ["easy_npc:villager=minecraft:lectern"]

  # per NPC name: "text=block_id" (same matching as requiredWorkstations, one block only)
  workstationByName = []
```

These accept a single block id only. Use `requiredWorkstations` as soon as you need wildcards,
tags, several options or a label.

---

## NPC Catcher modes

```toml
[catcher]
  # CAPTURE: the NPC is removed and sits in the catcher
  # CLONE:   the NPC stays, the catcher gets a copy
  # BOTH:    capture, or clone while sneaking
  # DISABLED
  defaultMode = "CAPTURE"

  # per NPC: "match=MODE". match is an entity type id (contains ':') or a part of the NPC's
  # name (case-insensitive). The first entry that fits wins. All Easy NPC humanoids share the
  # type easy_npc:humanoid, so use names to treat individual shopkeepers differently.
  typeOverrides = ["beastmaster=BOTH", "wayfarer=CAPTURE", "jeweler=CLONE", "easy_npc:villager=DISABLED"]
```

Things worth knowing when you build a pack around the catcher:

- A **captured** NPC remembers where it came from. If its catcher is destroyed (explosion, lava,
  fire, void, despawn) the NPC *dies at that place*: the normal death events run, so your scripts
  react as if the NPC had been killed (for example a respawn ledger that frees the shopkeeper's
  slot). **Clones** carry no origin and are simply lost.
- Catchers are used up when the NPC leaves them (released into the world or put into a station).
- The NPC standing in a station can not be caught or cloned again.
- Stations are explosion-proof so a stored NPC is not lost.

---

## Extra station variants (modded woods, any block)

Stations exist for stone, a futuristic style and all vanilla woods. Wood types registered by other
mods get a station automatically. For anything else edit
`config/easy_npc_trading_station-extra-variants.txt`:

```text
# one entry per line: block_id or block_id=style   (style: medieval (default) or futuristic)
biomesoplenty:fir_planks
minecraft:copper_block=futuristic
```

The block supplies the lower part of the station and its texture (`<namespace>:block/<path>`).
Changes need a restart. These variants get models, loot table and recipe generated at startup
(folder `config/easy_npc_trading_station_generated_pack`); an unknown block id is skipped with a
warning in the log.

> This file is a plain text file on purpose: normal mod configs are not loaded yet while blocks
> are registered.

---

## Common config

`easy_npc_trading_station-common.toml`:

```toml
[variants]
  # crafting recipes for the automatically generated variants (modded woods, extra variants)
  dynamicRecipes = true
```

Disable it if you want to supply your own recipes (KubeJS, datapack). Block ids are
`easy_npc_trading_station:station_<variant>`, for example `station_futuristic`, `station_stone`,
`station_oak`.

---

## Shipping the config with your modpack

- Put your edited `easy_npc_trading_station-server.toml` into the modpack's **`defaultconfigs/`**
  folder. NeoForge copies it into the world of every *new* world. Existing worlds keep their
  own copy: edit `<world>/serverconfig/easy_npc_trading_station-server.toml` there (or delete the
  file to regenerate defaults).
- Put `easy_npc_trading_station-extra-variants.txt` into the modpack's `config/` folder.
- If a changed rule does not apply, restart the world: the server config is read when the world
  loads.

---

## Recipes for modpack makers

**Find the id or name of an NPC.** Look at it in the game with `/data get entity @e[type=easy_npc:humanoid,limit=1,sort=nearest]`
(`CustomName` is the name, the type id is the part before the `[`). With `F3 + H` the item ids of
blocks are shown in their tooltips.

**A shop NPC that only works with its workstation (KubeJS pack).** The station fires the same
right-click event a player would, *after* the workstation check. So your existing
`ItemEvents.entityInteracted` shop script keeps working unchanged and is simply not reached while
the workstation is missing.

**Different workstations for NPCs of the same type.** Use names:

```toml
requiredWorkstations = [
  "wayfarer=waystones:*waystone;Waystone",
  "beastmaster=iceandfire:lectern;Bestiary Lectern",
  "easy_npc:humanoid=minecraft:crafting_table;Crafting Table"
]
```

Named entries first, the type entry last as a catch-all (or just rely on `defaultWorkstation`).

**Make workstations a quest or progression goal.** The sign and the chat hint always name the
missing block, so the player knows what to build or find. Ship the workstation as a normal
recipe or loot and keep the NPC's shop locked behind it.

---

## Troubleshooting

| Symptom | Check |
|---------|-------|
| Sign stays although the block is in the slot | The block's **item id** must match an `<accepted>` entry. Wildcards and tags need the full id (`waystones:*waystone`, not `waystone`). |
| Wrong block asked for | Entries are read top to bottom, the first matching `<match>` wins. Move specific entries up. |
| Name entry never matches | Names can be translation keys; use a part of the key (`beastmaster`), not the translated text of another language. |
| Config change has no effect | The world has its own copy under `serverconfig/`; edit that one. Restart the world. |
| Modded wood has no station | Look for `Generated assets for station variant` in `latest.log`; unknown planks blocks are skipped. Add the block to the extra-variants file. |
| NPC does nothing after a station interaction | The NPC has no trade, dialog or action. The station gives the NPC a few seconds to run its actions, then stores it again. |
