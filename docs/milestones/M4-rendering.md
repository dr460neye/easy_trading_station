# M4 – Client rendering (implemented, needs in-game check)

Reference: Easy NPC `client/renderer/blockentity/BaseEasyNPCSpawnerBlockEntityRenderer` (see skill `easy-npc-api`). Register in `EntityRenderersEvent.RegisterRenderers`.

| Story | Description | Acceptance criteria | Status |
|-------|-------------|---------------------|--------|
| S4.1 | Display entity | Client builds an `EasyNPC` entity from stored `PresetData` (`registerEasyNPCDefaultData`, `importPresetData`), never `addFreshEntity`, cached per BlockEntity | done, needs in-game check |
| S4.2 | Render NPC on podium | BER renders the NPC on the `BACK_LOW` podium, rotated by `FACING`, facing the front window | done, needs in-game check |
| S4.3 | Cache invalidation | Cache rebuilt when preset changes (insert/remove/update packet) and cleared on block removal/chunk unload | done, needs in-game check |
| S4.4 | Skins/models | Humanoid, slim and villager-type NPCs show correct model and skin (incl. remote/player skins) | done, needs in-game check |
| S4.5 | Workstation visual | BER draws the workstation block model in `FRONT_LOW` via block render dispatcher; placeholder = config default until M6 | done, needs in-game check |
| S4.6 | Performance guard | No tick on display entity; render only inside normal BER range; no per-frame allocation | done, needs in-game check |
| S4.7 | Empty station | Station without NPC renders only glass/podium/workstation, no errors | done, needs in-game check |

Verification: many stations in view, F3 entity count unchanged, NPC visible after relog.
