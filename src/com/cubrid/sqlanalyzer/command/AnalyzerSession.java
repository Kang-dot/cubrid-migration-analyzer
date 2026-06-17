package com.cubrid.sqlanalyzer.command;

import java.util.ArrayList;
import java.util.List;

import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.dbobject.AnalyzerCatalog;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTableSizeViewModel;

public class AnalyzerSession {
    private final AnalyzerConfiguration config = new AnalyzerConfiguration();
    private final AnalyzerReport report = new AnalyzerReport();

    private AnalyzerSourceType sourceType;
    private AnalyzerTargetType targetType;
    private AnalyzerExecutionMode executionMode;

    private String sourceJdbcUrl;
    private String sourceUser;
    private String sourcePassword;

    private String xmlDirectory;
    private String xmlCharset;

    private String targetJdbcUrl;
    private String targetUser;
    private String targetPassword;

    private Catalog sourceCatalog;
    private AnalyzerCatalog analyzerCatalog;
    private List<AnalyzerTableSizeViewModel> oracleTableSizes = List.of();
    private int analyzedStatementCount;
    private int succeededStatementCount;
    private int failedStatementCount;
    private final List<String> failureMessages = new ArrayList<String>();

    public AnalyzerConfiguration getConfig() {
        return config;
    }

    public AnalyzerSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(AnalyzerSourceType sourceType) {
        this.sourceType = sourceType;
        report.setSourceType(sourceType);
    }

    public AnalyzerTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(AnalyzerTargetType targetType) {
        this.targetType = targetType;
        report.setTargetType(targetType);
    }

    public AnalyzerExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(AnalyzerExecutionMode executionMode) {
        this.executionMode = executionMode;
        report.setExecutionMode(executionMode);
    }

    public String getSourceJdbcUrl() {
        return sourceJdbcUrl;
    }

    public void setSourceJdbcUrl(String sourceJdbcUrl) {
        this.sourceJdbcUrl = sourceJdbcUrl;
    }

    public String getSourceUser() {
        return sourceUser;
    }

    public void setSourceUser(String sourceUser) {
        this.sourceUser = sourceUser;
    }

    public String getSourcePassword() {
        return sourcePassword;
    }

    public void setSourcePassword(String sourcePassword) {
        this.sourcePassword = sourcePassword;
    }

    public String getXmlDirectory() {
        return xmlDirectory;
    }

    public void setXmlDirectory(String xmlDirectory) {
        this.xmlDirectory = xmlDirectory;
    }

    public String getXmlCharset() {
        return xmlCharset;
    }

    public void setXmlCharset(String xmlCharset) {
        this.xmlCharset = xmlCharset;
    }

    public String getTargetJdbcUrl() {
        return targetJdbcUrl;
    }

    public void setTargetJdbcUrl(String targetJdbcUrl) {
        this.targetJdbcUrl = targetJdbcUrl;
    }

    public String getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(String targetUser) {
        this.targetUser = targetUser;
    }

    public String getTargetPassword() {
        return targetPassword;
    }

    public void setTargetPassword(String targetPassword) {
        this.targetPassword = targetPassword;
    }

    public Catalog getSourceCatalog() {
        return sourceCatalog;
    }

    public void setSourceCatalog(Catalog sourceCatalog) {
        this.sourceCatalog = sourceCatalog;
    }

    public AnalyzerCatalog getAnalyzerCatalog() {
        return analyzerCatalog;
    }

    public void setAnalyzerCatalog(AnalyzerCatalog analyzerCatalog) {
        this.analyzerCatalog = analyzerCatalog;
    }

    public List<AnalyzerTableSizeViewModel> getOracleTableSizes() {
        return oracleTableSizes;
    }

    public void setOracleTableSizes(List<AnalyzerTableSizeViewModel> oracleTableSizes) {
        this.oracleTableSizes =
                oracleTableSizes == null ? List.of() : List.copyOf(oracleTableSizes);
    }

    public int getAnalyzedStatementCount() {
        return analyzedStatementCount;
    }

    public void setAnalyzedStatementCount(int analyzedStatementCount) {
        this.analyzedStatementCount = analyzedStatementCount;
        report.setAnalyzedStatementCount(analyzedStatementCount);
    }

    public int getSucceededStatementCount() {
        return succeededStatementCount;
    }

    public void setSucceededStatementCount(int succeededStatementCount) {
        this.succeededStatementCount = succeededStatementCount;
        report.setSucceededStatementCount(succeededStatementCount);
    }

    public int getFailedStatementCount() {
        return failedStatementCount;
    }

    public void setFailedStatementCount(int failedStatementCount) {
        this.failedStatementCount = failedStatementCount;
        report.setFailedStatementCount(failedStatementCount);
    }

    public List<String> getFailureMessages() {
        return report.getFailureMessages();
    }

    public void clearFailures() {
        failureMessages.clear();
        report.clearFailures();
    }

    public void addFailureMessage(String failureMessage) {
        failureMessages.add(failureMessage);
        report.addFailureMessage(failureMessage);
    }

    public void addFailure(AnalyzerFailure failure) {
        report.addFailure(failure);
    }

    public AnalyzerReport getReport() {
        return report;
    }
}
