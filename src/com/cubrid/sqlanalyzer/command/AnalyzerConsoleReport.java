package com.cubrid.sqlanalyzer.command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.cubrid.cubridmigration.core.common.Closer;
import com.cubrid.cubridmigration.cubrid.CUBRIDTimeUtil;

public class AnalyzerConsoleReport {
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

    private AnalyzerSourceType sourceType;
    private AnalyzerTargetType targetType;
    private AnalyzerExecutionMode executionMode;
    private int analyzedStatementCount;
    private int succeededStatementCount;
    private int failedStatementCount;
    private long generatedAt;
    private final List<String> failureMessages = new ArrayList<String>();
    private final List<AnalyzerConsoleFailure> failures = new ArrayList<AnalyzerConsoleFailure>();
    private final List<StatementResult> statementResults = new ArrayList<StatementResult>();

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

    public List<AnalyzerConsoleFailure> getFailures() {
        return failures;
    }

    public void addFailure(AnalyzerConsoleFailure failure) {
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

    public String saveResultReport() {
        PrintWriter writer = null;
        try {
            File reportDir = getReportDirectory();
            if (!reportDir.exists() && !reportDir.mkdirs()) {
                throw new IOException("Failed to create report directory: " + reportDir);
            }

            generatedAt = System.currentTimeMillis();
            File reportFile = new File(reportDir, buildReportFileName());
            writer =
                    new PrintWriter(
                            new OutputStreamWriter(new FileOutputStream(reportFile), "UTF-8"));
            writer.print(buildResultText());
            writer.flush();
            return reportFile.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save console report: " + e.getMessage(), e);
        } finally {
            Closer.close(writer);
        }
    }

    public String buildResultText() {
        String lineSeparator = System.lineSeparator();
        StringBuilder sb = new StringBuilder();

        sb.append("Result summary").append(lineSeparator);
        sb.append("Source : ").append(sourceType).append(lineSeparator);
        sb.append("Target : ").append(targetType).append(lineSeparator);
        sb.append("Mode   : ").append(executionMode).append(lineSeparator);
        sb.append("Total  : ").append(analyzedStatementCount).append(lineSeparator);
        sb.append("OK     : ").append(succeededStatementCount).append(lineSeparator);
        sb.append("FAIL   : ").append(failedStatementCount).append(lineSeparator);

        sb.append(lineSeparator).append("Statement results").append(lineSeparator);
        if (statementResults.isEmpty()) {
            sb.append("(no statement results)").append(lineSeparator);
        } else {
            for (StatementResult result : statementResults) {
                sb.append("- ")
                        .append(safeText(result.statementType))
                        .append(" ")
                        .append(safeText(result.statementId))
                        .append(" : ")
                        .append(result.success ? "OK" : "FAIL");
                if (result.failureStage != null) {
                    sb.append(" [").append(result.failureStage).append("]");
                }
                sb.append(lineSeparator);
                if (!safeText(result.detail).isEmpty()) {
                    sb.append("  Detail: ").append(result.detail).append(lineSeparator);
                }
                if (!safeText(result.sql).isEmpty()) {
                    sb.append("  SQL   : ").append(result.sql).append(lineSeparator);
                }
            }
        }

        if (!failures.isEmpty()) {
            sb.append(lineSeparator).append("Failed statements").append(lineSeparator);
            for (AnalyzerConsoleFailure failure : failures) {
                sb.append("- ")
                        .append(safeText(failure.getStatementType()))
                        .append(" ")
                        .append(safeText(failure.getStatementId()))
                        .append(" [")
                        .append(failure.getFailureStage())
                        .append("]")
                        .append(lineSeparator);
                sb.append("  Reason: ").append(safeText(failure.getReason())).append(lineSeparator);
                sb.append("  SQL   : ").append(safeText(failure.getSql())).append(lineSeparator);
            }
        } else if (!failureMessages.isEmpty()) {
            sb.append(lineSeparator).append("Failed statements").append(lineSeparator);
            for (String failureMessage : failureMessages) {
                sb.append("- ").append(safeText(failureMessage)).append(lineSeparator);
            }
        }

        return sb.toString();
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
                    AnalyzerConsoleReport.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI());
        } catch (URISyntaxException e) {
            return new File(
                    AnalyzerConsoleReport.class
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
}
