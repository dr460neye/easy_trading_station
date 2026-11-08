package de.dragoni.tradingstation.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** COMMON config. Not loaded yet while registries fill, so only read it lazily. */
public class TradingStationCommonConfig {

  public static final ModConfigSpec SPEC;

  public static final ModConfigSpec.BooleanValue DYNAMIC_RECIPES;

  static {
    ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
    builder.push("variants");
    DYNAMIC_RECIPES =
        builder
            .comment("Generate crafting recipes for automatically detected / configured variants.")
            .define("dynamicRecipes", true);
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
}
