package de.dragoni.tradingstation.station;

import de.dragoni.tradingstation.TradingStationMod;
import de.dragoni.tradingstation.config.TradingStationCommonConfig;
import de.dragoni.tradingstation.registry.ModBlocks;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * Story S6.10: stations for modded wood types / configured blocks have no static assets. This pack
 * writes them to a folder (once, lazily, after all blocks are registered) and serves them as an
 * always-active resource and data pack. Models reuse the base block's own texture.
 */
@EventBusSubscriber(modid = TradingStationMod.MOD_ID)
public class GeneratedStationPack {

  private static final String NS = TradingStationMod.MOD_ID;
  private static final Path DIR = Path.of("config", NS + "_generated_pack");
  private static final String[] PARTS = {"front_low", "back_low", "front_high", "back_high"};
  private static boolean generated;

  private GeneratedStationPack() {}

  @SubscribeEvent
  public static void addPack(AddPackFindersEvent event) {
    PackType type = event.getPackType();
    PackLocationInfo info =
        new PackLocationInfo(
            NS + "_generated",
            Component.literal("Trading Station (generated variants)"),
            PackSource.BUILT_IN,
            Optional.empty());
    Pack.ResourcesSupplier supplier =
        new Pack.ResourcesSupplier() {
          @Override
          public PackResources openPrimary(PackLocationInfo location) {
            return new LazyPack(location);
          }

          @Override
          public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
            return new LazyPack(location);
          }
        };
    try {
      Files.createDirectories(DIR);
      Files.writeString(
          DIR.resolve("pack.mcmeta"),
          "{\"pack\":{\"description\":\"Trading Station generated variants\","
              + "\"min_format\":[101,0],\"max_format\":[101,2147483647]}}",
          StandardCharsets.UTF_8);
      Pack pack =
          Pack.readMetaAndCreate(
              info, supplier, type, new PackSelectionConfig(true, Pack.Position.TOP, false));
      if (pack != null) {
        event.addRepositorySource((Consumer<Pack> consumer) -> consumer.accept(pack));
      }
    } catch (IOException | RuntimeException e) {
      TradingStationMod.LOGGER.warn("Generated station pack unavailable, modded variants have no models", e);
    }
  }

  /** Generates the files on first access, i.e. when resources are actually loaded. */
  private static final class LazyPack extends PathPackResources {

    LazyPack(PackLocationInfo location) {
      super(location, DIR);
    }

    @Override
    public IoSupplier<java.io.InputStream> getResource(PackType type, net.minecraft.resources.Identifier id) {
      ensure();
      return super.getResource(type, id);
    }

    @Override
    public void listResources(
        PackType type, String namespace, String path, PackResources.ResourceOutput output) {
      ensure();
      super.listResources(type, namespace, path, output);
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
      ensure();
      return super.getNamespaces(type);
    }
  }

  private static synchronized void ensure() {
    if (generated || ModBlocks.STATIONS.isEmpty()) {
      return;
    }
    generated = true;
    try {
      for (String sub : new String[] {"assets", "data"}) {
        Path path = DIR.resolve(sub);
        if (Files.exists(path)) {
          try (var walk = Files.walk(path)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
              Files.delete(p);
            }
          }
        }
      }
      List<String> pickaxe = new ArrayList<>();
      StringBuilder lang = new StringBuilder("{\n");
      boolean any = false;
      for (StationVariant variant : ModBlocks.STATIONS.keySet()) {
        if (variant.builtIn()) {
          continue;
        }
        any = true;
        write(variant);
        pickaxe.add("\"" + NS + ":" + variant.blockName() + "\"");
        lang.append("  \"block.")
            .append(NS)
            .append('.')
            .append(variant.blockName())
            .append("\": \"")
            .append(displayName(variant))
            .append("\",\n");
      }
      if (any) {
        put("data/minecraft/tags/block/mineable/pickaxe.json",
            "{\"replace\":false,\"values\":[" + String.join(",", pickaxe) + "]}");
        String body = lang.toString();
        put("assets/" + NS + "/lang/en_us.json", body.substring(0, body.length() - 2) + "\n}");
      }
    } catch (IOException e) {
      TradingStationMod.LOGGER.warn("Could not generate station assets for modded variants", e);
    }
  }

  private static String displayName(StationVariant variant) {
    String path = variant.baseBlock().getPath().replaceAll("_?planks$", "");
    StringBuilder name = new StringBuilder();
    for (String word : path.split("_")) {
      if (!word.isEmpty()) {
        name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
      }
    }
    return name.toString().strip().replace("\"", "") + " Trading Station";
  }

  private static void write(StationVariant variant) throws IOException {
    String id = variant.id();
    String style = variant.futuristic() ? "futuristic" : "medieval";
    String base = BlockTextures.of(variant.baseBlock()).toString();
    String glass =
        variant.futuristic() ? "minecraft:block/light_blue_stained_glass" : "minecraft:block/glass";
    String podium = variant.futuristic() ? "minecraft:block/iron_block" : "minecraft:block/smooth_stone";
    String blockId = NS + ":" + variant.blockName();
    StringBuilder states = new StringBuilder("{\"variants\":{");
    String[] facings = {"north", "east", "south", "west"};
    boolean first = true;
    for (int i = 0; i < 4; i++) {
      for (String part : PARTS) {
        if (!first) {
          states.append(',');
        }
        first = false;
        states.append("\"facing=").append(facings[i]).append(",part=").append(part).append("\":{");
        states.append("\"model\":\"").append(NS).append(":block/station_").append(id).append('_').append(part).append('"');
        if (i > 0) {
          states.append(",\"y\":").append(90 * i);
        }
        states.append('}');
      }
    }
    states.append("}}");
    put("assets/" + NS + "/blockstates/station_" + id + ".json", states.toString());
    for (String part : PARTS) {
      put(
          "assets/" + NS + "/models/block/station_" + id + "_" + part + ".json",
          "{\"parent\":\"" + NS + ":block/station_" + part + "_" + style + "\",\"textures\":{"
              + "\"base\":\"" + base + "\","
              + "\"glass\":{\"force_translucent\":true,\"sprite\":\"" + glass + "\"},"
              + "\"podium\":\"" + podium + "\"}}");
    }
    put(
        "assets/" + NS + "/items/station_" + id + ".json",
        "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"" + NS + ":block/station_" + id + "_front_low\"}}");
    put(
        "data/" + NS + "/loot_table/blocks/station_" + id + ".json",
        "{\"type\":\"minecraft:block\",\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\","
            + "\"name\":\"" + blockId + "\"}],\"conditions\":[{\"condition\":\"minecraft:block_state_property\","
            + "\"block\":\"" + blockId + "\",\"properties\":{\"part\":\"front_low\"}},"
            + "{\"condition\":\"minecraft:survives_explosion\"}]}]}");
    if (TradingStationCommonConfig.dynamicRecipes()) {
      // same recipe as the built-in stations: glass on top, valuables in the middle, base below
      String key =
          "{\"G\":\"minecraft:glass\",\"E\":\"minecraft:emerald\",\"Y\":\"minecraft:ender_eye\","
              + "\"D\":\"minecraft:diamond\",\"P\":\""
              + (variant.futuristic() ? "minecraft:iron_block" : variant.baseBlock())
              + "\"}";
      String pattern = "[\"GGG\",\"EYD\",\"PPP\"]";
      put(
          "data/" + NS + "/recipe/station_" + id + ".json",
          "{\"type\":\"minecraft:crafting_shaped\",\"category\":\"building\",\"pattern\":" + pattern
              + ",\"key\":" + key + ",\"result\":{\"id\":\"" + blockId + "\",\"count\":1}}");
    }
    TradingStationMod.LOGGER.info(
        "Generated assets for station variant '{}' (texture {})", id, base.toLowerCase(Locale.ROOT));
  }

  private static void put(String relative, String content) throws IOException {
    Path file = DIR.resolve(relative);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content, StandardCharsets.UTF_8);
  }
}
