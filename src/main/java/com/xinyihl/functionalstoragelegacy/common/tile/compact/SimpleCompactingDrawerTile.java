package com.xinyihl.functionalstoragelegacy.common.tile.compact;

/**
 * TileEntity for simple compacting drawers (2-slot compression storage).
 * A simplified variant of CompactingDrawerTile with only 2 tiers.
 */
public class SimpleCompactingDrawerTile extends CompactingDrawerTile {

    public SimpleCompactingDrawerTile() {
        super(2);
    }

    @Override
    protected int getSlotCount() {
        return 2;
    }
}
