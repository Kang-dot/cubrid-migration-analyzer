package com.cubrid.sqlanalyzer.command.service;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cubrid.cubridmigration.core.common.Closer;
import com.cubrid.cubridmigration.core.dbmetadata.JDBCDBSchemaFetcherFacade;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbobject.PlcsqlFunction;
import com.cubrid.cubridmigration.core.dbobject.PlcsqlProcedure;
import com.cubrid.cubridmigration.core.dbobject.Sequence;
import com.cubrid.cubridmigration.core.dbobject.Synonym;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbobject.Version;
import com.cubrid.cubridmigration.core.dbobject.View;
import com.cubrid.cubridmigration.core.dbtype.DatabaseType;
import com.cubrid.cubridmigration.core.engine.config.SourceGrantConfig;
import com.cubrid.cubridmigration.cubrid.CUBRIDSQLHelper;
import com.cubrid.sqlanalyzer.command.AnalyzerConnParametersFactory;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleArguments;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleConfig;
import com.cubrid.sqlanalyzer.command.AnalyzerFailure;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleReport;
import com.cubrid.sqlanalyzer.command.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.AnalyzerFailureStage;
import com.cubrid.sqlanalyzer.command.AnalyzerJdbcConnectionInfo;
import com.cubrid.sqlanalyzer.command.AnalyzerJdbcConnectionSupport;
import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.dto.AnalyzerObjectCountPreview;
import com.cubrid.sqlanalyzer.command.dto.AnalyzerOverview;
import com.cubrid.sqlanalyzer.command.dto.AnalyzerProgressEvent;
import com.cubrid.sqlanalyzer.command.dto.AnalyzerProgressStage;
import com.cubrid.sqlanalyzer.command.dto.AnalyzerResult;
import com.cubrid.sqlanalyzer.command.dto.AnalyzerSourceOverview;
import com.cubrid.sqlanalyzer.command.dto.AnalyzerTargetOverview;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.cost.AnalyzerCostCalculator;
import com.cubrid.sqlanalyzer.core.cost.FailureCostCalculator;
import com.cubrid.sqlanalyzer.core.dbobject.AnalyzerCatalog;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPlan;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatement;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes;
import com.cubrid.sqlanalyzer.core.plan.CatalogDDLPlanBuilder;
import com.cubrid.sqlanalyzer.core.plan.QueryDictionaryPlanBuilder;
import com.cubrid.sqlanalyzer.core.runner.QueryParser;
import com.cubrid.sqlanalyzer.core.runner.SQLParserException;
import com.cubrid.sqlanalyzer.xmlmetadata.XMLDirSchemaFetcher;
import com.cubrid.sqlanalyzer.xmlmetadata.XMLDirSource;

