package com.cubrid.sqlanalyzer.command.tui.page;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressEventViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressStage;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.ProgressBar;

public class AnalyzerTuiProgressPage {
    private static final int RECENT_EVENT_LIMIT = 5;

    public Panel build() {
        return buildView().getPanel();
    }

    public ProgressView buildView() {
        return new ProgressView();
    }

    public static class ProgressView {
        private final Panel panel;
        private final Label progressLabel;
        private final Label okLabel;
        private final Label failLabel;
        private final Label currentLabel;
        private final Label statusLabel;
        private final ProgressBar progressBar;
        private final List<Label> recentLabels = new ArrayList<Label>();
        private final Deque<String> recentMessages = new ArrayDeque<String>();

        private ProgressView() {
            panel = new Panel();
            progressLabel = new Label("Progress : 0 / 0");
            okLabel = new Label("OK       : 0");
            failLabel = new Label("FAIL     : 0");
            currentLabel = new Label("Current  : Waiting for analysis to start.");
            statusLabel = new Label("Analysis is running...");
            progressBar = new ProgressBar(0, 100, 0);
            progressBar.setPreferredWidth(40);

            panel.addComponent(new Label("[3/4] Analysis progress"));
            panel.addComponent(new Label(""));
            panel.addComponent(progressLabel);
            panel.addComponent(okLabel);
            panel.addComponent(failLabel);
            panel.addComponent(progressBar);
            panel.addComponent(new Label(""));
            panel.addComponent(currentLabel);
            panel.addComponent(new Label("Recent"));
            for (int i = 0; i < RECENT_EVENT_LIMIT; i++) {
                Label recentLabel = new Label("  ");
                recentLabels.add(recentLabel);
                panel.addComponent(recentLabel);
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

            String message = formatMessage(event);
            if (!message.isEmpty()) {
                currentLabel.setText("Current  : " + message);
                if (shouldShowAsRecent(event.stage())) {
                    addRecentMessage(message);
                }
            }
        }

        public void markCompleted() {
            statusLabel.setText("Analysis completed. Press Enter to open result.");
        }

        private void addRecentMessage(String message) {
            recentMessages.addLast(message);
            while (recentMessages.size() > RECENT_EVENT_LIMIT) {
                recentMessages.removeFirst();
            }

            int labelIndex = 0;
            for (String recentMessage : recentMessages) {
                recentLabels.get(labelIndex).setText("  " + recentMessage);
                labelIndex++;
            }

            while (labelIndex < recentLabels.size()) {
                recentLabels.get(labelIndex).setText("  ");
                labelIndex++;
            }
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
