package com.cubrid.sqlanalyzer.command.service;

record AnalyzerJdbcExecutionResult(boolean hasResultSet, int rowCount, int updateCount) {
}
