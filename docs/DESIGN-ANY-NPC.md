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
- Open decision: optionally let the station follow the moon: when a session starts, an adapter may
  ask the NPC for its current form. Needs one adapter per mod and a check of what the mod does.
- The client display copy is never ticked, so it shows the stored form.

### 4. What may be caught (S7.5)

- Allow list by default: `Merchant` entities and Easy NPCs. Everything else only through the config
  (`match=MODE`, same grammar as `typeOverrides`).
- Never by default: players, bosses, tamed pets, anything with an owner, dragons and other large
  creatures (Ice and Fire), hostile mobs.
- Clone mode stays **off for non-Easy-NPC entities** by default: cloning a villager multiplies
  trades (trade farming). Config can enable it.

### 5. Workstations and upgrade tier (S7.4, M8)

- Villagers: the workstation is the job site block of the profession (read from the profession,
  `VillagerProfession`'s held job site), overridable by name in `requiredWorkstations`. Modded
  professions (Jeweler, Scribe, ...) work the same way.
- Upgrade tier (M8): vanilla villagers and wandering traders are level 0, special NPCs and
  Easy NPC humanoids level 1, set by the same `match=LEVEL` list.

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

## Open decisions

1. Must-have NPCs for the first step (suggestion: vanilla villager and wandering trader).
2. Werewolf trader: which mod and entity exactly, and should the station keep the stored form or
   follow the moon?
3. Clone mode for non-Easy-NPC entities: off by default (suggested), or allowed?
4. Which Ice and Fire NPCs are meant (the Scribe villager, or creatures)?
