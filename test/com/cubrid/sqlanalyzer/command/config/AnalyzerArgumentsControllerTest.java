package com.cubrid.sqlanalyzer.command.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerUiMode;

class AnalyzerArgumentsControllerTest {
    @Test
    @DisplayName("empty args keep interactive mode")
    void shouldUseInteractiveModeWhenNoArgumentsAreProvided() {
        AnalyzerArgumentsController arguments = AnalyzerArgumentsController.parse(new String[0]);

        assertTrue(arguments.isInteractive());
    }

    @Test
    @DisplayName("XML to parser arguments are parsed correctly")
    void shouldParseXmlToParserArguments() {
        AnalyzerArgumentsController arguments = AnalyzerArgumentsController
                .parse(new String[] { "-sx", "-xd", "/tmp/sqlmap", "-tp" });

        assertEquals(AnalyzerSourceType.XML, arguments.getSourceType());
        assertEquals("/tmp/sqlmap", arguments.getXmlDirectory());
        assertEquals(AnalyzerTargetType.PARSER, arguments.getTargetType());
        assertEquals("UTF-8", arguments.getXmlCharset());
        assertEquals(AnalyzerUiMode.TUI, arguments.getUiMode());
        assertTrue(arguments.isTuiMode());
        assertEquals(100, arguments.getTuiWidth());
        assertEquals(30, arguments.getTuiHeight());
    }

    @Test
    @DisplayName("console UI mode can be selected explicitly")
    void shouldParseConsoleModeOption() {
        AnalyzerArgumentsController arguments = AnalyzerArgumentsController.parse(
                new String[] { "-ui", "console", "-sx", "-xd", "/tmp/sqlmap", "-tp" });

        assertEquals(AnalyzerUiMode.CONSOLE, arguments.getUiMode());
    }

    @Test
    @DisplayName("TUI mode option is parsed correctly")
    void shouldParseTuiModeOption() {
        AnalyzerArgumentsController arguments = AnalyzerArgumentsController.parse(
                new String[] { "-ui", "tui", "-sx", "-xd", "/tmp/sqlmap", "-tp" });

        assertEquals(AnalyzerUiMode.TUI, arguments.getUiMode());
        assertTrue(arguments.isTuiMode());
    }

    @Test
    @DisplayName("TUI terminal size options are parsed correctly")
    void shouldParseTuiTerminalSizeOptions() {
        AnalyzerArgumentsController arguments = AnalyzerArgumentsController.parse(
                new String[] {
                        "-ui",
                        "tui",
                        "-tw",
                        "120",
                        "-th",
                        "40",
                        "-sx",
                        "-xd",
                        "/tmp/sqlmap",
                        "-tp"
                });

        assertEquals(120, arguments.getTuiWidth());
        assertEquals(40, arguments.getTuiHeight());
    }

    @Test
    @DisplayName("TUI shortcut option is parsed correctly")
    void shouldParseTuiShortcutOption() {
        AnalyzerArgumentsController arguments = AnalyzerArgumentsController
                .parse(new String[] { "-tui", "-sx", "-xd", "/tmp/sqlmap", "-tp" });

        assertEquals(AnalyzerUiMode.TUI, arguments.getUiMode());
        assertTrue(arguments.isTuiMode());
    }

    @Test
    @DisplayName("missing XML directory value is deferred to source loading")
    void shouldAllowXmlSourceWithoutDirectory() {
        AnalyzerArgumentsController arguments =
                AnalyzerArgumentsController.parse(new String[] { "-sx", "-tp" });

        assertEquals(AnalyzerSourceType.XML, arguments.getSourceType());
        assertTrue(arguments.isXmlSourceRequested());
        assertEquals(AnalyzerTargetType.PARSER, arguments.getTargetType());
    }

    @Test
    @DisplayName("Oracle and XML source options can be combined")
    void shouldParseCombinedSourceOptions() {
        AnalyzerArgumentsController arguments = AnalyzerArgumentsController.parse(
                new String[] { "-sx", "-xd", "/tmp/sqlmap", "-so", "-oj", "jdbc|user|pw", "-tp" });

        assertEquals(AnalyzerSourceType.ALL, arguments.getSourceType());
        assertTrue(arguments.isOracleSourceRequested());
        assertTrue(arguments.isXmlSourceRequested());
        assertEquals("jdbc", arguments.getSourceJdbcUrl());
        assertEquals("/tmp/sqlmap", arguments.getXmlDirectory());
    }

    @Test
    @DisplayName("invalid Oracle connection spec is recorded without blocking XML source")
    void shouldRecordInvalidOracleSpecWithoutBlockingXmlSource() {
        AnalyzerArgumentsController arguments = AnalyzerArgumentsController.parse(
                new String[] { "-so", "-oj", "bad-spec", "-sx", "-xd", "/tmp/sqlmap" });

        assertEquals(AnalyzerSourceType.ALL, arguments.getSourceType());
        assertTrue(arguments.isOracleSourceRequested());
        assertTrue(arguments.isXmlSourceRequested());
        assertEquals("/tmp/sqlmap", arguments.getXmlDirectory());
        assertTrue(arguments.getSourceInputMessages().get(0).contains("-oj is invalid"));
    }

    @Test
    @DisplayName("non-positive TUI terminal size is rejected")
    void shouldRejectNonPositiveTuiTerminalSize() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AnalyzerArgumentsController.parse(
                        new String[] { "-tui", "-tw", "0", "-sx", "-xd", "/tmp/sqlmap", "-tp" }));

        assertTrue(exception.getMessage().contains("-tw must be greater than 0."));
    }
}
