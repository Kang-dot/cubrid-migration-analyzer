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

    private void appendOverview(StringBuilder sb, String lineSeparator) {
        sb.append("Overview").append(lineSeparator);
        if (overview != null) {
            sb.append("Program     : ").append(formatText(overview.programVersion())).append(lineSeparator);
            appendSourceOverviews(sb, overview.sources(), lineSeparator);
            appendTargetOverview(sb, overview.source(), overview.target(), lineSeparator);
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
                sb.append("- ").append(safeText(failureMessage)).append(lineSeparator);
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

    private String displayObjectType(String statementType) {
        String type = safeText(statementType);
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
                .append(formatEstimatedCostWithTime(failure.getEstimatedCost()))
                .append(lineSeparator);
        appendCostDetails(sb, failure, lineSeparator);
        appendAnnotatedSql(sb, failure, lineSeparator);
    }

    private void appendAnnotatedSql(
            StringBuilder sb,
            AnalyzerFailure failure,
            String lineSeparator) {
        String sql = safeText(failure.getSql());
        String[] lines = splitSqlLines(sql);
        SqlContextLocation location =
                findSqlContextLocation(failure.getReason(), sql);
        if (location != null
                && location.lineNumber >= 1
                && location.lineNumber <= lines.length) {
            sb.append("  Location: line ")
                    .append(location.lineNumber)
                    .append(", column ")
                    .append(location.columnNumber);
            if (location.estimated) {
                sb.append(" (estimated)");
            }
            sb.append(lineSeparator);
        } else {
            location = null;
        }

        sb.append("  SQL:").append(lineSeparator);
        int lineNumberWidth = String.valueOf(lines.length).length();
        for (int lineNumber = 1; lineNumber <= lines.length; lineNumber++) {
            String sqlLine = lines[lineNumber - 1];
            sb.append("    ")
                    .append(formatLineNumber(lineNumber, lineNumberWidth))
                    .append(" | ")
                    .append(sqlLine)
                    .append(lineSeparator);
            if (location != null && lineNumber == location.lineNumber) {
                sb.append("    ")
                        .append(" ".repeat(lineNumberWidth))
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

    private SqlContextLocation findSqlContextLocation(String reason, String sql) {
        if (sql == null || sql.isEmpty()) {
            return null;
        }

        String[] lines = splitSqlLines(sql);
        Matcher locationMatcher = ERROR_LOCATION_PATTERN.matcher(safeText(reason));
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
        Matcher matcher = pattern.matcher(safeText(reason));
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (candidate != null && !candidate.isEmpty()) {
                candidates.add(candidate);
            }
        }
    }

    private String[] splitSqlLines(String sql) {
        return safeText(sql).split("\\R", -1);
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
                    .append(safeText(costDetail.getItemName()))
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
