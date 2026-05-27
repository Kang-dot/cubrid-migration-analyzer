package com.cubrid.sqlanalyzer.command.tui.page;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerObjectCountPreviewViewModel;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

class AnalyzerTuiObjectCountPageTest {
    @Test
    @DisplayName("object count TUI page renders XML query counts")
    void shouldRenderXmlQueryCounts() {
        AnalyzerObjectCountPreviewViewModel preview = new AnalyzerObjectCountPreviewViewModel(
                AnalyzerSourceType.XML,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                3,
                2,
                1,
                4);

        Panel panel = new AnalyzerTuiObjectCountPage().build(preview);
        String screenText = String.join(System.lineSeparator(), collectLabelTexts(panel));

        assertTrue(screenText.contains("[2/4] Object count preview"));
        assertTrue(screenText.contains("SELECT count    : 3"));
        assertTrue(screenText.contains("INSERT count    : 2"));
        assertTrue(screenText.contains("UPDATE count    : 1"));
        assertTrue(screenText.contains("DELETE count    : 4"));
    }

    @Test
    @DisplayName("object count TUI page renders Oracle trigger count")
    void shouldRenderOracleTriggerCount() {
        AnalyzerObjectCountPreviewViewModel preview =
                new AnalyzerObjectCountPreviewViewModel(
                        AnalyzerSourceType.ORACLE,
                        1,
                        2,
                        3,
                        4,
                        5,
                        6,
                        7,
                        8,
                        9,
                        10,
                        11,
                        0,
                        0,
                        0,
                        0);

        Panel panel = new AnalyzerTuiObjectCountPage().build(preview);
        String screenText = String.join(System.lineSeparator(), collectLabelTexts(panel));

        assertTrue(screenText.contains("Target triggers : 11"));
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
