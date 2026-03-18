package com.cubrid.sqlanalyzer.core.engine;

public class AnalyzeStatus {
    private String source;
    private long totalExpCount = 0;
    private long totalImpCount = 0;
    private boolean expDoneFlag = false;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getTotalExpCount() {
        return totalExpCount;
    }

    /**
     * Add total export record count
     *
     * @param totalExpCount to be added
     */
    public void addTotalExpCount(long totalExpCount) {
        this.totalExpCount += totalExpCount;
    }

    public long getTotalImpCount() {
        return totalImpCount;
    }

    /**
     * Add total import record count
     *
     * @param totalImpCount to be added
     */
    public void addTotalImpCount(long totalImpCount) {
        this.totalImpCount += totalImpCount;
    }

    public boolean isExpDoneFlag() {
        return expDoneFlag;
    }

    public void setExpDoneFlag(boolean expDoneFlag) {
        this.expDoneFlag = expDoneFlag;
    }
}
