package de.dragoni.tradingstation.config;

import java.util.Locale;

/**
 * Which station upgrade an NPC needs. Pure logic, no Minecraft classes, so it can be unit tested.
 * A station has one upgrade slot; the installed upgrade decides what the station can host.
 */
public final class UpgradeRules {

  /** Upgrade for Easy NPCs. */
  public static final String ADVANCED = "advanced";

  /** Upgrade for wandering traders (comes with the standard villager support). */
  public static final String WANDERING = "wandering";

  /** No upgrade needed. */
  public static final String NONE = "";

  private UpgradeRules() {}

  /**
   * The upgrade kind the NPC needs: the first config entry that fits (entity type id or part of the
   * name, "none" means no upgrade), else the default for the entity type.
   *
   * @return {@link #NONE} (empty string) if no upgrade is needed
   */
  public static String required(
      Iterable<? extends String> entries, String typeId, String npcName) {
    return ConfigEntries.valueForNpc(entries, typeId, npcName)
        .map(value -> value.strip().toLowerCase(Locale.ROOT))
        .map(value -> value.equals("none") ? NONE : value)
        .orElseGet(() -> defaultFor(typeId));
  }

  /** Standard villagers need nothing, wandering traders the wandering upgrade, all others advanced. */
  public static String defaultFor(String typeId) {
    if ("minecraft:villager".equals(typeId)) {
      return NONE;
    }
    if ("minecraft:wandering_trader".equals(typeId)) {
      return WANDERING;
    }
    return ADVANCED;
  }

  /** True if the installed upgrade kind fulfils what the NPC needs. */
  public static boolean satisfied(String required, String installed) {
    return required.isEmpty() || required.equals(installed);
  }
}
