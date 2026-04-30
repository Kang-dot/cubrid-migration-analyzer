package com.cubrid.sqlanalyzer.command;

public class AnalyzerConsoleFailure {
    private AnalyzerFailureStage failureStage;
    private String statementType;
    private String statementId;
    private String sql;
    private String reason;
    private float estimatedCost;

    public AnalyzerFailureStage getFailureStage() {
        return failureStage;
    }

    public void setFailureStage(AnalyzerFailureStage failureStage) {
        this.failureStage = failureStage;
    }

    public String getStatementType() {
        return statementType;
    }

    public void setStatementType(String statementType) {
        this.statementType = statementType;
    }

    public String getStatementId() {
        return statementId;
    }

    public void setStatementId(String statementId) {
        this.statementId = statementId;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public float getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(float estimatedCost) {
        this.estimatedCost = estimatedCost;
    }
}
