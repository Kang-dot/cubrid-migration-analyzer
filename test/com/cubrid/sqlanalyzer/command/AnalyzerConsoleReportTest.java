package com.cubrid.sqlanalyzer.command;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
        report.addFailure(failure);

        String resultText = report.buildResultText();

        assertTrue(resultText.contains("Result summary"));
        assertTrue(resultText.contains("Source : XML"));
        assertTrue(resultText.contains("Target : PARSER"));
        assertTrue(resultText.contains("Mode   : DML"));
        assertTrue(resultText.contains("FAIL   : 1"));
        assertTrue(resultText.contains("- SELECT Q1 : FAIL [PARSER]"));
        assertTrue(resultText.contains("Reason: syntax error"));
    }
}
