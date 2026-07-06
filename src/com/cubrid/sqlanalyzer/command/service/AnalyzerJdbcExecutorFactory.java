package com.cubrid.sqlanalyzer.command.service;

import java.sql.SQLException;

import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;

interface AnalyzerJdbcExecutorFactory {
    AnalyzerJdbcExecutor open(AnalyzerConfiguration config) throws SQLException;
}
