# Releasing and versioning

- [Naming at a glance](#naming-at-a-glance)
- [Semantic versioning](#semantic-versioning)
- [Releasing step by step](#releasing-step-by-step)
- [What the pipeline does](#what-the-pipeline-does)
- [One-time setup](#one-time-setup)
- [Supporting several Minecraft, NeoForge and Java versions](#supporting-several-minecraft-neoforge-and-java-versions)

---

## Naming at a glance

Example: mod version `1.2.0` for Minecraft `26.1.2` on NeoForge.

| Thing | Pattern | Example |
|-------|---------|---------|
| Mod version | `MAJOR.MINOR.PATCH`; pre-releases `MAJOR.MINOR.PATCH-beta.N` (N = workflow run number) | `1.2.0`, `1.3.0-beta.12` |
| Jar file | `easy-trading-station-neoforge-<minecraft>-<version>.jar` | `easy-trading-station-neoforge-26.1.2-1.2.0.jar` |
| Git tag (created by the workflow) | `<minecraft>-<version>` | `26.1.2-1.2.0`, `26.1.2-1.3.0-beta.12` |
| GitHub release / CurseForge file name | `Easy Trading Station <version> (NeoForge <minecraft>)` | `Easy Trading Station 1.2.0 (NeoForge 26.1.2)` |
| CurseForge release type | from the suffix | no suffix = **Release**, `-beta` = **Beta** |

The jar name follows the usual CurseForge scheme *name – loader – Minecraft version – mod version*.
The Minecraft version is part of the tag and the file name, so the same mod version number can
exist for several Minecraft versions without collisions.

The mod id (`easy_npc_trading_station`) never changes; it is only used inside the game.

---

## Semantic versioning

Counted from the point of view of players and modpack makers:

| Change | Bump | Examples |
|--------|------|----------|
| **MAJOR** | breaking | removed or renamed block/item ids, incompatible config format, saved data that old worlds can not read |
| **MINOR** | new, compatible | new station styles, new config options (with defaults), new features |
| **PATCH** | fixes | bug fixes, performance, translations, docs-only changes that ship |

Pre-releases are `-beta.N` versions made by the pipeline. They are marked *pre-release* on GitHub
and are *Beta* on CurseForge.

Every change that players could notice goes into `CHANGELOG.md` under `## [Unreleased]` in the same
pull request.

---

## Releasing step by step

There are two kinds of builds, both made by GitHub Actions from a pushed commit:

| | Pre-release | Release |
|---|-------------|---------|
| Started | **automatically** on every push to `main` / `mc/*` that changes the mod (not for docs-only or `.github/` changes) | **manually**: Actions → **Release** → Run workflow |
| Version | `<mod_version>-beta.<run number>`, for example `1.1.0-beta.12` | `<mod_version>`, for example `1.1.0` |
| GitHub | pre-release | release (marked latest) |
| CurseForge | never by itself; on request as **Beta** | **always**, as Release |
| Release notes | `[Unreleased]` section of `CHANGELOG.md` | section `[1.1.0]` of `CHANGELOG.md` (the run stops if it is missing or empty) |
| Where the notes go | GitHub: the whole section. CurseForge: short version (opening line + first 8 bullets, link to the GitHub release) | same |

`mod_version` in `gradle.properties` always means *the next stable version*. Pre-releases are
numbered below it (`1.1.0-beta.12` sorts before `1.1.0`).

### Make a release

1. `main` is green (**Build** workflow) and `CHANGELOG.md` → `## [Unreleased]` describes the release.
2. Prepare locally:

   ```powershell
   python tools/prepare_release.py 1.1.0
   ```

   It sets `mod_version`, turns `[Unreleased]` into `[1.1.0] - <date>` and prints the next commands.
   It never pushes.
3. Review, commit and push:

   ```powershell
   git commit -am "chore(release): 1.1.0"
   git push
   ```

4. GitHub → **Actions** → **Release** → *Run workflow* on `main` (or `mc/<version>`), enter `1.1.0`.
   The version has to match `mod_version`; that is a safety check against releasing the wrong state.
   The workflow builds, tests, creates the tag `26.1.2-1.1.0` and the GitHub release, and uploads the
   jar to CurseForge.
5. Set `mod_version` to the next planned version (for example `1.1.1` or `1.2.0`) and push.
   Until you do, automatic pre-releases are skipped with a warning, because `1.1.0` is already released.

### Pre-releases

- Nothing to do: push to `main` and a few minutes later the GitHub pre-release with the jar exists.
- To test a build on CurseForge: Actions → **Pre-release** → *Run workflow*, tick **curseforge**.
  The file appears there as *Beta*.

If a workflow fails before the release exists, fix the problem and run it again. If it failed after
the GitHub release was created (for example the CurseForge upload), delete the release and tag first
(`gh release delete <tag> --cleanup-tag`), otherwise the rerun stops with "Tag already exists".

---

## What the pipeline does

**Build** (`build.yml`, every push and pull request)
1. Reads `java_version` from `gradle.properties` and installs exactly that Temurin JDK.
2. Checks that the generated station assets are up to date
   (`tools/generate_station_assets.py` must not change anything).
3. `./gradlew build`: compile, unit tests, jar.
4. Uploads the jar as a workflow artifact (14 days) and the test report on failure.

**Publish** (`publish.yml`, reusable; called by *Release* and *Pre-release*)
1. Validates the version, refuses an existing tag and, if CurseForge was requested, a missing
   token or project id (no silent skip).
2. Builds and tests with `-Pmod_version=<version>`, checks the jar name.
3. Writes the release notes, creates the tag on the built commit and the **GitHub release**
   with the jar.
4. Uploads the same jar to **CurseForge** (if requested) with game version, loader `neoforge`,
   Java version, release type, changelog and the required dependency *Easy NPC*.

**Release** and **Pre-release** only decide the version and whether CurseForge is used.

---

## One-time setup

| What | Where | Value |
|------|-------|-------|
| CurseForge API token | GitHub → Settings → Secrets and variables → Actions → **Secrets** | `CURSEFORGE_TOKEN` (create at *CurseForge → Account → API tokens*) |
| CurseForge project id | same page → **Variables** | `CURSEFORGE_PROJECT_ID` (number shown on the project page, *About Project*) |
| Tag protection (recommended) | Settings → Rules → Rulesets | protect tags `*-*.*.*` against deletion/rewriting |
| Branch protection (recommended) | Settings → Branches | require the **Build** check for `main` and for `mc/*` |

`GITHUB_TOKEN` is provided automatically. Releases need both CurseForge values; without them the
**Release** workflow stops before it publishes anything.

The CurseForge step uses the community action
[`Kir-Antipov/mc-publish`](https://github.com/Kir-Antipov/mc-publish). If you want it pinned
harder than the `v3.3` tag, replace the tag with the commit hash.

CurseForge project page texts are in [`curseforge/`](../curseforge). Create the project there once,
then the pipeline only uploads files.

---

## Supporting several Minecraft, NeoForge and Java versions

### What is already version-driven

Everything that differs between targets lives in `gradle.properties` and flows from there into
Gradle, the mod metadata, the jar name, CI and the release workflow:

| Property | Used for |
|----------|----------|
| `minecraft_version`, `minecraft_version_range` | Gradle, `neoforge.mods.toml`, jar name, tag, CurseForge game version |
| `neo_version`, `loader_version_range` | NeoForge dependency, mod metadata |
| `java_version` | Gradle toolchain, `setup-java` in the workflows, CurseForge Java tag |
| `easy_npc_version_id`, `easy_npc_version_range` | compile dependency (Modrinth maven version id), required dependency in the mod metadata |
| `mod_version` | next stable version of the mod (SemVer); pre-releases are numbered below it |

### Branch model

One branch per Minecraft line, same pipeline files on every branch:

| Branch | Purpose |
|--------|---------|
| `main` | newest supported Minecraft version (now 26.1.2) |
| `mc/<version>` (for example `mc/1.21.1`) | maintenance branch for an older Minecraft version |

- A new Minecraft version: create the branch from `main` (or promote the new line to `main` and
  move the old one to `mc/<old>`), change the properties above, fix compile errors and run
  the **Release** workflow on that branch (tag `<new minecraft>-<version>`).
- Fixes: land on `main`, then `git cherry-pick -x` to the `mc/*` branches that are still supported.
- Each branch is versioned independently, so `26.1.2-1.3.0` and `1.21.1-1.2.1` are both valid and
  show up as separate CurseForge files of one project.
- Remove support by deleting the branch and noting it in the changelog; old files stay on CurseForge.

Why branches and not one source tree for everything: Minecraft changes the Java API of items,
rendering, networking and registries between versions, so a single tree would need one adapter per
version for almost every class. The effort only pays off with three or more active versions; at that
point a multi-version build tool such as Stonecutter is worth evaluating.

### Keeping the port small

These areas change most between Minecraft versions; keep version specific details inside them and
the rest of the code stays portable:

| Area (where) | Typical differences |
|--------------|---------------------|
| Identifiers (`Identifier` in 26.1, `ResourceLocation` before) | class name |
| Player messages (`sendOverlayMessage`) | method name |
| Block entity data (`saveAdditional(ValueOutput)`) | save/load API |
| Rendering (`client/TradingStationRenderer`) | render state / submit API versus direct buffers |
| Items (`items/*.json` item definitions, `appendHoverText`) | item model files, tooltip signature |
| Registration (`registry/`) | registry event details |
| Easy NPC calls (`station/StationNpc`, `station/StationSessions`, `item/NpcCatcherItem`) | Easy NPC API per version |

Logic that has no Minecraft dependency (`config/ConfigEntries`, `WorkstationRequirement`,
`TradingStationPart`) is covered by unit tests, which makes ports safer: run `./gradlew test` first.

### Java versions

`java_version` is the single switch. When a Minecraft version needs a different Java (for example
21 for 1.21.1), set it on that branch only; the workflows read it from there.
