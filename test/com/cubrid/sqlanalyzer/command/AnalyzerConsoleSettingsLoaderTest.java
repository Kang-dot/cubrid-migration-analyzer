package com.cubrid.sqlanalyzer.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnalyzerConsoleSettingsLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldLoadStructuredSettingsWhenNoConsoleArgumentsAreProvided() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(
                settingsFile,
                "source.type=xml\n"
                        + "xml.directory=/tmp/sqlmap\n"
                        + "xml.charset=EUC-KR\n"
                        + "target.type=parser\n");

        String[] args = AnalyzerConsoleSettingsLoader.loadStartupArguments(new String[0], settingsFile);
        AnalyzerConsoleArguments arguments = AnalyzerConsoleArguments.parse(args);

        assertEquals(AnalyzerSourceType.XML, arguments.getSourceType());
        assertEquals("/tmp/sqlmap", arguments.getXmlDirectory());
        assertEquals("EUC-KR", arguments.getXmlCharset());
        assertEquals(AnalyzerTargetType.PARSER, arguments.getTargetType());
    }

    @Test
    void shouldPreferConsoleArgumentsOverDefaultSettings() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(
                settingsFile,
                "source.type=xml\n"
                        + "xml.directory=/from/settings\n"
                        + "target.type=parser\n");

        String[] cliArgs = new String[] {"-sx", "-xd", "/from/cli", "-tp"};
        String[] args = AnalyzerConsoleSettingsLoader.loadStartupArguments(cliArgs, settingsFile);

        assertArrayEquals(cliArgs, args);
    }

    @Test
    void shouldLoadExplicitSettingsFile() throws Exception {
        Path settingsFile = tempDir.resolve("custom.conf");
        Files.writeString(settingsFile, "arguments=-sx -xd /tmp/sqlmap -tp\n");

        String[] args =
                AnalyzerConsoleSettingsLoader.loadStartupArguments(
                        new String[] {"-conf", settingsFile.toString()}, null);

        assertArrayEquals(new String[] {"-sx", "-xd", "/tmp/sqlmap", "-tp"}, args);
    }
}
