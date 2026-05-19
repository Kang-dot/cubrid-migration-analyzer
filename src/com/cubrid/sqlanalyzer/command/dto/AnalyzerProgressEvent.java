package com.cubrid.sqlanalyzer.command.dto;

import com.cubrid.sqlanalyzer.command.AnalyzerFailureStage;

public record AnalyzerProgressEvent(
        AnalyzerProgressStage stage,
        String message,
        String statementType,
        String statementId,
        String sql,
        String detail,
        AnalyzerFailureStage failureStage,
        int totalCount,
        int completedCount,
        int succeededCount,
        int failedCount) {
}
