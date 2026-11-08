# M6 – Variants, workstation slot, polish (implemented, needs in-game check)

Design rule: the variant list is data, never hard-coded per block (S6.1 and S6.10 share one `StationVariant` definition: id, base planks/stone block id, glass style).

### S6.10 – Dynamic variants for modpacks (details)
Modpacks add wood types (BYG, Biomes O' Plenty, Quark ...); stations must exist for them without code changes.
- **Discovery at registration time:** variant list = built-in list + wood types found in the `WoodType` registry (`WoodType.values()`) + entries from a text file `config/easy_npc_trading_station-extra-variants.txt` (mod configs are not loaded during RegisterEvent) (`"modid:planks_block_id"`, optional `style`). **Correction:** mod configs (even COMMON) are NOT loaded during RegisterEvent ("Cannot get config value before config is loaded" crashes the pack), so extra variants come from a plain text file.
- **Registration:** `StationVariant` -> block + item per entry through the same loop as S6.1; missing/invalid block ids are skipped with one warning, never a crash.
- **Assets without a jar edit:** blockstates/models/item models/lang/loot/recipes for dynamic variants are generated at runtime (client: in-memory resource pack added via `AddPackFindersEvent`; server: in-memory data pack). Models reference the planks block's own texture (`#planks`), so no new textures are needed. Built-in variants keep their static generated assets.
- **Recipes:** dynamic recipe = planks block of the variant + glass (same shape as built-ins); can be disabled via config.
- **Icons:** dynamic variants use a generic icon tinted with the planks' average color (or a 3D block model parent), so no PNG per variant is required.
- **Acceptance:** with a test mod/modpack adding one wood type, a station for it appears (creative tab, recipe, correct model, name via translation fallback `"<Wood> Trading Station"`); removing the mod loads the world without errors (items become missing, no crash); config entry for a custom block id also works; documented in README.
- **Risk:** ordering between mod registries and `WoodType` population; if values are incomplete at registration, fall back to the config list only and document it.

Variants: `futuristic` (done in M3), `stone`, wood types oak, spruce, birch, jungle, acacia, dark_oak, mangrove, cherry, bamboo, crimson, warped. Medieval: clear glass, lower part stone/wood. Icons already exist (`tools/generate_station_icons.ps1`).

| Story | Description | Acceptance criteria | Status |
|-------|-------------|---------------------|--------|
| S6.1 | Register all variants in a loop | 13 blocks + items from one variant list, shared block/BE classes | done, needs in-game check |
| S6.2 | Medieval models/blockstates | Generator script produces models/blockstates/loot from parent models + texture variables; clear glass, base material per variant | done, needs in-game check |
| S6.3 | Workstation slot | Station stores one workstation block item; insert with item in hand on the front, remove with Shift + empty hand; drops on break | done, needs in-game check |
| S6.4 | Workstation defaults | Config `workstationDefaults` (`entity_type_id` or profession -> block id), used when slot is empty; Easy NPC default is freely configurable | done, needs in-game check |
| S6.5 | Recipes | Catcher recipe + recipes per variant (futuristic needs glass + redstone/amethyst/iron, medieval uses planks/stone + glass) | done, needs in-game check |
| S6.6 | Lang, tab, tooltips | en_us + de_de complete; own creative tab for catcher + stations | done, needs in-game check |
| S6.7 | Final art pass | Replace placeholder catcher icon, check all 13 icons in game | done, needs in-game check |
| S6.8 | Server & performance test | `runServer` (user accepts EULA), 200 stations placed, no TPS drop; multiplayer sync of renderer | done, needs in-game check |
| S6.10 | **Dynamic variants for modpacks** | See details below | done, needs in-game check |
| S6.9 | Release prep | Version bump, README, license, jar named `easy_npc_trading_station-<version>.jar` | done, needs in-game check |
