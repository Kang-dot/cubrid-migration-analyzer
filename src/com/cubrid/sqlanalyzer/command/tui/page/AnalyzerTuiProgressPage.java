package com.cubrid.sqlanalyzer.command.tui.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.googlecode.lanterna.TerminalSize;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressEventViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressObjectCount;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressStage;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.ProgressBar;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.table.Table;

public class AnalyzerTuiProgressPage {
    private static final int RECENT_EVENT_VISIBLE_ROWS = 8;
    private static final int RECENT_MESSAGE_WIDTH = 72;
    private static final int RECENT_TABLE_RESERVED_ROWS = 17;

    public Panel build() {
        return buildView(null).getPanel();
    }

    public ProgressView buildView() {
        return buildView(null);
    }

    public ProgressView buildView(TerminalSize terminalSize) {
        return new ProgressView(terminalSize);
    }

    public static class ProgressView {
        private final Panel panel;
        private final Label progressLabel;
        private final Label okLabel;
        private final Label failLabel;
        private final Label currentLabel;
        private final Label statusLabel;
        private final Label objectSummarySpacer;
        private final ProgressBar progressBar;
        private final Table<String> recentTable;
        private final TextBox recentTextBox;
        private final List<String> recentMessages = new ArrayList<String>();
        private final List<Label> objectSummaryLabels = new ArrayList<Label>();

        private final int recentVisibleRows;
        private final int recentMessageWidth;

        private ProgressView(TerminalSize terminalSize) {
            int recentTableWidth = Math.min(82, AnalyzerTuiLayout.contentWidth(terminalSize));
            recentVisibleRows = AnalyzerTuiLayout.cappedVisibleRows(
                    terminalSize,
                    RECENT_TABLE_RESERVED_ROWS,
                    RECENT_EVENT_VISIBLE_ROWS,
                    RECENT_EVENT_VISIBLE_ROWS);
            recentMessageWidth = Math.max(16, Math.min(RECENT_MESSAGE_WIDTH, recentTableWidth - 10));
            panel = new Panel();
            progressLabel = new Label("Progress : 0 / 0");
            okLabel = new Label("OK       : 0");
            failLabel = new Label("FAIL     : 0");
            currentLabel = new Label("Current  : Waiting for analysis to start.");
            statusLabel = new Label("Analysis is running...");
            objectSummarySpacer = new Label("");
            progressBar = new ProgressBar(0, 100, 0);
            progressBar.setPreferredWidth(40);
            if (terminalSize == null) {
                recentTable = new Table<String>("No.", "Event");
                recentTable.setVisibleColumns(2);
                recentTable.setVisibleRows(recentVisibleRows);
                recentTable.setPreferredSize(new TerminalSize(recentTableWidth, recentVisibleRows + 2));
                recentTextBox = null;
            } else {
                recentTable = null;
                recentTextBox = new TextBox(
                        new TerminalSize(recentTableWidth, recentVisibleRows),
                        "",
                        TextBox.Style.MULTI_LINE);
                recentTextBox.setReadOnly(true);
            }

            panel.addComponent(new Label("[3/4] Analysis progress"));
            panel.addComponent(new Label(""));
            panel.addComponent(progressLabel);
            panel.addComponent(okLabel);
            panel.addComponent(failLabel);
            panel.addComponent(progressBar);
            panel.addComponent(new Label(""));
            panel.addComponent(currentLabel);
            panel.addComponent(new Label(""));
            panel.addComponent(new Label("Object summary"));
            panel.addComponent(new Label(
                    "  Type             Total   OK FAIL"));
            panel.addComponent(objectSummarySpacer);
            panel.addComponent(new Label("Recent"));
            if (recentTable != null) {
                panel.addComponent(recentTable);
            } else {
                panel.addComponent(recentTextBox);
            }
            panel.addComponent(new Label(""));
            panel.addComponent(statusLabel);
        }

        public Panel getPanel() {
            return panel;
        }

