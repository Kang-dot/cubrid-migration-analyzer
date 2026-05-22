package com.cubrid.sqlanalyzer.command.tui.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerResultViewModel;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

public class AnalyzerTuiResultPage {
    public Panel build(AnalyzerResultViewModel result) {
        Panel panel = new Panel();
        for (String line : buildLines(result)) {
            panel.addComponent(new Label(line));
        }
        return panel;
    }

    List<String> buildLines(AnalyzerResultViewModel result) {
        List<String> lines = new ArrayList<String>();
        lines.add("[4/4] Result summary");
        lines.add("Source : " + result.sourceType());
        lines.add("Target : " + result.targetType());
        lines.add("Mode   : " + result.executionMode());
        lines.add("Total  : " + result.analyzedStatementCount());
        lines.add("OK     : " + result.succeededStatementCount());
        lines.add("FAIL   : " + result.failedStatementCount());
        lines.add("Cost   : " + formatCost(result.totalEstimatedFailureCost()));

        lines.add("");
        lines.add("Report : " + formatText(result.savedReportPath()));
        lines.add("See the report file for detailed execution logs.");
        return lines;
    }

    private String formatCost(float cost) {
        return String.format(Locale.US, "%.1f", cost);
    }

    private String formatText(String value) {
        return value == null ? "" : value;
    }
}
