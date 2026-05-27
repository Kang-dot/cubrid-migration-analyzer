package com.cubrid.sqlanalyzer.command.tui.page;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.cubrid.sqlanalyzer.command.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerSourceOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTargetOverviewViewModel;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

class AnalyzerTuiOverviewPageTest {
    @TempDir
    Path xmlDirectory;

    @Test
    @DisplayName("overview TUI page renders analyzer overview view model")
    void shouldRenderOverviewViewModelAsLanternaPage() {
        AnalyzerOverviewViewModel overview = new AnalyzerOverviewViewModel(
                "0.0.1-SNAPSHOT",
                new AnalyzerSourceOverviewViewModel(
                        AnalyzerSourceType.XML,
                        null,
                        null,
                        0,
                        null,
                        null,
                        null,
                        xmlDirectory.toString(),
                        "UTF-8",
                        1),
                new AnalyzerTargetOverviewViewModel(
                        AnalyzerTargetType.PARSER,
                        null,
                        null,
                        0,
                        null,
                        null,
                        null,
                        "CUBRID parser"),
                AnalyzerExecutionMode.DML);
        Panel panel = new AnalyzerTuiOverviewPage().build(overview);
        String screenText = String.join(System.lineSeparator(), collectLabelTexts(panel));

        assertTrue(screenText.contains("CUBRID SQL Analyzer"));
        assertTrue(screenText.contains("[1/4] Overview"));
        assertTrue(screenText.contains("Source      : XML"));
        assertTrue(screenText.contains("XML dir     : " + xmlDirectory));
        assertTrue(screenText.contains("XML charset : UTF-8"));
        assertTrue(screenText.contains("XML files   : 1"));
        assertTrue(screenText.contains("Target      : PARSER"));
        assertTrue(screenText.contains("Parser      : CUBRID parser"));
        assertEquals(2, countOccurrences(screenText, "XML files   : 1"));
        assertTrue(screenText.contains("Mode        : DML"));
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

    private List<String> collectLabelTexts(Panel panel) {
        List<String> texts = new ArrayList<String>();
        for (Component component : panel.getChildren()) {
            if (component instanceof Label) {
                texts.add(((Label) component).getText());
            }
        }
        return texts;
    }
}
