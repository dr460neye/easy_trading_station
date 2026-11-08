package de.dragoni.tradingstation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConfigEntriesTest {

  @Test
  void valueForKeyNeedsExactKey() {
    List<String> entries =
        List.of("easy_npc:villager=minecraft:lectern", "easy_npc:humanoid = minecraft:anvil");
    assertEquals(
        Optional.of("minecraft:lectern"), ConfigEntries.valueForKey(entries, "easy_npc:villager"));
    assertEquals(
        Optional.of("minecraft:anvil"), ConfigEntries.valueForKey(entries, "easy_npc:humanoid"));
    assertEquals(Optional.empty(), ConfigEntries.valueForKey(entries, "easy_npc"));
    assertEquals(Optional.empty(), ConfigEntries.valueForKey(entries, null));
  }

  @Test
  void malformedEntriesAreIgnored() {
    List<String> entries = List.of("no equals sign", "=novalue-key", "");
    assertEquals(Optional.empty(), ConfigEntries.valueForKey(entries, "no equals sign"));
    assertEquals(Optional.empty(), ConfigEntries.valueForNpc(entries, "easy_npc:humanoid", "Bob"));
  }

  @Test
  void typeIdKeysMatchExactlyAndNameKeysMatchPartsIgnoringCase() {
    assertTrue(ConfigEntries.npcMatches("easy_npc:villager", "easy_npc:villager", "x"));
    assertFalse(ConfigEntries.npcMatches("easy_npc:villager", "easy_npc:humanoid", "villager"));
    assertTrue(
        ConfigEntries.npcMatches("wayfarer", "easy_npc:humanoid", "ftb.shopkeeper.wayfarer"));
    assertTrue(ConfigEntries.npcMatches("WAYFARER", "easy_npc:humanoid", "The Wayfarer"));
    assertFalse(ConfigEntries.npcMatches("wayfarer", "easy_npc:humanoid", "Beastmaster"));
    assertFalse(ConfigEntries.npcMatches("wayfarer", "easy_npc:humanoid", null));
    assertFalse(ConfigEntries.npcMatches("", "easy_npc:humanoid", "anything"));
  }

  @Test
  void firstMatchingEntryWins() {
    List<String> entries =
        List.of(
            "wayfarer=waystones:*waystone;Waystone", "easy_npc:humanoid=minecraft:crafting_table");
    assertEquals(
        Optional.of("waystones:*waystone;Waystone"),
        ConfigEntries.valueForNpc(entries, "easy_npc:humanoid", "The Wayfarer"));
    assertEquals(
        Optional.of("minecraft:crafting_table"),
        ConfigEntries.valueForNpc(entries, "easy_npc:humanoid", "Somebody else"));
    assertEquals(
        Optional.empty(), ConfigEntries.valueForNpc(entries, "easy_npc:villager", "Somebody else"));
  }

  @Test
  void catcherModeFallsBackWhenMissingOrInvalid() {
    List<String> entries = List.of("easy_npc:humanoid=clone", "easy_npc:villager=NOPE");
    assertEquals(
        CatcherMode.CLONE,
        ConfigEntries.modeFor(entries, "easy_npc:humanoid", CatcherMode.CAPTURE));
    assertEquals(
        CatcherMode.CAPTURE,
        ConfigEntries.modeFor(entries, "easy_npc:villager", CatcherMode.CAPTURE));
    assertEquals(
        CatcherMode.BOTH, ConfigEntries.modeFor(entries, "easy_npc:other", CatcherMode.BOTH));
  }
}
