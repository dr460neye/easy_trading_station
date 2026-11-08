package de.dragoni.tradingstation.station;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class BlockTexturesTest {

  private static Map<String, String> files() {
    return Map.of(
        "assets/animusnv/blockstates/blood_wood_planks.json",
        "{\"variants\":{\"\":{\"model\":\"animusnv:block/blood_wood_planks\"}}}",
        "assets/animusnv/models/block/blood_wood_planks.json",
        "{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\"animusnv:block/bloodwood_planks\"}}",
        "assets/stellaris/blockstates/lunar_planks.json",
        "{\"variants\":{\"\":[{\"model\":\"stellaris:block/lunar_planks\"}]}}",
        "assets/stellaris/models/block/lunar_planks.json",
        "{\"textures\":{\"all\":\"stellaris:block/lunar_forest/lunar_planks\"}}",
        "assets/mod/blockstates/log_like.json",
        "{\"multipart\":[{\"apply\":{\"model\":\"mod:block/log_like\"}}]}",
        "assets/mod/models/block/log_like.json",
        "{\"textures\":{\"end\":\"#top\",\"top\":\"mod:block/log_top\",\"side\":\"mod:block/log_side\"}}");
  }

  private static Identifier texture(String block) {
    var files = files();
    return BlockTextures.resolve(Identifier.parse(block), path -> Optional.ofNullable(files.get(path)));
  }

  @Test
  void followsTheModelToTheRealTexture() {
    assertEquals(Identifier.parse("animusnv:block/bloodwood_planks"), texture("animusnv:blood_wood_planks"));
    assertEquals(
        Identifier.parse("stellaris:block/lunar_forest/lunar_planks"), texture("stellaris:lunar_planks"));
  }

  @Test
  void resolvesTextureReferencesAndMultipartStates() {
    assertEquals(Identifier.parse("mod:block/log_top"), texture("mod:log_like"));
  }

  @Test
  void fallsBackToTheBlockNameWhenNothingIsFound() {
    assertEquals(Identifier.parse("minecraft:block/oak_planks"), texture("minecraft:oak_planks"));
    assertEquals(Identifier.parse("other:block/x_planks"), texture("other:x_planks"));
  }
}
