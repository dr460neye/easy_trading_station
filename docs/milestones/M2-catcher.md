# M2 – NPC Catcher (done, not tested in game)

Item id `npc_catcher`. Files: `item/NpcCatcherItem`, `event/NpcCatcherEvents`, `config/CatcherMode`, `config/TradingStationConfig`, `registry/ModItems`, `registry/ModDataComponents`.

| Story | Description | Acceptance criteria | Status |
|-------|-------------|---------------------|--------|
| S2.1 | Capture an Easy NPC | Right-click with empty catcher on NPC stores `PresetData` in DataComponent `captured_npc`, NPC removed, NPC's own dialog does not open | done, needs in-game check |
| S2.2 | Clone mode + per-type config | `defaultMode` and `typeOverrides` (`entity_type_id=MODE`) decide CAPTURE/CLONE/BOTH/DISABLED; BOTH clones while sneaking | done, needs in-game check |
| S2.3 | Release | Right-click block with filled catcher spawns NPC beside the block, catcher becomes empty | done, **needs in-game check: trades/dialogs survive** |
| S2.4 | Access check | `SecurityManager.checkAccess` must allow, otherwise message and no capture | done |
| S2.5 | Item polish | Fun icon (net with NPC head), tooltip, glint when filled, en/de lang, creative tab | done (icon is a placeholder) |
| S2.6 | Insert into station | Right-click station with filled catcher -> handled in M3 (S3.5) | moved to M3 |
| S2.7 | Crafting recipe (3 string + 2 sticks) and "unsupported mob" message | done |

Open risk: `PresetDataUtils.spawnEntity(..., player)` runs `SecurityManager.sanitizePresetDataForSpawn`; it may strip trading/dialog data for non-privileged players. Relevant to S5.1.