public class AnalyzerService {
    private static final String SOURCE_CONNECTION_NAME = "console-source";
    private static final String TARGET_CONNECTION_NAME = "console-target";
    private static final Pattern ORACLE_VERSION_NAME_PATTERN = Pattern.compile("\\b\\d+(?:ai|c|g)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final AnalyzerConnParametersFactory ORACLE_CONN_PARAMETERS_FACTORY = AnalyzerJdbcConnectionSupport
            .createFactory(DatabaseType.ORACLE);
    private static final AnalyzerConnParametersFactory CUBRID_CONN_PARAMETERS_FACTORY = AnalyzerJdbcConnectionSupport
            .createFactory(DatabaseType.CUBRID);

    private final AnalyzerCostCalculator costCalculator = new FailureCostCalculator();

    public void applyArguments(AnalyzerConsoleConfig session, AnalyzerConsoleArguments arguments) {
        session.setSourceType(arguments.getSourceType());
        if (AnalyzerSourceType.ORACLE.equals(arguments.getSourceType())) {
            session.setSourceJdbcUrl(arguments.getSourceJdbcUrl());
            session.setSourceUser(arguments.getSourceUser());
            session.setSourcePassword(arguments.getSourcePassword());
            validateOracleSourceConnection(session);
        } else if (AnalyzerSourceType.XML.equals(arguments.getSourceType())) {
            session.setXmlDirectory(arguments.getXmlDirectory());
            session.setXmlCharset(arguments.getXmlCharset());
        }

        session.setTargetType(arguments.getTargetType());
        if (AnalyzerTargetType.JDBC.equals(arguments.getTargetType())) {
            session.setTargetJdbcUrl(arguments.getTargetJdbcUrl());
            session.setTargetUser(arguments.getTargetUser());
            session.setTargetPassword(arguments.getTargetPassword());
            validateJdbcTargetConnection(session);
        }

        applyExecutionMode(session);
    }

    public void applyExecutionMode(AnalyzerConsoleConfig session) {
        if (AnalyzerSourceType.ORACLE.equals(session.getSourceType())) {
            session.setExecutionMode(AnalyzerExecutionMode.DDL);
            return;
        }

        if (AnalyzerSourceType.XML.equals(session.getSourceType())) {
            session.setExecutionMode(AnalyzerExecutionMode.DML);
            return;
        }

        throw new IllegalStateException("Unsupported source type: " + session.getSourceType());
    }

    public void validateOracleSourceConnection(AnalyzerConsoleConfig session) {
        AnalyzerJdbcConnectionInfo profile = AnalyzerJdbcConnectionSupport.parseOracleProfile(
                session.getSourceJdbcUrl(),
                session.getSourceUser(),
                session.getSourcePassword());
        AnalyzerJdbcConnectionSupport.validateConnection(
                SOURCE_CONNECTION_NAME, profile, ORACLE_CONN_PARAMETERS_FACTORY);
    }

    public void validateJdbcTargetConnection(AnalyzerConsoleConfig session) {
        AnalyzerJdbcConnectionInfo profile = AnalyzerJdbcConnectionSupport.parseCubridProfile(
                session.getTargetJdbcUrl(),
                session.getTargetUser(),
                session.getTargetPassword());
        AnalyzerJdbcConnectionSupport.validateConnection(
                TARGET_CONNECTION_NAME, profile, CUBRID_CONN_PARAMETERS_FACTORY);
    }

    public void prepareConfiguration(AnalyzerConsoleConfig session) {
        AnalyzerConfiguration config = session.getConfig();

        if (session.getSourceType() == AnalyzerSourceType.ORACLE) {
            config.setSourceType(AnalyzerConfiguration.SOURCE_TYPE_DB);
            config.setSourceConParams(
                    ORACLE_CONN_PARAMETERS_FACTORY.create(
                            SOURCE_CONNECTION_NAME,
                            AnalyzerJdbcConnectionSupport.parseOracleProfile(
                                    session.getSourceJdbcUrl(),
                                    session.getSourceUser(),
                                    session.getSourcePassword())));
        } else if (session.getSourceType() == AnalyzerSourceType.XML) {
            config.setSourceType(AnalyzerConfiguration.SOURCE_TYPE_XML);
        }

        if (session.getTargetType() == AnalyzerTargetType.PARSER) {
            config.setDestType(AnalyzerConfiguration.TARGET_TYPE_PARSER);
        } else {
            config.setDestType(AnalyzerConfiguration.TARGET_TYPE_CUBRID);
            config.setTargetConParams(
                    CUBRID_CONN_PARAMETERS_FACTORY.create(
                            TARGET_CONNECTION_NAME,
                            AnalyzerJdbcConnectionSupport.parseCubridProfile(
                                    session.getTargetJdbcUrl(),
                                    session.getTargetUser(),
                                    session.getTargetPassword())));
        }
    }

    public void loadSourceCatalog(AnalyzerConsoleConfig session) {
        if (session.getSourceType() == AnalyzerSourceType.ORACLE) {
            loadOracleSourceCatalog(session);
            return;
        }

        if (session.getSourceType() == AnalyzerSourceType.XML) {
            loadXmlQueryDictionary(session);
            return;
        }

        throw new IllegalStateException("Unsupported source type: " + session.getSourceType());
    }

    public AnalyzerOverview getOverview(AnalyzerConsoleConfig session) {
        return new AnalyzerOverview(
                getProgramVersion(),
                getSourceOverview(session),
                getTargetOverview(session),
                session.getExecutionMode());
    }

    public AnalyzerObjectCountPreview getObjectCountPreview(AnalyzerConsoleConfig session) {
        AnalyzerConfiguration config = session.getConfig();
        if (session.getSourceType() == AnalyzerSourceType.ORACLE) {
            return new AnalyzerObjectCountPreview(
                    session.getSourceType(),
                    session.getSourceCatalog().getSchemas().size(),
                    config.getTargetTableSchema().size(),
                    countTargetPrimaryKeys(config.getTargetTableSchema()),
                    countTargetForeignKeys(config.getTargetTableSchema()),
                    config.getTargetViewSchema().size(),
                    config.getTargetSerialSchema().size(),
                    config.getTargetSynonymSchema().size(),
                    config.getExpGrantCfg().size(),
                    config.getTargetPlcsqlProcedureSchema().size(),
                    config.getTargetPlcsqlFunctionSchema().size(),
                    0,
                    0,
                    0,
                    0);
        }

        QueryDictionary dict = config.getQueryDict();
        return new AnalyzerObjectCountPreview(
                session.getSourceType(),
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                dict.getSelectQueryMap().size(),
                dict.getInsertQueryMap().size(),
                dict.getUpdateQueryMap().size(),
                dict.getDeleteQueryMap().size());
    }

    public AnalyzerResult saveResult(AnalyzerConsoleConfig session) {
        AnalyzerConsoleReport report = session.getConsoleReport();
        String savedReportPath = report.saveResultReport();

        return new AnalyzerResult(
                report.getSourceType(),
                report.getTargetType(),
                report.getExecutionMode(),
                report.getAnalyzedStatementCount(),
                report.getSucceededStatementCount(),
                report.getFailedStatementCount(),
                report.getTotalEstimatedFailureCost(),
                savedReportPath,
                report.getFailureMessages(),
                report.getFailures());
    }

    public void runAnalysis(
            AnalyzerConsoleConfig session, AnalyzerProgressListener progressListener) {
        AnalyzerExecutionPlan executionPlan = buildExecutionPlan(session);
        int totalCount = executionPlan.getStatements().size();
        notifyProgress(
                progressListener,
                new AnalyzerProgressEvent(
                        AnalyzerProgressStage.PLANNING,
                        "Generated SQL statements: " + totalCount,
                        null,
                        null,
                        null,
                        null,
                        null,
                        totalCount,
                        0,
                        0,
                        0));
        costCalculator.analyzeBeforeExecution(executionPlan, session.getConsoleReport());
        if (executionPlan.isEmpty()) {
            session.setAnalyzedStatementCount(0);
            session.setSucceededStatementCount(0);
            session.setFailedStatementCount(0);
            session.clearFailures();
            notifyProgress(
                    progressListener,
                    new AnalyzerProgressEvent(
                            AnalyzerProgressStage.EMPTY,
                            "No SQL statements were generated for the selected source/mode.",
                            null,
                            null,
                            null,
                            null,
                            null,
                            totalCount,
                            0,
                            0,
                            0));
            return;
        }

        if (session.getTargetType() == AnalyzerTargetType.PARSER) {
            runParserAnalysis(session, executionPlan, progressListener);
            return;
        }

        if (session.getTargetType() == AnalyzerTargetType.JDBC) {
            runJdbcAnalysis(session, executionPlan, progressListener);
            return;
        }

        throw new IllegalStateException("Unsupported target type: " + session.getTargetType());
    }

    public long countTargetPrimaryKeys(List<Table> tables) {
        long count = 0;

        for (Table table : tables) {
            if (table.getPk() != null && !table.getPk().getPkColumns().isEmpty()) {
                count++;
            }
        }

        return count;
    }

    public long countTargetForeignKeys(List<Table> tables) {
        long count = 0;

        for (Table table : tables) {
            count += table.getFks().size();
        }

        return count;
    }

    private AnalyzerSourceOverview getSourceOverview(AnalyzerConsoleConfig session) {
        if (session.getSourceType() == AnalyzerSourceType.ORACLE) {
            AnalyzerJdbcConnectionInfo profile = AnalyzerJdbcConnectionSupport.parseOracleProfile(
                    session.getSourceJdbcUrl(),
                    session.getSourceUser(),
                    session.getSourcePassword());
            Catalog catalog = session.getSourceCatalog();
            String version = catalog == null || catalog.getVersion() == null
                    ? null
                    : getOracleVersionName(catalog.getVersion());
            return new AnalyzerSourceOverview(
                    session.getSourceType(),
                    session.getSourceJdbcUrl(),
                    profile.getHost(),
                    profile.getPort(),
                    profile.getDatabaseName(),
                    session.getSourceUser(),
                    version,
                    null,
                    null,
                    0);
        }

        return new AnalyzerSourceOverview(
                session.getSourceType(),
                null,
                null,
                0,
                null,
                null,
                null,
                session.getXmlDirectory(),
                session.getXmlCharset(),
                countXmlFiles(session.getXmlDirectory()));
    }

    private AnalyzerTargetOverview getTargetOverview(AnalyzerConsoleConfig session) {
        if (session.getTargetType() == AnalyzerTargetType.JDBC) {
            AnalyzerJdbcConnectionInfo profile = AnalyzerJdbcConnectionSupport.parseCubridProfile(
                    session.getTargetJdbcUrl(),
                    session.getTargetUser(),
                    session.getTargetPassword());
            return new AnalyzerTargetOverview(
                    session.getTargetType(),
                    session.getTargetJdbcUrl(),
                    profile.getHost(),
                    profile.getPort(),
                    profile.getDatabaseName(),
                    session.getTargetUser(),
                    getTargetVersionName(session),
                    null);
        }

        return new AnalyzerTargetOverview(
                session.getTargetType(),
                null,
                null,
                0,
                null,
                null,
                null,
                getParserVersion());
    }

    private String getProgramVersion() {
        Package packageInfo = AnalyzerService.class.getPackage();
        String version = packageInfo == null ? null : packageInfo.getImplementationVersion();
        return version == null || version.isEmpty() ? "0.0.1-SNAPSHOT" : version;
    }

    private String getParserVersion() {
        return "CUBRID parser";
    }

    private String getTargetVersionName(AnalyzerConsoleConfig session) {
        if (session.getConfig().getTargetConParams() == null) {
            return null;
        }

        try (Connection connection = session.getConfig().getTargetConParams().createConnection()) {
            return getCubridVersionName(connection.getMetaData());
        } catch (SQLException ex) {
            return null;
        }
    }

    private int countXmlFiles(String xmlDirectory) {
        if (xmlDirectory == null || xmlDirectory.isEmpty()) {
            return 0;
        }

        File directory = new File(xmlDirectory);
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
        return files == null ? 0 : files.length;
    }

    private void loadOracleSourceCatalog(AnalyzerConsoleConfig session) {
        AnalyzerConfiguration config = session.getConfig();
        JDBCDBSchemaFetcherFacade fetcher = new JDBCDBSchemaFetcherFacade();
        Catalog catalog = fetcher.fetchSchema(config.getSourceConParams(), null);

        if (catalog == null) {
            throw new RuntimeException("Failed to fetch Oracle catalog.");
        }

        session.setSourceCatalog(catalog);
        config.setSrcCatalog(catalog, false);
        config.parsingProcedureFunction(true);
    }

    private void loadXmlQueryDictionary(AnalyzerConsoleConfig session) {
        AnalyzerConfiguration config = session.getConfig();
        XMLDirSource source = new XMLDirSource(session.getXmlDirectory(), session.getXmlCharset());
        XMLDirSchemaFetcher fetcher = new XMLDirSchemaFetcher();
        AnalyzerCatalog catalog = fetcher.fetchSchema(source, null);

        if (catalog == null || catalog.getQueryDictionary() == null) {
            throw new RuntimeException("Failed to build query dictionary from XML directory.");
        }

        QueryDictionary queryDictionary = catalog.getQueryDictionary();
        session.setAnalyzerCatalog(catalog);
        session.setSourceCatalog(catalog);
        config.setQueryDict(queryDictionary);
    }

    private String getOracleVersionName(Version version) {
        String productVersion = version.getDbProductVersion();
        if (productVersion != null) {
            Matcher matcher = ORACLE_VERSION_NAME_PATTERN.matcher(productVersion);
            if (matcher.find()) {
                return matcher.group().toLowerCase();
            }
        }

        int majorVersion = version.getDbMajorVersion();
        if (majorVersion <= 0) {
            return null;
        }
        return majorVersion == 11 || majorVersion == 10
                ? majorVersion + "g"
                : majorVersion + "c";
    }

    private String getCubridVersionName(DatabaseMetaData metaData) throws SQLException {
        int majorVersion = metaData.getDatabaseMajorVersion();
        int minorVersion = metaData.getDatabaseMinorVersion();
        if (majorVersion > 0 && minorVersion >= 0) {
            return majorVersion + "." + minorVersion;
        }

        return metaData.getDatabaseProductVersion();
    }

    private AnalyzerExecutionPlan buildExecutionPlan(AnalyzerConsoleConfig session) {
        if (session.getSourceType() == AnalyzerSourceType.XML) {
            if (session.getExecutionMode() == AnalyzerExecutionMode.DDL) {
                return new AnalyzerExecutionPlan();
            }
            return new QueryDictionaryPlanBuilder().build(session.getConfig());
        }

        if (session.getSourceType() == AnalyzerSourceType.ORACLE) {
            if (session.getExecutionMode() == AnalyzerExecutionMode.DML) {
                return new AnalyzerExecutionPlan();
            }
            return new CatalogDDLPlanBuilder().build(session.getConfig());
        }

        throw new IllegalStateException("Unsupported source type: " + session.getSourceType());
    }

    private void runParserAnalysis(
            AnalyzerConsoleConfig session,
            AnalyzerExecutionPlan executionPlan,
            AnalyzerProgressListener progressListener) {
        QueryParser queryParser = new QueryParser();
        int totalCount = executionPlan.getStatements().size();
        int analyzed = 0;
        int succeeded = 0;
        int failed = 0;

        session.clearFailures();

        for (AnalyzerStatement statement : executionPlan.getStatements()) {
            analyzed++;
            try {
                queryParser.checkSQL(statement.getSQL());
                succeeded++;
                session.getConsoleReport()
                        .addStatementResult(
                                statement.getType(),
                                statement.getId(),
                                statement.getSQL(),
                                true,
                                "parsed",
                                null);
                notifyProgress(
                        progressListener,
                        new AnalyzerProgressEvent(
                                AnalyzerProgressStage.STATEMENT_SUCCEEDED,
                                "[OK] " + statement.getType() + " " + statement.getId(),
                                statement.getType(),
                                statement.getId(),
                                statement.getSQL(),
                                "parsed",
                                null,
                                totalCount,
                                analyzed,
                                succeeded,
                                failed));
            } catch (SQLParserException ex) {
                failed++;
                String failureMessage = buildFailureMessage(statement.getType(), statement.getId(), ex.getMessage());
                session.addFailureMessage(failureMessage);
                session.addFailure(
                        buildFailure(statement, ex.getMessage(), AnalyzerFailureStage.PARSER));
                session.getConsoleReport()
                        .addStatementResult(
                                statement.getType(),
                                statement.getId(),
                                statement.getSQL(),
                                false,
                                ex.getMessage(),
                                AnalyzerFailureStage.PARSER);
                notifyProgress(
                        progressListener,
                        new AnalyzerProgressEvent(
                                AnalyzerProgressStage.STATEMENT_FAILED,
                                "[FAIL] " + failureMessage,
                                statement.getType(),
                                statement.getId(),
                                statement.getSQL(),
                                ex.getMessage(),
                                AnalyzerFailureStage.PARSER,
                                totalCount,
                                analyzed,
                                succeeded,
                                failed));
            } catch (Exception ex) {
                failed++;
                String failureMessage = buildFailureMessage(statement.getType(), statement.getId(), ex.toString());
                session.addFailureMessage(failureMessage);
                session.addFailure(
                        buildFailure(statement, ex.toString(), AnalyzerFailureStage.PARSER));
                session.getConsoleReport()
                        .addStatementResult(
                                statement.getType(),
                                statement.getId(),
                                statement.getSQL(),
                                false,
                                ex.toString(),
                                AnalyzerFailureStage.PARSER);
                notifyProgress(
                        progressListener,
                        new AnalyzerProgressEvent(
                                AnalyzerProgressStage.STATEMENT_FAILED,
                                "[FAIL] " + failureMessage,
                                statement.getType(),
                                statement.getId(),
                                statement.getSQL(),
                                ex.toString(),
                                AnalyzerFailureStage.PARSER,
                                totalCount,
                                analyzed,
                                succeeded,
                                failed));
            }
        }

        session.setAnalyzedStatementCount(analyzed);
        session.setSucceededStatementCount(succeeded);
        session.setFailedStatementCount(failed);
        costCalculator.analyzeAfterExecution(session.getConsoleReport());

        publishAnalysisCompleted(progressListener, analyzed, succeeded, failed);
    }

    private void runJdbcAnalysis(
            AnalyzerConsoleConfig session,
            AnalyzerExecutionPlan executionPlan,
            AnalyzerProgressListener progressListener) {
        Connection connection = null;
        List<String> cleanupQueries = new ArrayList<String>();
        int totalCount = executionPlan.getStatements().size();
        int analyzed = 0;
        int succeeded = 0;
        int failed = 0;

        session.clearFailures();

        try {
            connection = session.getConfig().getTargetConParams().createConnection();
            for (AnalyzerStatement statement : executionPlan.getStatements()) {
                analyzed++;
                try {
                    String resultSummary = executeJdbcStatement(connection, statement);
                    succeeded++;
                    session.getConsoleReport()
                            .addStatementResult(
                                    statement.getType(),
                                    statement.getId(),
                                    statement.getSQL(),
                                    true,
                                    resultSummary,
                                    null);
                    notifyProgress(
                            progressListener,
                            new AnalyzerProgressEvent(
                                    AnalyzerProgressStage.STATEMENT_SUCCEEDED,
                                    "[OK] "
                                            + statement.getType()
                                            + " "
                                            + statement.getId()
                                            + " : "
                                            + resultSummary,
                                    statement.getType(),
                                    statement.getId(),
                                    statement.getSQL(),
                                    resultSummary,
                                    null,
                                    totalCount,
                                    analyzed,
                                    succeeded,
                                    failed));

                    String cleanupQuery = buildCleanupQuery(session, statement);
                    if (cleanupQuery != null) {
                        cleanupQueries.add(cleanupQuery);
                    }
                } catch (Exception ex) {
                    failed++;
                    String failureMessage = buildFailureMessage(statement.getType(), statement.getId(), ex.toString());
                    session.addFailureMessage(failureMessage);
                    session.addFailure(
                            buildFailure(statement, ex.toString(), AnalyzerFailureStage.JDBC));
                    session.getConsoleReport()
                            .addStatementResult(
                                    statement.getType(),
                                    statement.getId(),
                                    statement.getSQL(),
                                    false,
                                    ex.toString(),
                                    AnalyzerFailureStage.JDBC);
                    notifyProgress(
                            progressListener,
                            new AnalyzerProgressEvent(
                                    AnalyzerProgressStage.STATEMENT_FAILED,
                                    "[FAIL] " + failureMessage,
                                    statement.getType(),
                                    statement.getId(),
                                    statement.getSQL(),
                                    ex.toString(),
                                    AnalyzerFailureStage.JDBC,
                                    totalCount,
                                    analyzed,
                                    succeeded,
                                    failed));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("JDBC execution failed to start: " + ex.getMessage(), ex);
        } finally {
            if (!cleanupQueries.isEmpty()) {
                runJdbcCleanup(
                        connection,
                        cleanupQueries,
                        session,
                        progressListener,
                        totalCount,
                        analyzed,
                        succeeded,
                        failed);
            }
            Closer.close(connection);
        }

        session.setAnalyzedStatementCount(analyzed);
        session.setSucceededStatementCount(succeeded);
        session.setFailedStatementCount(failed);
        costCalculator.analyzeAfterExecution(session.getConsoleReport());

        publishAnalysisCompleted(progressListener, analyzed, succeeded, failed);
    }

    private String buildFailureMessage(String type, String id, String reason) {
        return type + " " + id + " : " + reason;
    }

    private AnalyzerFailure buildFailure(
            AnalyzerStatement statement, String reason, AnalyzerFailureStage stage) {
        return buildFailure(
                statement.getType(), statement.getId(), statement.getSQL(), reason, stage);
    }

    private AnalyzerFailure buildFailure(
            String statementType,
            String statementId,
            String sql,
            String reason,
            AnalyzerFailureStage stage) {
        AnalyzerFailure failure = new AnalyzerFailure();
        failure.setFailureStage(stage);
        failure.setStatementType(statementType);
        failure.setStatementId(statementId);
        failure.setSql(sql);
        failure.setReason(reason);
        return failure;
    }

    private String executeJdbcStatement(Connection connection, AnalyzerStatement statement)
            throws SQLException {
        Statement jdbcStatement = null;
        ResultSet resultSet = null;

        try {
            jdbcStatement = connection.createStatement();
            boolean hasResultSet = jdbcStatement.execute(statement.getSQL());

            if (hasResultSet) {
                resultSet = jdbcStatement.getResultSet();
                int rowCount = 0;
                while (resultSet.next()) {
                    rowCount++;
                }
                return "rows=" + rowCount;
            }

            int updateCount = jdbcStatement.getUpdateCount();
            if (shouldCommit(statement)) {
                connection.commit();
            }

            if (isDDL(statement)) {
                return "ddl executed";
            }

            return "updated=" + updateCount;
        } finally {
            Closer.close(resultSet);
            Closer.close(jdbcStatement);
        }
    }

    private void runJdbcCleanup(
            Connection connection,
            List<String> cleanupQueries,
            AnalyzerConsoleConfig session,
            AnalyzerProgressListener progressListener,
            int totalCount,
            int completedCount,
            int succeededCount,
            int failedCount) {
        for (int i = cleanupQueries.size() - 1; i >= 0; i--) {
            String cleanupQuery = cleanupQueries.get(i);
            String cleanupId = "CLEANUP_" + (cleanupQueries.size() - i);
            Statement statement = null;
            try {
                statement = connection.createStatement();
                statement.execute(cleanupQuery);
                connection.commit();
                session.getConsoleReport()
                        .addStatementResult(
                                "CLEANUP",
                                cleanupId,
                                cleanupQuery,
                                true,
                                "cleanup executed",
                                null);
                notifyProgress(
                        progressListener,
                        new AnalyzerProgressEvent(
                                AnalyzerProgressStage.CLEANUP_SUCCEEDED,
                                "[CLEANUP OK] " + cleanupQuery,
                                "CLEANUP",
                                cleanupId,
                                cleanupQuery,
                                "cleanup executed",
                                null,
                                totalCount,
                                completedCount,
                                succeededCount,
                                failedCount));
            } catch (Exception ex) {
                String failureMessage = "CLEANUP : " + cleanupQuery + " : " + ex.toString();
                session.addFailureMessage(failureMessage);
                session.addFailure(
                        buildFailure(
                                "CLEANUP",
                                cleanupId,
                                cleanupQuery,
                                ex.toString(),
                                AnalyzerFailureStage.CLEANUP));
                session.getConsoleReport()
                        .addStatementResult(
                                "CLEANUP",
                                cleanupId,
                                cleanupQuery,
                                false,
                                ex.toString(),
                                AnalyzerFailureStage.CLEANUP);
                notifyProgress(
                        progressListener,
                        new AnalyzerProgressEvent(
                                AnalyzerProgressStage.CLEANUP_FAILED,
                                "[CLEANUP FAIL] " + failureMessage,
                                "CLEANUP",
                                cleanupId,
                                cleanupQuery,
                                ex.toString(),
                                AnalyzerFailureStage.CLEANUP,
                                totalCount,
                                completedCount,
                                succeededCount,
                                failedCount));
            } finally {
                Closer.close(statement);
            }
        }
    }

    private boolean shouldCommit(AnalyzerStatement statement) {
        return isDDL(statement)
                || "INSERT".equals(statement.getType())
                || "UPDATE".equals(statement.getType())
                || "DELETE".equals(statement.getType());
    }

    private boolean isDDL(AnalyzerStatement statement) {
        return statement.getType() != null && statement.getType().startsWith("DDL_");
    }

    private String buildCleanupQuery(AnalyzerConsoleConfig session, AnalyzerStatement statement) {
        if (!isDDL(statement)) {
            return null;
        }

        AnalyzerConfiguration config = session.getConfig();
        CUBRIDSQLHelper helper = CUBRIDSQLHelper.getInstance(null);

        if (AnalyzerStatementTypes.TYPE_DDL_TABLE.equals(statement.getType())) {
            int tableIndex = parseStatementIndex(statement.getId(), "TABLE_");
            Table table = config.getTargetTableSchema().get(tableIndex);
            return "DROP TABLE "
                    + helper.getOwnerNameWithDot(table.getOwner(), config.isAddUserSchema())
                    + helper.getQuotedObjName(table.getName())
                    + ";";
        }

        if (AnalyzerStatementTypes.TYPE_DDL_VIEW.equals(statement.getType())
                || AnalyzerStatementTypes.TYPE_DDL_VIEW_CREATE.equals(statement.getType())) {
            int viewIndex = parseStatementIndex(statement.getId(), "VIEW_");
            View view = config.getTargetViewSchema().get(viewIndex);
            return "DROP VIEW "
                    + helper.getOwnerNameWithDot(view.getOwner(), config.isAddUserSchema())
                    + helper.getQuotedObjName(view.getName())
                    + ";";
        }

        if (AnalyzerStatementTypes.TYPE_DDL_SEQUENCE.equals(statement.getType())) {
            int sequenceIndex = parseStatementIndex(statement.getId(), "SEQ_");
            Sequence sequence = config.getTargetSerialSchema().get(sequenceIndex);
            return "DROP SERIAL "
                    + helper.getOwnerNameWithDot(sequence.getOwner(), config.isAddUserSchema())
                    + helper.getQuotedObjName(sequence.getName())
                    + ";";
        }

        if (AnalyzerStatementTypes.TYPE_DDL_SYNONYM.equals(statement.getType())) {
            int synonymIndex = parseStatementIndex(statement.getId(), "SYNONYM_");
            Synonym synonym = config.getTargetSynonymSchema().get(synonymIndex);
            return "DROP SYNONYM "
                    + helper.getOwnerNameWithDot(synonym.getOwner(), config.isAddUserSchema())
                    + helper.getQuotedObjName(synonym.getName())
                    + ";";
        }

        if (AnalyzerStatementTypes.TYPE_DDL_GRANT.equals(statement.getType())) {
            int grantIndex = parseStatementIndex(statement.getId(), "GRANT_");
            SourceGrantConfig grant = config.getExpGrantCfg().get(grantIndex);
            return "REVOKE "
                    + grant.getAuthType()
                    + " ON "
                    + helper.getOwnerNameWithDot(grant.getClassOwner(), config.isAddUserSchema())
                    + helper.getQuotedObjName(grant.getClassName())
                    + " FROM "
                    + helper.getQuotedObjName(grant.getGranteeName())
                    + ";";
        }

        if (AnalyzerStatementTypes.TYPE_DDL_PROC_HEADER.equals(statement.getType())) {
            int procIndex = parseStatementIndex(statement.getId(), "PROC_");
            PlcsqlProcedure procedure = config.getTargetPlcsqlProcedureSchema().get(procIndex);
            return helper.getPlcsqlProcedureDropDDL(procedure, config.isAddUserSchema());
        }

        if (AnalyzerStatementTypes.TYPE_DDL_FUNC_HEADER.equals(statement.getType())) {
            int functionIndex = parseStatementIndex(statement.getId(), "FUNC_");
            PlcsqlFunction function = config.getTargetPlcsqlFunctionSchema().get(functionIndex);
            return helper.getPlcsqlFunctionDropDDL(function, config.isAddUserSchema());
        }

        if (AnalyzerStatementTypes.TYPE_DDL_PK.equals(statement.getType())
                || AnalyzerStatementTypes.TYPE_DDL_FK.equals(statement.getType())
                || AnalyzerStatementTypes.TYPE_DDL_INDEX.equals(statement.getType())
                || AnalyzerStatementTypes.TYPE_DDL_VIEW_ALTER.equals(statement.getType())
                || AnalyzerStatementTypes.TYPE_DDL_PROC_BODY.equals(statement.getType())
                || AnalyzerStatementTypes.TYPE_DDL_FUNC_BODY.equals(statement.getType())) {
            return null;
        }

        return null;
    }

    private int parseStatementIndex(String id, String prefix) {
        if (id == null || !id.startsWith(prefix)) {
            throw new IllegalArgumentException("Unexpected statement id: " + id);
        }

        return Integer.parseInt(id.substring(prefix.length())) - 1;
    }

    private void publishAnalysisCompleted(
            AnalyzerProgressListener progressListener, int analyzed, int succeeded, int failed) {
        notifyProgress(
                progressListener,
                new AnalyzerProgressEvent(
                        AnalyzerProgressStage.COMPLETED,
                        "Analysis completed. Total="
                                + analyzed
                                + ", Success="
                                + succeeded
                                + ", Failed="
                                + failed,
                        null,
                        null,
                        null,
                        null,
                        null,
                        analyzed,
                        analyzed,
                        succeeded,
                        failed));
    }

    private void notifyProgress(AnalyzerProgressListener progressListener, AnalyzerProgressEvent event) {
        if (progressListener != null) {
            progressListener.onProgress(event);
        }
    }
}
