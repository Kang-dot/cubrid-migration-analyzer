package com.cubrid.sqlanalyzer.command.viewmodel;

import java.util.List;

import com.cubrid.sqlanalyzer.command.AnalyzerFailure;
import com.cubrid.sqlanalyzer.command.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.AnalyzerTargetType;

public record AnalyzerResultViewModel(
        List<AnalyzerSourceType> sourceTypes,
        AnalyzerTargetType targetType,
        AnalyzerExecutionMode executionMode,
        int analyzedStatementCount,
        int succeededStatementCount,
        int failedStatementCount,
        float totalEstimatedFailureCost,
        String savedReportPath,
        List<String> sourceStatusMessages,
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
            List<AnalyzerFailure> failures,
            List<AnalyzerProgressObjectCount> objectExecutionCounts) {
        this(
                sourceType == null ? List.of() : List.of(sourceType),
                targetType,
                executionMode,
                analyzedStatementCount,
                succeededStatementCount,
                failedStatementCount,
                totalEstimatedFailureCost,
                savedReportPath,
                List.of(),
                failureMessages,
                failures,
                objectExecutionCounts);
    }

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
                sourceType == null ? List.of() : List.of(sourceType),
                targetType,
                executionMode,
                analyzedStatementCount,
                succeededStatementCount,
                failedStatementCount,
                totalEstimatedFailureCost,
                savedReportPath,
                List.of(),
                failureMessages,
                failures,
                List.of());
    }

    public AnalyzerResultViewModel {
        sourceTypes = sourceTypes == null ? List.of() : List.copyOf(sourceTypes);
        sourceStatusMessages =
                sourceStatusMessages == null ? List.of() : List.copyOf(sourceStatusMessages);
        failureMessages = failureMessages == null ? List.of() : List.copyOf(failureMessages);
        failures = failures == null ? List.of() : List.copyOf(failures);
        objectExecutionCounts =
                objectExecutionCounts == null ? List.of() : List.copyOf(objectExecutionCounts);
    }

    public AnalyzerSourceType sourceType() {
        if (sourceTypes.isEmpty()) {
            return null;
        }
        return sourceTypes.size() > 1 ? AnalyzerSourceType.ALL : sourceTypes.get(0);
    }
}
