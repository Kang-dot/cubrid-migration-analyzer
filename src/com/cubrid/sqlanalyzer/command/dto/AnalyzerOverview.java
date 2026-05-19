package com.cubrid.sqlanalyzer.command.dto;

import com.cubrid.sqlanalyzer.command.AnalyzerExecutionMode;

public record AnalyzerOverview(
        String programVersion,
        AnalyzerSourceOverview source,
        AnalyzerTargetOverview target,
        AnalyzerExecutionMode executionMode) {
}
