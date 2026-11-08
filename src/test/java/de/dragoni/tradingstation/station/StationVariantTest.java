package de.dragoni.tradingstation.station;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class StationVariantTest {

  @Test
  void vanillaIdsKeepTheirPath() {
    assertEquals("oak", StationVariant.idFor(Identifier.fromNamespaceAndPath("minecraft", "oak")));
  }

  @Test
  void moddedIdsGetTheNamespaceAsPrefix() {
    assertEquals(
        "biomesoplenty_fir",
        StationVariant.idFor(Identifier.fromNamespaceAndPath("biomesoplenty", "fir")));
  }

  @Test
  void pathSeparatorsBecomeUnderscores() {
    assertEquals(
        "mymod_wood_dark",
        StationVariant.idFor(Identifier.fromNamespaceAndPath("mymod", "wood/dark")));
  }

  @Test
  void blockNameIsPrefixed() {
    assertEquals("station_stone", new StationVariant("stone", null, false, true).blockName());
  }

  @Test
  void planksTagListsBlocksAndSkipsTagReferences() {
    String json =
        "{\"replace\": false, \"values\": [\"occultism:otherplanks\", \"#c:planks\","
            + " {\"id\": \"witchery:rowan_planks\", \"required\": false}]}";
    assertEquals(
        java.util.List.of(
            net.minecraft.resources.Identifier.parse("occultism:otherplanks"),
            net.minecraft.resources.Identifier.parse("witchery:rowan_planks")),
        StationVariant.parsePlanksTag(json));
  }
}
