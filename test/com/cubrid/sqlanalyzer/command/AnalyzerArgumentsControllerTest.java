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
}
