package de.dragoni.tradingstation.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/** The four cells of the 1 wide x 2 deep x 2 high station. FRONT is the side the player stands on. */
public enum TradingStationPart implements StringRepresentable {
  FRONT_LOW("front_low", false, false),
  BACK_LOW("back_low", true, false),
  FRONT_HIGH("front_high", false, true),
  BACK_HIGH("back_high", true, true);

  private final String name;
  private final boolean back;
  private final boolean high;

  TradingStationPart(String name, boolean back, boolean high) {
    this.name = name;
    this.back = back;
    this.high = high;
  }

  public boolean isBack() {
    return back;
  }

  public boolean isHigh() {
    return high;
  }

  /** Cell of this part, given the position of the FRONT_LOW cell. */
  public BlockPos cell(BlockPos origin, Direction facing) {
    BlockPos pos = origin;
    if (back) {
      pos = pos.relative(facing.getOpposite());
    }
    if (high) {
      pos = pos.above();
    }
    return pos;
  }

  /** Position of the FRONT_LOW cell, given the position of this part. */
  public BlockPos origin(BlockPos pos, Direction facing) {
    BlockPos origin = pos;
    if (back) {
      origin = origin.relative(facing);
    }
    if (high) {
      origin = origin.below();
    }
    return origin;
  }

  /** The part that lies in the given direction from this one, or null if that cell is not ours. */
  public TradingStationPart neighbour(Direction direction, Direction facing) {
    if (direction == Direction.UP && !high) {
      return of(back, true);
    }
    if (direction == Direction.DOWN && high) {
      return of(back, false);
    }
    if (direction == facing.getOpposite() && !back) {
      return of(true, high);
    }
    if (direction == facing && back) {
      return of(false, high);
    }
    return null;
  }

  private static TradingStationPart of(boolean back, boolean high) {
    for (TradingStationPart part : values()) {
      if (part.back == back && part.high == high) {
        return part;
      }
    }
    throw new IllegalStateException();
  }

  @Override
  public String getSerializedName() {
    return name;
  }
}
