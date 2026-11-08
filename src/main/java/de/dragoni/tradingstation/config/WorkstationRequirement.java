package de.dragoni.tradingstation.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A workstation an NPC needs inside the station before it appears and answers. Config syntax of the
 * part after '=': {@code accepted[|accepted...][;Label]}. Each accepted entry is an item id, an id
 * with '*' wildcards ({@code waystones:*waystone}) or an item tag ({@code #c:chests}).
 */
public record WorkstationRequirement(List<String> accepted, String label) {

  public static WorkstationRequirement parse(String value) {
    String[] labelSplit = value.split(";", 2);
    List<String> accepted = new ArrayList<>();
    for (String token : labelSplit[0].split("\\|")) {
      if (!token.isBlank()) {
        accepted.add(token.strip());
      }
    }
    String label = labelSplit.length > 1 ? labelSplit[1].strip() : "";
    return accepted.isEmpty() ? null : new WorkstationRequirement(accepted, label);
  }

  public boolean matches(ItemStack stack) {
    if (stack.isEmpty()) {
      return false;
    }
    String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    for (String token : accepted) {
      if (token.startsWith("#")) {
        Identifier tag = Identifier.tryParse(token.substring(1));
        if (tag != null && stack.is(TagKey.<Item>create(Registries.ITEM, tag))) {
          return true;
        }
      } else if (matchesId(id, token)) {
        return true;
      }
    }
    return false;
  }

  /** Item id against an accepted entry that is an exact id or contains '*' wildcards. */
  public static boolean matchesId(String id, String token) {
    if (!token.contains("*")) {
      return id.equals(token);
    }
    String regex =
        String.join(
            ".*", java.util.Arrays.stream(token.split("\\*", -1)).map(Pattern::quote).toList());
    return id.matches(regex);
  }

  /** Name shown in the hint: the configured label, else the first concrete item's name. */
  public Component displayName() {
    if (!label.isEmpty()) {
      return Component.literal(label);
    }
    for (String token : accepted) {
      Identifier id = Identifier.tryParse(token);
      if (id != null && !token.startsWith("#") && !token.contains("*")
          && BuiltInRegistries.ITEM.containsKey(id)) {
        return BuiltInRegistries.ITEM.getValue(id).getName(new ItemStack(BuiltInRegistries.ITEM.getValue(id)));
      }
    }
    return Component.literal(accepted.get(0));
  }
}
