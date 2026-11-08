# M7 – Later version: catch any NPC type (future)

Goal: the catcher captures/clones villagers, wandering traders, Coven Witch and other mod NPCs; each gets its fitting workstation.

| Story | Description | Notes |
|-------|-------------|-------|
| S7.1 | Generic capture | Store entity type + `saveWithoutId` NBT for any `Mob`; Easy NPC keeps using `PresetData` |
| S7.2 | Generic display entity | Client entity from type + NBT, same non-ticking approach |
| S7.3 | Generic trading | Vanilla `Merchant` entities (Villager, WanderingTrader) open trading via `openTradingScreen` |
| S7.4 | Workstations by profession | Villager profession -> job site block; mod NPCs via config `workstationDefaults` |
| S7.5 | Blacklist/whitelist config | Bosses, players, tamed pets excluded by default |
| S7.6 | Compat tests | Coven Witch and one more NPC mod |

Constraint for M2-M6: keep `PresetData`-specific code behind a small interface so S7.1 does not require a rewrite.
