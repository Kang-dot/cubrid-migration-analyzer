package com.cubrid.sqlanalyzer.command.dto;

import com.cubrid.sqlanalyzer.command.AnalyzerTargetType;

public record AnalyzerTargetOverview(
        AnalyzerTargetType type,
        String jdbcUrl,
        String host,
        int port,
        String databaseName,
        String user,
        String version,
        String parserVersion) {
}
