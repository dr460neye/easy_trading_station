# Changelog

All notable changes of this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versions follow
[Semantic Versioning](https://semver.org/). Every release is published for one Minecraft version;
the tag is `<minecraft version>-<mod version>` (see [docs/RELEASING.md](docs/RELEASING.md)).

The text of a version section is used as the release notes on GitHub and CurseForge, so write it
for players and modpack makers. GitHub gets the whole section; CurseForge gets a short version: the
opening line of the section plus the first 8 top-level bullets of *Added*, *Changed*, *Fixed*,
*Removed*, *Deprecated* and *Security* (each cut at 160 characters), with a link to the GitHub
release. Put the most important entries first and keep a bullet's first sentence self-contained.

## [Unreleased]

### Added
- **Catcher mode per NPC name**: `typeOverrides` entries now match an NPC name as well as an entity
  type id (`"beastmaster=BOTH"`, `"wayfarer=CAPTURE"`, `"jeweler=CLONE"`). All Easy NPC humanoids
  share one entity type, so names are the way to treat individual shopkeepers differently.

### Fixed
- More modded woods get a station automatically: every planks block in another mod's `minecraft:planks` tag is found, not only registered wood types (for example Menril, Rowan, Alder, Hawthorn, Occultism's Otherworld wood).
- NPCs in claimed or otherwise protected chunks can no longer be caught: the catcher now respects claim and protection mods (such as FTB Chunks).
- Workstations made of two parts (doors, tall plants, beds) are shown completely; they are scaled to stay inside the front cell of the booth.

### Changed
- Recipes are more expensive. Catcher: 3 string, 2 iron ingots, 1 ender pearl. Stations: 3 glass, emerald, eye of ender, diamond and 3 planks or stone (futuristic: 3 iron blocks).
- Station menu: the two slots are stacked with their labels to the right, so the texts no longer overlap.
- When a released NPC can not be spawned, the reason is written to `latest.log`.

## [1.0.0] - 2026-11-08

First release for Minecraft 26.1.2, NeoForge 26.1.2.126, Easy NPC 7.18.0+.

### Added
- **NPC Catcher**: capture or clone Easy NPCs; modes `CAPTURE`, `CLONE`, `BOTH`, `DISABLED`, global and per NPC type.
- **Trading Station**: 1 × 2 × 2 glass booth with podium, access window and workstation spot.
  13 styles: futuristic, stone and all 11 vanilla woods; wood types of other mods and extra blocks
  from a text file get stations automatically.
- Station menu (Shift + right-click) with a catcher slot and a workstation slot.
- Per-NPC workstations: `requiredWorkstations` config (match by entity type or name, item ids,
  wildcards, item tags, label); fallback Crafting Table. Missing workstation: strike sign in the
  station and a chat hint naming the missing block.
- Interaction goes through the normal right-click event, so shop scripts (KubeJS) keep working.
- Destroying the catcher of a captured NPC makes the NPC die at its home position (normal death
  events). Stations are explosion-proof.
- English and German translations, crafting recipes for the catcher and all stations.

### Known issues
- Only Easy NPC entities can be caught.
- Multiplayer and large servers are not tested yet.

[Unreleased]: https://github.com/dr460neye/easy_trading_station/compare/26.1.2-1.0.0...HEAD
[1.0.0]: https://github.com/dr460neye/easy_trading_station/releases/tag/26.1.2-1.0.0
