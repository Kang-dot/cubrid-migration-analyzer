package com.cubrid.sqlanalyzer.command.viewmodel;

public record AnalyzerProgressCounts(
        int totalCount,
        int completedCount,
        int succeededCount,
        int failedCount) {

    public static AnalyzerProgressCounts initial(int totalCount) {
        return new AnalyzerProgressCounts(totalCount, 0, 0, 0);
    }
}
