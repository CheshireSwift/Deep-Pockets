package com.rebelkeithy.deeppockets.item;

import net.minecraft.util.IStringSerializable;

import java.util.Locale;

public enum PackTypes implements IStringSerializable {

    BASIC(5),
    ADVANCED(10),
    ;

    private final int slots;
    private final String simpleName;

    PackTypes(int slots) {
        this.slots = slots;
        this.simpleName = name().toLowerCase(Locale.ROOT);
    }

    public int getSlots() {
        return slots;
    }


    @Override
    public String getName() {
        return simpleName;
    }
}
