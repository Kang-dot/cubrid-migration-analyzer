package com.cubrid.sqlanalyzer.command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cubrid.cubridmigration.cubrid.CUBRIDTimeUtil;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerObjectCountPreviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressObjectCount;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerSourceOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTableSizeViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTargetOverviewViewModel;

public class AnalyzerReport {
    private static final Pattern ERROR_LOCATION_PATTERN = Pattern.compile(
            "In line\\s+(\\d+),\\s*column\\s+(\\d+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern UNEXPECTED_TOKEN_PATTERN = Pattern.compile(
            "unexpected\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BEFORE_TOKEN_PATTERN = Pattern.compile(
            "before\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE);

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

    private static class HtmlSummaryRow {
        private final String objectType;
        private long totalCount;
        private int errorCount;
        private float cost;

        HtmlSummaryRow(String objectType, long totalCount) {
            this.objectType = objectType;
            this.totalCount = totalCount;
        }
    }

    private static class SqlContextLocation {
        private final int lineNumber;
        private final int columnNumber;
        private final boolean estimated;

        SqlContextLocation(int lineNumber, int columnNumber, boolean estimated) {
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
            this.estimated = estimated;
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
    private final List<String> sourceStatusMessages = new ArrayList<String>();
    private final List<AnalyzerFailure> failures = new ArrayList<AnalyzerFailure>();
    private final List<StatementResult> statementResults = new ArrayList<StatementResult>();
    private AnalyzerOverviewViewModel overview;
    private AnalyzerObjectCountPreviewViewModel objectCountPreview;

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

    public List<String> getSourceStatusMessages() {
        return sourceStatusMessages;
    }

    public void clearSourceStatusMessages() {
        sourceStatusMessages.clear();
    }

    public void addSourceStatusMessage(String sourceStatusMessage) {
        if (sourceStatusMessage != null && !sourceStatusMessage.isEmpty()) {
            sourceStatusMessages.add(sourceStatusMessage);
        }
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

    public List<AnalyzerProgressObjectCount> getObjectExecutionCounts() {
        Map<String, StatementTypeSummary> summaries = new LinkedHashMap<String, StatementTypeSummary>();
        for (StatementResult statementResult : statementResults) {
            if ("CLEANUP".equals(statementResult.statementType)) {
                continue;
            }

            String objectType = displayObjectType(statementResult.statementType);
            StatementTypeSummary summary = summaries.get(objectType);
            if (summary == null) {
                summary = new StatementTypeSummary();
                summaries.put(objectType, summary);
            }
            summary.add(statementResult.success);
        }

        List<AnalyzerProgressObjectCount> objectExecutionCounts = new ArrayList<AnalyzerProgressObjectCount>();
        for (Map.Entry<String, StatementTypeSummary> entry : summaries.entrySet()) {
            StatementTypeSummary summary = entry.getValue();
            objectExecutionCounts.add(
                    new AnalyzerProgressObjectCount(
                            entry.getKey(),
                            summary.totalCount,
                            summary.succeededCount,
                            summary.failedCount));
        }
        return objectExecutionCounts;
    }

    public AnalyzerOverviewViewModel getOverview() {
        return overview;
    }

    public void setOverview(AnalyzerOverviewViewModel overview) {
        this.overview = overview;
    }

    public AnalyzerObjectCountPreviewViewModel getObjectCountPreview() {
        return objectCountPreview;
    }

    public void setObjectCountPreview(AnalyzerObjectCountPreviewViewModel objectCountPreview) {
        this.objectCountPreview = objectCountPreview;
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
            File htmlReportFile = new File(reportDir, buildHtmlReportFileName());
            try (PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(htmlReportFile), "UTF-8"))) {
                writer.print(buildResultHtml());
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
        appendAnalysisSummary(sb, lineSeparator);
        appendFailedStatements(sb, lineSeparator);

        return sb.toString();
    }

    public String buildResultHtml() {
        StringBuilder sb = new StringBuilder();
        float totalCost = getTotalEstimatedFailureCost();

        sb.append("<!DOCTYPE html>\n");
        sb.append("<html>\n");
        sb.append("<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<title>SQL Analyzer Report</title>\n");
        sb.append("<style>\n");
        appendHtmlStyle(sb);
        sb.append("</style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");
        sb.append("<header>\n");
        sb.append("<h1>SQL Analyzer Report</h1>\n");
        sb.append("<p>Generated at ").append(escapeHtml(formatGeneratedAt())).append("</p>\n");
        sb.append("</header>\n");

        appendHtmlConnectionInfo(sb);
        appendHtmlTableSummary(sb);
        appendHtmlFailureDetails(sb);
        appendHtmlConclusion(sb, totalCost);

        sb.append("</body>\n");
        sb.append("</html>\n");
        return sb.toString();
    }

    private void appendHtmlStyle(StringBuilder sb) {
        sb.append("body{margin:0;padding:32px;background:#f5f7f9;color:#1f2933;font-family:Arial,sans-serif;font-size:13px;}\n");
        sb.append("header{border-bottom:1px solid #d7dde4;margin-bottom:24px;}\n");
        sb.append("h1{margin:0 0 8px;color:#006f9f;font-size:26px;}\n");
        sb.append("h2{margin:28px 0 12px;color:#006f9f;font-size:18px;}\n");
        sb.append("p{margin:0 0 16px;}\n");
        sb.append("table{border-collapse:collapse;width:100%;background:#fff;margin-bottom:16px;}\n");
        sb.append("th,td{border:1px solid #d7dde4;padding:8px 10px;text-align:left;vertical-align:top;}\n");
        sb.append("th{background:#eef3f7;color:#c14900;font-weight:bold;}\n");
        sb.append(".metric{font-weight:bold;color:#006f9f;}\n");
        sb.append(".number{text-align:right;white-space:nowrap;}\n");
        sb.append(".muted{color:#667085;}\n");
        sb.append(".status-ok{color:#147a3b;font-weight:bold;}\n");
        sb.append(".status-fail{color:#b42318;font-weight:bold;}\n");
        sb.append("details{background:#fff;border:1px solid #d7dde4;margin:0 0 12px;padding:10px;}\n");
        sb.append("summary{cursor:pointer;color:#006f9f;font-weight:bold;}\n");
        sb.append("pre{white-space:pre-wrap;word-break:break-word;background:#f8fafc;border:1px solid #e5e9ef;padding:10px;margin:8px 0 0;}\n");
        sb.append(".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:16px;}\n");
        sb.append(".box{background:#fff;border:1px solid #d7dde4;padding:12px;}\n");
    }

    private void appendHtmlConnectionInfo(StringBuilder sb) {
        sb.append("<section>\n");
        sb.append("<h2>Connection Info</h2>\n");
        sb.append("<table>\n");
        sb.append("<tr><th>Item</th><th>Value</th></tr>\n");
        appendHtmlInfoRow(sb, "Program", overview == null ? "" : overview.programVersion());
        appendHtmlInfoRow(sb, "Source", sourceType == null ? "" : String.valueOf(sourceType));
        if (overview != null && !overview.sources().isEmpty()) {
            for (AnalyzerSourceOverviewViewModel source : overview.sources()) {
                appendHtmlSourceInfo(sb, source);
            }
        }
        appendHtmlInfoRow(sb, "Target", targetType == null ? "" : String.valueOf(targetType));
        appendHtmlTargetInfo(sb, overview == null ? null : overview.target());
        appendHtmlInfoRow(sb, "Parser", isParserTarget() ? "Yes" : "No");
        appendHtmlInfoRow(sb, "Mode", executionMode == null ? "" : String.valueOf(executionMode));
        appendHtmlInfoRow(sb, "Schema name", resolveSchemaName());
        appendHtmlInfoRow(sb, "Catalog schema count", objectCountPreview == null
                ? ""
                : formatNumber(objectCountPreview.catalogSchemaCount()));
        appendHtmlInfoRow(sb, "Source table size", objectCountPreview == null
                ? ""
                : formatBytes(objectCountPreview.totalTableBytes()));
        sb.append("</table>\n");
        sb.append("</section>\n");
    }

    private void appendHtmlSourceInfo(StringBuilder sb, AnalyzerSourceOverviewViewModel source) {
        if (source == null) {
            return;
        }
        if (source.type() == AnalyzerSourceType.ORACLE) {
            appendHtmlInfoRow(sb, "Source Oracle URL",
                    formatText(source.jdbcUrl()) + formatVersionSuffix(source.version()));
            appendHtmlInfoRow(sb, "Source Oracle Host", formatHost(source.host(), source.port()));
            appendHtmlInfoRow(sb, "Source Oracle DB", source.databaseName());
            appendHtmlInfoRow(sb, "Source Oracle User", source.user());
            return;
        }

        if (source.type() == AnalyzerSourceType.XML) {
            appendHtmlInfoRow(sb, "Source XML Directory", source.xmlDirectory());
            appendHtmlInfoRow(sb, "Source XML Charset", source.xmlCharset());
            appendHtmlInfoRow(sb, "Source XML Files", formatNumber(source.xmlFileCount()));
        }
    }

    private void appendHtmlTargetInfo(StringBuilder sb, AnalyzerTargetOverviewViewModel target) {
        if (target == null) {
            return;
        }
        if (target.type() == AnalyzerTargetType.JDBC) {
            appendHtmlInfoRow(sb, "Target JDBC URL",
                    formatText(target.jdbcUrl()) + formatVersionSuffix(target.version()));
            appendHtmlInfoRow(sb, "Target Host", formatHost(target.host(), target.port()));
            appendHtmlInfoRow(sb, "Target DB", target.databaseName());
            appendHtmlInfoRow(sb, "Target User", target.user());
            return;
        }

        if (target.type() == AnalyzerTargetType.PARSER) {
            appendHtmlInfoRow(sb, "Parser Version", target.parserVersion());
        }
    }

    private void appendHtmlInfoRow(StringBuilder sb, String label, String value) {
        sb.append("<tr><td class=\"metric\">")
                .append(escapeHtml(label))
                .append("</td><td>")
                .append(escapeHtml(formatText(value)))
                .append("</td></tr>\n");
    }

    private void appendHtmlTableSummary(StringBuilder sb) {
        sb.append("<section>\n");
        sb.append("<h2>Table Summary</h2>\n");
        sb.append("<table>\n");
        sb.append("<tr><th>Object</th><th>Total</th><th>Error</th><th>Cost</th></tr>\n");
        List<HtmlSummaryRow> rows = buildHtmlSummaryRows();
        if (rows.isEmpty()) {
            sb.append("<tr><td colspan=\"4\" class=\"muted\">(none)</td></tr>\n");
        } else {
            for (HtmlSummaryRow row : rows) {
                sb.append("<tr><td>")
                        .append(escapeHtml(row.objectType))
                        .append("</td><td class=\"number\">")
                        .append(formatNumber(row.totalCount))
                        .append("</td><td class=\"number ")
                        .append(row.errorCount > 0 ? "status-fail" : "status-ok")
                        .append("\">")
                        .append(formatNumber(row.errorCount))
                        .append("</td><td class=\"number\">")
                        .append(escapeHtml(formatEstimatedCostWithTime(row.cost)))
                        .append("</td></tr>\n");
            }
        }
        sb.append("</table>\n");
        sb.append("</section>\n");
    }

    private void appendHtmlFailureDetails(StringBuilder sb) {
        sb.append("<section>\n");
        sb.append("<h2>Detail</h2>\n");
        if (failures.isEmpty() && failureMessages.isEmpty()) {
            sb.append("<p class=\"muted\">(none)</p>\n");
            sb.append("</section>\n");
            return;
        }

        for (AnalyzerFailure failure : failures) {
            appendHtmlFailureDetail(sb, failure);
        }
        for (String failureMessage : failureMessages) {
            sb.append("<details open><summary>Failure message</summary><pre>")
                    .append(escapeHtml(failureMessage))
                    .append("</pre></details>\n");
        }
        sb.append("</section>\n");
    }

    private void appendHtmlFailureDetail(StringBuilder sb, AnalyzerFailure failure) {
        SqlContextLocation location = findSqlContextLocation(failure.getReason(), failure.getSql());
        sb.append("<details open>\n");
        sb.append("<summary>")
                .append(escapeHtml(formatText(failure.getStatementType())))
                .append(" ")
                .append(escapeHtml(formatText(failure.getStatementId())))
                .append(" [")
                .append(escapeHtml(String.valueOf(failure.getFailureStage())))
                .append("]</summary>\n");
        sb.append("<table>\n");
        appendHtmlInfoRow(sb, "Object", displayObjectType(failure.getStatementType()));
        appendHtmlInfoRow(sb, "Statement ID", failure.getStatementId());
        appendHtmlInfoRow(sb, "Failure stage", String.valueOf(failure.getFailureStage()));
        appendHtmlInfoRow(sb, "Location", formatHtmlLocation(location));
        appendHtmlInfoRow(sb, "Cost", formatEstimatedCostWithTime(failure.getEstimatedCost()));
        appendHtmlInfoRow(sb, "Reason", failure.getReason());
        sb.append("</table>\n");
        appendHtmlCostDetails(sb, failure);
        appendHtmlAnnotatedSql(sb, failure, location);
        sb.append("</details>\n");
    }

    private void appendHtmlAnnotatedSql(
            StringBuilder sb,
            AnalyzerFailure failure,
            SqlContextLocation location) {
        String sql = formatText(failure.getSql());
        sb.append("<h3>Full Query</h3>\n");
        sb.append("<pre>");
        appendAnnotatedSqlLines(sb, sql, location, "\n", true, "");
        sb.append("</pre>\n");
    }

    private void appendHtmlCostDetails(StringBuilder sb, AnalyzerFailure failure) {
        sb.append("<h3>Error Cost Details</h3>\n");
        sb.append("<table>\n");
        sb.append("<tr><th>Item</th><th>Count</th><th>Unit Cost</th><th>Total Cost</th></tr>\n");
        if (failure.getCostDetails().isEmpty()) {
            sb.append("<tr><td colspan=\"4\" class=\"muted\">(none)</td></tr>\n");
        } else {
            for (AnalyzerCostDetail costDetail : failure.getCostDetails()) {
                sb.append("<tr><td>")
                        .append(escapeHtml(costDetail.getItemName()))
                        .append("</td><td class=\"number\">")
                        .append(formatNumber(costDetail.getCount()))
                        .append("</td><td class=\"number\">")
                        .append(escapeHtml(formatEstimatedCostWithTime(costDetail.getUnitCost())))
                        .append("</td><td class=\"number\">")
                        .append(escapeHtml(formatEstimatedCostWithTime(costDetail.getTotalCost())))
                        .append("</td></tr>\n");
            }
        }
        sb.append("</table>\n");
    }

    private void appendHtmlConclusion(StringBuilder sb, float totalCost) {
        sb.append("<section>\n");
        sb.append("<h2>Conclusion</h2>\n");
        sb.append("<div class=\"grid\">\n");
        appendHtmlConclusionBox(sb, "Total Cost", AnalyzerCostFormatter.formatCost(totalCost));
        appendHtmlConclusionBox(sb, "Estimated Time",
                String.format(Locale.US, "%.1f min", AnalyzerCostFormatter.toMinutes(totalCost)));
        appendHtmlConclusionBox(sb, "Analyzed", formatNumber(analyzedStatementCount));
        appendHtmlConclusionBox(sb, "Failed", formatNumber(failedStatementCount));
        sb.append("</div>\n");
        sb.append("</section>\n");
    }

    private void appendHtmlConclusionBox(StringBuilder sb, String label, String value) {
        sb.append("<div class=\"box\"><div class=\"muted\">")
                .append(escapeHtml(label))
                .append("</div><div class=\"metric\">")
                .append(escapeHtml(value))
                .append("</div></div>\n");
    }

    private List<HtmlSummaryRow> buildHtmlSummaryRows() {
        Map<String, HtmlSummaryRow> rows = new LinkedHashMap<String, HtmlSummaryRow>();
        appendObjectCountSummaryRows(rows);

        Map<String, StatementTypeSummary> executionSummaries = buildDisplayStatementTypeSummaries();
        for (Map.Entry<String, StatementTypeSummary> entry : executionSummaries.entrySet()) {
            HtmlSummaryRow row = getOrCreateHtmlSummaryRow(rows, entry.getKey());
            row.totalCount = Math.max(row.totalCount, entry.getValue().totalCount);
            row.errorCount = entry.getValue().failedCount;
        }

        for (AnalyzerFailure failure : failures) {
            HtmlSummaryRow row = getOrCreateHtmlSummaryRow(rows, displayObjectType(failure.getStatementType()));
            row.cost += failure.getEstimatedCost();
            if (row.errorCount == 0) {
                row.errorCount = 1;
            }
        }

        return new ArrayList<HtmlSummaryRow>(rows.values());
    }

    private void appendObjectCountSummaryRows(Map<String, HtmlSummaryRow> rows) {
        if (objectCountPreview == null) {
            return;
        }

        if (objectCountPreview.oracleSourceLoaded()) {
            putHtmlSummaryRow(rows, "SCHEMA", objectCountPreview.catalogSchemaCount());
            putHtmlSummaryRow(rows, "TABLE", objectCountPreview.targetTableCount());
            putHtmlSummaryRow(rows, "PK", objectCountPreview.targetPkCount());
            putHtmlSummaryRow(rows, "FK", objectCountPreview.targetFkCount());
            putHtmlSummaryRow(rows, "VIEW", objectCountPreview.targetViewCount());
            putHtmlSummaryRow(rows, "SERIAL", objectCountPreview.targetSerialCount());
            putHtmlSummaryRow(rows, "SYNONYM", objectCountPreview.targetSynonymCount());
            putHtmlSummaryRow(rows, "GRANT", objectCountPreview.targetGrantCount());
            putHtmlSummaryRow(rows, "PROCEDURE", objectCountPreview.targetProcedureCount());
            putHtmlSummaryRow(rows, "FUNCTION", objectCountPreview.targetFunctionCount());
            putHtmlSummaryRow(rows, "TRIGGER", objectCountPreview.targetTriggerCount());
        }

        if (objectCountPreview.xmlSourceLoaded()) {
            putHtmlSummaryRow(rows, "SELECT", objectCountPreview.selectCount());
            putHtmlSummaryRow(rows, "INSERT", objectCountPreview.insertCount());
            putHtmlSummaryRow(rows, "UPDATE", objectCountPreview.updateCount());
            putHtmlSummaryRow(rows, "DELETE", objectCountPreview.deleteCount());
        }
    }

    private void putHtmlSummaryRow(Map<String, HtmlSummaryRow> rows, String objectType, long count) {
        rows.put(objectType, new HtmlSummaryRow(objectType, count));
    }

    private HtmlSummaryRow getOrCreateHtmlSummaryRow(
            Map<String, HtmlSummaryRow> rows,
            String objectType) {
        String safeObjectType = formatText(objectType);
        if (safeObjectType.isEmpty()) {
            safeObjectType = "UNKNOWN";
        }
        HtmlSummaryRow row = rows.get(safeObjectType);
        if (row == null) {
            row = new HtmlSummaryRow(safeObjectType, 0);
            rows.put(safeObjectType, row);
        }
        return row;
    }

    private Map<String, StatementTypeSummary> buildDisplayStatementTypeSummaries() {
        Map<String, StatementTypeSummary> summaries = new LinkedHashMap<String, StatementTypeSummary>();
        for (StatementResult statementResult : statementResults) {
            if ("CLEANUP".equals(statementResult.statementType)) {
                continue;
            }

            String objectType = displayObjectType(statementResult.statementType);
            StatementTypeSummary summary = summaries.get(objectType);
            if (summary == null) {
                summary = new StatementTypeSummary();
                summaries.put(objectType, summary);
            }
            summary.add(statementResult.success);
        }
        return summaries;
    }

    private boolean isParserTarget() {
        if (targetType == AnalyzerTargetType.PARSER) {
            return true;
        }
        return overview != null
                && overview.target() != null
                && overview.target().type() == AnalyzerTargetType.PARSER;
    }

    private String resolveSchemaName() {
        if (overview == null || overview.sources().isEmpty()) {
            return "";
        }
        for (AnalyzerSourceOverviewViewModel source : overview.sources()) {
            if (source.type() == AnalyzerSourceType.ORACLE && !formatText(source.user()).isEmpty()) {
                return source.user();
            }
        }
        AnalyzerSourceOverviewViewModel source = overview.source();
        return source == null ? "" : formatText(source.databaseName());
    }

    private String formatHtmlLocation(SqlContextLocation location) {
        if (location == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("line ")
                .append(location.lineNumber)
                .append(", column ")
                .append(location.columnNumber);
        if (location.estimated) {
            sb.append(" (estimated)");
        }
        return sb.toString();
    }

    private String formatGeneratedAt() {
        long timeValue = generatedAt > 0 ? generatedAt : System.currentTimeMillis();
        return CUBRIDTimeUtil.getDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.US, TimeZone.getDefault())
                .format(new Date(timeValue));
    }

    private String escapeHtml(String value) {
        String text = formatText(value);
        StringBuilder escaped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '&':
                    escaped.append("&amp;");
                    break;
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    escaped.append("&#39;");
                    break;
                default:
                    escaped.append(ch);
                    break;
            }
        }
        return escaped.toString();
    }

    private void appendOverview(StringBuilder sb, String lineSeparator) {
        sb.append("Overview").append(lineSeparator);
        if (overview != null) {
            sb.append("Program     : ").append(formatText(overview.programVersion())).append(lineSeparator);
            appendSourceOverviews(sb, overview.sources(), lineSeparator);
            appendTargetOverview(sb, overview.target(), lineSeparator);
            sb.append("Mode        : ").append(overview.executionMode()).append(lineSeparator);
            appendSourceStatusMessages(sb, overview.sourceStatusMessages(), lineSeparator);
        } else {
            sb.append("Source      : ").append(sourceType).append(lineSeparator);
            sb.append("Target      : ").append(targetType).append(lineSeparator);
            sb.append("Mode        : ").append(executionMode).append(lineSeparator);
            appendSourceStatusMessages(sb, sourceStatusMessages, lineSeparator);
        }
        sb.append("Total       : ").append(analyzedStatementCount).append(lineSeparator);
        sb.append("OK          : ").append(succeededStatementCount).append(lineSeparator);
        sb.append("FAIL        : ").append(failedStatementCount).append(lineSeparator);
        sb.append("Cost        : ").append(formatEstimatedCostWithTime(getTotalEstimatedFailureCost()))
                .append(lineSeparator);
        sb.append(lineSeparator);
    }

    private void appendAnalysisSummary(StringBuilder sb, String lineSeparator) {
        sb.append("Analysis summary").append(lineSeparator);
        appendObjectCounts(sb, lineSeparator);
        appendObjectExecutionSummary(sb, lineSeparator);
        sb.append(lineSeparator);
    }

    private void appendObjectCounts(StringBuilder sb, String lineSeparator) {
        sb.append("Object counts").append(lineSeparator);
        if (objectCountPreview == null) {
            sb.append("(none)").append(lineSeparator).append(lineSeparator);
            return;
        }

        sb.append("DDL objects").append(lineSeparator);
        if (objectCountPreview.oracleSourceLoaded()) {
            sb.append("Catalog schemas : ")
                    .append(objectCountPreview.catalogSchemaCount())
                    .append(lineSeparator);
            sb.append("Target tables   : ")
                    .append(objectCountPreview.targetTableCount())
                    .append(lineSeparator);
            sb.append("Target PKs      : ")
                    .append(objectCountPreview.targetPkCount())
                    .append(lineSeparator);
            sb.append("Target FKs      : ")
                    .append(objectCountPreview.targetFkCount())
                    .append(lineSeparator);
            sb.append("Target views    : ")
                    .append(objectCountPreview.targetViewCount())
                    .append(lineSeparator);
            sb.append("Target serials  : ")
                    .append(objectCountPreview.targetSerialCount())
                    .append(lineSeparator);
            sb.append("Target synonyms : ")
                    .append(objectCountPreview.targetSynonymCount())
                    .append(lineSeparator);
            sb.append("Target grants   : ")
                    .append(objectCountPreview.targetGrantCount())
                    .append(lineSeparator);
            sb.append("Target procs    : ")
                    .append(objectCountPreview.targetProcedureCount())
                    .append(lineSeparator);
            sb.append("Target funcs    : ")
                    .append(objectCountPreview.targetFunctionCount())
                    .append(lineSeparator);
            sb.append("Target triggers : ")
                    .append(objectCountPreview.targetTriggerCount())
                    .append(lineSeparator);
            sb.append(lineSeparator);
            appendOracleTableSizes(sb, lineSeparator);
            sb.append(lineSeparator);
        } else {
            sb.append("  (none)").append(lineSeparator).append(lineSeparator);
        }

        sb.append("DML statements").append(lineSeparator);
        if (objectCountPreview.xmlSourceLoaded()) {
            sb.append("SELECT count    : ").append(objectCountPreview.selectCount()).append(lineSeparator);
            sb.append("INSERT count    : ").append(objectCountPreview.insertCount()).append(lineSeparator);
            sb.append("UPDATE count    : ").append(objectCountPreview.updateCount()).append(lineSeparator);
            sb.append("DELETE count    : ").append(objectCountPreview.deleteCount()).append(lineSeparator);
        } else {
            sb.append("  (none)").append(lineSeparator);
        }
        sb.append(lineSeparator);
    }

    private void appendOracleTableSizes(StringBuilder sb, String lineSeparator) {
        sb.append("Oracle table size total : ")
                .append(formatBytes(objectCountPreview.totalTableBytes()))
                .append(lineSeparator);
        sb.append("Oracle table sizes").append(lineSeparator);
        if (objectCountPreview.tableSizes().isEmpty()) {
            sb.append("  (none)").append(lineSeparator);
            return;
        }

        sb.append(String.format(Locale.US, "  %-32s %12s %12s", "Table", "Size", "Est. rows"))
                .append(lineSeparator);
        for (AnalyzerTableSizeViewModel tableSize : objectCountPreview.tableSizes()) {
            sb.append(
                    String.format(
                            Locale.US,
                            "  %-32s %12s %12s",
                            fitText(tableSize.tableName(), 32),
                            formatBytes(tableSize.bytes()),
                            formatNumber(tableSize.estimatedRows())))
                    .append(lineSeparator);
        }
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

    private void appendSourceOverviews(
            StringBuilder sb,
            List<AnalyzerSourceOverviewViewModel> sources,
            String lineSeparator) {
        if (sources == null || sources.isEmpty()) {
            sb.append("Source      : ").append(formatText(null)).append(lineSeparator);
            return;
        }

        for (AnalyzerSourceOverviewViewModel source : sources) {
            appendSourceOverview(sb, source, lineSeparator);
        }
    }

    private void appendSourceStatusMessages(
            StringBuilder sb,
            List<String> messages,
            String lineSeparator) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        sb.append("Source status").append(lineSeparator);
        for (String message : messages) {
            sb.append("  - ").append(formatText(message)).append(lineSeparator);
        }
    }

    private void appendTargetOverview(
            StringBuilder sb,
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
        }
    }

