package com.cubrid.sqlanalyzer.command.tui.page;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerObjectCountPreviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTableSizeViewModel;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.table.Table;

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

    @Test
    @DisplayName("object count TUI page renders Oracle table sizes")
    void shouldRenderOracleTableSizes() {
        AnalyzerObjectCountPreviewViewModel preview =
                new AnalyzerObjectCountPreviewViewModel(
                        AnalyzerSourceType.ORACLE,
                        1,
                        2,
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
                        0,
                        0,
                        3_145_728L,
                        List.of(
                                new AnalyzerTableSizeViewModel("EMP", 2_097_152L),
                                new AnalyzerTableSizeViewModel("DEPT", 1_048_576L)));

        Panel panel = new AnalyzerTuiObjectCountPage().build(preview);
        String screenText = String.join(System.lineSeparator(), collectLabelTexts(panel));
        String tableText = collectTableText(panel);

        assertTrue(screenText.contains("Oracle table size total : 3.00 MB"));
        assertTrue(screenText.contains("Oracle table sizes"));
        assertTrue(tableText.contains("EMP"));
        assertTrue(tableText.contains("2.00 MB"));
        assertTrue(tableText.contains("DEPT"));
        assertTrue(tableText.contains("1.00 MB"));
    }

    @Test
    @DisplayName("object count TUI page limits visible Oracle table size rows")
    void shouldLimitVisibleOracleTableSizeRows() {
        List<AnalyzerTableSizeViewModel> tableSizes = new ArrayList<AnalyzerTableSizeViewModel>();
        for (int i = 1; i <= 20; i++) {
            tableSizes.add(new AnalyzerTableSizeViewModel("TABLE_" + i, 1024L * i, i * 10L));
        }
        AnalyzerObjectCountPreviewViewModel preview =
                new AnalyzerObjectCountPreviewViewModel(
                        AnalyzerSourceType.ORACLE,
                        1,
                        20,
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
                        0,
                        0,
                        21_504L,
                        tableSizes);

        Panel panel = new AnalyzerTuiObjectCountPage().build(preview);
        Table<?> table = collectTables(panel).get(0);

        assertTrue(table.getTableModel().getRowCount() == 20);
        assertTrue(table.getVisibleRows() < table.getTableModel().getRowCount());
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

    private List<Table<?>> collectTables(Panel panel) {
        List<Table<?>> tables = new ArrayList<Table<?>>();
        for (Component component : panel.getChildren()) {
            if (component instanceof Table<?>) {
                tables.add((Table<?>) component);
            }
        }
        return tables;
    }

    private String collectTableText(Panel panel) {
        List<String> values = new ArrayList<String>();
        for (Table<?> table : collectTables(panel)) {
            for (List<?> row : table.getTableModel().getRows()) {
                for (Object cell : row) {
                    values.add(String.valueOf(cell));
                }
            }
        }
        return String.join(System.lineSeparator(), values);
    }
}
