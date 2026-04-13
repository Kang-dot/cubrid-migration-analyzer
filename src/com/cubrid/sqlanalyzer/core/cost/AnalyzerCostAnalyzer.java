package com.cubrid.sqlanalyzer.core.cost;

import com.cubrid.sqlanalyzer.command.AnalyzerConsoleReport;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPlan;

public interface AnalyzerCostAnalyzer {
    default void analyzeBeforeExecution(
            AnalyzerExecutionPlan executionPlan, AnalyzerConsoleReport report) {
        // no-op
    }

    default void analyzeAfterExecution(AnalyzerConsoleReport report) {
        // no-op
    }
}
