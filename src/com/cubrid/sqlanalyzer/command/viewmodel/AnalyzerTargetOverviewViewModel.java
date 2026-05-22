package com.cubrid.sqlanalyzer.command.viewmodel;

import com.cubrid.sqlanalyzer.command.AnalyzerTargetType;

public record AnalyzerTargetOverviewViewModel(
        AnalyzerTargetType type,
        String jdbcUrl,
        String host,
        int port,
        String databaseName,
        String user,
        String version,
        String parserVersion) {
}
