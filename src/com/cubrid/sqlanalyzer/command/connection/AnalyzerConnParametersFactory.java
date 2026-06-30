package com.cubrid.sqlanalyzer.command.connection;

import com.cubrid.cubridmigration.core.connection.ConnParameters;

public interface AnalyzerConnParametersFactory {
    ConnParameters create(String connectionName, AnalyzerJdbcConnectionInfo profile);
}
