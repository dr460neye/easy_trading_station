# M5 – Trading / dialog interaction (implemented, needs in-game check)

| Story | Description | Acceptance criteria | Status |
|-------|-------------|---------------------|--------|
| S5.1 | **Spike** (do first) | Written result in this file: (a) does `sanitizePresetDataForSpawn` keep trades/dialogs with `player == null` / non-OP, (b) does `MerchantMenu` work with a transient entity, (c) can the entity be added to the level paused and removed on close | done, needs in-game check |
| S5.2 | Open trading | Right-click station (not sneaking) opens vanilla merchant screen with the stored offers via `openTradingScreen` | done, needs in-game check |
| S5.3 | Open dialog | NPCs with dialogs open them via `EasyNPCActionHandler.openDialog` | done, needs in-game check |
| S5.4 | Choose action | `ActionEventType.ON_INTERACTION` configured on the NPC wins (`trigger`); fallback: trading, then default dialog | done, needs in-game check |
| S5.5 | Write-back | On menu close: `serializePresetData` -> BlockEntity, entity discarded; trade uses/XP/restock state persist across relog | done, needs in-game check |
| S5.6 | Restock | `resetExpiredTradingOffers` evaluated on open using game time; no ticking while closed | done, needs in-game check |
| S5.7 | Fixed & safe | Transient entity is paused (`EasyNPCPauseHandler.pause`), NoAI, invulnerable; actions/objectives/intervals never fire; no leftover entity after crash/unload | done, needs in-game check |
| S5.8 | Concurrency | Second player gets a "busy" message while someone trades | done, needs in-game check |

Verification: trade, close, relog, trade again; `/kill @e[type=easy_npc:*]` finds nothing; server console shows no entity leaks.
