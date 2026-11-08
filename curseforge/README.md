# CurseForge material

Everything needed to fill in the CurseForge project page and to publish files. Nothing in this
folder is part of the mod jar.

| File | Use |
|------|-----|
| [`project-info.md`](project-info.md) | Fields of the CurseForge project form: name, slug, summary, categories, tags, links, dependencies. |
| [`description.md`](description.md) | The long project description (copy into the *Description* editor; Markdown, switch the editor to Markdown or paste as rich text). |
| [`changelog.md`](changelog.md) | Changelog text for each uploaded file. |
| [`images/`](images) | Images for the gallery. Currently the technical sketches; replace with in-game screenshots (see list in `project-info.md`). |

## Publishing checklist

1. `./gradlew build` (JDK 25) and take the jar from `build/libs/`.
2. Set `mod_version` in `gradle.properties` and add the version to `changelog.md`.
3. Create / update the project with the data from `project-info.md`.
4. Upload the jar. Game version **26.1.2**, loader **NeoForge**, Java **25**, relation:
   *Required dependency* Easy NPC.
5. Paste the matching changelog section into the file's changelog field.
6. Add screenshots (cover image, then station variants, menu, strike sign, config example).

## Notes

- License is MIT (`../LICENSE`); pick *MIT License* in the CurseForge license field.
- The mod id stays `easy_npc_trading_station`, the display name is **Easy Trading Station**.
- Keep `description.md` and `../docs/CONFIGURATION.md` in sync; the description only contains the
  short version and links to the full guide on GitHub.
