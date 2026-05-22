package com.cubrid.sqlanalyzer.command.viewmodel;

import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;

public record AnalyzerSourceOverviewViewModel(
        AnalyzerSourceType type,
        String jdbcUrl,
        String host,
        int port,
        String databaseName,
        String user,
        String version,
        String xmlDirectory,
        String xmlCharset,
        int xmlFileCount) {
}
