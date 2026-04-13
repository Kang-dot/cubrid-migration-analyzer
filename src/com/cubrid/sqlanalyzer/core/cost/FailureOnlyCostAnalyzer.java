package com.cubrid.sqlanalyzer.core.cost;

import com.cubrid.sqlanalyzer.command.AnalyzerConsoleReport;

public class FailureOnlyCostAnalyzer implements AnalyzerCostAnalyzer {
    @Override
    public void analyzeAfterExecution(AnalyzerConsoleReport report) {
        // Cost analysis will be implemented later.
        // The current skeleton keeps the post-execution extension point explicit.
    }
}
