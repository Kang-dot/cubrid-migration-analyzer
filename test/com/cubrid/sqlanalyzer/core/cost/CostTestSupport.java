package com.cubrid.sqlanalyzer.core.cost;

import com.cubrid.sqlanalyzer.command.AnalyzerConsoleFailure;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleReport;

abstract class CostTestSupport {
    protected static final float DELTA = 0.0001f;

    private final FailureCostCalculator calculator = new FailureCostCalculator();

    protected float estimateCost(String statementType, String sql) {
        AnalyzerConsoleReport report = new AnalyzerConsoleReport();
        AnalyzerConsoleFailure failure = new AnalyzerConsoleFailure();
        failure.setStatementType(statementType);
        failure.setStatementId("TEST_1");
        failure.setSql(sql);
        failure.setReason("test");
        report.addFailure(failure);

        calculator.analyzeAfterExecution(report);
        return failure.getEstimatedCost();
    }

    protected String buildSqlWithLength(int targetLength) {
        String prefix = "SELECT '";
        String suffix = "'";
        int payloadLength = targetLength - prefix.length() - suffix.length();
        if (payloadLength < 0) {
            throw new IllegalArgumentException(
                    "targetLength must be at least " + (prefix.length() + suffix.length()));
        }

        // Whitespace is normalized by the calculator, so use literal content to control length.
        return prefix + "x".repeat(payloadLength) + suffix;
    }
}
