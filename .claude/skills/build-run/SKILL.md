---
name: build-run
description: Build, check and run the Easy Trading Station mod on this Windows machine with minimal output. Use before any Gradle call, when a build fails, or when the user wants to test in game.
---

# Build & run (token-light)

JDK 25 is required (Minecraft 26.1.2) and is not on PATH. Always set it per call (PowerShell):

```powershell
Set-Location C:\Users\oli\easy_npc_trading_station
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\openjdk-25.0.1"
cmd /c ".\gradlew.bat build --console=plain 2>&1" | Select-String -Pattern 'error|Warnung|warning:|BUILD|\.java:' | Select-Object -Last 20
```

- Never show full Gradle output. For a failure, widen the filter or use `-Last 60`, not the whole log.
- First run after dependency changes takes minutes (NeoForge sources); normal builds are ~1-5 s.
- `gradlew.bat` must be invoked via `cmd /c` from PowerShell; `2>&1` on the native call otherwise produces NativeCommandError noise.
- Game tests are done by the user: tell them `$env:JAVA_HOME="$env:USERPROFILE\.jdks\openjdk-25.0.1"; .\gradlew.bat runClient`, or for the real modpack run `tools\deploy-to-test-instance.ps1` and start the instance in CurseForge.
- Do **not** start `runServer` yourself: it needs `eula.txt` = accepting the Minecraft EULA, which only the user may do.
- Look at Easy NPC API details through skill `easy-npc-api`, or `javap -public -cp <jar> <class>` for ONE class (jar: `~\.gradle\caches\modules-2\files-2.1\maven.modrinth\easy-npc-core\Amlap22E\*\easy-npc-core-Amlap22E.jar`).
- Deprecation warnings: fix them (e.g. use `DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID)`).
- Minecraft classes for javap: `build\moddev\artifacts\minecraft-patched-26.1.2.126-merged.jar` (ignore stale `neoforge-21.1.265-*` files). NeoForge classes: `~\.gradle\caches\modules-2\files-2.1\net.neoforged\neoforge\26.1.2.126\*\neoforge-26.1.2.126-universal.jar`.
- Use `javap` from the JDK 25 folder.
