package com.cubrid.sqlanalyzer.command.viewmodel;

import com.cubrid.sqlanalyzer.command.AnalyzerExecutionMode;

public record AnalyzerOverviewViewModel(
        String programVersion,
        AnalyzerSourceOverviewViewModel source,
        AnalyzerTargetOverviewViewModel target,
        AnalyzerExecutionMode executionMode) {
}
