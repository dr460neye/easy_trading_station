# M1 – Project setup (done, now on 26.1.2)

| Story | Description | Status |
|-------|-------------|--------|
| S1.1 | NeoForge 1.21.1 MDK (Gradle 9.2.1, toolchain Java 21), mod id `easy_npc_trading_station` | done |
| S1.2 | Easy NPC core as `implementation "maven.modrinth:easy-npc-core:9TVwS7rU"`; required dep `easy_npc [7.18.0,)` in `neoforge.mods.toml` | done |
| S1.4 | **Retarget to Minecraft 26.1.2** (FTB Evolution 2 test pack): NeoForge 26.1.2.126, Java 25, no Parchment, Easy NPC `Amlap22E`; code ported (`Identifier`, `sendOverlayMessage`, `appendHoverText`, `registerItem`, item definition json) | done |
| S1.5 | Deploy script `tools/deploy-to-test-instance.ps1` for the dev clone of FTB Evolution 2 | done |
| S1.3 | Verify key Easy NPC classes exist in the jar (PresetData, PresetDataUtils, PauseHandler, ActionHandler, SecurityManager, TradingDataCapable) | done |

Notes: initial setup was 1.21.1; retargeted to 26.1.2 because the test modpack runs it. JDK 25 is `~\.jdks\openjdk-25.0.1`. (`temurin-21.0.13` is unused now and can be deleted.) Version `7.18.0` is ambiguous across MC versions, always use the Modrinth version ID.
