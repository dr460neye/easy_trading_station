"""Generates blockstates, block models, loot tables, tags and lang entries for all built-in
station variants. Run from the project root: python tools/generate_station_assets.py
Item icons come from tools/generate_station_icons.ps1."""
import json
import os

NS = "easy_npc_trading_station"
ROOT = os.path.join("src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", NS)
DATA = os.path.join(ROOT, "data", NS)

WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo",
         "crimson", "warped"]
# id -> (planks/base block, futuristic, english name, german name)
VARIANTS = {"futuristic": ("iron_block", True, "Futuristic", "Futuristische"),
            "stone": ("stone", False, "Stone", "Stein")}
NAMES_DE = {"oak": "Eichen", "spruce": "Fichten", "birch": "Birken", "jungle": "Tropenholz",
            "acacia": "Akazien", "dark_oak": "Schwarzeichen", "mangrove": "Mangroven",
            "cherry": "Kirsch", "bamboo": "Bambus", "crimson": "Karmesin", "warped": "Wirrholz"}
for w in WOODS:
    VARIANTS[w] = (w + "_planks", False, w.replace("_", " ").title(), NAMES_DE[w])


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def box(x1, y1, z1, x2, y2, z2, tex):
    face = {"texture": "#" + tex}
    return {"from": [x1, y1, z1], "to": [x2, y2, z2],
            "faces": {d: dict(face) for d in ("north", "south", "east", "west", "up", "down")}}


def wall(x1, y1, z1, x2, y2, z2, solid_band):
    """Glass wall; medieval stations get an opaque lower band of the base material (low cells)."""
    if not solid_band:
        return [box(x1, y1, z1, x2, y2, z2, "glass")]
    band = min(y2, 7)
    out = [box(x1, y1, z1, x2, band, z2, "base")]
    if y2 > band:
        out.append(box(x1, band, z1, x2, y2, z2, "glass"))
    return out


def part_elements(part, futuristic):
    low = part.endswith("low")
    back = part.startswith("back")
    band = low and not futuristic
    yb = 1 if low else 0  # lowest wall y (floor in low cells)
    # side walls run between the outer corner posts; front cells start behind the front posts
    za, zb = (0, 15) if back else (1, 16)
    ez = (15, 16) if back else (0, 1)  # z range of the outer end wall / posts
    el = []
    if low:
        el.append(box(0, 0, 0, 16, 1, 16, "base"))  # floor
    else:
        el.append(box(1, 15, za, 15, 16, zb, "glass"))  # roof glass
    for x in (0, 15):
        el.append(box(x, yb, ez[0], x + 1, 16, ez[1], "base"))  # corner post
        el += wall(x, yb, za, x + 1, 15, zb, band)  # side wall
        el.append(box(x, 15, za, x + 1, 16, zb, "base"))  # rail on top (mid rail / roof edge)
    if back:  # back wall
        el += wall(1, yb, 15, 15, 15, 16, band)
        el.append(box(1, 15, 15, 15, 16, 16, "base"))
    if part == "front_low":  # lower front glass
        el += wall(1, 1, 0, 15, 15, 1, band)
        el.append(box(1, 15, 0, 15, 16, 1, "base"))
    if part == "back_low":
        el.append(box(2, 1, 2, 14, 2, 14, "podium"))  # podium, pressure plate look
    if part == "front_high":
        # frame with a large access window: x 3..13, y 3..12, plus a small counter sill
        el.append(box(1, 0, 0, 15, 3, 1, "base"))
        el.append(box(1, 12, 0, 15, 16, 1, "base"))
        el.append(box(1, 3, 0, 3, 12, 1, "base"))
        el.append(box(13, 3, 0, 15, 12, 1, "base"))
        el.append(box(2, 2, -2, 14, 3, 0, "base"))
    return el


def parent_model(part, futuristic):
    return {"parent": "minecraft:block/block", "textures": {"particle": "#base"},
            "elements": part_elements(part, futuristic)}


def glass_ref(sprite):
    return {"force_translucent": True, "sprite": sprite}


def main():
    parts = ["front_low", "back_low", "front_high", "back_high"]
    for style, fut in (("futuristic", True), ("medieval", False)):
        for part in parts:
            write(os.path.join(ASSETS, "models", "block", f"station_{part}_{style}.json"),
                  parent_model(part, fut))

    en_lang, de_lang = {}, {}
    for vid, (base, fut, en, de) in VARIANTS.items():
        style = "futuristic" if fut else "medieval"
        glass = ("minecraft:block/light_blue_stained_glass" if fut else "minecraft:block/glass")
        podium = "minecraft:block/smooth_stone" if not fut else "minecraft:block/iron_block"
        for part in parts:
            write(os.path.join(ASSETS, "models", "block", f"station_{vid}_{part}.json"),
                  {"parent": f"{NS}:block/station_{part}_{style}",
                   "textures": {"base": f"minecraft:block/{base}", "glass": glass_ref(glass),
                                "podium": podium}})
        variants = {}
        for i, facing in enumerate(("north", "east", "south", "west")):
            for part in parts:
                entry = {"model": f"{NS}:block/station_{vid}_{part}"}
                if i:
                    entry["y"] = 90 * i
                variants[f"facing={facing},part={part}"] = entry
        write(os.path.join(ASSETS, "blockstates", f"station_{vid}.json"), {"variants": variants})

        block_id = f"{NS}:station_{vid}"
        write(os.path.join(DATA, "loot_table", "blocks", f"station_{vid}.json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": block_id}],
                       "conditions": [
                           {"condition": "minecraft:block_state_property", "block": block_id,
                            "properties": {"part": "front_low"}},
                           {"condition": "minecraft:survives_explosion"}]}]})
        en_lang[f"block.{NS}.station_{vid}"] = (
            "Futuristic Trading Station" if fut else f"{en} Trading Station")
        de_lang[f"block.{NS}.station_{vid}"] = (
            "Futuristische Handelsstation" if fut else f"{de}-Handelsstation")

    write(os.path.join(ROOT, "data", "minecraft", "tags", "block", "mineable", "pickaxe.json"),
          {"replace": False, "values": [f"{NS}:station_{v}" for v in VARIANTS]})

    for name, extra in (("en_us", en_lang), ("de_de", de_lang)):
        path = os.path.join(ASSETS, "lang", name + ".json")
        with open(path, encoding="utf-8") as f:
            lang = json.load(f)
        lang = {k: v for k, v in lang.items() if not k.startswith(f"block.{NS}.station_")}
        lang.update(extra)
        write(path, lang)
    print("generated", len(VARIANTS), "variants")


if __name__ == "__main__":
    main()
