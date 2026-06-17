package com.cubrid.sqlanalyzer.command.viewmodel;

public record AnalyzerTableSizeViewModel(
        String tableName,
        long bytes,
        long estimatedRows) {

    public AnalyzerTableSizeViewModel(String tableName, long bytes) {
        this(tableName, bytes, 0);
    }
}
