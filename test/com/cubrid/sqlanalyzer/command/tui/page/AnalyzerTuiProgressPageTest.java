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
