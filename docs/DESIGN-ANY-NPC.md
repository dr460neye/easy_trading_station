# Design: stations for any NPC (vanilla villagers and special NPCs)

Status: plan, nothing built yet. Milestone M7 on GitHub (stories S7.1 to S7.10).
Facts below come from reading the mods of the maintainer's test pack (FTB Evolution 2, Minecraft
26.1.2); "not verified" marks everything that was not tried in the game.

## Goal

The NPC Catcher and the Trading Station also work for NPCs that are not Easy NPCs:

- vanilla **villagers** (including professions added by mods) and **wandering traders**,
- trader NPCs of other mods, **special NPCs** with their own behaviour, for example a trader that
  changes its form with the moon phase.

Easy NPC keeps working exactly as today. The architecture rule stays: **no ticking entity in the
station**; the block entity stores data only, the client draws a cached display copy, the server
creates a paused entity only while somebody uses the station.

## Target NPCs (named by the maintainer)

All of these are **villagers with a profession** from a mod (the lang files show
`entity.minecraft.villager.<mod>.<profession>`), except the two werewolf entities:

| NPC | Mod | Kind | Profession id (from the lang key, not verified) |
|-----|-----|------|-----------------------------------------------|
| Scribe | Ice and Fire | villager profession | `iceandfire:scribe` |
| Fluix Researcher | AE2 | villager profession | `ae2:fluix_researcher` |
| Jeweler | Iron's Jewelry | villager profession | `irons_jewelry:jeweler` |
| Electrician | Energized Power | villager profession | `energizedpower:electrician` (key `villager.electrician`) |
| Beekeeper | Productive Bees | villager profession | `productivebees:beekeeper` |
| Fortune Teller | Witchery | villager profession | `witchery:fortune_teller` |
| Werewolvian | EvilCraft | villager profession (`VillagerProfessionWerewolfConfig`) that turns into the monster `evilcraft:werewolf` | `evilcraft:werewolf` (not verified) |
| Werewolf | Witchery | villager that is infected (command `villagerwerewolf`: "transformation in 20 seconds") and becomes the entity `witchery:werewolf`; can be cured | none, state in the villager |

So the first adapter is the **villager adapter**: one adapter covers vanilla and all these
professions. The werewolves are the hard part (section 3).

## What exists in the test pack

| Mod | Entity or profession | Kind (from lang files and mod jars, not verified in game) |
|-----|----------------------|----------------------------------------------------------|
| minecraft | `villager`, `wandering_trader` | `Merchant` with offers, brain, profession, reputation |
| Ice and Fire | villager profession "Scribe" | normal villager |
| Iron's Jewelry | villager profession "Jeweler" | normal villager |
| AE2 | villager profession "Fluix Researcher" | normal villager |
| Productive Bees | villager profession "Beekeeper" | normal villager |
| EvilCraft | `villager.werewolf` (Werewolvian), `werewolf` | villager type that turns into a werewolf (moon dependent, not verified) |
| Goblin Traders | `goblin_trader`, `vein_goblin_trader` | wandering-trader style merchants |
| Farming for Blockheads | `merchant` | market merchant, own trading screen |
| Occultism | `wondering_trader` | trader |
| Witchery | `coven_witch`, `hunter`, `vampire`, `werewolf` | special NPCs / hostile mobs |

## Design

### 1. Generic NPC data (S7.1, S7.8)

- New record `StationNpcData(entityTypeId, nbt, formatVersion)`. Easy NPC keeps its `PresetData`
  path; both are wrapped by one small interface so the station code does not care.
- The catcher stores the data in a new data component. Old catchers (with `PresetData`) stay valid:
  the reader accepts both forms and never rewrites an old catcher unless it is used.
- Station block entities keep the existing field for Easy NPCs and get a second optional field for
  generic NPCs. Old saves load unchanged; older mod versions ignore the new field.

### 2. NPC adapters (S7.2, S7.3, S7.7)

One `NpcAdapter` per kind of NPC, chosen by entity type (config can override):

| Adapter | Used for | Does |
|---------|----------|------|
| `EasyNpcAdapter` | Easy NPC | existing code |
| `MerchantAdapter` | anything implementing vanilla `Merchant` (villager, wandering trader, many mod traders) | create from NBT, freeze, open the vanilla trading screen, write back offers and xp |
| `GenericAdapter` | everything else that is allowed | create from NBT, freeze, interact like a normal right-click, write back |

An adapter provides: `capture(entity) -> data`, `create(level, data) -> entity` (never added to
the level on the client), `freeze(entity)` (no AI, silent, invulnerable, no movement), `open(player,
entity)` and `writeBack(entity) -> data`.

Villager specifics (not verified in game): brain, memories and the claimed job site must not break
when the villager is stored; the stored NBT keeps offers, xp, level, gossip and profession. While
stored the villager does not restock by time but by game time at the next use, like Easy NPC does
today.

### 3. NPCs that change by themselves (S7.7)

Conversions (a villager that becomes a werewolf, a zombie villager that is cured) replace the entity
with a new one. In a session that would create a second entity and lose the station's link.

- Cancel `LivingConversionEvent.Pre` for session entities (tag `easy_npc_trading_station:transient`).
- Stored form: the station keeps the form that was captured. This is the default ("frozen").
- Werewolf cases (not verified, to be checked in S7.10): EvilCraft's werewolf profession and
  Witchery's infected villager change into a monster by themselves. The station must keep them as
  villagers: cancel the conversion for session entities and, if the mod uses a timer in the NBT
  (Witchery: "transformation in 20 seconds"), reset or hold it while stored.
