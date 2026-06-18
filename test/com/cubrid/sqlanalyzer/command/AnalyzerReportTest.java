package com.cubrid.sqlanalyzer.command;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerObjectCountPreviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerSourceOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTableSizeViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTargetOverviewViewModel;

import java.util.List;

class AnalyzerReportTest {
    @Test
    @DisplayName("result text includes summary and failure details")
    void shouldBuildResultTextWithStatementAndFailureSections() {
        AnalyzerReport report = new AnalyzerReport();
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

        AnalyzerFailure failure = new AnalyzerFailure();
        failure.setStatementType("SELECT");
        failure.setStatementId("Q1");
        failure.setSql("SELECT * FROM emp");
        failure.setReason("syntax error");
        failure.setFailureStage(AnalyzerFailureStage.PARSER);
        failure.setEstimatedCost(0.7f);
        failure.addCostDetail(new AnalyzerCostDetail("Base DML", 1, 0.2f, 0.2f));
        failure.addCostDetail(new AnalyzerCostDetail("JOIN detected", 1, 0.5f, 0.5f));
        report.addFailure(failure);

        String resultText = report.buildResultText();

        assertTrue(resultText.contains("Overview"));
        assertTrue(resultText.contains("Source      : XML"));
        assertTrue(resultText.contains("Target      : PARSER"));
        assertTrue(resultText.contains("Mode        : DML"));
        assertTrue(resultText.contains("FAIL        : 1"));
        assertTrue(resultText.contains("Cost        : 0.7"));
        assertTrue(resultText.contains("Analysis summary"));
        assertTrue(resultText.contains("Object counts"));
        assertTrue(resultText.contains("Execution results"));
        assertTrue(resultText.contains("Type"));
        assertTrue(resultText.contains("Total"));
        assertTrue(resultText.contains("OK"));
        assertTrue(resultText.contains("SELECT                         2       1       1"));
        assertTrue(resultText.contains("- SELECT Q1 [PARSER]"));
        assertTrue(resultText.contains("Reason: syntax error"));
        assertTrue(resultText.contains("Cost  : 0.7"));
        assertTrue(resultText.contains("Cost details:"));
        assertTrue(resultText.contains("Base DML : count=1, unit=0.2, total=0.2"));
        assertTrue(resultText.contains("JOIN detected : count=1, unit=0.5, total=0.5"));
        assertTrue(resultText.contains("----------------------------------------"));
        assertFalse(resultText.contains("Result summary"));
        assertFalse(resultText.contains("Statement results"));
        assertFalse(resultText.contains("SELECT Q0"));
    }

