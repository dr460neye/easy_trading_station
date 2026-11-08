# Easy Trading Station

NeoForge **26.1.2** (Minecraft 26.1.2, NeoForge 26.1.2.126) addon for Easy NPC 7.18.0. Target = FTB Evolution 2 modpack. NPC Catcher captures/clones an Easy NPC; the Trading Station (1x2x2 multiblock) holds it without a ticking entity.
Mod id `easy_npc_trading_station`, package `de.dragoni.tradingstation`, Java 25, Mojang names (no Parchment), 2-space Google style. MC 26.1 API: `Identifier` (not ResourceLocation), `player.sendOverlayMessage`, `appendHoverText(stack, ctx, TooltipDisplay, Consumer<Component>, flag)`, items via `registerItem(...)`, every item needs `assets/<ns>/items/<id>.json`.

## Read first (in this order, stop when you have enough)
1. GitHub Issues (`gh issue list --milestone "<milestone>" --state open`) – stories with acceptance criteria, grouped by GitHub milestone (M1..M7). Work on the first open story of the current milestone only.
2. `docs/IMPLEMENTATION-NOTES.md` – how the pieces fit together.
3. Skill `easy-npc-api` – verified Easy NPC classes/methods. Use it instead of re-researching or running javap.

## Architecture rules (do not change without asking)
- **No ticking entity in the station.** Store only `PresetData` in the BlockEntity. Client renders a cached, never-added display entity. Server creates a paused entity only while a menu is open, then writes state back and discards it.
- NPC is fixed + invulnerable; actions/objectives never tick.
- Station = own block per variant (`futuristic`, `stone`, wood types), blockstates `FACING` + `PART` (4 parts), BlockEntity only on `BACK_LOW`.
- Variants are **data** (`StationVariant`): built-ins + modded `WoodType`s + `config/easy_npc_trading_station-extra-variants.txt` (COMMON configs are not loaded during registration). Never hard-code a block class per wood (story S6.10).
- Workstation = item in a station slot, rendered visually only; empty slot -> config default.
- Keep NPC logic generic (preset = entity type + NBT) so later versions can catch non-Easy-NPC mobs.

## Commands (Windows, PowerShell)
Gradle needs the JDK from `java_version` in `gradle.properties` (25): set `JAVA_HOME` (own path in git-ignored `tools/local.json` as `javaHome`) then `.\gradlew.bat <task>`. See skill `build-run`. Unit tests: `.\gradlew.bat test`.
- Build check: `.\gradlew.bat build --console=plain 2>&1 | Select-String 'error|Warnung|BUILD'`
- Never print full Gradle output; always filter. Never open the Easy NPC jar or Gradle caches except via `javap -public` on one class.
- In-game test, one command: `python tools/test_in_game.py` (skill `test-instance`): deploys, starts the instance and loads the world. "fertig" = `python tools/test_in_game.py --close` (saves and closes the running game like the window's X, never kills it).
- Test instance (clone of FTB Evolution 2, MC 26.1.2, has Easy NPC 7.18.0, Easy Villagers, Witchery): `powershell -NoProfile -ExecutionPolicy Bypass -File tools\deploy-to-test-instance.ps1` builds and copies our jar into its `mods` folder (replaces only our own jar); instance path comes from git-ignored `tools/local.json`. `python tools/launch_test_instance.py "<world>"` starts it directly (game must be closed to deploy).
- `runClient` / `runServer` need a person at the game; do not start `runServer` (needs EULA acceptance by the user).

## Conventions
- Registration via `DeferredRegister` in `registry/`. Config in `config/TradingStationConfig` (SERVER). Lang: always add `en_us.json` + `de_de.json` together.
- Assets for variants come from `tools/generate_station_icons.ps1` (see skill `station-variants`); do not hand-write 13 JSON files.
- Bulk text replacements: use exact `.Replace()` on literal strings, never broad regex (an earlier rename corrupted identifiers). Rebuild after every rename.
- Comments only for non-obvious "why". No speculative abstractions.

## Working agreement
- Finish a story -> comment the result on its GitHub issue and close it (stories that still need a check in the game get the label `needs-in-game-test` and stay open until the user confirms) -> short summary to the user. Do not start the next milestone without being asked.
- In-game behaviour cannot be verified by the build. Say "not tested in game" unless the user confirmed.
- Ask before: accepting EULAs, installing software system-wide, publishing anything.
