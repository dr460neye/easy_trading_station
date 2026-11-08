package de.dragoni.tradingstation.item;

import de.dragoni.tradingstation.TradingStationMod;
import de.dragoni.tradingstation.config.CatcherMode;
import de.dragoni.tradingstation.config.TradingStationConfig;
import de.dragoni.tradingstation.registry.ModDataComponents;
import de.dragoni.tradingstation.registry.ModItems;
import de.dragoni.tradingstation.station.NpcOrigin;
import de.markusbordihn.easynpc.data.preset.PresetData;
import de.markusbordihn.easynpc.data.preset.PresetDataUtils;
import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;
import de.markusbordihn.easynpc.security.SecurityManager;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class NpcCatcherItem extends Item {

  public NpcCatcherItem(Properties properties) {
    super(properties);
  }

  public static boolean isFilled(ItemStack stack) {
    return stack.has(ModDataComponents.CAPTURED_NPC.get());
  }

  public static ItemStack filled(PresetData data) {
    ItemStack stack = new ItemStack(ModItems.NPC_CATCHER.get());
    stack.set(ModDataComponents.CAPTURED_NPC.get(), data);
    return stack;
  }

  public static PresetData getNpc(ItemStack stack) {
    return stack.get(ModDataComponents.CAPTURED_NPC.get());
  }

  /** Server side: captures or clones the target into the catcher held in the given hand. */
  public static InteractionResult capture(ServerPlayer player, InteractionHand hand, Entity target) {
    if (!(target instanceof EasyNPC<?> npc)) {
      return InteractionResult.PASS;
    }
    if (isProtected(player, target)) {
      player.sendOverlayMessage(Component.translatable("message.easy_npc_trading_station.no_access"));
      return InteractionResult.FAIL;
    }
    PresetData data = PresetData.get(PresetDataUtils.toItemStack(npc));
    String name = data == null ? "" : npcName(data);
    CatcherMode mode = TradingStationConfig.modeFor(target.getType(), name);
    TradingStationMod.LOGGER.info("Capture: type={} name='{}' mode={}", target.getType(), name, mode);
    if (mode == CatcherMode.DISABLED) {
      player.sendOverlayMessage(Component.translatable("message.easy_npc_trading_station.disabled"));
      return InteractionResult.FAIL;
    }
    var decision = SecurityManager.checkAccess(player, npc);
    TradingStationMod.LOGGER.info("Capture access: allowed={} reason={}", decision.allowed(), decision.reason());
    if (!decision.allowed()) {
      player.sendOverlayMessage(Component.translatable("message.easy_npc_trading_station.no_access"));
      return InteractionResult.FAIL;
    }
    TradingStationMod.LOGGER.info("Capture preset: present={} valid={}", data != null, data != null && data.hasValidData());
    if (data == null || !data.hasValidData()) {
      player.sendOverlayMessage(Component.translatable("message.easy_npc_trading_station.failed"));
      return InteractionResult.FAIL;
    }
    boolean clone = mode == CatcherMode.CLONE || (mode == CatcherMode.BOTH && player.isShiftKeyDown());
    if (!clone) {
      // remember the home so the NPC can "die" there if this catcher is destroyed
      BlockPos home =
          npc.getEasyNPCNavigationData().hasNPCHomePosition()
              ? npc.getEasyNPCNavigationData().getNPCHomePosition()
              : target.blockPosition();
      data = NpcOrigin.mark(data, target.level(), home);
    }
    ItemStack filled = player.getItemInHand(hand).copyWithCount(1);
    filled.set(ModDataComponents.CAPTURED_NPC.get(), data);
    player.setItemInHand(hand, filled);

    if (!clone) {
      target.discard();
    }
    return InteractionResult.SUCCESS;
  }

  /**
   * Asks claim and protection mods whether this player may change things where the NPC stands, by
   * posting the break-block event for that position (the same question they answer for breaking
   * blocks; the event is only asked, nothing is broken).
   */
  private static boolean isProtected(ServerPlayer player, Entity target) {
    var level = target.level();
    BlockPos pos = target.blockPosition();
    var event = new BreakBlockEvent(level, pos, level.getBlockState(pos), player);
    return NeoForge.EVENT_BUS.post(event).isCanceled();
  }

  /** The catcher is used up once its NPC left it (creative keeps an empty one). */
  public static void consume(Player player, ItemStack stack) {
    if (player.getAbilities().instabuild) {
      stack.remove(ModDataComponents.CAPTURED_NPC.get());
    } else {
      stack.shrink(1);
    }
  }

  /** Releases the stored NPC next to the clicked block. */
  @Override
  public InteractionResult useOn(UseOnContext context) {
    Level level = context.getLevel();
    ItemStack stack = context.getItemInHand();
    PresetData data = getNpc(stack);
    if (data == null) {
      return InteractionResult.PASS;
    }
    if (level.isClientSide() || !(context.getPlayer() instanceof ServerPlayer player)) {
      return InteractionResult.SUCCESS;
    }
    var pos = context.getClickedPos().relative(context.getClickedFace());
    if (PresetDataUtils.spawnEntity(data, level, pos, player)) {
      consume(player, stack);
      return InteractionResult.CONSUME;
    }
    TradingStationMod.LOGGER.warn(
        "Release failed: type={} name='{}' valid={} at {}",
        data.entityType(),
        npcName(data),
        data.hasValidData(),
        pos);
    player.sendOverlayMessage(Component.translatable("message.easy_npc_trading_station.failed"));
    return InteractionResult.FAIL;
  }

  /** Plain text of the NPC's name (or its translation key); empty if unnamed. */
  public static String npcName(PresetData data) {
    Component name = customName(data.data(), 0);
    if (name == null) {
      return "";
    }
    if (name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) {
      return t.getKey() + " " + name.getString();
    }
    return name.getString();
  }

  /** The NPC's own name is stored somewhere inside the preset NBT as "CustomName". */
  private static Component customName(net.minecraft.nbt.CompoundTag tag, int depth) {
    if (tag.contains("CustomName")) {
      var parsed = tag.read("CustomName", net.minecraft.network.chat.ComponentSerialization.CODEC);
      if (parsed.isPresent()) {
        return parsed.get();
      }
    }
    if (depth < 3) {
      for (String key : tag.keySet()) {
        if (tag.get(key) instanceof net.minecraft.nbt.CompoundTag child) {
          Component found = customName(child, depth + 1);
          if (found != null) {
            return found;
          }
        }
      }
    }
    return null;
  }

  /** Lets Shift + right-click with a catcher in hand reach the station menu. */
  @Override
  public boolean doesSneakBypassUse(
      ItemStack stack,
      net.minecraft.world.level.LevelReader level,
      net.minecraft.core.BlockPos pos,
      Player player) {
    return level.getBlockState(pos).getBlock()
        instanceof de.dragoni.tradingstation.station.TradingStationBlock;
  }

  @Override
  public boolean isFoil(ItemStack stack) {
    return isFilled(stack);
  }

  @Override
  public void appendHoverText(
      ItemStack stack,
      TooltipContext context,
      TooltipDisplay display,
      Consumer<Component> tooltip,
      TooltipFlag flag) {
    PresetData data = getNpc(stack);
    if (data == null) {
      tooltip.accept(Component.translatable("item.easy_npc_trading_station.npc_catcher.empty"));
    } else {
      Component type = data.entityType().getDescription();
      Component name = customName(data.data(), 0);
      if (name != null && !name.getString().isBlank()) {
        tooltip.accept(name);
        tooltip.accept(type.copy().withStyle(net.minecraft.ChatFormatting.GRAY));
      } else {
        tooltip.accept(type);
      }
    }
  }
}
