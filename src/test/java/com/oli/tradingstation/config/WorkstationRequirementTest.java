package com.oli.tradingstation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WorkstationRequirementTest {

  @Test
  void parsesSeveralAcceptedEntriesAndLabel() {
    WorkstationRequirement r =
        WorkstationRequirement.parse(
            "minecraft:smithing_table|minecraft:anvil;Smithing Table or Anvil");
    assertNotNull(r);
    assertEquals(List.of("minecraft:smithing_table", "minecraft:anvil"), r.accepted());
    assertEquals("Smithing Table or Anvil", r.label());
  }

  @Test
  void labelIsOptional() {
    WorkstationRequirement r = WorkstationRequirement.parse(" iceandfire:lectern ");
    assertNotNull(r);
    assertEquals(List.of("iceandfire:lectern"), r.accepted());
    assertEquals("", r.label());
  }

  @Test
  void emptyAcceptedListIsRejected() {
    assertNull(WorkstationRequirement.parse(""));
    assertNull(WorkstationRequirement.parse(" | ;Label"));
  }

  @Test
  void exactIdsMustMatchCompletely() {
    assertTrue(WorkstationRequirement.matchesId("iceandfire:lectern", "iceandfire:lectern"));
    assertFalse(WorkstationRequirement.matchesId("iceandfire:lectern_2", "iceandfire:lectern"));
  }

  @Test
  void wildcardsMatchAnyPartButOnlyWhereGiven() {
    assertTrue(
        WorkstationRequirement.matchesId("waystones:andesite_waystone", "waystones:*waystone"));
    assertTrue(WorkstationRequirement.matchesId("waystones:waystone", "waystones:*waystone"));
    assertFalse(
        WorkstationRequirement.matchesId("waystones:waystone_scroll", "waystones:*waystone"));
    assertFalse(
        WorkstationRequirement.matchesId("other:andesite_waystone", "waystones:*waystone"));
    assertTrue(WorkstationRequirement.matchesId("mymod:green_station", "mymod:*_station"));
  }

  @Test
  void regexCharactersInTokensAreNotInterpreted() {
    assertFalse(WorkstationRequirement.matchesId("modxid:thing", "mod.id:*"));
    assertTrue(WorkstationRequirement.matchesId("mod.id:thing", "mod.id:*"));
  }
}
