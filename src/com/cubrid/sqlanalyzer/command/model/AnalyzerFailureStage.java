package com.cubrid.sqlanalyzer.command.model;

public enum AnalyzerFailureStage {
    PARSER,
    JDBC,
    CLEANUP,
    UNSUPPORTED
}
