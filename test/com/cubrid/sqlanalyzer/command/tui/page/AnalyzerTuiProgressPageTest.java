package com.cubrid.sqlanalyzer.command.tui.page;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.command.tui.page.AnalyzerTuiProgressPage.ProgressView;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressCounts;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressEventViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressObjectCount;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatement;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

class AnalyzerTuiProgressPageTest {
    @Test
    @DisplayName("progress TUI page renders live running state")
    void shouldRenderRunningState() {
        Panel panel = new AnalyzerTuiProgressPage().build();
        String screenText = String.join(System.lineSeparator(), collectLabelTexts(panel));

        assertTrue(screenText.contains("[3/4] Analysis progress"));
        assertTrue(screenText.contains("Progress : 0 / 0"));
        assertTrue(screenText.contains("OK       : 0"));
        assertTrue(screenText.contains("FAIL     : 0"));
        assertTrue(screenText.contains("Object summary"));
        assertTrue(screenText.contains("Analysis is running..."));
    }

    @Test
    @DisplayName("progress TUI page keeps only the latest five events")
    void shouldRenderOnlyLatestFiveEvents() {
        ProgressView progressView = new AnalyzerTuiProgressPage().buildView();

        for (int i = 1; i <= 6; i++) {
            progressView.update(
                    AnalyzerProgressEventViewModel.statementSucceeded(
                            new AnalyzerStatement("SELECT", "q" + i, "select " + i),
                            "parsed",
                            new AnalyzerProgressCounts(6, i, i, 0)));
        }

        String screenText = String.join(
                System.lineSeparator(), collectLabelTexts(progressView.getPanel()));

        assertTrue(screenText.contains("Progress : 6 / 6"));
        assertTrue(screenText.contains("OK       : 6"));
        assertTrue(screenText.contains("FAIL     : 0"));
        assertTrue(screenText.contains("Current  : [OK] SELECT q6"));
        assertFalse(screenText.contains("[OK] SELECT q1"));
        assertTrue(screenText.contains("[OK] SELECT q2"));
        assertTrue(screenText.contains("[OK] SELECT q6"));
    }

    @Test
    @DisplayName("progress TUI page renders object summary counts")
    void shouldRenderObjectSummaryCounts() {
        ProgressView progressView = new AnalyzerTuiProgressPage().buildView();

        progressView.update(
                AnalyzerProgressEventViewModel.statementFailed(
                        new AnalyzerStatement("DDL_TABLE", "TABLE_2", "create table t2(c int)"),
                        "syntax error",
                        null,
                        new AnalyzerProgressCounts(
                                4,
                                2,
                                1,
                                1,
                                List.of(
                                        new AnalyzerProgressObjectCount("TABLE", 2, 1, 1),
                                        new AnalyzerProgressObjectCount("VIEW_CREATE", 2, 0, 0)))));

        String screenText = String.join(
                System.lineSeparator(), collectLabelTexts(progressView.getPanel()));

        assertTrue(screenText.contains("TABLE"));
        assertTrue(screenText.contains("VIEW_CREATE"));
        assertTrue(screenText.contains("    2    1    1"));
    }

    @Test
    @DisplayName("progress TUI page renders every object summary row")
    void shouldRenderEveryObjectSummaryRow() {
        ProgressView progressView = new AnalyzerTuiProgressPage().buildView();
        List<AnalyzerProgressObjectCount> objectCounts = new ArrayList<AnalyzerProgressObjectCount>();
        for (int i = 1; i <= 10; i++) {
            objectCounts.add(new AnalyzerProgressObjectCount("TYPE_" + i, 1, 0, 0));
        }

        progressView.update(
                AnalyzerProgressEventViewModel.statementSucceeded(
                        new AnalyzerStatement("TYPE_10", "ID_10", "select 1"),
                        "parsed",
                        new AnalyzerProgressCounts(10, 1, 1, 0, objectCounts)));

        String screenText = String.join(
                System.lineSeparator(), collectLabelTexts(progressView.getPanel()));

        assertTrue(screenText.contains("TYPE_1"));
        assertTrue(screenText.contains("TYPE_10"));
        assertFalse(screenText.contains("more"));
    }

    @Test
    @DisplayName("progress TUI page renders completion state")
    void shouldRenderCompletionState() {
        ProgressView progressView = new AnalyzerTuiProgressPage().buildView();

        progressView.markCompleted();

        String screenText = String.join(
                System.lineSeparator(), collectLabelTexts(progressView.getPanel()));

        assertTrue(screenText.contains("Analysis completed. Open the result."));
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
