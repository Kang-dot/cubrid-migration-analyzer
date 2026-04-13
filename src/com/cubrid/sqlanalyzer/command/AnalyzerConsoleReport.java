package com.cubrid.sqlanalyzer.command;

import java.util.ArrayList;
import java.util.List;

public class AnalyzerConsoleReport {
    private AnalyzerSourceType sourceType;
    private AnalyzerTargetType targetType;
    private AnalyzerExecutionMode executionMode;
    private int analyzedStatementCount;
    private int succeededStatementCount;
    private int failedStatementCount;
    private final List<String> failureMessages = new ArrayList<String>();
    private final List<AnalyzerConsoleFailure> failures = new ArrayList<AnalyzerConsoleFailure>();

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
}
