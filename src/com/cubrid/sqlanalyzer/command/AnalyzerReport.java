package com.cubrid.sqlanalyzer.command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.cubrid.cubridmigration.cubrid.CUBRIDTimeUtil;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerSourceOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTargetOverviewViewModel;

public class AnalyzerReport {
    private static class StatementResult {
        private final String statementType;
        private final String statementId;
        private final String sql;
        private final boolean success;
        private final String detail;
        private final AnalyzerFailureStage failureStage;

        StatementResult(
                String statementType,
                String statementId,
                String sql,
                boolean success,
                String detail,
                AnalyzerFailureStage failureStage) {
            this.statementType = statementType;
            this.statementId = statementId;
            this.sql = sql;
            this.success = success;
            this.detail = detail;
            this.failureStage = failureStage;
        }
    }

    private static class StatementTypeSummary {
        private int totalCount;
        private int succeededCount;
        private int failedCount;

        private void add(boolean success) {
            totalCount++;
            if (success) {
                succeededCount++;
            } else {
                failedCount++;
            }
        }
    }

    private AnalyzerSourceType sourceType;
    private AnalyzerTargetType targetType;
    private AnalyzerExecutionMode executionMode;
    private int analyzedStatementCount;
    private int succeededStatementCount;
    private int failedStatementCount;
    private long generatedAt;
    private final List<String> failureMessages = new ArrayList<String>();
    private final List<AnalyzerFailure> failures = new ArrayList<AnalyzerFailure>();
    private final List<StatementResult> statementResults = new ArrayList<StatementResult>();
    private AnalyzerOverviewViewModel overview;

    public AnalyzerSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(AnalyzerSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public AnalyzerTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(AnalyzerTargetType targetType) {
        this.targetType = targetType;
    }

    public AnalyzerExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(AnalyzerExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

    public int getAnalyzedStatementCount() {
        return analyzedStatementCount;
    }

    public void setAnalyzedStatementCount(int analyzedStatementCount) {
        this.analyzedStatementCount = analyzedStatementCount;
    }

    public int getSucceededStatementCount() {
        return succeededStatementCount;
    }

    public void setSucceededStatementCount(int succeededStatementCount) {
        this.succeededStatementCount = succeededStatementCount;
    }

    public int getFailedStatementCount() {
        return failedStatementCount;
    }

    public void setFailedStatementCount(int failedStatementCount) {
        this.failedStatementCount = failedStatementCount;
    }

    public List<String> getFailureMessages() {
        return failureMessages;
    }

    public void clearFailures() {
        failureMessages.clear();
        failures.clear();
        statementResults.clear();
    }

    public void addFailureMessage(String failureMessage) {
        failureMessages.add(failureMessage);
    }

    public List<AnalyzerFailure> getFailures() {
        return failures;
    }

    public void addFailure(AnalyzerFailure failure) {
        failures.add(failure);
    }

    public void addStatementResult(
            String statementType,
            String statementId,
            String sql,
            boolean success,
            String detail,
            AnalyzerFailureStage failureStage) {
        statementResults.add(
                new StatementResult(
                        statementType, statementId, sql, success, detail, failureStage));
    }

    public AnalyzerOverviewViewModel getOverview() {
        return overview;
    }

    public void setOverview(AnalyzerOverviewViewModel overview) {
        this.overview = overview;
    }

    public String saveResultReport() {
        try {
            File reportDir = getReportDirectory();
            if (!reportDir.exists() && !reportDir.mkdirs()) {
                throw new IOException("Failed to create report directory: " + reportDir);
            }

            generatedAt = System.currentTimeMillis();
            File reportFile = new File(reportDir, buildReportFileName());
            try (PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(reportFile), "UTF-8"))) {
                writer.print(buildResultText());
                writer.flush();
            }
            return reportFile.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save analyzer report: " + e.getMessage(), e);
        }
    }

    public String buildResultText() {
        String lineSeparator = System.lineSeparator();
        StringBuilder sb = new StringBuilder();

        appendOverview(sb, lineSeparator);

        sb.append("Result summary").append(lineSeparator);
        sb.append("Source : ").append(sourceType).append(lineSeparator);
        sb.append("Target : ").append(targetType).append(lineSeparator);
        sb.append("Mode   : ").append(executionMode).append(lineSeparator);
        sb.append("Total  : ").append(analyzedStatementCount).append(lineSeparator);
        sb.append("OK     : ").append(succeededStatementCount).append(lineSeparator);
        sb.append("FAIL   : ").append(failedStatementCount).append(lineSeparator);
        sb.append("Cost   : ").append(formatEstimatedCost(getTotalEstimatedFailureCost()))
                .append(lineSeparator);

        appendObjectExecutionSummary(sb, lineSeparator);

        if (!failures.isEmpty()) {
            sb.append(lineSeparator).append("Failed statements").append(lineSeparator);
            for (AnalyzerFailure failure : failures) {
                appendFailureBlock(sb, failure, lineSeparator);
            }
            sb.append("----------------------------------------").append(lineSeparator);
        } else if (!failureMessages.isEmpty()) {
            sb.append(lineSeparator).append("Failed statements").append(lineSeparator);
            for (String failureMessage : failureMessages) {
                sb.append("----------------------------------------").append(lineSeparator);
                sb.append("- ").append(safeText(failureMessage)).append(lineSeparator);
            }
            sb.append("----------------------------------------").append(lineSeparator);
        } else {
            sb.append(lineSeparator).append("Failed statements").append(lineSeparator);
            sb.append("(none)").append(lineSeparator);
        }

        return sb.toString();
    }

