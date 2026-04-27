package com.cubrid.sqlanalyzer.command;

import com.cubrid.cubridmigration.core.connection.ConnParameters;

public interface AnalyzerConnParametersFactory {
    ConnParameters create(String connectionName, AnalyzerJdbcConnectionInfo profile);
}
