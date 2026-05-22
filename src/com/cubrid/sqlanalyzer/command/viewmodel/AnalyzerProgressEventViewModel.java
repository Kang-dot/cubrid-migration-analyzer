package com.cubrid.sqlanalyzer.command.viewmodel;

import com.cubrid.sqlanalyzer.command.AnalyzerFailureStage;

public record AnalyzerProgressEventViewModel(
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
