# Easy Trading Station

**An addon for [Easy NPC](https://modrinth.com/mod/easy-npc).** Catch an Easy NPC with the
**NPC Catcher** and keep it in a glass **Trading Station**: shops, dialogs and scripts keep working,
the NPC stands still and costs (almost) no performance.

*Minecraft 26.1.2 · NeoForge · requires Easy NPC 7.18.0+*

---

## Why

Villagers and NPCs that walk around, tick and wander off are expensive and annoying in bases. The
Trading Station gives every NPC a place of its own, like a shop booth, without a ticking entity
in your world. Perfect for trading halls and for modpacks with quest or shop NPCs.

## What you get

- **NPC Catcher**: capture or clone an Easy NPC. The mode is configurable globally and per NPC type.
- **Trading Station**: a 1 × 2 × 2 glass booth with a podium for the NPC, an access window and a
  workstation in front. Shift + right-click opens a menu with a slot for the catcher and a slot for
  the workstation.
- **Everything of the NPC keeps working**: trades, dialogs, actions, click handlers of other mods
  (for example KubeJS shops).
- **Almost no cost**: the station only stores data. The NPC exists in the world only while somebody
  talks to it; the client draws a cached, non-ticking copy.
- **Workstations**: every NPC needs its workstation in the station. Modpack makers decide which
  one per NPC (*the Wayfarer needs a Waystone*). If it is missing, the NPC puts up a strike sign
  and the player gets a hint which workstation is missing.
- **Many styles**: a futuristic stasis-field station, stone, and every wood type. Wood types from
  other mods get a station automatically.
- **Safe for scripted NPCs**: if the catcher of a captured NPC is destroyed, the NPC *dies at
  its home position*, so respawn logic of the modpack keeps working. Stations survive explosions.

## How to use

1. Craft an **NPC Catcher** (3 string, 2 iron ingots, 1 ender pearl) and right-click an Easy NPC.
2. Craft and place a **Trading Station** (3 glass, emerald, eye of ender, diamond and 3 planks / stone; the futuristic one uses 3 iron
   blocks instead).
3. Right-click the station with the filled catcher, or Shift + right-click for the menu.
4. Put the NPC's workstation into the workstation slot.
5. Right-click the station to trade or talk.

## For modpack makers

The workstation for each NPC is a plain config entry:

```toml
[station]
  requiredWorkstations = [
    "wayfarer=waystones:*waystone;Waystone",
    "beastmaster=iceandfire:lectern;Bestiary Lectern"
  ]
```

Match by NPC type or by name, accept item ids, wildcards or tags, add a label for the hint. NPCs
without an entry use a fallback (Crafting Table by default). The catcher mode can be set per NPC type
(capture, clone, both, disabled). Full guide:
**[Configuration guide](https://github.com/dr460neye/easy_trading_station/blob/main/docs/CONFIGURATION.md)**

## Good to know

- Only Easy NPC entities can be caught right now; other mobs are planned.
- Needs to be installed on server and client.
- Licensed under MIT. Source code and issue tracker:
  [github.com/dr460neye/easy_trading_station](https://github.com/dr460neye/easy_trading_station)
