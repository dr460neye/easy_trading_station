# Roadmap

Status: `done` (built), `tested` (confirmed in game by user), `open`, `blocked`.

| ID | Milestone | Status | File |
|----|-----------|--------|------|
| M1 | Project setup & Easy NPC dependency (26.1.2, Java 25) | done | [M1](milestones/M1-setup.md) |
| M2 | NPC Catcher (capture / clone / release) | done, not tested in game | [M2](milestones/M2-catcher.md) |
| M3 | Station multiblock, BlockEntity, persistence | done, not tested in game | [M3](milestones/M3-station-block.md) |
| M4 | Client rendering (NPC, podium, workstation) | done, not tested in game | [M4](milestones/M4-rendering.md) |
| M5 | Trading / dialog interaction & write-back | done, not tested in game | [M5](milestones/M5-interaction.md) |
| M6 | Variants, workstation slot, recipes, polish | done, not tested in game | [M6](milestones/M6-variants-polish.md) |
| M7 | Later version: catch any NPC type | future | [M7](milestones/M7-any-npc.md) |

All of M2-M6 are implemented but **not yet tested in game**; test order: catcher -> place station -> insert NPC -> trade/dialog -> relog.

Order matters: M5 contains the riskiest spike (S5.1: does `sanitizePresetDataForSpawn` keep trades/dialogs?). If the user reports M2 release losing trades, move S5.1 forward.

## Definition of done (every story)
1. Code compiles (`build` successful), no new deprecation warnings.
2. Acceptance criteria met; in-game criteria marked "needs in-game check" until the user confirms.
3. en_us + de_de lang entries, status updated here and in the milestone file.
