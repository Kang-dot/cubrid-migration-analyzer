package com.cubrid.sqlanalyzer.command.page;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.command.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.ConsoleIO;
import com.cubrid.sqlanalyzer.command.dto.AnalyzerOverview;
import com.cubrid.sqlanalyzer.command.dto.AnalyzerSourceOverview;
import com.cubrid.sqlanalyzer.command.dto.AnalyzerTargetOverview;

class AnalyzerOverviewPageTest {
    @Test
    @DisplayName("XML parser overview renders service DTO details")
    void shouldRenderXmlParserOverviewFromDto() {
        RecordingConsoleIO io = new RecordingConsoleIO();
        AnalyzerOverview overview =
                new AnalyzerOverview(
                        "0.0.1-SNAPSHOT",
                        new AnalyzerSourceOverview(
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
                        new AnalyzerTargetOverview(
                                AnalyzerTargetType.PARSER,
                                null,
                                null,
                                0,
                                null,
                                null,
                                null,
                                "CUBRID parser"),
                        AnalyzerExecutionMode.DML);

        new AnalyzerOverviewPage(io).render(overview);
        String output = io.output();

        assertTrue(output.contains("[3/4] Overview"));
        assertTrue(output.contains("Program     : 0.0.1-SNAPSHOT"));
        assertTrue(output.contains("Source      : XML"));
        assertTrue(output.contains("XML dir     : /tmp/sqlmap"));
        assertTrue(output.contains("XML charset : UTF-8"));
        assertTrue(output.contains("XML files   : 3"));
        assertTrue(output.contains("Target      : PARSER"));
        assertTrue(output.contains("Parser      : CUBRID parser"));
        assertTrue(output.contains("Mode        : DML"));
    }

    @Test
    @DisplayName("JDBC overview renders connection details from DTO")
    void shouldRenderJdbcOverviewConnectionDetailsFromDto() {
        RecordingConsoleIO io = new RecordingConsoleIO();
        AnalyzerOverview overview =
                new AnalyzerOverview(
                        "1.2.3",
                        new AnalyzerSourceOverview(
                                AnalyzerSourceType.ORACLE,
                                "jdbc:oracle:thin:@//oracle.example.com:1521/XEPDB1",
                                "oracle.example.com",
                                1521,
                                "XEPDB1",
                                "oracle_user",
                                "21c",
                                null,
                                null,
                                0),
                        new AnalyzerTargetOverview(
                                AnalyzerTargetType.JDBC,
                                "jdbc:cubrid:cubrid.example.com:33000:demodb:::",
                                "cubrid.example.com",
                                33000,
                                "demodb",
                                "dba",
                                "11.3",
                                null),
                        AnalyzerExecutionMode.DDL);

        new AnalyzerOverviewPage(io).render(overview);
        String output = io.output();

        assertTrue(output.contains("Program     : 1.2.3"));
        assertTrue(output.contains("Oracle URL  : jdbc:oracle:thin:@//oracle.example.com:1521/XEPDB1 (21c)"));
        assertTrue(output.contains("Oracle Host : oracle.example.com:1521"));
        assertTrue(output.contains("Oracle DB   : XEPDB1"));
        assertTrue(output.contains("Oracle User : oracle_user"));
        assertTrue(output.contains("Target URL  : jdbc:cubrid:cubrid.example.com:33000:demodb::: (11.3)"));
        assertTrue(output.contains("Target Host : cubrid.example.com:33000"));
        assertTrue(output.contains("Target DB   : demodb"));
        assertTrue(output.contains("Target User : dba"));
        assertTrue(output.contains("Mode        : DDL"));
    }

    private static class RecordingConsoleIO implements ConsoleIO {
        private final List<String> lines = new ArrayList<String>();

        public void print(String text) {
            lines.add(text);
        }

        public void println(String text) {
            lines.add(text);
        }

        public String readLine() {
            return "";
        }

        public String readRequired(String prompt) {
            return "";
        }

        public boolean confirm(String prompt) {
            return false;
        }

        String output() {
            return String.join(System.lineSeparator(), lines);
        }
    }
}
