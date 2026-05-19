package com.cubrid.sqlanalyzer.command.dto;

import java.util.List;

import com.cubrid.sqlanalyzer.command.AnalyzerFailure;
import com.cubrid.sqlanalyzer.command.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.AnalyzerTargetType;

public record AnalyzerResult(
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

    public AnalyzerResult {
        failureMessages = failureMessages == null ? List.of() : List.copyOf(failureMessages);
        failures = failures == null ? List.of() : List.copyOf(failures);
    }
}
