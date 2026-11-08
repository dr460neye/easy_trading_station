package de.dragoni.tradingstation.config;

import de.dragoni.tradingstation.TradingStationMod;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.neoforged.neoforge.common.ModConfigSpec;

/** COMMON config. Not loaded yet while registries fill, so only read it lazily. */
public class TradingStationCommonConfig {

  public static final ModConfigSpec SPEC;

  public static final ModConfigSpec.BooleanValue DYNAMIC_RECIPES;
  public static final ModConfigSpec.BooleanValue CATCHER_RECIPE;
  public static final ModConfigSpec.ConfigValue<List<? extends String>> CATCHER_PATTERN;
  public static final ModConfigSpec.ConfigValue<List<? extends String>> CATCHER_KEYS;
  public static final ModConfigSpec.BooleanValue STATION_RECIPES;
  public static final ModConfigSpec.ConfigValue<List<? extends String>> STATION_PATTERN;
  public static final ModConfigSpec.ConfigValue<List<? extends String>> STATION_KEYS;
  public static final ModConfigSpec.ConfigValue<String> FUTURISTIC_BASE;
  public static final ModConfigSpec.BooleanValue ADVANCED_UPGRADE_RECIPE;
  public static final ModConfigSpec.ConfigValue<List<? extends String>> ADVANCED_UPGRADE_PATTERN;
  public static final ModConfigSpec.ConfigValue<List<? extends String>> ADVANCED_UPGRADE_KEYS;

  private static final List<String> DEFAULT_CATCHER_PATTERN = List.of(" SS", "IPS", "I  ");
  private static final List<String> DEFAULT_CATCHER_KEYS =
      List.of("S=minecraft:string", "I=minecraft:iron_ingot", "P=minecraft:ender_pearl");
  private static final List<String> DEFAULT_STATION_PATTERN = List.of("GGG", "EYD", "PPP");
  private static final List<String> DEFAULT_STATION_KEYS =
      List.of(
          "G=minecraft:glass",
          "E=minecraft:emerald",
          "Y=minecraft:ender_eye",
          "D=minecraft:diamond");
  private static final String DEFAULT_FUTURISTIC_BASE = "minecraft:iron_ingot";
  private static final List<String> DEFAULT_ADVANCED_PATTERN = List.of("EBE", "NRG", "HHH");
  private static final List<String> DEFAULT_ADVANCED_KEYS =
      List.of(
          "E=minecraft:emerald",
          "B=minecraft:emerald_block",
          "N=minecraft:netherite_ingot",
          "R=minecraft:redstone_block",
          "G=minecraft:gold_block",
          "H=#minecraft:skulls");

  static {
    ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
    builder.push("variants");
    DYNAMIC_RECIPES =
        builder
            .comment(
                "Generate crafting recipes for automatically detected / configured variants.",
                "Turn off if you add recipes for them yourself. Needs a restart.")
            .define("dynamicRecipes", true);
    builder.pop();

    builder
        .comment(
            "Crafting recipes. Needs a restart (the recipes are written when the game loads).",
            "Pattern: one to three rows of one to three characters, a space is an empty slot.",
            "Keys: \"X=item_id\" or \"X=#item_tag\" for every character used in the pattern.",
            "Invalid values are ignored and the default recipe is used (see latest.log).",
            "Datapacks and KubeJS can still replace the recipes (ids: easy_npc_trading_station:npc_catcher",
            "and easy_npc_trading_station:station_<variant>).")
        .push("recipes");
    CATCHER_RECIPE = builder.comment("Craftable NPC Catcher.").define("catcher", true);
    CATCHER_PATTERN =
        builder
            .comment("Pattern of the NPC Catcher.")
            .defineListAllowEmpty(
                "catcherPattern", DEFAULT_CATCHER_PATTERN, () -> "   ", o -> o instanceof String);
    CATCHER_KEYS =
        builder
            .comment("Keys of the NPC Catcher.")
            .defineListAllowEmpty(
                "catcherKeys", DEFAULT_CATCHER_KEYS, () -> "X=minecraft:stick", o -> o instanceof String);
    STATION_RECIPES =
        builder.comment("Craftable Trading Stations (all variants).").define("stations", true);
    STATION_PATTERN =
        builder
            .comment(
                "Pattern of every Trading Station. The character P is reserved: it is the",
                "material of the variant (planks, stone, ...), see futuristicBase.")
            .defineListAllowEmpty(
                "stationPattern", DEFAULT_STATION_PATTERN, () -> "   ", o -> o instanceof String);
    STATION_KEYS =
        builder
            .comment("Keys of every Trading Station (not P).")
            .defineListAllowEmpty(
                "stationKeys", DEFAULT_STATION_KEYS, () -> "X=minecraft:stick", o -> o instanceof String);
    FUTURISTIC_BASE =
        builder
            .comment("Block used for P in the futuristic station.")
            .define("futuristicBase", DEFAULT_FUTURISTIC_BASE);
    ADVANCED_UPGRADE_RECIPE =
        builder.comment("Craftable Advanced NPC Trading Station upgrade.").define("advancedUpgrade", true);
    ADVANCED_UPGRADE_PATTERN =
        builder
            .comment("Pattern of the Advanced NPC Trading Station upgrade.")
            .defineListAllowEmpty(
                "advancedUpgradePattern", DEFAULT_ADVANCED_PATTERN, () -> "   ", o -> o instanceof String);
    ADVANCED_UPGRADE_KEYS =
        builder
            .comment("Keys of the Advanced NPC Trading Station upgrade (heads: the item tag minecraft:skulls).")
            .defineListAllowEmpty(
                "advancedUpgradeKeys", DEFAULT_ADVANCED_KEYS, () -> "X=minecraft:stick", o -> o instanceof String);
    builder.pop();
    SPEC = builder.build();
  }