    private void appendObjectExecutionSummary(StringBuilder sb, String lineSeparator) {
        sb.append("Execution results").append(lineSeparator);
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

    private void appendFailedStatements(StringBuilder sb, String lineSeparator) {
        if (!failures.isEmpty()) {
            sb.append("Failed statements").append(lineSeparator);
            for (AnalyzerFailure failure : failures) {
                appendFailureBlock(sb, failure, lineSeparator);
            }
            sb.append("----------------------------------------").append(lineSeparator);
        } else if (!failureMessages.isEmpty()) {
            sb.append("Failed statements").append(lineSeparator);
            for (String failureMessage : failureMessages) {
                sb.append("----------------------------------------").append(lineSeparator);
                sb.append("- ").append(formatText(failureMessage)).append(lineSeparator);
            }
            sb.append("----------------------------------------").append(lineSeparator);
        } else {
            sb.append("Failed statements").append(lineSeparator);
            sb.append("(none)").append(lineSeparator);
        }
    }

    private Map<String, StatementTypeSummary> buildStatementTypeSummaries() {
        Map<String, StatementTypeSummary> summaries = new LinkedHashMap<String, StatementTypeSummary>();
        for (StatementResult statementResult : statementResults) {
            if ("CLEANUP".equals(statementResult.statementType)) {
                continue;
            }

            String statementType = formatText(statementResult.statementType);
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

    private String displayObjectType(String statementType) {
        String type = formatText(statementType);
        if (type.isEmpty()) {
            return "UNKNOWN";
        }

        if (type.startsWith("DDL_")) {
            return type.substring("DDL_".length());
        }
        return type;
    }

    private File getReportDirectory() {
        return new File(System.getProperty("user.dir"), "report");
    }

    private String buildReportFileName() {
        return getTimeStampSuffix() + ".txt";
    }

    private String buildHtmlReportFileName() {
        return getTimeStampSuffix() + ".html";
    }

    private String getTimeStampSuffix() {
        long timeValue = generatedAt > 0 ? generatedAt : System.currentTimeMillis();
        return "analyzer_result_"
                + CUBRIDTimeUtil.getDateFormat(
                        "yyyy_MM_dd_HH_mm_ss_SSS", Locale.US, TimeZone.getDefault())
                        .format(new Date(timeValue));
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
                .append(formatText(failure.getStatementType()))
                .append(" ")
                .append(formatText(failure.getStatementId()))
                .append(" [")
                .append(failure.getFailureStage())
                .append("]")
                .append(lineSeparator);
        sb.append("  Reason: ").append(formatText(failure.getReason())).append(lineSeparator);
        sb.append("  Cost  : ")
                .append(formatEstimatedCostWithTime(failure.getEstimatedCost()))
                .append(lineSeparator);
        appendCostDetails(sb, failure, lineSeparator);
        appendAnnotatedSql(sb, failure, lineSeparator);
    }

    private void appendAnnotatedSql(
            StringBuilder sb,
            AnalyzerFailure failure,
            String lineSeparator) {
        String sql = formatText(failure.getSql());
        String[] lines = splitSqlLines(sql);
        SqlContextLocation location = validSqlContextLocation(
                findSqlContextLocation(failure.getReason(), sql),
                lines.length);
        if (location != null) {
            sb.append("  Location: line ")
                    .append(location.lineNumber)
                    .append(", column ")
                    .append(location.columnNumber);
            if (location.estimated) {
                sb.append(" (estimated)");
            }
            sb.append(lineSeparator);
        }

        sb.append("  SQL:").append(lineSeparator);
        appendAnnotatedSqlLines(sb, sql, location, lineSeparator, false, "    ");
    }

    private void appendAnnotatedSqlLines(
            StringBuilder sb,
            String sql,
            SqlContextLocation location,
            String lineSeparator,
            boolean html,
            String linePrefix) {
        String[] lines = splitSqlLines(sql);
        location = validSqlContextLocation(location, lines.length);
        int lineNumberWidth = String.valueOf(lines.length).length();
        for (int lineNumber = 1; lineNumber <= lines.length; lineNumber++) {
            String sqlLine = lines[lineNumber - 1];
            appendMaybeEscaped(sb, linePrefix, html);
            appendMaybeEscaped(sb, formatLineNumber(lineNumber, lineNumberWidth), html);
            sb.append(" | ");
            appendMaybeEscaped(sb, sqlLine, html);
            sb.append(lineSeparator);
            if (location != null && lineNumber == location.lineNumber) {
                appendMaybeEscaped(sb, linePrefix, html);
                sb.append(" ".repeat(lineNumberWidth))
                        .append(" | ")
                        .append(" ".repeat(caretOffset(sqlLine, location.columnNumber)))
                        .append("^");
                if (location.estimated) {
                    sb.append(" estimated");
                }
                sb.append(lineSeparator);
            }
        }
    }

    private SqlContextLocation validSqlContextLocation(
            SqlContextLocation location,
            int lineCount) {
        if (location == null
                || location.lineNumber < 1
                || location.lineNumber > lineCount) {
            return null;
        }
        return location;
    }

    private void appendMaybeEscaped(StringBuilder sb, String value, boolean html) {
        sb.append(html ? escapeHtml(value) : formatText(value));
    }

    private SqlContextLocation findSqlContextLocation(String reason, String sql) {
        if (sql == null || sql.isEmpty()) {
            return null;
        }

        String[] lines = splitSqlLines(sql);
        Matcher locationMatcher = ERROR_LOCATION_PATTERN.matcher(formatText(reason));
        if (locationMatcher.find()) {
            int lineNumber = parsePositiveInt(locationMatcher.group(1));
            int columnNumber = parsePositiveInt(locationMatcher.group(2));
            if (lineNumber >= 1 && lineNumber <= lines.length) {
                return new SqlContextLocation(lineNumber, Math.max(1, columnNumber), false);
            }
        }

        return findTokenLocation(reason, lines);
    }

    private SqlContextLocation findTokenLocation(String reason, String[] lines) {
        List<String> candidates = new ArrayList<String>();
        addTokenCandidates(candidates, UNEXPECTED_TOKEN_PATTERN, reason);
        addTokenCandidates(candidates, BEFORE_TOKEN_PATTERN, reason);

        for (String candidate : candidates) {
            if (candidate.isEmpty()) {
                continue;
            }
            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                int columnIndex = lines[lineIndex].indexOf(candidate);
                if (columnIndex >= 0) {
                    return new SqlContextLocation(lineIndex + 1, columnIndex + 1, true);
                }
            }
        }
        return null;
    }

    private void addTokenCandidates(List<String> candidates, Pattern pattern, String reason) {
        Matcher matcher = pattern.matcher(formatText(reason));
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (candidate != null && !candidate.isEmpty()) {
                candidates.add(candidate);
            }
        }
    }

    private String[] splitSqlLines(String sql) {
        return formatText(sql).split("\\R", -1);
    }

    private int parsePositiveInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private String formatLineNumber(int lineNumber, int width) {
        return String.format(Locale.US, "%" + width + "d", lineNumber);
    }

    private int caretOffset(String sqlLine, int columnNumber) {
        int safeColumnNumber = Math.max(1, columnNumber);
        int maxOffset = sqlLine == null ? 0 : sqlLine.length();
        return Math.min(safeColumnNumber - 1, maxOffset);
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
                    .append(formatText(costDetail.getItemName()))
                    .append(" : count=")
                    .append(costDetail.getCount())
                    .append(", unit=")
                    .append(formatEstimatedCostWithTime(costDetail.getUnitCost()))
                    .append(", total=")
                    .append(formatEstimatedCostWithTime(costDetail.getTotalCost()))
                    .append(lineSeparator);
        }
    }

    private String formatEstimatedCostWithTime(float estimatedCost) {
        return AnalyzerCostFormatter.formatCostWithTime(estimatedCost);
    }

    private String formatBytes(long bytes) {
        long safeBytes = Math.max(0L, bytes);
        if (safeBytes < 1024L) {
            return safeBytes + " B";
        }

        double value = safeBytes;
        String[] units = { "B", "KB", "MB", "GB", "TB", "PB" };
        int unitIndex = 0;
        while (value >= 1024.0 && unitIndex < units.length - 1) {
            value /= 1024.0;
            unitIndex++;
        }
        return String.format(Locale.US, "%.2f %s", value, units[unitIndex]);
    }

    private String formatNumber(long value) {
        return String.format(Locale.US, "%,d", Math.max(0L, value));
    }

    private String fitText(String value, int maxLength) {
        String text = value == null ? "" : value;
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 1) + ".";
    }
}
