package com.xinyihl.functionalstoragelegacy.api;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum Attachment implements IStringSerializable {
    WALL("wall", 0),
    FLOOR("floor", 1),
    CEILING("ceiling", 2);

    private final String name;
    private final int index;

    Attachment(String name, int index) {
        this.name = name;
        this.index = index;
    }

    public static Attachment byIndex(int index) {
        for (Attachment attachment : values()) {
            if (attachment.index == index) {
                return attachment;
            }
        }
        return WALL;
    }

    public int getIndex() {
        return index;
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
