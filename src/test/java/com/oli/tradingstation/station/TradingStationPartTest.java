package com.oli.tradingstation.station;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class TradingStationPartTest {

  private static final BlockPos ORIGIN = new BlockPos(10, 64, -3);

  @Test
  void originIsRecoveredFromEveryCell() {
    for (Direction facing : Direction.Plane.HORIZONTAL) {
      for (TradingStationPart part : TradingStationPart.values()) {
        BlockPos cell = part.cell(ORIGIN, facing);
        assertEquals(ORIGIN, part.origin(cell, facing), part + " facing " + facing);
      }
    }
  }

  @Test
  void fourDistinctCellsBehindAndAboveTheFront() {
    for (Direction facing : Direction.Plane.HORIZONTAL) {
      Set<BlockPos> cells = new HashSet<>();
      for (TradingStationPart part : TradingStationPart.values()) {
        cells.add(part.cell(ORIGIN, facing));
      }
      assertEquals(4, cells.size());
      assertEquals(ORIGIN, TradingStationPart.FRONT_LOW.cell(ORIGIN, facing));
      assertEquals(ORIGIN.above(), TradingStationPart.FRONT_HIGH.cell(ORIGIN, facing));
      assertEquals(
          ORIGIN.relative(facing.getOpposite()), TradingStationPart.BACK_LOW.cell(ORIGIN, facing));
      assertEquals(
          ORIGIN.relative(facing.getOpposite()).above(),
          TradingStationPart.BACK_HIGH.cell(ORIGIN, facing));
    }
  }

  @Test
  void neighboursAreSymmetricAndPointAtTheRightCell() {
    for (Direction facing : Direction.Plane.HORIZONTAL) {
      for (TradingStationPart part : TradingStationPart.values()) {
        int links = 0;
        for (Direction direction : Direction.values()) {
          TradingStationPart neighbour = part.neighbour(direction, facing);
          if (neighbour == null) {
            continue;
          }
          links++;
          assertEquals(part, neighbour.neighbour(direction.getOpposite(), facing));
          assertEquals(
              part.cell(ORIGIN, facing).relative(direction), neighbour.cell(ORIGIN, facing));
        }
        assertEquals(2, links, part + " must be linked to exactly two other cells");
      }
    }
  }

  @Test
  void cellsOnTheOutsideHaveNoNeighbourThere() {
    assertNull(TradingStationPart.FRONT_LOW.neighbour(Direction.DOWN, Direction.NORTH));
    assertNull(TradingStationPart.BACK_HIGH.neighbour(Direction.UP, Direction.NORTH));
    assertNotNull(TradingStationPart.FRONT_LOW.neighbour(Direction.UP, Direction.NORTH));
  }

  @Test
  void serializedNamesAreStable() {
    assertEquals("front_low", TradingStationPart.FRONT_LOW.getSerializedName());
    assertEquals("back_high", TradingStationPart.BACK_HIGH.getSerializedName());
  }
}
