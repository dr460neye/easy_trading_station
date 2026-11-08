package de.dragoni.tradingstation.station;

import com.mojang.serialization.MapCodec;
import de.dragoni.tradingstation.item.NpcCatcherItem;
import de.dragoni.tradingstation.item.StationUpgradeItem;
import de.dragoni.tradingstation.registry.ModDataComponents;
import de.markusbordihn.easynpc.data.preset.PresetData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TradingStationBlock extends Block implements EntityBlock {

  public static final MapCodec<TradingStationBlock> CODEC = simpleCodec(TradingStationBlock::new);
  public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
  public static final EnumProperty<TradingStationPart> PART =
      EnumProperty.create("part", TradingStationPart.class);

  public TradingStationBlock(Properties properties) {
    super(properties);
    registerDefaultState(
        stateDefinition
            .any()
            .setValue(FACING, Direction.NORTH)
            .setValue(PART, TradingStationPart.FRONT_LOW));
  }

  @Override
  protected MapCodec<? extends Block> codec() {
    return CODEC;
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING, PART);
  }

  // ---- placement ---------------------------------------------------------------------------

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    Direction facing = context.getHorizontalDirection().getOpposite();
    BlockPos origin = context.getClickedPos();
    Level level = context.getLevel();
    for (TradingStationPart part : TradingStationPart.values()) {
      BlockPos cell = part.cell(origin, facing);
      if (!level.isInWorldBounds(cell) || !level.getBlockState(cell).canBeReplaced(context)) {
        if (!level.isClientSide() && context.getPlayer() instanceof ServerPlayer player) {
          player.sendOverlayMessage(
              Component.translatable("message.easy_npc_trading_station.no_space"));
        }
        return null;
      }
    }
    return defaultBlockState()
        .setValue(FACING, facing)
        .setValue(PART, TradingStationPart.FRONT_LOW);
  }

  @Override
  public void setPlacedBy(
      Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    Direction facing = state.getValue(FACING);
    for (TradingStationPart part : TradingStationPart.values()) {
      if (part != TradingStationPart.FRONT_LOW) {
        level.setBlock(part.cell(pos, facing), state.setValue(PART, part), Block.UPDATE_ALL);
      }
    }
  }

  // ---- keeping the four cells together -----------------------------------------------------

  @Override
  protected BlockState updateShape(
      BlockState state,
      LevelReader level,
      ScheduledTickAccess ticks,
      BlockPos pos,
      Direction direction,
      BlockPos neighborPos,
      BlockState neighborState,
      RandomSource random) {
    Direction facing = state.getValue(FACING);
    TradingStationPart expected = state.getValue(PART).neighbour(direction, facing);
    if (expected != null
        && !(neighborState.is(this)
            && neighborState.getValue(PART) == expected
            && neighborState.getValue(FACING) == facing)) {
      return Blocks.AIR.defaultBlockState();
    }
    return state;
  }

  @Override
  public BlockState playerWillDestroy(
      Level level, BlockPos pos, BlockState state, Player player) {
    if (!level.isClientSide()) {
      TradingStationPart part = state.getValue(PART);
      if (part != TradingStationPart.FRONT_LOW && !player.isCreative()) {
        // The station item drops from FRONT_LOW (loot table); removing it cascades to all cells.
        level.destroyBlock(part.origin(pos, state.getValue(FACING)), true);
      }
    }
    return super.playerWillDestroy(level, pos, state, player);
  }

  // ---- block entity ------------------------------------------------------------------------

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return state.getValue(PART) == TradingStationPart.BACK_LOW
        ? new TradingStationBlockEntity(pos, state)
        : null;
  }

  private static TradingStationBlockEntity master(Level level, BlockPos pos, BlockState state) {
    Direction facing = state.getValue(FACING);
    BlockPos origin = state.getValue(PART).origin(pos, facing);
    return level.getBlockEntity(TradingStationPart.BACK_LOW.cell(origin, facing))
            instanceof TradingStationBlockEntity station
        ? station
        : null;
  }

  // ---- shapes ------------------------------------------------------------------------------

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return Shapes.block();
  }

  @Override
  protected VoxelShape getOcclusionShape(BlockState state) {
    return Shapes.empty();
  }

  @Override
  protected boolean propagatesSkylightDown(BlockState state) {
    return true;
  }

  // ---- interaction -------------------------------------------------------------------------

  @Override
  protected InteractionResult useItemOn(
      ItemStack stack,
      BlockState state,
      Level level,
      BlockPos pos,
      Player player,
      InteractionHand hand,
      BlockHitResult hit) {
    TradingStationBlockEntity station = master(level, pos, state);
    if (station == null) {
      return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
    if (player.isShiftKeyDown()) {
      return openMenu(player, station);
    }
    if (stack.getItem() instanceof StationUpgradeItem) {
      return installUpgrade(player, station, stack, level);
    }
    PresetData carried =
        stack.getItem() instanceof NpcCatcherItem ? NpcCatcherItem.getNpc(stack) : null;
    if (carried != null) {
      // quick insert: right-click with a filled catcher
      if (level.isClientSide()) {
        return InteractionResult.SUCCESS;
      }
      if (station.hasNpc()) {
        message(player, "occupied");
        return InteractionResult.FAIL;
      }
      String needed = StationContainer.requiredUpgrade(stack);
      if (!de.dragoni.tradingstation.config.UpgradeRules.satisfied(needed, station.upgradeKind())) {
        if (player instanceof ServerPlayer sp) {
          StationMenu.needsUpgrade(sp, needed);
        }
        return InteractionResult.FAIL;
      }
      station.setNpc(carried);
      NpcCatcherItem.consume(player, stack);
      if (!station.isNpcActive() && player instanceof ServerPlayer sp) {
        StationSessions.sendMissingWorkstation(sp, station);
      }
      return InteractionResult.SUCCESS_SERVER;
    }
    return InteractionResult.TRY_WITH_EMPTY_HAND;
  }

  /** Right-click with an upgrade: it goes into the upgrade slot if that is free. */
  private static InteractionResult installUpgrade(
      Player player, TradingStationBlockEntity station, ItemStack stack, Level level) {
    if (level.isClientSide()) {
      return InteractionResult.SUCCESS;
    }
    if (!station.getUpgrade().isEmpty()) {
      message(player, "upgrade_present");
      return InteractionResult.FAIL;
    }
    String kind = StationUpgradeItem.kindOf(stack);
    if (station.hasNpc()) {
      String needed =
          StationContainer.requiredUpgrade(NpcCatcherItem.filled(station.getNpc()));
      if (!de.dragoni.tradingstation.config.UpgradeRules.satisfied(needed, kind)) {
        message(player, "upgrade_mismatch");
        return InteractionResult.FAIL;
      }
    }
    station.setUpgrade(stack.copyWithCount(1));
    if (!player.getAbilities().instabuild) {
      stack.shrink(1);
    }
    message(player, "upgrade_installed");
    return InteractionResult.SUCCESS_SERVER;
  }

  private static InteractionResult openMenu(Player player, TradingStationBlockEntity station) {
    if (player.level().isClientSide()) {
      return InteractionResult.SUCCESS;
    }
    if (player instanceof ServerPlayer serverPlayer) {
      // a running NPC session of this player ends first, so its NPC is back in the station
      StationSessions.onMenuClosed(serverPlayer);
    }
    player.openMenu(
        new SimpleMenuProvider(
            (id, inventory, p) -> new StationMenu(id, inventory, new StationContainer(station)),
            Component.translatable("container.easy_npc_trading_station.station")));
    return InteractionResult.SUCCESS_SERVER;
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    TradingStationBlockEntity station = master(level, pos, state);
    if (station == null) {
      return InteractionResult.PASS;
    }
    if (level.isClientSide()) {
      return InteractionResult.SUCCESS;
    }
    if (!(player instanceof ServerPlayer serverPlayer)
        || !(level instanceof ServerLevel serverLevel)) {
      return InteractionResult.PASS;
    }
    if (player.isShiftKeyDown()) {
      return openMenu(player, station);
    }
    if (!station.hasNpc()) {
      message(player, "empty");
      return InteractionResult.SUCCESS_SERVER;
    }
    StationSessions.open(serverPlayer, serverLevel, station, state.getValue(FACING), hit);
    return InteractionResult.SUCCESS_SERVER;
  }

  private static void message(Player player, String key) {
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.sendOverlayMessage(
          Component.translatable("message.easy_npc_trading_station." + key));
    }
  }
}