    private void appendOverview(StringBuilder sb, String lineSeparator) {
        if (overview == null) {
            return;
        }

        sb.append("Overview").append(lineSeparator);
        sb.append("Program     : ").append(formatText(overview.programVersion())).append(lineSeparator);
        appendSourceOverview(sb, overview.source(), lineSeparator);
        appendTargetOverview(sb, overview.source(), overview.target(), lineSeparator);
        sb.append("Mode        : ").append(overview.executionMode()).append(lineSeparator);
        sb.append(lineSeparator);
    }

    private void appendSourceOverview(
            StringBuilder sb,
            AnalyzerSourceOverviewViewModel source,
            String lineSeparator) {
        if (source == null) {
            return;
        }

        sb.append("Source      : ").append(source.type()).append(lineSeparator);
        if (source.type() == AnalyzerSourceType.ORACLE) {
            sb.append("Oracle URL  : ")
                    .append(formatText(source.jdbcUrl()))
                    .append(formatVersionSuffix(source.version()))
                    .append(lineSeparator);
            sb.append("Oracle Host : ")
                    .append(formatHost(source.host(), source.port()))
                    .append(lineSeparator);
            sb.append("Oracle DB   : ")
                    .append(formatText(source.databaseName()))
                    .append(lineSeparator);
            sb.append("Oracle User : ")
                    .append(formatText(source.user()))
                    .append(lineSeparator);
            return;
        }

        sb.append("XML dir     : ").append(formatText(source.xmlDirectory())).append(lineSeparator);
        sb.append("XML charset : ").append(formatText(source.xmlCharset())).append(lineSeparator);
        sb.append("XML files   : ").append(source.xmlFileCount()).append(lineSeparator);
    }

    private void appendTargetOverview(
            StringBuilder sb,
            AnalyzerSourceOverviewViewModel source,
            AnalyzerTargetOverviewViewModel target,
            String lineSeparator) {
        if (target == null) {
            return;
        }

        sb.append("Target      : ").append(target.type()).append(lineSeparator);
        if (target.type() == AnalyzerTargetType.JDBC) {
            sb.append("Target URL  : ")
                    .append(formatText(target.jdbcUrl()))
                    .append(formatVersionSuffix(target.version()))
                    .append(lineSeparator);
            sb.append("Target Host : ")
                    .append(formatHost(target.host(), target.port()))
                    .append(lineSeparator);
            sb.append("Target DB   : ")
                    .append(formatText(target.databaseName()))
                    .append(lineSeparator);
            sb.append("Target User : ")
                    .append(formatText(target.user()))
                    .append(lineSeparator);
            return;
        }

        if (target.type() == AnalyzerTargetType.PARSER) {
            sb.append("Parser      : ").append(formatText(target.parserVersion())).append(lineSeparator);
            if (source != null
                    && source.xmlDirectory() != null
                    && !source.xmlDirectory().isEmpty()) {
                sb.append("XML dir     : ")
                        .append(formatText(source.xmlDirectory()))
                        .append(lineSeparator);
                sb.append("XML files   : ")
                        .append(source.xmlFileCount())
                        .append(lineSeparator);
            }
        }
    }

    private void appendObjectExecutionSummary(StringBuilder sb, String lineSeparator) {
        sb.append(lineSeparator).append("Object execution summary").append(lineSeparator);
        Map<String, StatementTypeSummary> summaries = buildStatementTypeSummaries();
        if (summaries.isEmpty()) {
            sb.append("(none)").append(lineSeparator);
            return;
        }

        sb.append(String.format(Locale.US, "%-24s %7s %7s %7s", "Type", "Total", "OK", "FAIL"))
                .append(lineSeparator);
        for (Map.Entry<String, StatementTypeSummary> entry : summaries.entrySet()) {
            StatementTypeSummary summary = entry.getValue();
            sb.append(
                            String.format(
                                    Locale.US,
                                    "%-24s %7d %7d %7d",
                                    entry.getKey(),
                                    summary.totalCount,
                                    summary.succeededCount,
                                    summary.failedCount))
                    .append(lineSeparator);
        }
    }

