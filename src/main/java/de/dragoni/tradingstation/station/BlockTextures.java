package de.dragoni.tradingstation.station;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.dragoni.tradingstation.TradingStationMod;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;

/**
 * Finds the texture a block really uses, by following its blockstate to its model. Mods do not
 * always name textures after the block (blood_wood_planks uses bloodwood_planks, lunar_planks sits in
 * a sub folder), so "block/<path>" is only the fallback.
 */
final class BlockTextures {

  private static final List<String> PREFERRED_KEYS = List.of("all", "up", "top", "end", "side", "particle");

  private BlockTextures() {}

  static Identifier of(Identifier block) {
    return resolve(block, BlockTextures::readFromMods);
  }

  static Identifier resolve(Identifier block, Function<String, Optional<String>> reader) {
    Identifier fallback = block.withPath("block/" + block.getPath());
    try {
      Optional<String> state = reader.apply("assets/" + block.getNamespace() + "/blockstates/" + block.getPath() + ".json");
      if (state.isEmpty()) {
        return fallback;
      }
      Identifier model = firstModel(JsonParser.parseString(state.get()).getAsJsonObject());
      Identifier texture = model == null ? null : textureOf(model, reader, 0);
      return texture != null ? texture : fallback;
    } catch (RuntimeException e) {
      TradingStationMod.LOGGER.debug("Could not read the texture of {}, using {}", block, fallback, e);
      return fallback;
    }
  }

  private static Identifier firstModel(JsonObject state) {
    JsonElement entry = null;
    if (state.has("variants")) {
      var variants = state.getAsJsonObject("variants");
      if (!variants.keySet().isEmpty()) {
        entry = variants.get(variants.keySet().iterator().next());
      }
    } else if (state.has("multipart") && !state.getAsJsonArray("multipart").isEmpty()) {
      entry = state.getAsJsonArray("multipart").get(0).getAsJsonObject().get("apply");
    }
    if (entry != null && entry.isJsonArray() && !entry.getAsJsonArray().isEmpty()) {
      entry = entry.getAsJsonArray().get(0);
    }
    if (entry == null || !entry.isJsonObject() || !entry.getAsJsonObject().has("model")) {
      return null;
    }
    return Identifier.tryParse(entry.getAsJsonObject().get("model").getAsString());
  }

  private static Identifier textureOf(Identifier model, Function<String, Optional<String>> reader, int depth) {
    if (depth > 3) {
      return null;
    }
    Optional<String> json = reader.apply("assets/" + model.getNamespace() + "/models/" + model.getPath() + ".json");
    if (json.isEmpty()) {
      return null;
    }
    JsonObject object = JsonParser.parseString(json.get()).getAsJsonObject();
    if (object.has("textures") && !object.getAsJsonObject("textures").keySet().isEmpty()) {
      JsonObject textures = object.getAsJsonObject("textures");
      String key = PREFERRED_KEYS.stream().filter(textures::has).findFirst().orElse(textures.keySet().iterator().next());
      return resolveReference(textures, key, 0);
    }
    if (object.has("parent")) {
      Identifier parent = Identifier.tryParse(object.get("parent").getAsString());
      return parent == null ? null : textureOf(parent, reader, depth + 1);
    }
    return null;
  }

  private static Identifier resolveReference(JsonObject textures, String key, int depth) {
    if (depth > 4 || !textures.has(key) || !textures.get(key).isJsonPrimitive()) {
      return null;
    }
    String value = textures.get(key).getAsString();
    return value.startsWith("#") ? resolveReference(textures, value.substring(1), depth + 1) : Identifier.tryParse(value);
  }

  private static Optional<String> readFromMods(String path) {
    for (var info : ModList.get().getModFiles()) {
      var contents = info.getFile().getContents();
      if (contents.findFile(path).isPresent()) {
        try {
          return Optional.of(new String(contents.readFile(path), StandardCharsets.UTF_8));
        } catch (java.io.IOException | RuntimeException e) {
          return Optional.empty();
        }
      }
    }
    return Optional.empty();
  }
}
