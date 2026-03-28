package com.xinyihl.functionalstoragelegacy.util;

public class NumberUtils {

    private static final String[] UNITS = {"", "K", "M", "G", "T", "P", "E", "Z", "Y", "R", "Q"};

    public static String formatCompact(long amount) {
        if (amount == 0) return "0";
        int unitIndex = 0;
        double value = amount;
        while (value >= 1000 && unitIndex < UNITS.length - 1) {
            value /= 1000;
            unitIndex++;
        }
        if (unitIndex == 0) return String.valueOf(amount);
        return String.format("%.2f%s", value, UNITS[unitIndex]);
    }

    public static String formatCompactFluid(long amount) {
        return formatCompact(amount) + "B";
    }
}
