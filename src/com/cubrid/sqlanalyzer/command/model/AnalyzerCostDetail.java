package com.cubrid.sqlanalyzer.command.model;

public record AnalyzerCostDetail(String itemName, int count, float unitCost, float totalCost) {
}
