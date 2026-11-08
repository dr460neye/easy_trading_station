package de.dragoni.tradingstation.config;

import java.util.Locale;
import java.util.Optional;

/** Pure parsing and matching of the "key=value" list entries used in the server config. */
public final class ConfigEntries {

  private ConfigEntries() {}

  /** Value of the first entry whose key equals {@code key} exactly ("easy_npc:villager=..."). */
  public static Optional<String> valueForKey(Iterable<? extends String> entries, String key) {
    if (key == null) {
      return Optional.empty();
    }
    for (String entry : entries) {
      int split = entry.lastIndexOf('=');
      if (split > 0 && entry.substring(0, split).trim().equals(key)) {
        return Optional.of(entry.substring(split + 1).trim());
      }
    }
    return Optional.empty();
  }

  /**
   * Value of the first entry whose key matches the NPC: a key with a ':' is an entity type id
   * (exact), any other key is a case-insensitive part of the NPC name.
   */
  public static Optional<String> valueForNpc(
      Iterable<? extends String> entries, String typeId, String npcName) {
    for (String entry : entries) {
      int split = entry.indexOf('=');
      if (split > 0 && npcMatches(entry.substring(0, split).strip(), typeId, npcName)) {
        return Optional.of(entry.substring(split + 1));
      }
    }
    return Optional.empty();
  }

  public static boolean npcMatches(String match, String typeId, String npcName) {
    if (match.isEmpty()) {
      return false;
    }
    if (match.contains(":")) {
      return match.equals(typeId);
    }
    String lower = npcName == null ? "" : npcName.toLowerCase(Locale.ROOT);
    return !lower.isEmpty() && lower.contains(match.toLowerCase(Locale.ROOT));
  }

  /** Catcher mode for a type id from "id=MODE" entries, or the fallback if absent or invalid. */
  public static CatcherMode modeFor(
      Iterable<? extends String> entries, String typeId, CatcherMode fallback) {
    return valueForKey(entries, typeId)
        .map(
            value -> {
              try {
                return CatcherMode.valueOf(value.toUpperCase(Locale.ROOT));
              } catch (IllegalArgumentException invalid) {
                return fallback;
              }
            })
        .orElse(fallback);
  }
}
