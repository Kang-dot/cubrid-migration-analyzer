package com.cubrid.sqlanalyzer.command.tui.page;

import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

public class AnalyzerTuiProgressPage {
    public Panel build() {
        Panel panel = new Panel();
        panel.addComponent(new Label("[3/4] Analysis progress"));
        panel.addComponent(new Label("Analysis is running..."));
        panel.addComponent(new Label("The result page will open when analysis completes."));
        return panel;
    }
}
