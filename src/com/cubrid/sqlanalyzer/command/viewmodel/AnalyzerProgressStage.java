package com.cubrid.sqlanalyzer.command.viewmodel;

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