- Decision of the maintainer for Witchery: a trader may transform during use, as long as it turns
  back. So conversion is tolerated, and the rule becomes "the station always ends with the
  villager again":
  - at the start of a session the station keeps a snapshot of the villager data;
  - if the entity is converted during the session, the session follows the new entity, and when
    the session ends the station writes the **snapshot** back (or cures the villager through the
    mod's own cure, if there is a safe call), never the monster data;
  - a conversion never leaves a second entity in the world and never removes the NPC from the
    station.
  Cancelling the conversion stays available as the fallback for mods where the snapshot is not
  enough. Which of the two Witchery needs is decided in S7.10.
- Open decision: optionally let the station follow the moon: when a session starts, an adapter may
  ask the NPC for its current form. Needs one adapter per mod and a check of what the mod does.
- The client display copy is never ticked, so it shows the stored form.

### 4. What may be caught (S7.5)

- Allow list by default: `Merchant` entities and Easy NPCs. Everything else only through the config
  (`match=MODE`, same grammar as `typeOverrides`).
- Never by default: players, bosses, tamed pets, anything with an owner, dragons and other large
  creatures (Ice and Fire), hostile mobs.
- **Standard villagers can not be cloned** (decision of the maintainer; also avoids conflicts with
  Easy Villagers). CLONE and BOTH act as CAPTURE for them, without a config switch. Other
  non-Easy-NPC entities: clone off by default.

### 5. Workstations and upgrade tier (S7.4, M8)

The workstation of a villager comes from the same data JEI shows with the *Just Enough
Professions* addon, which reads the game registries; the station reads them too, so no list has to
be kept by hand and every modded profession works:

1. profession -> its job site test (`VillagerProfession#heldJobSite`),
2. every point of interest type (`PoiType`) that passes the test,
3. every block state of those types (`PoiType#matchingStates`) -> the block items.

All blocks found are **accepted alternatives** (any one is enough), which is what "some professions
have more than one workstation" needs; the requirement parser already handles alternatives
(`a|b`). The result is computed once the registries are complete (server start) and cached.

- The sign and the chat hint name the workstation; with several, the first two and "...".
- `requiredWorkstations` stays the override: an entry by profession id or by name replaces the
  registry result (for example when a pack wants a different block).
- Professions without a job site (nitwit, unemployed) fall back to the default workstation.
- Station upgrades (M8, decisions of the maintainer): a station has **one upgrade slot**; the
  installed upgrade decides which NPC class the station can host.

  | Installed upgrade | Station can host |
  |-------------------|------------------|
  | none | standard (vanilla) villagers, including modded professions |
  | Advanced NPC Trading Station | Easy NPCs |
  | Wandering Trader upgrade | wandering traders; their offers are reset once per day |
  | Bartering upgrade (long list, later) | piglins (bartering) |

  The requirement per NPC is a config list `match=UPGRADE` (entity type id or part of the name,
  first match wins). Defaults: Easy NPC types -> advanced, `minecraft:wandering_trader` ->
  wandering, `minecraft:villager` -> none.

### 6. Safety and exploits

- Claim check already exists (needs edit rights at the NPC position); keep it for all NPC types.
- Reputation and discounts: a stored villager does not gossip; the station does not add discounts
  from the opening player (decide in S7.9).
- Restock and xp farming through the station: restock only as often as the entity would do alone.
- Pack scripts that count villagers or keep a ledger (like the shopkeeper respawn ledger of FTB
  Evolution 2): death events for a destroyed catcher are fired as for Easy NPCs (S7.1 reuses the
  origin mechanism).

### 7. Rendering

The display copy is created from type + NBT on the client and never added to the level. Renderer
quirks (models that need ticking or animation state) are checked per mod in S7.10; fallback is the
static pose.

## Steps and order

1. S7.8 data model and migration, S7.1 generic capture (Easy NPC unchanged, tests).
2. S7.3 `MerchantAdapter` with the vanilla villager and wandering trader (the first visible result).
3. S7.2 generic display entity, S7.4 job sites, S7.5 allow/deny config.
4. S7.7 conversions, S7.9 exploit review.
5. S7.10 compat spikes, one pack mod after the other (list above), each with a result in its issue.

## Compatibility

- Easy NPC path, catchers and stations of 1.0.0 stay valid: new fields are optional, old data is
  read as before.
- Mixed versions of mod on client and server: the station menu is not changed by this plan.
- Config: new options with defaults that keep today's behaviour for Easy NPCs.

## Easy Villagers (installed in the test pack)

Easy Villagers (`easy_villagers`, 1.1.42 for 26.1.2) overlaps with this plan: it has Trader and
Auto Trader blocks that hold a villager, and its `VillagerEvents.onClick` listens to
`PlayerInteractEvent.EntityInteract` and picks a villager up on Shift + right-click (checked in the
bytecode: it reads `isShiftKeyDown` and cancels the event; which item is in the hand is not
verified). Decision of the maintainer: standard villager support must check whether Easy Villagers
is installed (S7.11). Because our catcher listener runs early (HIGH) and cancels the event, the
catcher in hand wins over the pickup; that is not tested in game.

## Open decisions

1. Order of the first steps (suggestion: vanilla villager and wandering trader, then the six professions of the list, then the werewolves).
2. (answered) Werewolves of Witchery and EvilCraft as in the table; still open: keep the stored form
   or follow the moon?
3. Clone mode for non-Easy-NPC entities: off by default (suggested), or allowed?
4. (answered) Ice and Fire means the Scribe villager.
