package com.cubrid.sqlanalyzer.command.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class AnalyzerJdbcConnectionExecutor implements AnalyzerJdbcExecutor {
    private final Connection connection;

    AnalyzerJdbcConnectionExecutor(Connection connection) {
        this.connection = connection;
    }

    @Override
    public AnalyzerJdbcExecutionResult execute(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            boolean hasResultSet = statement.execute(sql);
            if (hasResultSet) {
                try (ResultSet resultSet = statement.getResultSet()) {
                    int rowCount = 0;
                    while (resultSet.next()) {
                        rowCount++;
                    }
                    return new AnalyzerJdbcExecutionResult(true, rowCount, 0);
                }
            }
            return new AnalyzerJdbcExecutionResult(false, 0, statement.getUpdateCount());
        }
    }

    @Override
    public void commit() throws SQLException {
        connection.commit();
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
