package com.cubrid.sqlanalyzer.command;

import java.util.Locale;

public final class AnalyzerCostFormatter {
    public static final float MINUTES_PER_COST_POINT = 5.0f;

    private AnalyzerCostFormatter() {
    }

    public static String formatCost(float cost) {
        return String.format(Locale.US, "%.1f", cost);
    }

    public static String formatCostWithTime(float cost) {
        return formatCost(cost) + " (" + formatMinutes(toMinutes(cost)) + " min)";
    }

    public static float toMinutes(float cost) {
        return cost * MINUTES_PER_COST_POINT;
    }

    private static String formatMinutes(float minutes) {
        return String.format(Locale.US, "%.1f", minutes);
    }
}
