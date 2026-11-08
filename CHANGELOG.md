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

## [1.0.0] - 2026-11-08

First release for Minecraft 26.1.2, NeoForge 26.1.2.126, Easy NPC 7.18.0+.

### Added
- **NPC Catcher**: capture or clone Easy NPCs, standard villagers (any profession, also from other mods) and wandering traders. Modes `CAPTURE`, `CLONE`, `BOTH`, `DISABLED`, globally, per NPC type and per NPC name.
- **Trading Station**: 1 × 2 × 2 glass booth with podium, access window and workstation spot. 13 styles (futuristic, stone, 11 vanilla woods); stations for the woods of other mods appear automatically.
- **Station menu** (Shift + right-click) with a catcher slot, a workstation slot and an upgrade slot.
- **Station upgrades**: Easy NPCs need the *Advanced NPC Trading Station Upgrade*, wandering traders the *Wandering Trader Upgrade* (new offers once a day). Standard villagers need none. Recipes and the requirement per NPC are configurable; right-click a station with an upgrade to install it.
- Villagers without a profession and nitwits show a sign in the station instead of shaking their head (no session, no block checks). A released villager keeps its profession.
- **Villager workstations from game data**: a villager needs the job site block of its profession (all alternatives count), so modded professions work without any setup. Villagers restock up to twice a day. They are never cloned while Easy Villagers is installed.
- **Per-NPC workstations** for Easy NPCs: `requiredWorkstations` config (match by entity type or name, item ids, wildcards, item tags, label). Missing workstation: strike sign in the station and a chat hint naming the missing block.
- Interaction goes through the normal right-click event, so shop scripts (KubeJS) keep working.
- Destroying the catcher of a captured Easy NPC makes the NPC die at its home position (normal death events). Stations are explosion-proof.
- The catcher respects claims and protection mods (FTB Chunks and others).
- Crafting recipes of the catcher, the stations and the upgrades are set in `easy_npc_trading_station-common.toml`.
- English and German translations.

### Known issues
- Multiplayer and large servers are not tested yet.
- Besides Easy NPCs only standard villagers and wandering traders can be caught; the traders of other mods will follow.

[Unreleased]: https://github.com/dr460neye/easy_trading_station/compare/26.1.2-1.0.0...HEAD
[1.0.0]: https://github.com/dr460neye/easy_trading_station/releases/tag/26.1.2-1.0.0