  private TradingStationCommonConfig() {}

  public static boolean dynamicRecipes() {
    try {
      return DYNAMIC_RECIPES.get();
    } catch (IllegalStateException notLoaded) {
      return true;
    }
  }

  /** JSON of the catcher recipe from the config (default recipe if invalid); empty if disabled. */
  public static Optional<String> catcherRecipe(String resultId) {
    try {
      if (!CATCHER_RECIPE.get()) {
        return Optional.empty();
      }
      return Optional.of(
          ShapedRecipe.json("equipment", CATCHER_PATTERN.get(), CATCHER_KEYS.get(), Map.of(), resultId)
              .orElseGet(() -> fallback("catcher", "equipment", DEFAULT_CATCHER_PATTERN, DEFAULT_CATCHER_KEYS, Map.of(), resultId)));
    } catch (IllegalStateException notLoaded) {
      return Optional.of(
          fallback("catcher", "equipment", DEFAULT_CATCHER_PATTERN, DEFAULT_CATCHER_KEYS, Map.of(), resultId));
    }
  }

  /** JSON of a station recipe; {@code base} is the block for P. Empty if station recipes are off. */
  public static Optional<String> stationRecipe(String base, boolean futuristic, String resultId) {
    try {
      if (!STATION_RECIPES.get()) {
        return Optional.empty();
      }
      Map<Character, String> fixed = Map.of('P', futuristic ? FUTURISTIC_BASE.get().strip() : base);
      return Optional.of(
          ShapedRecipe.json("building", STATION_PATTERN.get(), STATION_KEYS.get(), fixed, resultId)
              .orElseGet(() -> fallback("station", "building", DEFAULT_STATION_PATTERN, DEFAULT_STATION_KEYS, fixed, resultId)));
    } catch (IllegalStateException notLoaded) {
      Map<Character, String> fixed = Map.of('P', futuristic ? DEFAULT_FUTURISTIC_BASE : base);
      return Optional.of(
          fallback("station", "building", DEFAULT_STATION_PATTERN, DEFAULT_STATION_KEYS, fixed, resultId));
    }
  }

  /** JSON of the recipe of the advanced upgrade; empty if turned off. */
  public static Optional<String> advancedUpgradeRecipe(String resultId) {
    try {
      if (!ADVANCED_UPGRADE_RECIPE.get()) {
        return Optional.empty();
      }
      return Optional.of(
          ShapedRecipe.json("equipment", ADVANCED_UPGRADE_PATTERN.get(), ADVANCED_UPGRADE_KEYS.get(), Map.of(), resultId)
              .orElseGet(() -> fallback("advanced upgrade", "equipment", DEFAULT_ADVANCED_PATTERN, DEFAULT_ADVANCED_KEYS, Map.of(), resultId)));
    } catch (IllegalStateException notLoaded) {
      return Optional.of(
          fallback("advanced upgrade", "equipment", DEFAULT_ADVANCED_PATTERN, DEFAULT_ADVANCED_KEYS, Map.of(), resultId));
    }
  }

  private static String fallback(
      String what,
      String category,
      List<String> pattern,
      List<String> keys,
      Map<Character, String> fixed,
      String resultId) {
    TradingStationMod.LOGGER.warn(
        "The {} recipe in easy_npc_trading_station-common.toml is invalid or not loaded yet, using the default",
        what);
    return ShapedRecipe.json(category, pattern, keys, fixed, resultId).orElseThrow();
  }
}
