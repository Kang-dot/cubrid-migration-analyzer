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
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cubrid.cubridmigration.cubrid.CUBRIDTimeUtil;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerObjectCountPreviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressObjectCount;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerSourceOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTableSizeViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTargetOverviewViewModel;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes;

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
        private final String objectName;
        private final String sql;
        private final boolean success;
        private final String detail;
        private final AnalyzerFailureStage failureStage;

        StatementResult(
                String statementType,
                String statementId,
                String objectName,
                String sql,
                boolean success,
                String detail,
                AnalyzerFailureStage failureStage) {
            this.statementType = statementType;
            this.statementId = statementId;
            this.objectName = objectName;
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

    private enum HtmlSummaryPart {
        DDL,
        DML,
        PLCSQL
    }

    private static class HtmlSummaryRow {
        private final String objectType;
        private final List<HtmlSummaryRow> childRows = new ArrayList<HtmlSummaryRow>();
        private final List<AnalyzerTableSizeViewModel> tableSizeRows =
                new ArrayList<AnalyzerTableSizeViewModel>();
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

    private static class HtmlFailureGroup {
        private StatementResult parentStatement;
        private AnalyzerFailure parentFailure;
        private final List<AnalyzerFailure> staticSqlFailures = new ArrayList<AnalyzerFailure>();
    }

    private static class HtmlConclusionSummary {
        private int analyzedCount;
        private int failedCount;
        private float cost;
    }

    private AnalyzerSourceType sourceType;
    private AnalyzerTargetType targetType;
    private AnalyzerExecutionMode executionMode;
    private int analyzedStatementCount;
    private int succeededStatementCount;
    private int failedStatementCount;
    private boolean debugFullQuery;
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

    public boolean isDebugFullQuery() {
        return debugFullQuery;
    }

    public void setDebugFullQuery(boolean debugFullQuery) {
        this.debugFullQuery = debugFullQuery;
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
        addStatementResult(
                statementType, statementId, null, sql, success, detail, failureStage);
    }

    public void addStatementResult(
            String statementType,
            String statementId,
            String objectName,
            String sql,
            boolean success,
            String detail,
            AnalyzerFailureStage failureStage) {
        statementResults.add(
                new StatementResult(
                        statementType, statementId, objectName, sql, success, detail, failureStage));
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
        appendExecutedFullQueries(sb, lineSeparator);

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
        appendHtmlExecutedFullQueries(sb);
        appendHtmlConclusion(sb, totalCost);

        appendHtmlScript(sb);
        sb.append("</body>\n");
        sb.append("</html>\n");
        return sb.toString();
    }

    private void appendHtmlStyle(StringBuilder sb) {
        sb.append("body{margin:0;padding:32px;background:#f5f7f9;color:#1f2933;font-family:Arial,sans-serif;font-size:13px;}\n");
        sb.append("header{border-bottom:1px solid #d7dde4;margin-bottom:24px;}\n");
        sb.append("h1{margin:0 0 8px;color:#006f9f;font-size:26px;}\n");
        sb.append("h2{margin:28px 0 12px;color:#006f9f;font-size:18px;}\n");
        sb.append("h3{margin:18px 0 8px;color:#1f2933;font-size:15px;}\n");
        sb.append("p{margin:0 0 16px;}\n");
        sb.append("table{border-collapse:collapse;width:100%;background:#fff;margin-bottom:16px;}\n");
        sb.append("th,td{border:1px solid #d7dde4;padding:8px 10px;text-align:left;vertical-align:top;}\n");
        sb.append("th{background:#eef3f7;color:#c14900;font-weight:bold;}\n");
        sb.append(".metric{font-weight:bold;color:#006f9f;}\n");
        sb.append(".number{text-align:right;white-space:nowrap;}\n");
        sb.append(".muted{color:#667085;}\n");
        sb.append(".status-ok{color:#147a3b;font-weight:bold;}\n");
        sb.append(".status-fail{color:#b42318;font-weight:bold;}\n");
        sb.append(".row-toggle{border:0;background:transparent;color:#006f9f;cursor:pointer;font-weight:bold;margin:0 6px 0 0;padding:0;width:16px;}\n");
        sb.append(".summary-child-object{display:inline-block;padding-left:22px;}\n");
        sb.append(".summary-part{padding:0;overflow:hidden;}\n");
        sb.append(".summary-part>summary{background:#f8fafc;padding:10px;}\n");
        sb.append(".summary-part>table{margin:0;}\n");
        sb.append(".summary-total-row td{background:#f8fafc;font-weight:bold;}\n");
        sb.append(".nested-summary-cell{background:#f8fafc;padding:10px 10px 10px 32px;}\n");
        sb.append(".nested-summary-table{margin:0;background:#fff;}\n");
        sb.append("details{background:#fff;border:1px solid #d7dde4;margin:0 0 12px;padding:10px;}\n");
        sb.append("summary{cursor:pointer;color:#006f9f;font-weight:bold;}\n");
        sb.append("details.detail-item[open]>summary{padding-bottom:10px;}\n");
        sb.append(".report-section{background:#fff;border:1px solid #d7dde4;margin:0 0 18px;padding:14px;}\n");
        sb.append(".report-section table:last-child{margin-bottom:0;}\n");
        sb.append(".section-heading{display:flex;align-items:center;gap:8px;margin:0 0 12px;padding-bottom:10px;border-bottom:1px solid #e5e9ef;cursor:pointer;}\n");
        sb.append(".section-heading h2{margin:0;}\n");
        sb.append(".section-toggle{border:1px solid #d7dde4;background:#f8fafc;color:#006f9f;cursor:pointer;font-weight:bold;width:24px;height:24px;line-height:20px;padding:0;}\n");
        sb.append(".collapsed-section-summary{margin:0;}\n");
        sb.append(".collapsed-section-summary .metric{width:180px;}\n");
        sb.append("pre{white-space:pre-wrap;word-break:break-word;background:#f8fafc;border:1px solid #e5e9ef;padding:10px;margin:8px 0 0;}\n");
        sb.append(".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:16px;}\n");
        sb.append(".box{background:#fff;border:1px solid #d7dde4;padding:12px;}\n");
    }

    private void appendHtmlScript(StringBuilder sb) {
        sb.append("<script>\n");
        sb.append("function toggleSummaryRows(button, groupId){\n");
        sb.append("var rows=document.querySelectorAll('[data-summary-parent=\"'+groupId+'\"]');\n");
        sb.append("var expanded=button.getAttribute('aria-expanded')==='true';\n");
        sb.append("for(var i=0;i<rows.length;i++){rows[i].hidden=expanded;}\n");
        sb.append("button.setAttribute('aria-expanded',String(!expanded));\n");
        sb.append("button.innerHTML=expanded?'&#9656;':'&#9662;';\n");
        sb.append("}\n");
        sb.append("function toggleReportSection(button, sectionId){\n");
        sb.append("if(!button){return;}\n");
        sb.append("var body=document.getElementById(sectionId+'-body');\n");
        sb.append("var summary=document.getElementById(sectionId+'-summary');\n");
        sb.append("var expanded=button.getAttribute('aria-expanded')==='true';\n");
        sb.append("if(body){body.hidden=expanded;}\n");
        sb.append("if(summary){summary.hidden=!expanded;}\n");
        sb.append("button.setAttribute('aria-expanded',String(!expanded));\n");
        sb.append("button.innerHTML=expanded?'&#9656;':'&#9662;';\n");
        sb.append("}\n");
        sb.append("function toggleReportSectionFromHeading(heading, sectionId){\n");
        sb.append("var button=heading?heading.querySelector('.section-toggle'):null;\n");
        sb.append("toggleReportSection(button,sectionId);\n");
        sb.append("}\n");
        sb.append("</script>\n");
    }

    private void appendHtmlReportSectionStart(StringBuilder sb, String title) {
        String sectionId = htmlSummaryGroupId("report-section-" + title);
        sb.append("<section class=\"report-section\">\n");
        sb.append("<div class=\"section-heading\" onclick=\"toggleReportSectionFromHeading(this,'")
                .append(sectionId)
                .append("')\"><button type=\"button\" class=\"section-toggle\"")
                .append(" aria-expanded=\"true\" onclick=\"toggleReportSection(this,'")
                .append(sectionId)
                .append("');event.stopPropagation();\">&#9662;</button><h2>")
                .append(escapeHtml(title))
                .append("</h2></div>\n");
        appendHtmlCollapsedSectionSummary(sb, sectionId);
        sb.append("<div class=\"section-body\" id=\"")
                .append(sectionId)
                .append("-body\">\n");
    }

    private void appendHtmlReportSectionEnd(StringBuilder sb) {
        sb.append("</div>\n");
        sb.append("</section>\n");
    }

    private void appendHtmlCollapsedSectionSummary(StringBuilder sb, String sectionId) {
        sb.append("<table class=\"collapsed-section-summary\" id=\"")
                .append(sectionId)
                .append("-summary\" hidden>\n");
        sb.append("<tr><td class=\"metric\">Source schema</td><td>")
                .append(escapeHtml(resolveSchemaName()))
                .append("</td></tr>\n");
        sb.append("<tr><td class=\"metric\">Target type</td><td>")
                .append(escapeHtml(resolveTargetTypeName()))
                .append("</td></tr>\n");
        sb.append("<tr><td class=\"metric\">Source table size</td><td>")
                .append(escapeHtml(resolveSourceTableSize()))
                .append("</td></tr>\n");
        sb.append("</table>\n");
    }

    private void appendHtmlConnectionInfo(StringBuilder sb) {
        appendHtmlReportSectionStart(sb, "Connection Info");
        sb.append("<table>\n");
        sb.append("<tr><th>Item</th><th>Value</th></tr>\n");
        appendHtmlInfoRow(sb, "Program", overview == null ? "" : overview.programVersion());
        if (overview != null && !overview.sources().isEmpty()) {
            for (AnalyzerSourceOverviewViewModel source : overview.sources()) {
                appendHtmlSourceInfo(sb, source);
            }
        }
        appendHtmlInfoRow(sb, "Target", targetType == null ? "" : String.valueOf(targetType));
        appendHtmlTargetInfo(sb, overview == null ? null : overview.target());
        appendHtmlInfoRow(sb, "Parser", isParserTarget() ? "Yes" : "No");
        appendHtmlInfoRow(sb, "Schema name", resolveSchemaName());
        appendHtmlInfoRow(sb, "Catalog schema count", objectCountPreview == null
                ? ""
                : formatNumber(objectCountPreview.catalogSchemaCount()));
        appendHtmlInfoRow(sb, "Source table size", objectCountPreview == null
                ? ""
                : formatBytes(objectCountPreview.totalTableBytes()));
        sb.append("</table>\n");
        appendHtmlReportSectionEnd(sb);
    }

    private void appendHtmlSourceInfo(StringBuilder sb, AnalyzerSourceOverviewViewModel source) {
        if (source == null) {
            return;
        }
        if (source.type() == AnalyzerSourceType.ORACLE) {
            appendHtmlInfoRow(sb, "Source Oracle URL",
                    nullToEmpty(source.jdbcUrl()) + formatVersionSuffix(source.version()));
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
                    nullToEmpty(target.jdbcUrl()) + formatVersionSuffix(target.version()));
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
                .append(escapeHtml(nullToEmpty(value)))
                .append("</td></tr>\n");
    }

    private String formatHtmlSummaryObjectName(String objectName) {
        String safeObjectName = nullToEmpty(objectName);
        if (safeObjectName.isEmpty()) {
            return "";
        }
        return " - " + escapeHtml(safeObjectName);
    }

    private void appendHtmlTableSummary(StringBuilder sb) {
        appendHtmlReportSectionStart(sb, "Table Summary");
        List<HtmlSummaryRow> rows = buildHtmlSummaryRows();
        if (rows.isEmpty()) {
            sb.append("<table>\n");
            sb.append("<tr><th>Object</th><th>Total</th><th>Error</th><th>Cost</th></tr>\n");
            sb.append("<tr><td colspan=\"4\" class=\"muted\">(none)</td></tr>\n");
            sb.append("</table>\n");
        } else {
            appendHtmlSummaryPart(
                    sb,
                    "1. Database Objects (DDL Migration)",
                    filterHtmlSummaryRows(rows, HtmlSummaryPart.DDL));
            appendHtmlSummaryPart(
                    sb,
                    "2. Application Queries (DML/SQL Mapping Migration)",
                    filterHtmlSummaryRows(rows, HtmlSummaryPart.DML));
            appendHtmlSummaryPart(
                    sb,
                    "PL/CSQL",
                    filterHtmlSummaryRows(rows, HtmlSummaryPart.PLCSQL));
        }
        appendHtmlReportSectionEnd(sb);
    }

    private void appendHtmlSummaryPart(
            StringBuilder sb,
            String title,
            List<HtmlSummaryRow> rows) {
        sb.append("<details class=\"summary-part\" open><summary>")
                .append(escapeHtml(title))
                .append("</summary>\n");
        sb.append("<table>\n");
        sb.append("<tr><th>Object</th><th>Total</th><th>Error</th><th>Cost</th></tr>\n");
        if (rows.isEmpty()) {
            sb.append("<tr><td colspan=\"4\" class=\"muted\">(none)</td></tr>\n");
        } else {
            for (HtmlSummaryRow row : rows) {
                appendHtmlSummaryRow(sb, row);
            }
        }
        appendHtmlSummaryTotalRow(sb, rows);
        sb.append("</table>\n");
        sb.append("</details>\n");
    }

    private void appendHtmlSummaryTotalRow(StringBuilder sb, List<HtmlSummaryRow> rows) {
        long totalCount = 0;
        int errorCount = 0;
        float cost = 0;
        for (HtmlSummaryRow row : rows) {
            totalCount += row.totalCount;
            errorCount += row.errorCount;
            cost += row.cost;
        }
        sb.append("<tr class=\"summary-total-row\"><td>Total</td><td class=\"number\">")
                .append(formatNumber(totalCount))
                .append("</td><td class=\"number ")
                .append(errorCount > 0 ? "status-fail" : "status-ok")
                .append("\">")
                .append(formatNumber(errorCount))
                .append("</td><td class=\"number\">")
                .append(escapeHtml(formatEstimatedCostWithTime(cost)))
                .append("</td></tr>\n");
    }

    private List<HtmlSummaryRow> filterHtmlSummaryRows(
            List<HtmlSummaryRow> rows,
            HtmlSummaryPart part) {
        List<HtmlSummaryRow> result = new ArrayList<HtmlSummaryRow>();
        for (HtmlSummaryRow row : rows) {
            if (htmlSummaryPart(row) == part) {
                result.add(row);
            }
        }
        return result;
    }

    private HtmlSummaryPart htmlSummaryPart(HtmlSummaryRow row) {
        String objectType = nullToEmpty(row == null ? null : row.objectType);
        if (isDmlSummaryType(objectType)) {
            return HtmlSummaryPart.DML;
        }
        if (isPlcsqlSummaryType(objectType)) {
            return HtmlSummaryPart.PLCSQL;
        }
        return HtmlSummaryPart.DDL;
    }

    private boolean isDmlSummaryType(String objectType) {
        return "SELECT".equals(objectType)
                || "INSERT".equals(objectType)
                || "UPDATE".equals(objectType)
                || "DELETE".equals(objectType)
                || AnalyzerStatementTypes.TYPE_STATIC_SQL.equals(objectType);
    }

    private boolean isPlcsqlSummaryType(String objectType) {
        return "PROCEDURE".equals(objectType)
                || "FUNCTION".equals(objectType)
                || "PROC_HEADER".equals(objectType)
                || "PROC_BODY".equals(objectType)
                || "FUNC_HEADER".equals(objectType)
                || "FUNC_BODY".equals(objectType);
    }

    private void appendHtmlSummaryRow(StringBuilder sb, HtmlSummaryRow row) {
        if (!row.tableSizeRows.isEmpty()) {
            appendHtmlTableSizeExpandableSummaryRow(sb, row);
            return;
        }

        if (!row.childRows.isEmpty()) {
            appendHtmlExpandableSummaryRow(sb, row);
            return;
        }

        appendHtmlPlainSummaryRow(sb, row);
    }

    private void appendHtmlPlainSummaryRow(StringBuilder sb, HtmlSummaryRow row) {
        appendHtmlPlainSummaryRow(sb, row, "", false);
    }

    private void appendHtmlPlainSummaryRow(
            StringBuilder sb,
            HtmlSummaryRow row,
            String rowAttributes,
            boolean childRow) {
        sb.append("<tr")
                .append(rowAttributes)
                .append("><td>");
        if (childRow) {
            sb.append("<span class=\"summary-child-object\">");
        }
        sb.append(escapeHtml(row.objectType));
        if (childRow) {
            sb.append("</span>");
        }
        sb.append("</td><td class=\"number\">")
                .append(formatNumber(row.totalCount))
                .append("</td><td class=\"number ")
                .append(row.errorCount > 0 ? "status-fail" : "status-ok")
                .append("\">")
                .append(formatNumber(row.errorCount))
                .append("</td><td class=\"number\">")
                .append(escapeHtml(formatEstimatedCostWithTime(row.cost)))
                .append("</td></tr>\n");
    }

    private void appendHtmlTableSizeExpandableSummaryRow(StringBuilder sb, HtmlSummaryRow row) {
        String groupId = htmlSummaryGroupId(row.objectType);
        sb.append("<tr><td><button type=\"button\" class=\"row-toggle\" aria-expanded=\"false\"")
                .append(" onclick=\"toggleSummaryRows(this,'")
                .append(groupId)
                .append("')\">&#9656;</button>")
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
        sb.append("<tr class=\"summary-child-row\" data-summary-parent=\"")
                .append(groupId)
                .append("\" hidden><td colspan=\"4\" class=\"nested-summary-cell\">\n");
        appendHtmlTableSizeNestedTable(sb, row.tableSizeRows);
        sb.append("</td></tr>\n");
    }

    private void appendHtmlTableSizeNestedTable(
            StringBuilder sb,
            List<AnalyzerTableSizeViewModel> tableSizes) {
        sb.append("<table class=\"nested-summary-table\">\n");
        sb.append("<tr><th>Table</th><th>Size</th><th>Est. rows</th></tr>\n");
        for (AnalyzerTableSizeViewModel tableSize : tableSizes) {
            sb.append("<tr><td>")
                    .append(escapeHtml(tableSize.tableName()))
                    .append("</td><td class=\"number\">")
                    .append(escapeHtml(formatBytes(tableSize.bytes())))
                    .append("</td><td class=\"number\">")
                    .append(formatNumber(tableSize.estimatedRows()))
                    .append("</td></tr>\n");
        }
        sb.append("</table>\n");
    }

    private void appendHtmlExpandableSummaryRow(StringBuilder sb, HtmlSummaryRow row) {
        String groupId = htmlSummaryGroupId(row.objectType);
        sb.append("<tr><td><button type=\"button\" class=\"row-toggle\" aria-expanded=\"false\"")
                .append(" onclick=\"toggleSummaryRows(this,'")
                .append(groupId)
                .append("')\">&#9656;</button>")
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
        for (HtmlSummaryRow childRow : row.childRows) {
            appendHtmlPlainSummaryRow(
                    sb,
                    childRow,
                    " class=\"summary-child-row\" data-summary-parent=\"" + groupId + "\" hidden",
                    true);
        }
    }

    private String htmlSummaryGroupId(String objectType) {
        String safeObjectType = nullToEmpty(objectType).toLowerCase(Locale.US);
        safeObjectType = safeObjectType.replaceAll("[^a-z0-9]+", "-");
        if (safeObjectType.isEmpty()) {
            return "summary-unknown";
        }
        return "summary-" + safeObjectType;
    }

    private void appendHtmlFailureDetails(StringBuilder sb) {
        appendHtmlReportSectionStart(sb, "Detail");
        if (failures.isEmpty()) {
            sb.append("<p class=\"muted\">(none)</p>\n");
            appendHtmlReportSectionEnd(sb);
            return;
        }

        Map<String, HtmlFailureGroup> staticSqlFailureGroups = buildStaticSqlFailureGroups();
        for (AnalyzerFailure failure : failures) {
            if (isStaticSqlStatement(failure.getStatementId())) {
                continue;
            }
            appendHtmlFailureDetail(sb, failure);
        }
        for (HtmlFailureGroup group : staticSqlFailureGroups.values()) {
            appendHtmlStaticSqlFailureGroup(sb, group);
        }
        appendHtmlReportSectionEnd(sb);
    }

    private void appendHtmlExecutedFullQueries(StringBuilder sb) {
        if (!debugFullQuery) {
            return;
        }

        appendHtmlReportSectionStart(sb, "Executed Full Queries");
        if (statementResults.isEmpty()) {
            sb.append("<p class=\"muted\">(none)</p>\n");
            appendHtmlReportSectionEnd(sb);
            return;
        }

        for (StatementResult statementResult : statementResults) {
            sb.append("<details>\n");
            sb.append("<summary>")
                    .append(escapeHtml(nullToEmpty(statementResult.statementType)))
                    .append(" ")
                    .append(escapeHtml(nullToEmpty(statementResult.statementId)))
                    .append(formatHtmlSummaryObjectName(statementResult.objectName))
                    .append(" [")
                    .append(statementResult.success ? "OK" : "FAIL")
                    .append("]</summary>\n");
            sb.append("<table>\n");
            appendHtmlInfoRow(sb, "Object", displayObjectType(statementResult.statementType));
            appendHtmlInfoRow(sb, "Statement ID", statementResult.statementId);
            appendHtmlInfoRow(sb, "Object name", statementResult.objectName);
            appendHtmlInfoRow(sb, "Status", statementResult.success ? "OK" : "FAIL");
            appendHtmlInfoRow(sb, "Stage", statementResult.failureStage == null
                    ? ""
                    : String.valueOf(statementResult.failureStage));
            appendHtmlInfoRow(sb, "Detail", statementResult.detail);
            sb.append("</table>\n");
            appendHtmlStatementSql(sb, "Full Query", statementResult.sql);
            sb.append("</details>\n");
        }
        appendHtmlReportSectionEnd(sb);
    }

    private Map<String, HtmlFailureGroup> buildStaticSqlFailureGroups() {
        Map<String, HtmlFailureGroup> groups = new LinkedHashMap<String, HtmlFailureGroup>();
        for (AnalyzerFailure failure : failures) {
            String parentId = staticSqlParentId(failure.getStatementId());
            if (parentId == null) {
                continue;
            }

            HtmlFailureGroup group = groups.get(parentId);
            if (group == null) {
                group = new HtmlFailureGroup();
                group.parentStatement = findStatementResult(parentId);
                group.parentFailure = findFailure(parentId);
                groups.put(parentId, group);
            }
            group.staticSqlFailures.add(failure);
        }
        return groups;
    }

    private void appendHtmlStaticSqlFailureGroup(StringBuilder sb, HtmlFailureGroup group) {
        StatementResult parent = group.parentStatement;
        AnalyzerFailure parentFailure = group.parentFailure;
        String parentType = parentFailure != null
                ? parentFailure.getStatementType()
                : parent == null ? "PLCSQL" : parent.statementType;
        String parentId = parentFailure != null
                ? parentFailure.getStatementId()
                : parent == null ? staticSqlParentId(group.staticSqlFailures.get(0).getStatementId()) : parent.statementId;
        String parentObjectName = parentFailure != null
                ? parentFailure.getObjectName()
                : parent == null ? "" : parent.objectName;

        sb.append("<details class=\"detail-item\" open>\n");
        sb.append("<summary>")
                .append(escapeHtml(nullToEmpty(parentType)))
                .append(" ")
                .append(escapeHtml(nullToEmpty(parentId)))
                .append(formatHtmlSummaryObjectName(parentObjectName))
                .append(" [STATIC SQL]</summary>\n");
        sb.append("<table>\n");
        appendHtmlInfoRow(sb, "Object", displayObjectType(parentType));
        appendHtmlInfoRow(sb, "Statement ID", parentId);
        appendHtmlInfoRow(sb, "Object name", parentObjectName);
        appendHtmlInfoRow(sb, "Status", parentFailure == null ? "parsed" : "failed");
        appendHtmlInfoRow(sb, "Static SQL failures", formatNumber(group.staticSqlFailures.size()));
        sb.append("</table>\n");

        if (parentFailure != null) {
            appendHtmlFailureBody(sb, parentFailure, "PL/CSQL Query");
        } else if (parent != null) {
            appendHtmlStatementSql(sb, "PL/CSQL Query", parent.sql);
        }

        for (AnalyzerFailure staticSqlFailure : group.staticSqlFailures) {
            sb.append("<h3>Static SQL Failure</h3>\n");
            appendHtmlFailureBody(sb, staticSqlFailure, "Static SQL Query");
        }
        sb.append("</details>\n");
    }

    private void appendHtmlStatementSql(StringBuilder sb, String title, String sql) {
        sb.append("<h3>").append(escapeHtml(title)).append("</h3>\n");
        sb.append("<pre>");
        appendAnnotatedSqlLines(sb, nullToEmpty(sql), null, "\n", this::escapeHtml, "");
        sb.append("</pre>\n");
    }

    private void appendHtmlFailureDetail(StringBuilder sb, AnalyzerFailure failure) {
        sb.append("<details class=\"detail-item\" open>\n");
        sb.append("<summary>")
                .append(escapeHtml(nullToEmpty(failure.getStatementType())))
                .append(" ")
                .append(escapeHtml(nullToEmpty(failure.getStatementId())))
                .append(formatHtmlSummaryObjectName(failure.getObjectName()))
                .append(" [")
                .append(escapeHtml(String.valueOf(failure.getFailureStage())))
                .append("]</summary>\n");
        appendHtmlFailureBody(sb, failure, "Full Query");
        sb.append("</details>\n");
    }

    private void appendHtmlFailureBody(
            StringBuilder sb,
            AnalyzerFailure failure,
            String queryTitle) {
        SqlContextLocation location = findSqlContextLocation(failure.getReason(), failure.getSql());
        sb.append("<table>\n");
        appendHtmlInfoRow(sb, "Object", displayObjectType(failure.getStatementType()));
        appendHtmlInfoRow(sb, "Statement ID", failure.getStatementId());
        appendHtmlInfoRow(sb, "Object name", failure.getObjectName());
        appendHtmlInfoRow(sb, "Failure stage", String.valueOf(failure.getFailureStage()));
        appendHtmlInfoRow(sb, "Location", formatHtmlLocation(location));
        appendHtmlInfoRow(sb, "Cost", formatEstimatedCostWithTime(failure.getEstimatedCost()));
        appendHtmlInfoRow(sb, "Reason", failure.getReason());
        sb.append("</table>\n");
        appendHtmlCostDetails(sb, failure);
        appendHtmlAnnotatedSql(sb, failure, location, queryTitle);
    }

    private void appendHtmlAnnotatedSql(
            StringBuilder sb,
            AnalyzerFailure failure,
            SqlContextLocation location) {
        appendHtmlAnnotatedSql(sb, failure, location, "Full Query");
    }

    private void appendHtmlAnnotatedSql(
            StringBuilder sb,
            AnalyzerFailure failure,
            SqlContextLocation location,
            String title) {
        String sql = nullToEmpty(failure.getSql());
        sb.append("<h3>").append(escapeHtml(title)).append("</h3>\n");
        sb.append("<pre>");
        appendAnnotatedSqlLines(sb, sql, location, "\n", this::escapeHtml, "");
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
        Map<String, HtmlConclusionSummary> summaries = buildHtmlConclusionSummaries(totalCost);
        sb.append("<table>\n");
        sb.append("<tr><th>Category</th><th>Analyzed</th><th>Failed</th><th>Total Cost</th><th>Estimated Time</th></tr>\n");
        appendHtmlConclusionRow(sb, "DDL + PL/CSQL", summaries.get("DDL_PLCSQL"));
        appendHtmlConclusionRow(sb, "DML", summaries.get("DML"));
        sb.append("</table>\n");
        sb.append("</section>\n");
    }

    private Map<String, HtmlConclusionSummary> buildHtmlConclusionSummaries(float totalCost) {
        Map<String, HtmlConclusionSummary> summaries = new LinkedHashMap<String, HtmlConclusionSummary>();
        summaries.put("DDL_PLCSQL", new HtmlConclusionSummary());
        summaries.put("DML", new HtmlConclusionSummary());

        for (StatementResult statementResult : statementResults) {
            if ("CLEANUP".equals(statementResult.statementType)) {
                continue;
            }
            HtmlConclusionSummary summary = summaries.get(htmlConclusionCategory(statementResult));
            summary.analyzedCount++;
            if (!statementResult.success) {
                summary.failedCount++;
            }
        }

        for (AnalyzerFailure failure : failures) {
            HtmlConclusionSummary summary = summaries.get(htmlConclusionCategory(failure));
            summary.cost += failure.getEstimatedCost();
        }

        if (statementResults.isEmpty() && analyzedStatementCount > 0) {
            HtmlConclusionSummary summary = summaries.get("DDL_PLCSQL");
            summary.analyzedCount = analyzedStatementCount;
            summary.failedCount = failedStatementCount;
            summary.cost = totalCost;
        }

        return summaries;
    }

    private String htmlConclusionCategory(StatementResult statementResult) {
        if (staticSqlParentObjectType(statementResult.statementId) != null) {
            return "DDL_PLCSQL";
        }
        HtmlSummaryPart part = htmlSummaryPart(new HtmlSummaryRow(
                displayObjectType(statementResult.statementType),
                0));
        return part == HtmlSummaryPart.DML ? "DML" : "DDL_PLCSQL";
    }

    private String htmlConclusionCategory(AnalyzerFailure failure) {
        if (staticSqlParentObjectType(failure.getStatementId()) != null) {
            return "DDL_PLCSQL";
        }
        HtmlSummaryPart part = htmlSummaryPart(new HtmlSummaryRow(
                displayObjectType(failure.getStatementType()),
                0));
        return part == HtmlSummaryPart.DML ? "DML" : "DDL_PLCSQL";
    }

    private void appendHtmlConclusionRow(
            StringBuilder sb,
            String category,
            HtmlConclusionSummary summary) {
        HtmlConclusionSummary safeSummary = summary == null ? new HtmlConclusionSummary() : summary;
        sb.append("<tr><td class=\"metric\">")
                .append(escapeHtml(category))
                .append("</td><td class=\"number\">")
                .append(formatNumber(safeSummary.analyzedCount))
                .append("</td><td class=\"number ")
                .append(safeSummary.failedCount > 0 ? "status-fail" : "status-ok")
                .append("\">")
                .append(formatNumber(safeSummary.failedCount))
                .append("</td><td class=\"number\">")
                .append(escapeHtml(AnalyzerCostFormatter.formatCost(safeSummary.cost)))
                .append("</td><td class=\"number\">")
                .append(escapeHtml(AnalyzerCostFormatter.formatTime(safeSummary.cost)))
                .append("</td></tr>\n");
    }

    private List<HtmlSummaryRow> buildHtmlSummaryRows() {
        Map<String, HtmlSummaryRow> rows = new LinkedHashMap<String, HtmlSummaryRow>();
        appendObjectCountSummaryRows(rows);

        Map<String, StatementTypeSummary> rootExecutionSummaries =
                new LinkedHashMap<String, StatementTypeSummary>();
        for (StatementResult statementResult : statementResults) {
            if ("CLEANUP".equals(statementResult.statementType)) {
                continue;
            }

            String objectType = displayObjectType(statementResult.statementType);
            String parentObjectType = staticSqlParentObjectType(statementResult.statementId);
            if (parentObjectType == null) {
                StatementTypeSummary summary = rootExecutionSummaries.get(objectType);
                if (summary == null) {
                    summary = new StatementTypeSummary();
                    rootExecutionSummaries.put(objectType, summary);
                }
                summary.add(statementResult.success);
                continue;
            }

            addHtmlSummaryChildResult(
                    rows,
                    parentObjectType,
                    "STATIC " + objectType,
                    statementResult.success);
        }

        for (Map.Entry<String, StatementTypeSummary> entry : rootExecutionSummaries.entrySet()) {
            HtmlSummaryRow row = getOrCreateHtmlSummaryRow(rows, entry.getKey());
            row.totalCount = Math.max(row.totalCount, entry.getValue().totalCount);
            row.errorCount = entry.getValue().failedCount;
        }

        for (AnalyzerFailure failure : failures) {
            String objectType = displayObjectType(failure.getStatementType());
            String parentObjectType = staticSqlParentObjectType(failure.getStatementId());
            HtmlSummaryRow row;
            if (parentObjectType == null) {
                row = getOrCreateHtmlSummaryRow(rows, objectType);
            } else {
                row = addHtmlSummaryChildCost(
                        rows,
                        parentObjectType,
                        "STATIC " + objectType,
                        failure.getEstimatedCost());
                continue;
            }
            row.cost += failure.getEstimatedCost();
            if (row.errorCount == 0) {
                row.errorCount = 1;
            }
        }

        groupChildSummaryRows(rows, "VIEW", "VIEW_CREATE", "VIEW_ALTER");
        groupChildSummaryRows(rows, "PROCEDURE", "PROC_HEADER", "PROC_BODY");
        groupChildSummaryRows(rows, "FUNCTION", "FUNC_HEADER", "FUNC_BODY");
        appendTableSizeSummaryRows(rows);
        return new ArrayList<HtmlSummaryRow>(rows.values());
    }

    private void appendTableSizeSummaryRows(Map<String, HtmlSummaryRow> rows) {
        if (objectCountPreview == null || objectCountPreview.tableSizes().isEmpty()) {
            return;
        }

        HtmlSummaryRow tableRow = getOrCreateHtmlSummaryRow(rows, "TABLE");
        tableRow.tableSizeRows.clear();
        tableRow.tableSizeRows.addAll(objectCountPreview.tableSizes());
    }

    private void groupChildSummaryRows(
            Map<String, HtmlSummaryRow> rows,
            String parentObjectType,
            String... childObjectTypes) {
        HtmlSummaryRow parentRow = rows.get(parentObjectType);
        boolean hadExistingChildRows = parentRow != null && !parentRow.childRows.isEmpty();
        long fallbackTotalCount = 0;
        int errorCount = 0;
        float cost = 0;
        for (String childObjectType : childObjectTypes) {
            HtmlSummaryRow childRow = rows.remove(childObjectType);
            if (childRow == null) {
                continue;
            }
            if (parentRow == null) {
                parentRow = new HtmlSummaryRow(parentObjectType, 0);
                rows.put(parentObjectType, parentRow);
            }
            parentRow.childRows.add(childRow);
            fallbackTotalCount = Math.max(fallbackTotalCount, childRow.totalCount);
            errorCount += childRow.errorCount;
            cost += childRow.cost;
        }

        if (parentRow == null || parentRow.childRows.isEmpty()) {
            return;
        }

        if (parentRow.totalCount == 0) {
            parentRow.totalCount = fallbackTotalCount;
        } else if (hadExistingChildRows) {
            parentRow.totalCount += fallbackTotalCount;
        }
        parentRow.errorCount = hadExistingChildRows
                ? parentRow.errorCount + errorCount
                : Math.max(parentRow.errorCount, errorCount);
        parentRow.cost += cost;
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
        String safeObjectType = nullToEmpty(objectType);
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

    private HtmlSummaryRow getOrCreateHtmlSummaryChildRow(
            Map<String, HtmlSummaryRow> rows,
            String parentObjectType,
            String childObjectType) {
        HtmlSummaryRow parentRow = getOrCreateHtmlSummaryRow(rows, parentObjectType);
        String safeChildObjectType = nullToEmpty(childObjectType);
        if (safeChildObjectType.isEmpty()) {
            safeChildObjectType = "UNKNOWN";
        }

        for (HtmlSummaryRow childRow : parentRow.childRows) {
            if (safeChildObjectType.equals(childRow.objectType)) {
                return childRow;
            }
        }

        HtmlSummaryRow childRow = new HtmlSummaryRow(safeChildObjectType, 0);
        parentRow.childRows.add(childRow);
        return childRow;
    }

    private void addHtmlSummaryChildResult(
            Map<String, HtmlSummaryRow> rows,
            String parentObjectType,
            String childObjectType,
            boolean success) {
        HtmlSummaryRow parentRow = getOrCreateHtmlSummaryRow(rows, parentObjectType);
        HtmlSummaryRow childRow =
                getOrCreateHtmlSummaryChildRow(rows, parentObjectType, childObjectType);
        parentRow.totalCount++;
        childRow.totalCount++;
        if (!success) {
            parentRow.errorCount++;
            childRow.errorCount++;
        }
    }

    private HtmlSummaryRow addHtmlSummaryChildCost(
            Map<String, HtmlSummaryRow> rows,
            String parentObjectType,
            String childObjectType,
            float cost) {
        HtmlSummaryRow parentRow = getOrCreateHtmlSummaryRow(rows, parentObjectType);
        HtmlSummaryRow childRow =
                getOrCreateHtmlSummaryChildRow(rows, parentObjectType, childObjectType);
        parentRow.cost += cost;
        childRow.cost += cost;
        if (childRow.errorCount == 0 && cost > 0) {
            parentRow.errorCount++;
            childRow.errorCount = 1;
        }
        return childRow;
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
            if (source.type() == AnalyzerSourceType.ORACLE && !nullToEmpty(source.user()).isEmpty()) {
                return source.user();
            }
        }
        AnalyzerSourceOverviewViewModel source = overview.source();
        return source == null ? "" : nullToEmpty(source.databaseName());
    }

    private String resolveTargetTypeName() {
        if (overview != null && overview.target() != null && overview.target().type() != null) {
            return String.valueOf(overview.target().type());
        }
        return targetType == null ? "" : String.valueOf(targetType);
    }

    private String resolveSourceTableSize() {
        return objectCountPreview == null ? "0 B" : formatBytes(objectCountPreview.totalTableBytes());
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
        String text = nullToEmpty(value);
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
            sb.append("Program     : ").append(nullToEmpty(overview.programVersion())).append(lineSeparator);
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
                    .append(nullToEmpty(source.jdbcUrl()))
                    .append(formatVersionSuffix(source.version()))
                    .append(lineSeparator);
            sb.append("Oracle Host : ")
                    .append(formatHost(source.host(), source.port()))
                    .append(lineSeparator);
            sb.append("Oracle DB   : ")
                    .append(nullToEmpty(source.databaseName()))
                    .append(lineSeparator);
            sb.append("Oracle User : ")
                    .append(nullToEmpty(source.user()))
                    .append(lineSeparator);
            return;
        }

        sb.append("XML dir     : ").append(nullToEmpty(source.xmlDirectory())).append(lineSeparator);
        sb.append("XML charset : ").append(nullToEmpty(source.xmlCharset())).append(lineSeparator);
        sb.append("XML files   : ").append(source.xmlFileCount()).append(lineSeparator);
    }

    private void appendSourceOverviews(
            StringBuilder sb,
            List<AnalyzerSourceOverviewViewModel> sources,
            String lineSeparator) {
        if (sources == null || sources.isEmpty()) {
            sb.append("Source      : ").append(nullToEmpty(null)).append(lineSeparator);
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
            sb.append("  - ").append(nullToEmpty(message)).append(lineSeparator);
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
                    .append(nullToEmpty(target.jdbcUrl()))
                    .append(formatVersionSuffix(target.version()))
                    .append(lineSeparator);
            sb.append("Target Host : ")
                    .append(formatHost(target.host(), target.port()))
                    .append(lineSeparator);
            sb.append("Target DB   : ")
                    .append(nullToEmpty(target.databaseName()))
                    .append(lineSeparator);
            sb.append("Target User : ")
                    .append(nullToEmpty(target.user()))
                    .append(lineSeparator);
            return;
        }

        if (target.type() == AnalyzerTargetType.PARSER) {
            sb.append("Parser      : ").append(nullToEmpty(target.parserVersion())).append(lineSeparator);
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
                sb.append("- ").append(nullToEmpty(failureMessage)).append(lineSeparator);
            }
            sb.append("----------------------------------------").append(lineSeparator);
        } else {
            sb.append("Failed statements").append(lineSeparator);
            sb.append("(none)").append(lineSeparator);
        }
    }

    private void appendExecutedFullQueries(StringBuilder sb, String lineSeparator) {
        if (!debugFullQuery) {
            return;
        }

        sb.append(lineSeparator);
        sb.append("Executed full queries").append(lineSeparator);
        if (statementResults.isEmpty()) {
            sb.append("(none)").append(lineSeparator);
            return;
        }

        for (StatementResult statementResult : statementResults) {
            sb.append("----------------------------------------").append(lineSeparator);
            sb.append("- ")
                    .append(nullToEmpty(statementResult.statementType))
                    .append(" ")
                    .append(nullToEmpty(statementResult.statementId))
                    .append(" [")
                    .append(statementResult.success ? "OK" : "FAIL")
                    .append("]")
                    .append(lineSeparator);
            if (!nullToEmpty(statementResult.objectName).isEmpty()) {
                sb.append("  Object: ")
                        .append(nullToEmpty(statementResult.objectName))
                        .append(lineSeparator);
            }
            sb.append("  Detail: ")
                    .append(nullToEmpty(statementResult.detail))
                    .append(lineSeparator);
            sb.append("  SQL:").append(lineSeparator);
            appendAnnotatedSqlLines(
                    sb,
                    statementResult.sql,
                    null,
                    lineSeparator,
                    this::nullToEmpty,
                    "    ");
        }
        sb.append("----------------------------------------").append(lineSeparator);
    }

    private Map<String, StatementTypeSummary> buildStatementTypeSummaries() {
        Map<String, StatementTypeSummary> summaries = new LinkedHashMap<String, StatementTypeSummary>();
        for (StatementResult statementResult : statementResults) {
            if ("CLEANUP".equals(statementResult.statementType)) {
                continue;
            }

            String statementType = displayStatementSummaryType(statementResult.statementType);

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
        String type = nullToEmpty(statementType);
        if (type.isEmpty()) {
            return "UNKNOWN";
        }

        if (AnalyzerStatementTypes.TYPE_DDL_SEQUENCE.equals(type)) {
            return "SERIAL";
        }
        if (type.startsWith("DDL_")) {
            return type.substring("DDL_".length());
        }
        return type;
    }

    private String displayStatementSummaryType(String statementType) {
        String type = nullToEmpty(statementType);
        if (type.isEmpty()) {
            return "UNKNOWN";
        }
        if (AnalyzerStatementTypes.TYPE_DDL_SEQUENCE.equals(type)) {
            return "SERIAL";
        }
        return type;
    }

    private boolean isStaticSqlStatement(String statementId) {
        return staticSqlParentId(statementId) != null;
    }

    private String staticSqlParentId(String statementId) {
        String id = nullToEmpty(statementId);
        int staticMarkerIndex = id.indexOf("_STATIC_");
        if (staticMarkerIndex <= 0) {
            return null;
        }
        return id.substring(0, staticMarkerIndex);
    }

    private String staticSqlParentObjectType(String statementId) {
        String parentId = staticSqlParentId(statementId);
        if (parentId == null) {
            return null;
        }
        if (parentId.startsWith("PROC_")) {
            return "PROCEDURE";
        }
        if (parentId.startsWith("FUNC_")) {
            return "FUNCTION";
        }
        return "PLCSQL";
    }

    private StatementResult findStatementResult(String statementId) {
        String id = nullToEmpty(statementId);
        for (StatementResult statementResult : statementResults) {
            if (id.equals(nullToEmpty(statementResult.statementId))) {
                return statementResult;
            }
        }
        return null;
    }

    private AnalyzerFailure findFailure(String statementId) {
        String id = nullToEmpty(statementId);
        for (AnalyzerFailure failure : failures) {
            if (id.equals(nullToEmpty(failure.getStatementId()))) {
                return failure;
            }
        }
        return null;
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

    private String nullToEmpty(String value) {
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
                .append(nullToEmpty(failure.getStatementType()))
                .append(" ")
                .append(nullToEmpty(failure.getStatementId()))
                .append(" [")
                .append(failure.getFailureStage())
                .append("]")
                .append(lineSeparator);
        if (!nullToEmpty(failure.getObjectName()).isEmpty()) {
            sb.append("  Object: ").append(nullToEmpty(failure.getObjectName())).append(lineSeparator);
        }
        sb.append("  Reason: ").append(nullToEmpty(failure.getReason())).append(lineSeparator);
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
        String sql = nullToEmpty(failure.getSql());
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
        appendAnnotatedSqlLines(sb, sql, location, lineSeparator, this::nullToEmpty, "    ");
    }

    private void appendAnnotatedSqlLines(
            StringBuilder sb,
            String sql,
            SqlContextLocation location,
            String lineSeparator,
            UnaryOperator<String> transform,
            String linePrefix) {
        String[] lines = splitSqlLines(sql);
        location = validSqlContextLocation(location, lines.length);
        int lineNumberWidth = String.valueOf(lines.length).length();
        for (int lineNumber = 1; lineNumber <= lines.length; lineNumber++) {
            String sqlLine = lines[lineNumber - 1];
            sb.append(transform.apply(linePrefix));
            sb.append(transform.apply(formatLineNumber(lineNumber, lineNumberWidth)));
            sb.append(" | ");
            sb.append(transform.apply(sqlLine));
            sb.append(lineSeparator);
            if (location != null && lineNumber == location.lineNumber) {
                sb.append(transform.apply(linePrefix));
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

    private SqlContextLocation findSqlContextLocation(String reason, String sql) {
        if (sql == null || sql.isEmpty()) {
            return null;
        }

        String[] lines = splitSqlLines(sql);
        Matcher locationMatcher = ERROR_LOCATION_PATTERN.matcher(nullToEmpty(reason));
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
        Matcher matcher = pattern.matcher(nullToEmpty(reason));
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (candidate != null && !candidate.isEmpty()) {
                candidates.add(candidate);
            }
        }
    }

    private String[] splitSqlLines(String sql) {
        return nullToEmpty(sql).split("\\R", -1);
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
                    .append(nullToEmpty(costDetail.getItemName()))
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
