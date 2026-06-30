package com.cubrid.sqlanalyzer.core.cost;

import com.cubrid.sqlanalyzer.command.report.AnalyzerReport;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPlan;

public interface AnalyzerCostCalculator {
    default void analyzeBeforeExecution(
            AnalyzerExecutionPlan executionPlan, AnalyzerReport report) {
        // no-op
    }

    default void analyzeAfterExecution(AnalyzerReport report) {
        // no-op
    }
}
