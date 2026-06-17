package com.cubrid.sqlanalyzer.command.tui.page;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.command.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressObjectCount;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerResultViewModel;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

class AnalyzerTuiResultPageTest {
    @Test
    @DisplayName("result TUI page renders summary")
    void shouldRenderResultSummary() {
        AnalyzerResultViewModel result = new AnalyzerResultViewModel(
                AnalyzerSourceType.XML,
                AnalyzerTargetType.PARSER,
                AnalyzerExecutionMode.DML,
                10,
                8,
                2,
                12.5f,
                "/tmp/analyzer-result.txt",
                List.of("SELECT q1 failed"),
                List.of());

        Panel panel = new AnalyzerTuiResultPage().build(result);
        String screenText = String.join(System.lineSeparator(), collectLabelTexts(panel));

        assertTrue(screenText.contains("[4/4] Result summary"));
        assertTrue(screenText.contains("Source : XML"));
        assertTrue(screenText.contains("Target : PARSER"));
        assertTrue(screenText.contains("Mode   : DML"));
        assertTrue(screenText.contains("Total  : 10"));
        assertTrue(screenText.contains("OK     : 8"));
        assertTrue(screenText.contains("FAIL   : 2"));
        assertTrue(screenText.contains("Cost   : 12.5"));
        assertTrue(screenText.contains("Report : /tmp/analyzer-result.txt"));
        assertTrue(screenText.contains("See the report file for detailed execution logs."));
        assertFalse(screenText.contains("- SELECT q1 failed"));
        assertFalse(screenText.contains("Failed statements"));
    }

    @Test
    @DisplayName("result TUI page renders object execution summary")
    void shouldRenderObjectExecutionSummary() {
        AnalyzerResultViewModel result = new AnalyzerResultViewModel(
                AnalyzerSourceType.ORACLE,
                AnalyzerTargetType.JDBC,
                AnalyzerExecutionMode.DDL,
                5,
                4,
                1,
                0.0f,
                "/tmp/analyzer-result.txt",
                List.of(),
                List.of(),
                List.of(
                        new AnalyzerProgressObjectCount("TABLE", 3, 2, 1),
                        new AnalyzerProgressObjectCount("VIEW_CREATE", 2, 2, 0)));

        Panel panel = new AnalyzerTuiResultPage().build(result);
        String screenText = String.join(System.lineSeparator(), collectLabelTexts(panel));

        assertTrue(screenText.contains("Object execution summary"));
        assertTrue(screenText.contains("Type"));
        assertTrue(screenText.contains("Total"));
        assertTrue(screenText.contains("TABLE"));
        assertTrue(screenText.contains("VIEW_CREATE"));
        assertTrue(screenText.contains("    3    2    1"));
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
