package com.cubrid.sqlanalyzer.command.dto;

public enum AnalyzerProgressStage {
    PLANNING,
    ANALYZING,
    STATEMENT_SUCCEEDED,
    STATEMENT_FAILED,
    CLEANUP_SUCCEEDED,
    CLEANUP_FAILED,
    COMPLETED,
    EMPTY
}
