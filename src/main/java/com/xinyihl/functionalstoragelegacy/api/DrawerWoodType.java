package com.xinyihl.functionalstoragelegacy.api;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.Locale;

public enum DrawerWoodType {

    OAK(Blocks.LOG, Blocks.PLANKS, 0),
    SPRUCE(Blocks.LOG, Blocks.PLANKS, 1),
    BIRCH(Blocks.LOG, Blocks.PLANKS, 2),
    JUNGLE(Blocks.LOG, Blocks.PLANKS, 3),
    ACACIA(Blocks.LOG2, Blocks.PLANKS, 4),
    DARK_OAK(Blocks.LOG2, Blocks.PLANKS, 5);

    private final Block log;
    private final Block planks;
    private final int meta;

    DrawerWoodType(Block log, Block planks, int meta) {
        this.log = log;
        this.planks = planks;
        this.meta = meta;
    }

    public Block getLog() {
        return log;
    }

    public Block getPlanks() {
        return planks;
    }

    public int getMeta() {
        return meta;
    }

    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
