# Contributing

Thanks for helping with Easy Trading Station. This guide gets you from a fresh clone to a pull
request.

- [Set up](#set-up)
- [Everyday commands](#everyday-commands)
- [Testing in the game](#testing-in-the-game)
- [Project rules](#project-rules)
- [Code style](#code-style)
- [Tests](#tests)
- [Assets and generators](#assets-and-generators)
- [Commits, changelog and pull requests](#commits-changelog-and-pull-requests)
- [Issues and milestones](#issues-and-milestones)
- [Releases and other Minecraft versions](#releases-and-other-minecraft-versions)

---

## Set up

You need:

- **JDK 25** (for example [Temurin](https://adoptium.net/)). Gradle's toolchain finds it when it is
  installed in a standard location; otherwise set `JAVA_HOME`.
- **Git**, and **Python 3** for the helper scripts (no packages needed).
- An IDE with Gradle support (IntelliJ IDEA recommended; run `./gradlew idea` if needed).

```bash
git clone https://github.com/dr460neye/easy_trading_station.git
cd easy_trading_station
./gradlew build          # Windows: .\gradlew.bat build
```

The first build downloads Minecraft, NeoForge and Easy NPC (a few minutes). The jar ends up in
`build/libs/easy-trading-station-neoforge-<minecraft>-<version>.jar`.

Target versions (Minecraft, NeoForge, Java, Easy NPC) are all in **`gradle.properties`**; nothing
else needs editing to retarget the build.

---

## Everyday commands

| Command | Purpose |
|---------|---------|
| `./gradlew build` | compile, run unit tests, build the jar (what CI runs) |
| `./gradlew test` | unit tests only |
| `./gradlew runClient` | development client with the mod and Easy NPC |
| `./gradlew runServer` | dedicated server (you have to accept the Minecraft EULA yourself in `run/eula.txt`) |
| `python tools/generate_station_assets.py` | regenerate station models, blockstates, loot, recipes, lang |
| `powershell -File tools/generate_station_icons.ps1` | regenerate station inventory icons |
| `python tools/sketch_station.py <dir>` | technical sketches of the station models (HTML) |

Never edit the generated station JSON files by hand; change the generator and rerun it. CI fails
when committed assets differ from the generator output.

---

## Testing in the game

Many things can only be checked in the game. Besides `runClient` you can test in a real modpack
instance (this is how the mod is developed, with a CurseForge instance of a modpack that contains
Easy NPC):

1. Copy `tools/local.example.json` to `tools/local.json` and fill in your instance path and JDK
   (the file is git-ignored, it never leaves your machine).
2. `powershell -NoProfile -ExecutionPolicy Bypass -File tools/deploy-to-test-instance.ps1` builds
   and copies the jar into the instance's `mods` folder (replacing only our own old jar).
3. `python tools/launch_test_instance.py "<world folder name>"` starts that instance directly
   (offline, no launcher) and opens the world. The game must be closed before you deploy, the
   running game locks the jar.

Checklist before a pull request that changes behaviour:

- [ ] capture, release, clone with an Easy NPC
- [ ] place the station in all four directions, break it from every part, relog
- [ ] NPC in and out through the menu (Shift + right-click) and by quick insert
- [ ] with and without the required workstation (strike sign, chat hint, shop/dialog opens)
- [ ] `latest.log` has no new errors or warnings from `easy_npc_trading_station`

---

## Project rules

The architecture rules of the project (they are also the reason the mod performs well):

- **No ticking entity in the station.** The block entity stores only the NPC's preset data. The
  client renders a cached display copy that is never added to the level; the server creates one
  paused NPC only while a player uses the station, writes its state back and discards it.
- Stations are data driven (`StationVariant`): never add a block class per wood type.
- Keep NPC logic generic (preset = entity type + NBT) so other mobs can be supported later.
- Config belongs in `config/`; pure parsing and matching lives in `ConfigEntries` and
  `WorkstationRequirement` so it can be unit tested.
- Mod configs are **not loaded** while registries are filled; anything needed at registration time
  must not read them (see `StationVariant`).

More background: [`docs/IMPLEMENTATION-NOTES.md`](docs/IMPLEMENTATION-NOTES.md)
(how the pieces fit together), [`docs/CONFIGURATION.md`](docs/CONFIGURATION.md) (the config), and
`.claude/skills/easy-npc-api/SKILL.md` (verified Easy NPC calls).

---

## Code style

- Java, **2 spaces**, Google Java Style (`.editorconfig` sets the basics).
- Comments only for the non-obvious *why*. No speculative abstractions.
- Add every player-visible string to **both** `en_us.json` and `de_de.json`.
- Use exact string replacements for bulk renames, then rebuild. Broad regex renames have corrupted
  identifiers in this project before.

---

## Tests

Unit tests live in `src/test/java` (JUnit 5) and run with `./gradlew test` without starting the
game. Gradle's `unitTest` support gives them the Minecraft classes, so simple types such as
`BlockPos`, `Direction` and `Identifier` work.

Write tests for logic that does not need a running game or registries:

| Covered | Class |
|---------|-------|
| config entry parsing and NPC matching | `ConfigEntriesTest` |
| workstation requirement parsing, wildcards | `WorkstationRequirementTest` |
| multiblock geometry | `TradingStationPartTest` |
| variant ids | `StationVariantTest` |

Anything that touches blocks, items, registries, rendering or networking is verified in the game
(see the checklist above). Prefer to move logic into a plain class and test that, instead of
testing around the game.

---

## Assets and generators

Station models, blockstates, loot tables, recipes, the mineable tag and the block lang entries come
from `tools/generate_station_assets.py`; the inventory icons from `tools/generate_station_icons.ps1`.
To add a built-in variant, add it to the generator table, rerun both and commit the output. See the
`station-variants` skill in `.claude/skills` for the details.

---

## Commits, changelog and pull requests

- Small, focused commits. [Conventional Commits](https://www.conventionalcommits.org/) are
  welcome (`feat:`, `fix:`, `docs:`, `chore:`, `ci:`).
- Anything players or modpack makers can notice goes into `CHANGELOG.md` under
  `## [Unreleased]` in the same pull request (the text becomes the release notes).
- Open the pull request against `main`. CI (**Build**) must be green: compile, tests, jar and the
  generated-assets check.
- Describe what you tested in the game. "Not tested in game" is fine, just say so.

---

## Issues and milestones

Planning and status live in [GitHub Issues](https://github.com/dr460neye/easy_trading_station/issues):

- **Milestones** `M1` … `M7` group the work (M7 is the future "catch any NPC" version).
- **Stories** are issues titled `S<milestone>.<n> <name>` with acceptance criteria. Use the *Story*
  template for new ones and the *Bug report* template for defects.
- Label `needs-in-game-test`: built, but not yet confirmed in the game. It stays open until someone
  has checked the criteria in the game.
- Reference issues in commits and pull requests (`Refs #12`, `Closes #12`).

A story is done when: the build is green without new warnings, the acceptance criteria are met
(in-game criteria are confirmed by a person), `en_us` and `de_de` strings exist, and the issue is
closed with a short comment on what changed.

---

## Releases and other Minecraft versions

Maintainers: see [`docs/RELEASING.md`](docs/RELEASING.md) for versioning (SemVer), naming of jars
and tags, the release pipeline (GitHub release + CurseForge) and how other Minecraft, NeoForge and
Java versions are supported (one branch per Minecraft line).

## AI assisted development

`CLAUDE.md` and `.claude/skills/` hold the project notes and workflows used with Claude Code. They
are optional and safe to ignore if you do not use it.
