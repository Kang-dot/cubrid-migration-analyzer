package com.cubrid.sqlanalyzer.command;

public class AnalyzerConsoleCostDetail {
    private String itemName;
    private int count;
    private float unitCost;
    private float totalCost;

    public AnalyzerConsoleCostDetail() {
    }

    public AnalyzerConsoleCostDetail(String itemName, int count, float unitCost, float totalCost) {
        this.itemName = itemName;
        this.count = count;
        this.unitCost = unitCost;
        this.totalCost = totalCost;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public float getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(float unitCost) {
        this.unitCost = unitCost;
    }

    public float getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(float totalCost) {
        this.totalCost = totalCost;
    }
}
