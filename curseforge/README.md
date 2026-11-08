# CurseForge material

Everything needed to fill in the CurseForge project page and to publish files. Nothing in this
folder is part of the mod jar.

| File | Use |
|------|-----|
| [`project-info.md`](project-info.md) | Fields of the CurseForge project form: name, slug, summary, categories, tags, links, dependencies. |
| [`description.md`](description.md) | The long project description (source text). |
| [`description.html`](description.html) | The same text as HTML, made by `python tools/curseforge_description.py`. CurseForge has no Markdown input: paste it into the *Description* editor in source mode (`<>`). Regenerate after every change of `description.md`. |
| [`../CHANGELOG.md`](../CHANGELOG.md) | The changelog (one section per version); the release pipeline uploads the matching section with each file. |
| [`images/`](images) | Images for the gallery and the description: in-game screenshots (`title-screen.png`, `all_trading_station.png`) and two technical sketches. The list of images that are still missing is in `project-info.md`. |

## Publishing

Normally the **release pipeline** uploads the file (see [`../docs/RELEASING.md`](../docs/RELEASING.md)):
push a tag `<minecraft version>-<mod version>` and GitHub Actions builds, creates the GitHub
release and uploads to CurseForge with the right game version, loader, Java version, release type,
changelog and the optional dependencies *Easy NPC* and *Easy Villagers*.

One-time work on CurseForge:
1. Create the project with the data from `project-info.md` and the text from `description.md`.
2. Copy the project id into the GitHub repository variable `CURSEFORGE_PROJECT_ID` and an API
   token into the secret `CURSEFORGE_TOKEN`.
3. Add screenshots (cover image, then station variants, menu, strike sign, config example).

Manual upload (if you ever need it): take the jar from `build/libs/`, game version **26.1.2**,
loader **NeoForge**, Java **25**, optional dependencies Easy NPC and Easy Villagers, changelog from `../CHANGELOG.md`.

## File naming

| | Pattern | Example |
|-|---------|---------|
| Jar | `easy-trading-station-neoforge-<minecraft>-<version>.jar` | `easy-trading-station-neoforge-26.1.2-1.0.0.jar` |
| File display name | `Easy Trading Station <version> (NeoForge <minecraft>)` | `Easy Trading Station 1.0.0 (NeoForge 26.1.2)` |
| Release type | no suffix: Release, `-beta`/`-rc`: Beta, `-alpha`: Alpha | `1.1.0-beta.1` is a Beta |

## Notes

- License is MIT (`../LICENSE`); pick *MIT License* in the CurseForge license field.
- The mod id stays `easy_npc_trading_station`, the display name is **Easy Trading Station**.
- Keep `description.md` and `../docs/CONFIGURATION.md` in sync; the description only contains the
  short version and links to the full guide on GitHub.
