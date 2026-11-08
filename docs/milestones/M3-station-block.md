# M3 – Station multiblock (implemented, needs in-game check)

Scope: one variant (`futuristic`) end to end; other variants in M6. Block footprint 1 wide x 2 deep x 2 high.
Blockstate: `FACING` (horizontal) + `PART` enum {FRONT_LOW, BACK_LOW, FRONT_HIGH, BACK_HIGH}. BlockEntity only on `BACK_LOW`.

| Story | Description | Acceptance criteria | Status |
|-------|-------------|---------------------|--------|
| S3.1 | Register block + item `station_futuristic`, BlockEntityType | Item in creative tab, uses existing icon `station_futuristic` | done, needs in-game check |
| S3.2 | Placement | Places 4 parts rotated by player facing; refused with message if any cell is not replaceable (pattern: bed/door) | done, needs in-game check |
| S3.3 | Breaking | Breaking any part removes all 4, no duplicate drops, survives piston/explosion without orphans | done, needs in-game check |
| S3.4 | BlockEntity data | Field `PresetData npc` (+ workstation slot placeholder), NBT save/load via CODEC, `getUpdateTag`/`getUpdatePacket` sync to client | done, needs in-game check |
| S3.5 | Insert NPC | Right-click any part with filled catcher stores NPC, catcher becomes empty; occupied station refuses | done, needs in-game check |
| S3.6 | Remove NPC | Shift + right-click with empty hand returns a filled catcher with the stored `PresetData` | done, needs in-game check |
| S3.7 | Block models (futuristic) | Side walls, back, top, lower front glass (blue tint); upper front frame with large access window; small podium (pressure-plate look) at `BACK_LOW`; workstation floor space at `FRONT_LOW`; textures included | done, needs in-game check |
| S3.8 | Shapes | Selection box covers the whole 4-cell footprint per part; collision follows walls/podium, player can click through the window area | done, needs in-game check |
| S3.9 | Drops & loot | Breaking drops the station item once; contained NPC drops as filled catcher (not lost) | done, needs in-game check |
| S3.10 | Lang | en_us + de_de names/messages | done, needs in-game check |

Verification: place, rotate in all 4 directions, break each part, save/reload world, NPC data still present.
