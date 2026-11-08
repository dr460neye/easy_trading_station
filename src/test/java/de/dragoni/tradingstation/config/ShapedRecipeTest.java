package de.dragoni.tradingstation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ShapedRecipeTest {

  private static final String RESULT = "easy_npc_trading_station:station_oak";

  @Test
  void buildsTheRecipeJsonAndAddsTheFixedKey() {
    Optional<String> json =
        ShapedRecipe.json(
            "building",
            List.of("GGG", "EYD", "PPP"),
            List.of("G=minecraft:glass", "E=minecraft:emerald", "Y=minecraft:ender_eye", "D=minecraft:diamond"),
            Map.of('P', "minecraft:oak_planks"),
            RESULT);
    assertTrue(json.isPresent());
    assertEquals(
        "{\"type\":\"minecraft:crafting_shaped\",\"category\":\"building\","
            + "\"pattern\":[\"GGG\",\"EYD\",\"PPP\"],\"key\":{\"G\":\"minecraft:glass\","
            + "\"E\":\"minecraft:emerald\",\"Y\":\"minecraft:ender_eye\",\"D\":\"minecraft:diamond\","
            + "\"P\":\"minecraft:oak_planks\"},\"result\":{\"id\":\"" + RESULT + "\",\"count\":1}}",
        json.get());
  }

  @Test
  void acceptsItemTagsAndEmptySlots() {
    Optional<String> json =
        ShapedRecipe.json("misc", List.of("H H"), List.of("H=#minecraft:skulls"), Map.of(), RESULT);
    assertTrue(json.isPresent());
    assertTrue(json.get().contains("\"pattern\":[\"H H\"]"));
    assertTrue(json.get().contains("\"H\":\"#minecraft:skulls\""));
  }

  @Test
  void fixedKeysWinOverConfiguredKeys() {
    Optional<String> json =
        ShapedRecipe.json("misc", List.of("P"), List.of("P=minecraft:stick"), Map.of('P', "minecraft:stone"), RESULT);
    assertTrue(json.get().contains("\"P\":\"minecraft:stone\""));
  }

  @Test
  void rejectsInvalidInput() {
    List<String> glass = List.of("G=minecraft:glass");
    assertEquals(Optional.empty(), ShapedRecipe.json("misc", List.of(), glass, Map.of(), RESULT));
    assertEquals(Optional.empty(), ShapedRecipe.json("misc", List.of("GGGG"), glass, Map.of(), RESULT));
    assertEquals(Optional.empty(), ShapedRecipe.json("misc", List.of("GG", "G"), glass, Map.of(), RESULT));
    assertEquals(Optional.empty(), ShapedRecipe.json("misc", List.of("GX"), glass, Map.of(), RESULT));
    assertEquals(Optional.empty(), ShapedRecipe.json("misc", List.of("   "), glass, Map.of(), RESULT));
    assertEquals(Optional.empty(), ShapedRecipe.json("misc", List.of("G"), List.of("G=glass"), Map.of(), RESULT));
    assertEquals(Optional.empty(), ShapedRecipe.json("misc", List.of("G"), List.of("Gminecraft:glass"), Map.of(), RESULT));
  }
}
