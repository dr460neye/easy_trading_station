---
name: station-variants
description: How trading station variants (futuristic, stone, wood types, modded woods) and their icons/models are defined and generated. Use when adding a variant, changing station art, or working on stories S3.7, S6.1, S6.2, S6.10.
---

# Station variants

- A variant is data (`StationVariant`: id, base block id, glass style). Never add a hand-written block class per wood.
- Built-ins: `futuristic` (blue-tinted stasis glass, metal base), `stone`, and woods oak, spruce, birch, jungle, acacia, dark_oak, mangrove, cherry, bamboo, crimson, warped (clear glass, base from planks/stone).
- Inventory icons: `tools/generate_station_icons.ps1` (writes `textures/item/station_<id>.png` + `models/item/station_<id>.json`). To add a built-in variant, add one line to its `$variants` table and rerun:
  `powershell -NoProfile -File tools\generate_station_icons.ps1`
- Block models: parent models with texture variables (`#base`, `#glass`) so each variant is a tiny JSON; blockstates cover `facing` x `part` (FRONT_LOW, BACK_LOW, FRONT_HIGH, BACK_HIGH). Extend the generator script (or add `tools/generate_station_models.ps1`) instead of writing files by hand.
- Layout reminder: front lower cell = workstation (rendered only), back lower cell = NPC on a pressure-plate-like podium, upper front = frame with large access window, rest glass.
- Modded woods (S6.10): discovered via `WoodType.values()` + text file `config/easy_npc_trading_station-extra-variants.txt` (mod configs are not loaded during RegisterEvent); their assets are generated at runtime (in-memory resource/data pack) using the planks block's texture. Invalid ids are skipped with a single warning.
- Always add lang keys in `en_us.json` and `de_de.json` together; dynamic variants use the fallback name `"<Wood> Trading Station"`.
