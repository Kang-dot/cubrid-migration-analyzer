package com.cubrid.sqlanalyzer.command;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnalyzerConsoleReportTest {
    @Test
    @DisplayName("result text includes summary and failure details")
    void shouldBuildResultTextWithStatementAndFailureSections() {
        AnalyzerConsoleReport report = new AnalyzerConsoleReport();
        report.setSourceType(AnalyzerSourceType.XML);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DML);
        report.setAnalyzedStatementCount(1);
        report.setSucceededStatementCount(0);
        report.setFailedStatementCount(1);
        report.addStatementResult(
                "SELECT",
                "Q0",
                "SELECT 1",
                true,
                "parsed",
                null);
        report.addStatementResult(
                "SELECT",
                "Q1",
                "SELECT * FROM emp",
                false,
                "syntax error",
                AnalyzerFailureStage.PARSER);

        AnalyzerConsoleFailure failure = new AnalyzerConsoleFailure();
        failure.setStatementType("SELECT");
        failure.setStatementId("Q1");
        failure.setSql("SELECT * FROM emp");
        failure.setReason("syntax error");
        failure.setFailureStage(AnalyzerFailureStage.PARSER);
        failure.setEstimatedCost(0.7f);
        failure.addCostDetail(new AnalyzerConsoleCostDetail("Base DML", 1, 0.2f, 0.2f));
        failure.addCostDetail(new AnalyzerConsoleCostDetail("JOIN detected", 1, 0.5f, 0.5f));
        report.addFailure(failure);

        String resultText = report.buildResultText();

        assertTrue(resultText.contains("Result summary"));
        assertTrue(resultText.contains("Source : XML"));
        assertTrue(resultText.contains("Target : PARSER"));
        assertTrue(resultText.contains("Mode   : DML"));
        assertTrue(resultText.contains("FAIL   : 1"));
        assertTrue(resultText.contains("Cost   : 0.7"));
        assertTrue(resultText.contains("- SELECT Q1 [PARSER]"));
        assertTrue(resultText.contains("Reason: syntax error"));
        assertTrue(resultText.contains("Cost  : 0.7"));
        assertTrue(resultText.contains("Cost details:"));
        assertTrue(resultText.contains("Base DML : count=1, unit=0.2, total=0.2"));
        assertTrue(resultText.contains("JOIN detected : count=1, unit=0.5, total=0.5"));
        assertTrue(resultText.contains("----------------------------------------"));
        assertFalse(resultText.contains("Statement results"));
        assertFalse(resultText.contains("SELECT Q0"));
    }
}
