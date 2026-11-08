package de.dragoni.tradingstation.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Builds the JSON of a shaped crafting recipe from the compact form used in the config. */
public final class ShapedRecipe {

  private static final Pattern INGREDIENT = Pattern.compile("^#?[a-z0-9_.-]+:[a-z0-9_./-]+$");

  private ShapedRecipe() {}

  /**
   * @param pattern one to three rows of one to three characters; a space is an empty slot
   * @param keys entries like {@code "G=minecraft:glass"} or {@code "G=#c:glass_blocks"}
   * @param fixedKeys keys the mod supplies itself (they win over {@code keys})
   * @return the recipe JSON, or empty if the pattern or a key is invalid
   */
  public static Optional<String> json(
      String category,
      List<? extends String> pattern,
      List<? extends String> keys,
      Map<Character, String> fixedKeys,
      String resultId) {
    if (pattern.isEmpty() || pattern.size() > 3) {
      return Optional.empty();
    }
    int width = pattern.get(0).length();
    if (width == 0 || width > 3) {
      return Optional.empty();
    }
    for (String row : pattern) {
      if (row.length() != width) {
        return Optional.empty();
      }
    }
    Map<Character, String> table = new LinkedHashMap<>();
    for (String entry : keys) {
      if (entry.length() < 3 || entry.charAt(1) != '=' || entry.charAt(0) == ' ') {
        return Optional.empty();
      }
      String ingredient = entry.substring(2).strip();
      if (!INGREDIENT.matcher(ingredient).matches()) {
        return Optional.empty();
      }
      table.put(entry.charAt(0), ingredient);
    }
    table.putAll(fixedKeys);

    StringBuilder rows = new StringBuilder();
    Map<Character, String> used = new LinkedHashMap<>();
    for (String row : pattern) {
      if (rows.length() > 0) {
        rows.append(',');
      }
      rows.append('"');
      for (char c : row.toCharArray()) {
        if (c == '"' || c == '\\') {
          return Optional.empty();
        }
        rows.append(c);
        if (c != ' ') {
          String ingredient = table.get(c);
          if (ingredient == null) {
            return Optional.empty();
          }
          used.put(c, ingredient);
        }
      }
      rows.append('"');
    }
    if (used.isEmpty()) {
      return Optional.empty();
    }
    StringBuilder key = new StringBuilder();
    used.forEach(
        (c, ingredient) -> {
          if (key.length() > 0) {
            key.append(',');
          }
          key.append('"').append(c).append("\":\"").append(ingredient).append('"');
        });
    return Optional.of(
        "{\"type\":\"minecraft:crafting_shaped\",\"category\":\"" + category + "\",\"pattern\":["
            + rows + "],\"key\":{" + key + "},\"result\":{\"id\":\"" + resultId + "\",\"count\":1}}");
  }
}
