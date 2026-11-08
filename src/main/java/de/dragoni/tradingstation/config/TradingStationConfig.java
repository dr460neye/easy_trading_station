package de.dragoni.tradingstation.config;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.ModConfigSpec;

public class TradingStationConfig {

  public static final ModConfigSpec SPEC;

  private static final ModConfigSpec.EnumValue<CatcherMode> DEFAULT_MODE;
  private static final ModConfigSpec.ConfigValue<List<? extends String>> TYPE_OVERRIDES;
  private static final ModConfigSpec.ConfigValue<String> DEFAULT_WORKSTATION;
  private static final ModConfigSpec.ConfigValue<List<? extends String>> WORKSTATION_DEFAULTS;
  private static final ModConfigSpec.ConfigValue<List<? extends String>> WORKSTATION_BY_NAME;
  private static final ModConfigSpec.ConfigValue<List<? extends String>> REQUIRED_WORKSTATIONS;
  private static final ModConfigSpec.BooleanValue UPGRADES_REQUIRED;
  private static final ModConfigSpec.ConfigValue<List<? extends String>> NPC_UPGRADES;

  static {
    ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
    builder.push("catcher");
    DEFAULT_MODE =
        builder
            .comment(
                "Default catcher behaviour: CAPTURE (NPC is removed), CLONE (NPC is copied),",
                "BOTH (capture, clone while sneaking) or DISABLED.")
            .defineEnum("defaultMode", CatcherMode.CAPTURE);
    TYPE_OVERRIDES =
        builder
            .comment(
                "Per NPC overrides in the format 'match=MODE'. match is an entity type id",
                "(contains ':') or part of the NPC name (case-insensitive); the first entry that fits wins.",
                "Example: [\"wayfarer=CAPTURE\", \"jeweler=CLONE\", \"easy_npc:villager=DISABLED\"]")
            .defineListAllowEmpty(
                "typeOverrides", List.of(), () -> "easy_npc:humanoid=CAPTURE", o -> o instanceof String);
    builder.pop();
    builder.push("station");
    DEFAULT_WORKSTATION =
        builder
            .comment(
                "Fallback workstation: every NPC needs this block in the station's workstation slot",
                "unless a more specific entry below applies.")
            .define("defaultWorkstation", "minecraft:crafting_table");
    WORKSTATION_DEFAULTS =
        builder
            .comment(
                "Required workstation per NPC type in the format 'entity_type_id=block_id' (fallback,",
                "used when neither requiredWorkstations nor workstationByName match).",
                "Example: [\"easy_npc:villager=minecraft:lectern\"]")
            .defineListAllowEmpty(
                "workstationDefaults",
                List.of(),
                () -> "easy_npc:humanoid=minecraft:crafting_table",
                o -> o instanceof String);
    WORKSTATION_BY_NAME =
        builder
            .comment(
                "Required workstation by NPC name in the format 'text=block_id' (fallback; prefer",
                "requiredWorkstations). The text is matched case-insensitively against the NPC's name",
                "or translation key and wins over workstationDefaults.")
            .defineListAllowEmpty(
                "workstationByName",
                List.of(),
                () -> "beastmaster=iceandfire:lectern",
                o -> o instanceof String);
    REQUIRED_WORKSTATIONS =
        builder
            .comment(
                "Workstations an NPC NEEDS in the station's workstation slot (highest priority).",
                "Without it the NPC does not react and a strike sign plus a chat hint tells the player",
                "which workstation is missing.",
                "Format: 'match=accepted[|accepted...][;Label]'.",
                "match: an entity type id ('easy_npc:villager') or a text that is matched",
                "case-insensitively against the NPC name / translation key ('wayfarer').",
                "accepted: item id, id with * wildcards ('waystones:*waystone') or item tag ('#c:chests').",
                "Example: [\"wayfarer=waystones:*waystone;Waystone\"]")
            .defineListAllowEmpty(
                "requiredWorkstations",
                List.of(
                    "wayfarer=waystones:*waystone;Waystone",
                    "beastmaster=iceandfire:lectern;Bestiary Lectern"),
                () -> "wayfarer=waystones:*waystone;Waystone",
                o -> o instanceof String);
    builder.pop();
    builder.push("upgrades");
    UPGRADES_REQUIRED =
        builder
            .comment(
                "Stations need an upgrade in the upgrade slot for NPCs that require one.",
                "false: every station takes every NPC (behaviour of version 1.0.0 without upgrades).")
            .define("required", true);
    NPC_UPGRADES =
        builder
            .comment(
                "Upgrade an NPC needs, in the format 'match=upgrade'. match is an entity type id",
                "(contains ':') or part of the NPC name (case-insensitive); the first entry that fits wins.",
                "upgrade is 'advanced' (Advanced NPC Trading Station upgrade), 'wandering' or 'none'.",
                "Without an entry: Easy NPCs and unknown NPCs need 'advanced', minecraft:villager needs",
                "'none', minecraft:wandering_trader needs 'wandering'.",
                "Example: [\"wayfarer=none\", \"easy_npc:villager=none\"]")
            .defineListAllowEmpty(
                "npcUpgrades", List.of(), () -> "wayfarer=none", o -> o instanceof String);
    builder.pop();
    SPEC = builder.build();
  }

  private TradingStationConfig() {}

  public static CatcherMode modeFor(EntityType<?> type, String npcName) {
    String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    return ConfigEntries.modeFor(TYPE_OVERRIDES.get(), id, npcName, DEFAULT_MODE.get());
  }

  /** The upgrade kind this NPC needs ("" = none). Falls back to the defaults if the config is not loaded. */
  public static String requiredUpgrade(EntityType<?> type, String npcName) {
    String typeId = type == null ? null : BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    try {
      if (!UPGRADES_REQUIRED.get()) {
        return UpgradeRules.NONE;
      }
      return UpgradeRules.required(NPC_UPGRADES.get(), typeId, npcName);
    } catch (IllegalStateException notLoaded) {
      return UpgradeRules.required(List.of(), typeId, npcName);
    }
  }

  /** The workstation this NPC needs: a specific entry, else the default block. Server side only. */
  public static WorkstationRequirement requirementFor(EntityType<?> type, String npcName) {
    String typeId = type == null ? null : BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    return ConfigEntries.valueForNpc(REQUIRED_WORKSTATIONS.get(), typeId, npcName)
        .map(WorkstationRequirement::parse)
        .orElseGet(
            () ->
                new WorkstationRequirement(
                    List.of(defaultWorkstationFor(type, npcName).toString()), ""));
  }

  public static Identifier defaultWorkstationFor(EntityType<?> type, String npcName) {
    String typeId = type == null ? null : BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    // by name only: the type id key form of valueForNpc is handled by workstationDefaults below
    return ConfigEntries.valueForNpc(WORKSTATION_BY_NAME.get(), typeId, npcName)
        .map(String::strip)
        .map(Identifier::tryParse)
        .orElseGet(() -> defaultWorkstationFor(type));
  }

  public static Identifier defaultWorkstationFor(EntityType<?> type) {
    String typeId = type == null ? null : BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    return ConfigEntries.valueForKey(WORKSTATION_DEFAULTS.get(), typeId)
        .map(Identifier::tryParse)
        .orElseGet(
            () -> {
              Identifier fallback = Identifier.tryParse(DEFAULT_WORKSTATION.get());
              return fallback != null ? fallback : Identifier.withDefaultNamespace("crafting_table");
            });
  }
}
