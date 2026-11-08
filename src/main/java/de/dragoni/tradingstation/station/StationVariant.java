package de.dragoni.tradingstation.station;

import de.dragoni.tradingstation.TradingStationMod;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * One station block. {@code baseBlock} supplies the lower material and its texture; {@code builtIn}
 * variants ship static assets, the others get theirs generated at runtime.
 */
public record StationVariant(String id, Identifier baseBlock, boolean futuristic, boolean builtIn) {

  private static final String[] BUILT_IN_WOODS = {
    "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo",
    "crimson", "warped"
  };

  public String blockName() {
    return "station_" + id;
  }

  public static List<StationVariant> discover() {
    Map<String, StationVariant> variants = new LinkedHashMap<>();
    add(variants, new StationVariant("futuristic", Identifier.withDefaultNamespace("iron_block"), true, true));
    add(variants, new StationVariant("stone", Identifier.withDefaultNamespace("stone"), false, true));
    for (String wood : BUILT_IN_WOODS) {
      add(variants, new StationVariant(wood, Identifier.withDefaultNamespace(wood + "_planks"), false, true));
    }
    try {
      WoodType.values().forEach(type -> addWoodType(variants, type));
    } catch (RuntimeException e) {
      TradingStationMod.LOGGER.warn("Could not enumerate wood types, using config list only", e);
    }
    for (String entry : readExtraVariants()) {
      String[] parts = entry.split("=", 2);
      Identifier block = Identifier.tryParse(parts[0].trim());
      boolean futuristic = parts.length > 1 && parts[1].trim().equalsIgnoreCase("futuristic");
      if (block == null || !BuiltInRegistries.BLOCK.containsKey(block)) {
        TradingStationMod.LOGGER.warn("Ignoring extra station variant '{}': unknown block", entry);
        continue;
      }
      add(variants, new StationVariant(idFor(block), block, futuristic, false));
    }
    return new ArrayList<>(variants.values());
  }

  /**
   * Extra variants live in a plain text file because mod configs are not loaded while registries
   * fill. One entry per line: 'block_id' or 'block_id=style' (medieval|futuristic), '#' = comment.
   */
  private static List<String> readExtraVariants() {
    Path file = Path.of("config", TradingStationMod.MOD_ID + "-extra-variants.txt");
    try {
      if (!Files.exists(file)) {
        Files.createDirectories(file.getParent());
        Files.writeString(
            file,
            String.join(
                System.lineSeparator(),
                "# Additional trading station variants, one per line: block_id or block_id=style",
                "# style is medieval (default) or futuristic. The block supplies the lower part and its texture.",
                "# Modded wood types are detected automatically. Example:",
                "# biomesoplenty:fir_planks",
                "# minecraft:copper_block=futuristic",
                ""));
        return List.of();
      }
      return Files.readAllLines(file).stream()
          .map(String::strip)
          .filter(line -> !line.isEmpty() && !line.startsWith("#"))
          .toList();
    } catch (IOException e) {
      TradingStationMod.LOGGER.warn("Could not read {}", file, e);
      return List.of();
    }
  }

  private static void addWoodType(Map<String, StationVariant> variants, WoodType type) {
    String name = type.name();
    Identifier woodId = Identifier.tryParse(name.contains(":") ? name : "minecraft:" + name);
    if (woodId == null) {
      return;
    }
    Identifier planks = woodId.withPath(woodId.getPath() + "_planks");
    if (!BuiltInRegistries.BLOCK.containsKey(planks)) {
      TradingStationMod.LOGGER.debug("Wood type '{}' has no planks block, no station created", name);
      return;
    }
    add(variants, new StationVariant(idFor(woodId), planks, false, woodId.getNamespace().equals("minecraft")
        && List.of(BUILT_IN_WOODS).contains(woodId.getPath())));
  }

  private static void add(Map<String, StationVariant> variants, StationVariant variant) {
    variants.putIfAbsent(variant.id(), variant);
  }

  static String idFor(Identifier id) {
    String path = id.getPath().replaceAll("[^a-z0-9_]", "_").toLowerCase(Locale.ROOT);
    return id.getNamespace().equals("minecraft") ? path : id.getNamespace() + "_" + path;
  }
}
