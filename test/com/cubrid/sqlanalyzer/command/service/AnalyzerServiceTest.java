package com.cubrid.sqlanalyzer.command.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.cubrid.sqlanalyzer.command.model.AnalyzerSession;

class AnalyzerServiceTest {
    @TempDir
    Path xmlDirectory;

    @Test
    void shouldReportNoXmlFilesWhenXmlDirectoryIsEmpty() {
        AnalyzerSession session = new AnalyzerSession();
        session.setXmlSourceRequested(true);
        session.setXmlDirectory(xmlDirectory.toString());
        session.setXmlCharset("UTF-8");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new AnalyzerService().loadSourceCatalog(session));

        assertEquals(AnalyzerService.NO_ANALYZER_SOURCE_LOADED_MESSAGE, ex.getMessage());
        assertFalse(session.isXmlSourceLoaded());
        assertTrue(session.getSourceStatusMessages().stream()
                .anyMatch(message -> message.contains("No XML files found in directory")));
    }
}
