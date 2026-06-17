package com.cubrid.sqlanalyzer.command.viewmodel;

public record AnalyzerProgressObjectCount(
        String objectType,
        int totalCount,
        int succeededCount,
        int failedCount) {
}
