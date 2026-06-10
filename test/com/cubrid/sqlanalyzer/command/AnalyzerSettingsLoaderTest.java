package com.cubrid.sqlanalyzer.command;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnalyzerSettingsLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldLoadStructuredSettingsWhenNoCliArgumentsAreProvided() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(
                settingsFile,
                "ui.mode=tui\n"
                        + "tui.width=120\n"
                        + "tui.height=40\n"
                        + "source.type=xml\n"
                        + "xml.directory=/tmp/sqlmap\n"
                        + "xml.charset=EUC-KR\n"
                        + "target.type=parser\n");

        String[] args = AnalyzerSettingsLoader.loadStartupArguments(new String[0], settingsFile);
        AnalyzerArgumentsController arguments = AnalyzerArgumentsController.parse(args);

        assertEquals(AnalyzerSourceType.XML, arguments.getSourceType());
        assertEquals("/tmp/sqlmap", arguments.getXmlDirectory());
        assertEquals("EUC-KR", arguments.getXmlCharset());
        assertEquals(AnalyzerTargetType.PARSER, arguments.getTargetType());
        assertEquals(AnalyzerUiMode.TUI, arguments.getUiMode());
        assertEquals(120, arguments.getTuiWidth());
        assertEquals(40, arguments.getTuiHeight());
    }

    @Test
    void shouldPreferCliArgumentsOverDefaultSettings() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(
                settingsFile,
                "source.type=xml\n"
                        + "xml.directory=/from/settings\n"
                        + "target.type=parser\n");

        String[] cliArgs = new String[] { "-sx", "-xd", "/from/cli", "-tp" };
        String[] args = AnalyzerSettingsLoader.loadStartupArguments(cliArgs, settingsFile);

        assertArrayEquals(cliArgs, args);
    }

    @Test
    void shouldBuildOracleConnectionFromSeparatedProperties() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(
                settingsFile,
                "source.type=oracle\n"
                        + "source.host=192.168.1.6\n"
                        + "source.port=1521\n"
                        + "source.sid=xe\n"
                        + "source.username=cubrid\n"
                        + "source.password=cubrid\n"
                        + "target.type=parser\n");

        String[] args = AnalyzerSettingsLoader.loadStartupArguments(new String[0], settingsFile);
        AnalyzerArgumentsController arguments = AnalyzerArgumentsController.parse(args);

        assertEquals(AnalyzerSourceType.ORACLE, arguments.getSourceType());
        assertEquals("jdbc:oracle:thin:@//192.168.1.6:1521/xe", arguments.getSourceJdbcUrl());
        assertEquals("cubrid", arguments.getSourceUser());
        assertEquals("cubrid", arguments.getSourcePassword());
        assertEquals(AnalyzerTargetType.PARSER, arguments.getTargetType());
    }

    @Test
    void shouldLoadExplicitSettingsFile() throws Exception {
        Path settingsFile = tempDir.resolve("custom.conf");
        Files.writeString(settingsFile, "arguments=-sx -xd /tmp/sqlmap -tp\n");

        String[] args = AnalyzerSettingsLoader.loadStartupArguments(
                new String[] { "-conf", settingsFile.toString() }, null);

        assertArrayEquals(new String[] { "-sx", "-xd", "/tmp/sqlmap", "-tp" }, args);
    }

    @Test
    void shouldLoadLogDirectoryFromSettingsFile() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(settingsFile, "log.dir=custom-logs\n");

        String logDirectory = AnalyzerSettingsLoader.loadLogDirectory(new String[0], settingsFile);

        assertEquals("custom-logs", logDirectory);
    }

    @Test
    void shouldUseDefaultLogDirectoryWhenSettingsFileHasNoLogDirectory() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(settingsFile, "source.type=xml\n");

        String logDirectory = AnalyzerSettingsLoader.loadLogDirectory(new String[0], settingsFile);

        assertEquals("logs", logDirectory);
    }

    @Test
    void shouldLoadLogDirectoryFromExplicitSettingsFile() throws Exception {
        Path settingsFile = tempDir.resolve("custom.conf");
        Files.writeString(settingsFile, "log.dir=/tmp/sql-analyzer-logs\n");

        String logDirectory = AnalyzerSettingsLoader.loadLogDirectory(
                new String[] { "-conf", settingsFile.toString() }, null);

        assertEquals("/tmp/sql-analyzer-logs", logDirectory);
    }
}
