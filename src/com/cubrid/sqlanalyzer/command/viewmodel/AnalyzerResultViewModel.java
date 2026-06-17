package com.cubrid.sqlanalyzer.command.viewmodel;

import java.util.List;

import com.cubrid.sqlanalyzer.command.AnalyzerFailure;
import com.cubrid.sqlanalyzer.command.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.AnalyzerTargetType;

public record AnalyzerResultViewModel(
        AnalyzerSourceType sourceType,
        AnalyzerTargetType targetType,
        AnalyzerExecutionMode executionMode,
        int analyzedStatementCount,
        int succeededStatementCount,
        int failedStatementCount,
        float totalEstimatedFailureCost,
        String savedReportPath,
        List<String> failureMessages,
        List<AnalyzerFailure> failures,
        List<AnalyzerProgressObjectCount> objectExecutionCounts) {

    public AnalyzerResultViewModel(
            AnalyzerSourceType sourceType,
            AnalyzerTargetType targetType,
            AnalyzerExecutionMode executionMode,
            int analyzedStatementCount,
            int succeededStatementCount,
            int failedStatementCount,
            float totalEstimatedFailureCost,
            String savedReportPath,
            List<String> failureMessages,
            List<AnalyzerFailure> failures) {
        this(
                sourceType,
                targetType,
                executionMode,
                analyzedStatementCount,
                succeededStatementCount,
                failedStatementCount,
                totalEstimatedFailureCost,
                savedReportPath,
                failureMessages,
                failures,
                List.of());
    }

    public AnalyzerResultViewModel {
        failureMessages = failureMessages == null ? List.of() : List.copyOf(failureMessages);
        failures = failures == null ? List.of() : List.copyOf(failures);
        objectExecutionCounts =
                objectExecutionCounts == null ? List.of() : List.copyOf(objectExecutionCounts);
    }
}