    private Map<String, StatementTypeSummary> buildStatementTypeSummaries() {
        Map<String, StatementTypeSummary> summaries = new LinkedHashMap<String, StatementTypeSummary>();
        for (StatementResult statementResult : statementResults) {
            if ("CLEANUP".equals(statementResult.statementType)) {
                continue;
            }

            String statementType = safeText(statementResult.statementType);
            if (statementType.isEmpty()) {
                statementType = "UNKNOWN";
            }

            StatementTypeSummary summary = summaries.get(statementType);
            if (summary == null) {
                summary = new StatementTypeSummary();
                summaries.put(statementType, summary);
            }
            summary.add(statementResult.success);
        }
        return summaries;
    }

    private File getReportDirectory() {
        return new File(resolveAnalyzerProjectDirectory(), "report");
    }

    private File resolveAnalyzerProjectDirectory() {
        File codeSource = getCodeSourceLocation();
        File current = codeSource;

        while (current != null) {
            if ("com.cubrid.SQLAnalyzer".equals(current.getName())) {
                return current;
            }
            current = current.getParentFile();
        }

        String userDir = System.getProperty("user.dir");
        if (userDir != null) {
            File currentDir = new File(userDir);
            if ("com.cubrid.SQLAnalyzer".equals(currentDir.getName())) {
                return currentDir;
            }

            File child = new File(currentDir, "com.cubrid.SQLAnalyzer");
            if (child.exists()) {
                return child;
            }
        }

        return new File("com.cubrid.SQLAnalyzer").getAbsoluteFile();
    }

    private File getCodeSourceLocation() {
        try {
            return new File(
                    AnalyzerReport.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI());
        } catch (URISyntaxException e) {
            return new File(
                    AnalyzerReport.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .getPath());
        }
    }

    private String buildReportFileName() {
        long timeValue = generatedAt > 0 ? generatedAt : System.currentTimeMillis();
        return "analyzer_result_"
                + CUBRIDTimeUtil.getDateFormat(
                        "yyyy_MM_dd_HH_mm_ss_SSS", Locale.US, TimeZone.getDefault())
                        .format(new Date(timeValue))
                + ".txt";
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String formatText(String value) {
        return value == null ? "" : value;
    }

    private String formatVersionSuffix(String version) {
        return version == null || version.isEmpty() ? "" : " (" + version + ")";
    }

    private String formatHost(String host, int port) {
        if (host == null || host.isEmpty()) {
            return "";
        }

        return port > 0 ? host + ":" + port : host;
    }

    public float getTotalEstimatedFailureCost() {
        float totalEstimatedFailureCost = 0.0f;
        for (AnalyzerFailure failure : failures) {
            totalEstimatedFailureCost += failure.getEstimatedCost();
        }
        return totalEstimatedFailureCost;
    }

    private void appendFailureBlock(
            StringBuilder sb, AnalyzerFailure failure, String lineSeparator) {
        sb.append("----------------------------------------").append(lineSeparator);
        sb.append("- ")
                .append(safeText(failure.getStatementType()))
                .append(" ")
                .append(safeText(failure.getStatementId()))
                .append(" [")
                .append(failure.getFailureStage())
                .append("]")
                .append(lineSeparator);
        sb.append("  Reason: ").append(safeText(failure.getReason())).append(lineSeparator);
        sb.append("  Cost  : ")
                .append(formatEstimatedCost(failure.getEstimatedCost()))
                .append(lineSeparator);
        appendCostDetails(sb, failure, lineSeparator);
        sb.append("   SQL  : ").append(safeText(failure.getSql())).append(lineSeparator);
    }

    private void appendCostDetails(
            StringBuilder sb, AnalyzerFailure failure, String lineSeparator) {
        sb.append("  Cost details:").append(lineSeparator);
        if (failure.getCostDetails().isEmpty()) {
            sb.append("    (none)").append(lineSeparator);
            return;
        }

        for (AnalyzerCostDetail costDetail : failure.getCostDetails()) {
            sb.append("    - ")
                    .append(safeText(costDetail.getItemName()))
                    .append(" : count=")
                    .append(costDetail.getCount())
                    .append(", unit=")
                    .append(formatEstimatedCost(costDetail.getUnitCost()))
                    .append(", total=")
                    .append(formatEstimatedCost(costDetail.getTotalCost()))
                    .append(lineSeparator);
        }
    }

    private String formatEstimatedCost(float estimatedCost) {
        return String.format(Locale.US, "%.1f", estimatedCost);
    }
}
