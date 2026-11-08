package de.dragoni.tradingstation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class UpgradeRulesTest {

  @Test
  void defaultsByEntityType() {
    assertEquals(UpgradeRules.NONE, UpgradeRules.required(List.of(), "minecraft:villager", "Farmer"));
    assertEquals(UpgradeRules.WANDERING, UpgradeRules.required(List.of(), "minecraft:wandering_trader", ""));
    assertEquals(UpgradeRules.ADVANCED, UpgradeRules.required(List.of(), "easy_npc:humanoid", "Wayfarer"));
    assertEquals(UpgradeRules.ADVANCED, UpgradeRules.required(List.of(), "somemod:trader", ""));
  }

  @Test
  void configEntriesWinByTypeOrName() {
    List<String> entries = List.of("wayfarer = none", "easy_npc:villager=WANDERING", "jeweler=advanced");
    assertEquals(UpgradeRules.NONE, UpgradeRules.required(entries, "easy_npc:humanoid", "The Wayfarer"));
    assertEquals(UpgradeRules.WANDERING, UpgradeRules.required(entries, "easy_npc:villager", "Anyone"));
    assertEquals(UpgradeRules.ADVANCED, UpgradeRules.required(entries, "easy_npc:humanoid", "Royal Jeweler"));
    assertEquals(UpgradeRules.ADVANCED, UpgradeRules.required(entries, "easy_npc:humanoid", "Blacksmith"));
  }

  @Test
  void satisfiedNeedsTheMatchingKindOrNothing() {
    assertTrue(UpgradeRules.satisfied(UpgradeRules.NONE, ""));
    assertTrue(UpgradeRules.satisfied(UpgradeRules.NONE, UpgradeRules.ADVANCED));
    assertTrue(UpgradeRules.satisfied(UpgradeRules.ADVANCED, UpgradeRules.ADVANCED));
    assertFalse(UpgradeRules.satisfied(UpgradeRules.ADVANCED, ""));
    assertFalse(UpgradeRules.satisfied(UpgradeRules.ADVANCED, UpgradeRules.WANDERING));
  }
}
