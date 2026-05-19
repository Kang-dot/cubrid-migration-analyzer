package com.cubrid.sqlanalyzer.command.page;

import com.cubrid.sqlanalyzer.command.AnalyzerConsoleConfig;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleCostDetail;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleFailure;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleReport;
import com.cubrid.sqlanalyzer.command.ConsoleIO;

public class AnalyzerResultPage {
    private final ConsoleIO io;

    public AnalyzerResultPage(ConsoleIO io) {
        this.io = io;
    }

    public void render(AnalyzerConsoleConfig session) {
        AnalyzerConsoleReport report = session.getConsoleReport();

        io.println("");
        io.println("Result summary");
        io.println("Source : " + report.getSourceType());
        io.println("Target : " + report.getTargetType());
        io.println("Mode   : " + report.getExecutionMode());
        io.println("Total  : " + report.getAnalyzedStatementCount());
        io.println("OK     : " + report.getSucceededStatementCount());
        io.println("FAIL   : " + report.getFailedStatementCount());
        io.println(
                "Cost   : "
                        + String.format(
                                java.util.Locale.US,
                                "%.1f",
                                report.getTotalEstimatedFailureCost()));

        if (!report.getFailures().isEmpty()) {
            io.println("");
            io.println("Failed statements");
            for (AnalyzerConsoleFailure failure : report.getFailures()) {
                renderFailure(failure);
            }
            io.println("----------------------------------------");
        } else if (!report.getFailureMessages().isEmpty()) {
            io.println("");
            io.println("Failed statements");
            for (String failureMessage : report.getFailureMessages()) {
                io.println("----------------------------------------");
                io.println("- " + failureMessage);
            }
            io.println("----------------------------------------");
        }

        String savedReportPath = report.saveResultReport();
        io.println("");
        io.println("Saved result report: " + savedReportPath);
    }

    private void renderFailure(AnalyzerConsoleFailure failure) {
        io.println("----------------------------------------");
        io.println(
                "- "
                        + failure.getStatementType()
                        + " "
                        + failure.getStatementId()
                        + " ["
                        + failure.getFailureStage()
                        + "]");
        io.println("  Reason: " + failure.getReason());
        io.println(
                "  Cost  : "
                        + String.format(
                                java.util.Locale.US,
                                "%.1f",
                                failure.getEstimatedCost()));
        io.println("  Cost details:");
        if (failure.getCostDetails().isEmpty()) {
            io.println("    (none)");
        } else {
            for (AnalyzerConsoleCostDetail costDetail : failure.getCostDetails()) {
                renderCostDetail(costDetail);
            }
        }
        io.println("  SQL : " + String.valueOf(failure.getSql()));
    }

    private void renderCostDetail(AnalyzerConsoleCostDetail costDetail) {
        io.println(
                "    - "
                        + costDetail.getItemName()
                        + " : count="
                        + costDetail.getCount()
                        + ", unit="
                        + String.format(java.util.Locale.US, "%.1f", costDetail.getUnitCost())
                        + ", total="
                        + String.format(java.util.Locale.US, "%.1f", costDetail.getTotalCost()));
    }
}