        public void update(AnalyzerProgressEventViewModel event) {
            if (event == null) {
                return;
            }

            int total = Math.max(0, event.totalCount());
            int completed = Math.max(0, event.completedCount());
            int progressMax = Math.max(1, total);
            int progressValue = Math.min(completed, progressMax);

            progressLabel.setText("Progress : " + completed + " / " + total);
            okLabel.setText("OK       : " + Math.max(0, event.succeededCount()));
            failLabel.setText("FAIL     : " + Math.max(0, event.failedCount()));
            progressBar.setMax(progressMax);
            progressBar.setValue(progressValue);
            updateObjectSummary(event.counts().objectCounts());

            String message = formatMessage(event);
            if (!message.isEmpty()) {
                currentLabel.setText("Current  : " + message);
                if (shouldShowAsRecent(event.stage())) {
                    addRecentMessage(message);
                }
            }
        }

        private void updateObjectSummary(List<AnalyzerProgressObjectCount> objectCounts) {
            int objectCountSize = objectCounts == null ? 0 : objectCounts.size();
            resizeObjectSummaryLabels(objectCountSize);

            for (int i = 0; i < objectCountSize; i++) {
                objectSummaryLabels.get(i).setText("  " + formatObjectSummary(objectCounts.get(i)));
            }
        }

        private void resizeObjectSummaryLabels(int objectCountSize) {
            while (objectSummaryLabels.size() > objectCountSize) {
                Label removedLabel = objectSummaryLabels.remove(objectSummaryLabels.size() - 1);
                panel.removeComponent(removedLabel);
            }

            while (objectSummaryLabels.size() < objectCountSize) {
                Label objectSummaryLabel = new Label("  ");
                objectSummaryLabels.add(objectSummaryLabel);
                panel.addComponent(panel.getChildrenList().indexOf(objectSummarySpacer), objectSummaryLabel);
            }
        }

        private String formatObjectSummary(AnalyzerProgressObjectCount objectCount) {
            return String.format(
                    Locale.US,
                    "%-15s %5d %4d %4d",
                    fitText(objectCount.objectType(), 15),
                    Math.max(0, objectCount.totalCount()),
                    Math.max(0, objectCount.succeededCount()),
                    Math.max(0, objectCount.failedCount()));
        }

        private String fitText(String value, int maxLength) {
            String text = value == null ? "" : value;
            if (text.length() <= maxLength) {
                return text;
            }
            return text.substring(0, maxLength - 1) + ".";
        }

        public void markCompleted() {
            statusLabel.setText("Analysis completed. Open the result.");
        }

        private void addRecentMessage(String message) {
            recentMessages.add(fitText(message, recentMessageWidth));
            if (recentTextBox != null) {
                int firstMessageIndex = Math.max(0, recentMessages.size() - recentVisibleRows);
                recentTextBox.setText(String.join("\n", recentMessages.subList(
                        firstMessageIndex,
                        recentMessages.size())));
                return;
            }

            int rowNumber = recentTable.getTableModel().getRowCount() + 1;
            recentTable.getTableModel().addRow(
                    String.valueOf(rowNumber),
                    recentMessages.get(recentMessages.size() - 1));
            recentTable.setVisibleRows(
                    Math.min(recentVisibleRows, recentTable.getTableModel().getRowCount()));
            int firstVisibleRow = Math.max(
                    0,
                    recentTable.getTableModel().getRowCount() - recentTable.getVisibleRows());
            recentTable.setViewTopRow(firstVisibleRow);
        }

        private boolean shouldShowAsRecent(AnalyzerProgressStage stage) {
            return stage == AnalyzerProgressStage.STATEMENT_SUCCEEDED
                    || stage == AnalyzerProgressStage.STATEMENT_FAILED
                    || stage == AnalyzerProgressStage.CLEANUP_SUCCEEDED
                    || stage == AnalyzerProgressStage.CLEANUP_FAILED;
        }

        private String formatMessage(AnalyzerProgressEventViewModel event) {
            if (event.message() != null && !event.message().isEmpty()) {
                return event.message();
            }
            return String.valueOf(event.stage());
        }
    }
}
