package com.cubrid.sqlanalyzer.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        assertEquals(100, arguments.getTuiWidth());
        assertEquals(30, arguments.getTuiHeight());
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
    @DisplayName("missing XML directory value is rejected")
    void shouldRejectXmlSourceWithoutDirectory() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AnalyzerArgumentsController.parse(new String[] { "-sx", "-tp" }));

        assertTrue(exception.getMessage().contains("-sx requires -xd <xmlDirectory>."));
    }

    @Test
    @DisplayName("duplicate source options are rejected")
    void shouldRejectDuplicateSourceOptions() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AnalyzerArgumentsController.parse(
                        new String[] { "-sx", "-so", "-oj", "jdbc|user|pw", "-tp" }));

        assertTrue(exception.getMessage().contains("Only one source option is allowed"));
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
