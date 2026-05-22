package com.cubrid.sqlanalyzer.command.tui.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.cubrid.sqlanalyzer.command.AnalyzerCostDetail;
import com.cubrid.sqlanalyzer.command.AnalyzerFailure;
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

        appendFailures(lines, result);

        lines.add("");
        lines.add("Saved result report: " + formatText(result.savedReportPath()));
        return lines;
    }

    private void appendFailures(List<String> lines, AnalyzerResultViewModel result) {
        if (!result.failures().isEmpty()) {
            lines.add("");
            lines.add("Failed statements");
            for (AnalyzerFailure failure : result.failures()) {
                appendFailure(lines, failure);
            }
            lines.add("----------------------------------------");
            return;
        }

        if (!result.failureMessages().isEmpty()) {
            lines.add("");
            lines.add("Failed statements");
            for (String failureMessage : result.failureMessages()) {
                lines.add("----------------------------------------");
                lines.add("- " + failureMessage);
            }
            lines.add("----------------------------------------");
        }
    }

    private void appendFailure(List<String> lines, AnalyzerFailure failure) {
        lines.add("----------------------------------------");
        lines.add(
                "- "
                        + failure.getStatementType()
                        + " "
                        + failure.getStatementId()
                        + " ["
                        + failure.getFailureStage()
                        + "]");
        lines.add("  Reason: " + failure.getReason());
        lines.add("  Cost  : " + formatCost(failure.getEstimatedCost()));
        lines.add("  Cost details:");
        if (failure.getCostDetails().isEmpty()) {
            lines.add("    (none)");
        } else {
            for (AnalyzerCostDetail costDetail : failure.getCostDetails()) {
                appendCostDetail(lines, costDetail);
            }
        }
        lines.add("  SQL : " + String.valueOf(failure.getSql()));
    }

    private void appendCostDetail(List<String> lines, AnalyzerCostDetail costDetail) {
        lines.add(
                "    - "
                        + costDetail.getItemName()
                        + " : count="
                        + costDetail.getCount()
                        + ", unit="
                        + formatCost(costDetail.getUnitCost())
                        + ", total="
                        + formatCost(costDetail.getTotalCost()));
    }

    private String formatCost(float cost) {
        return String.format(Locale.US, "%.1f", cost);
    }

    private String formatText(String value) {
        return value == null ? "" : value;
    }
}