    @Test
    @DisplayName("result text includes overview connection details when available")
    void shouldBuildResultTextWithOverviewConnectionDetails() {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.XML);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DML);
        report.setOverview(
                new AnalyzerOverviewViewModel(
                        "0.0.1-SNAPSHOT",
                        new AnalyzerSourceOverviewViewModel(
                                AnalyzerSourceType.XML,
                                null,
                                null,
                                0,
                                null,
                                null,
                                null,
                                "/tmp/sqlmap",
                                "UTF-8",
                                3),
                        new AnalyzerTargetOverviewViewModel(
                                AnalyzerTargetType.PARSER,
                                null,
                                null,
                                0,
                                null,
                                null,
                                null,
                                "CUBRID parser"),
                        AnalyzerExecutionMode.DML));

        String resultText = report.buildResultText();

        assertTrue(resultText.contains("Overview"));
        assertTrue(resultText.contains("Program     : 0.0.1-SNAPSHOT"));
        assertTrue(resultText.contains("Source      : XML"));
        assertTrue(resultText.contains("XML dir     : /tmp/sqlmap"));
        assertTrue(resultText.contains("XML charset : UTF-8"));
        assertTrue(resultText.contains("XML files   : 3"));
        assertTrue(resultText.contains("Target      : PARSER"));
        assertTrue(resultText.contains("Parser      : CUBRID parser"));
        assertEquals(1, countOccurrences(resultText, "XML files   : 3"));
        assertTrue(resultText.contains("Mode        : DML"));
        assertTrue(resultText.contains("Total       : 0"));
        assertFalse(resultText.contains("Result summary"));
    }

    @Test
    @DisplayName("result text includes zero object counts for empty Oracle catalog")
    void shouldBuildResultTextWithZeroOracleObjectCounts() {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.ORACLE);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DDL);
        report.setAnalyzedStatementCount(0);
        report.setSucceededStatementCount(0);
        report.setFailedStatementCount(0);
        report.setObjectCountPreview(
                new AnalyzerObjectCountPreviewViewModel(
                        AnalyzerSourceType.ORACLE,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0));

        String resultText = report.buildResultText();

        assertTrue(resultText.contains("Analysis summary"));
        assertTrue(resultText.contains("Object counts"));
        assertTrue(resultText.contains("Catalog schemas : 0"));
        assertTrue(resultText.contains("Target tables   : 0"));
        assertTrue(resultText.contains("Target PKs      : 0"));
        assertTrue(resultText.contains("Target FKs      : 0"));
        assertTrue(resultText.contains("Target views    : 0"));
        assertTrue(resultText.contains("Target serials  : 0"));
        assertTrue(resultText.contains("Target synonyms : 0"));
        assertTrue(resultText.contains("Target grants   : 0"));
        assertTrue(resultText.contains("Target procs    : 0"));
        assertTrue(resultText.contains("Target funcs    : 0"));
        assertTrue(resultText.contains("Target triggers : 0"));
        assertTrue(resultText.contains("Execution results"));
        assertTrue(resultText.contains("(none)"));
    }

    @Test
    @DisplayName("result text includes Oracle table size summary")
    void shouldBuildResultTextWithOracleTableSizes() {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.ORACLE);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DDL);
        report.setObjectCountPreview(
                new AnalyzerObjectCountPreviewViewModel(
                        AnalyzerSourceType.ORACLE,
                        1,
                        2,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        3_145_728L,
                        List.of(
                                new AnalyzerTableSizeViewModel("EMP", 2_097_152L, 1_234L),
                                new AnalyzerTableSizeViewModel("DEPT", 1_048_576L, 56L))));

        String resultText = report.buildResultText();

        assertTrue(resultText.contains("Oracle table size total : 3.00 MB"));
        assertTrue(resultText.contains("Oracle table sizes"));
        assertTrue(resultText.contains("Est. rows"));
        assertTrue(resultText.contains("EMP"));
        assertTrue(resultText.contains("2.00 MB"));
        assertTrue(resultText.contains("1,234"));
        assertTrue(resultText.contains("DEPT"));
        assertTrue(resultText.contains("1.00 MB"));
        assertTrue(resultText.contains("56"));
    }

    @Test
    @DisplayName("result text highlights parser error line and column")
    void shouldBuildResultTextWithSqlErrorContext() {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.ORACLE);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DDL);

        AnalyzerFailure failure = new AnalyzerFailure();
        failure.setStatementType("DDL_TABLE");
        failure.setStatementId("TABLE_1");
        failure.setSql("CREATE TABLE t(\ncol_a int,\ncol_b DEFAULT broken\n);");
        failure.setReason("In line 3, column 15, Syntax error");
        failure.setFailureStage(AnalyzerFailureStage.PARSER);
        report.addFailure(failure);

        String resultText = report.buildResultText();

        assertTrue(resultText.contains("Location: line 3, column 15"));
        assertTrue(resultText.contains("SQL:"));
        assertTrue(resultText.contains("1 | CREATE TABLE t("));
        assertTrue(resultText.contains("3 | col_b DEFAULT broken"));
        assertTrue(resultText.contains("4 | );"));
        assertTrue(resultText.contains("^"));
        assertFalse(resultText.contains("SQL context:"));
    }

    @Test
    @DisplayName("result text estimates SQL error context from parser token when parser line is external")
    void shouldBuildResultTextWithEstimatedSqlErrorContext() {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.ORACLE);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DDL);

        AnalyzerFailure failure = new AnalyzerFailure();
        failure.setStatementType("DDL_TABLE");
        failure.setStatementId("TABLE_25");
        failure.setSql(
                "CREATE TABLE tools4644(\n"
                        + "col_raw bit varying(800) DEFAULT X'HEXTORAW('64656661756C745F726177')',\n"
                        + "col_nvarchar2 varchar(100) DEFAULT (u'default_nvarchar2')\n"
                        + ");");
        failure.setReason(
                "In line 150, column 27 before 'C745F726177')', "
                        + "Syntax error: unexpected '64656661756', expecting REFERENCES");
        failure.setFailureStage(AnalyzerFailureStage.PARSER);
        report.addFailure(failure);

        String resultText = report.buildResultText();

        assertTrue(resultText.contains("Location: line 2, column"));
        assertTrue(resultText.contains("(estimated)"));
        assertTrue(resultText.contains("1 | CREATE TABLE tools4644("));
        assertTrue(resultText.contains("col_raw bit varying"));
        assertTrue(resultText.contains("4 | );"));
        assertTrue(resultText.contains("^ estimated"));
        assertFalse(resultText.contains("SQL context:"));
    }

    @Test
    @DisplayName("result text includes zero query counts for empty XML source")
    void shouldBuildResultTextWithZeroXmlQueryCounts() {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.XML);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DML);
        report.setObjectCountPreview(
                new AnalyzerObjectCountPreviewViewModel(
                        AnalyzerSourceType.XML,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0));

        String resultText = report.buildResultText();

        assertTrue(resultText.contains("Object counts"));
        assertTrue(resultText.contains("SELECT count    : 0"));
        assertTrue(resultText.contains("INSERT count    : 0"));
        assertTrue(resultText.contains("UPDATE count    : 0"));
        assertTrue(resultText.contains("DELETE count    : 0"));
    }

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = text.indexOf(pattern);
        while (index >= 0) {
            count++;
            index = text.indexOf(pattern, index + pattern.length());
        }
        return count;
    }
}
