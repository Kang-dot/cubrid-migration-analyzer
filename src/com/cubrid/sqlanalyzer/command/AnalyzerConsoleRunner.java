package com.cubrid.sqlanalyzer.command;

import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_FK;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_FUNC_BODY;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_FUNC_HEADER;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_GRANT;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_INDEX;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_PK;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_PROC_BODY;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_PROC_HEADER;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_SEQUENCE;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_SYNONYM;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_TABLE;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_VIEW;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_VIEW_ALTER;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_VIEW_CREATE;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPlan;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatement;
import com.cubrid.sqlanalyzer.core.plan.CatalogDDLPlanBuilder;
import com.cubrid.sqlanalyzer.core.plan.QueryDictionaryPlanBuilder;
import com.cubrid.cubridmigration.core.common.Closer;
import com.cubrid.cubridmigration.core.dbmetadata.JDBCDBSchemaFetcherFacade;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbobject.PlcsqlFunction;
import com.cubrid.cubridmigration.core.dbobject.PlcsqlProcedure;
import com.cubrid.cubridmigration.core.dbobject.Sequence;
import com.cubrid.cubridmigration.core.dbobject.Synonym;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbobject.View;
import com.cubrid.cubridmigration.core.engine.config.SourceGrantConfig;
import com.cubrid.cubridmigration.core.dbtype.DatabaseType;
import com.cubrid.cubridmigration.cubrid.CUBRIDSQLHelper;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.cost.AnalyzerCostCalculator;
import com.cubrid.sqlanalyzer.core.cost.FailureCostCalculator;
import com.cubrid.sqlanalyzer.core.dbobject.AnalyzerCatalog;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;
import com.cubrid.sqlanalyzer.core.runner.QueryParser;
import com.cubrid.sqlanalyzer.core.runner.SQLParserException;
import com.cubrid.sqlanalyzer.xmlmetadata.XMLDirSchemaFetcher;
import com.cubrid.sqlanalyzer.xmlmetadata.XMLDirSource;

public class AnalyzerConsoleRunner {
    private static final String DEFAULT_XML_CHARSET = "UTF-8";
    private static final String SOURCE_CONNECTION_NAME = "console-source";
    private static final String TARGET_CONNECTION_NAME = "console-target";
    private static final AnalyzerConnParametersFactory ORACLE_CONN_PARAMETERS_FACTORY = AnalyzerJdbcConnectionSupport
            .createFactory(DatabaseType.ORACLE);
    private static final AnalyzerConnParametersFactory CUBRID_CONN_PARAMETERS_FACTORY = AnalyzerJdbcConnectionSupport
            .createFactory(DatabaseType.CUBRID);

    private final ConsoleIO io;
    private final AnalyzerCostCalculator costCalculator = new FailureCostCalculator();

    public AnalyzerConsoleRunner(ConsoleIO io) {
        this.io = io;
    }

    public int startAnalyzer() {
        io.println("======================================");
        io.println("CUBRID SQL Analyzer Console");
        io.println("======================================");

        AnalyzerConsoleConfig session = new AnalyzerConsoleConfig();

        try {
            selectSource(session);
            selectTarget(session);
            applyExecutionMode(session);
            prepareConfiguration(session);
            loadSourceCatalog(session);
            printObjectCountPage(session);

            if (!io.confirm("Continue analysis? (y/n): ")) {
                io.println("Analysis canceled.");
                return 0;
            }

            runAnalysis(session);
            printResult(session);
            return 0;
        } catch (RuntimeException ex) {
            io.println("Analyzer failed: " + ex.getMessage());
            ex.printStackTrace();
            return 1;
        }
    }

    public int startAnalyzer(AnalyzerConsoleArguments arguments) {
        io.println("======================================");
        io.println("CUBRID SQL Analyzer Console");
        io.println("======================================");

        AnalyzerConsoleConfig session = new AnalyzerConsoleConfig();

        try {
            applyArguments(session, arguments);
            prepareConfiguration(session);
            loadSourceCatalog(session);
            printObjectCountPage(session);
            runAnalysis(session);
            printResult(session);
            return 0;
        } catch (RuntimeException ex) {
            io.println("Analyzer failed: " + ex.getMessage());
            ex.printStackTrace();
            return 1;
        }
    }

    private void selectSource(AnalyzerConsoleConfig session) {
        io.println("");
        io.println("[1/4] Select source");
        io.println("1. Oracle JDBC connection");
        io.println("2. XML directory");

        while (true) {
            String input = io.readRequired("Select source (1-2): ");
            if ("1".equals(input)) {
                session.setSourceType(AnalyzerSourceType.ORACLE);
                promptOracleSource(session);
                return;
            }
            if ("2".equals(input)) {
                session.setSourceType(AnalyzerSourceType.XML);
                promptXmlSource(session);
                return;
            }
            io.println("Invalid selection.");
        }
    }

    private void promptOracleSource(AnalyzerConsoleConfig session) {
        while (true) {
            session.setSourceJdbcUrl(io.readRequired("Oracle JDBC URL: "));
            session.setSourceUser(io.readRequired("Oracle user: "));
            session.setSourcePassword(io.readRequired("Oracle password: "));

            try {
                validateOracleSourceConnection(session);
                io.println("Oracle connection validation succeeded.");
                return;
            } catch (RuntimeException ex) {
                io.println(ex.getMessage());
                if (!io.confirm("Retry Oracle connection input? (y/n): ")) {
                    throw ex;
                }
            }
        }
    }

    private void promptXmlSource(AnalyzerConsoleConfig session) {
        session.setXmlDirectory(io.readRequired("XML directory path: "));
        String charset = readLineWithDefault("XML charset [UTF-8]: ", DEFAULT_XML_CHARSET);
        session.setXmlCharset(charset.isEmpty() ? DEFAULT_XML_CHARSET : charset);
    }

    private void selectTarget(AnalyzerConsoleConfig session) {
        io.println("");
        io.println("[2/4] Select target");
        io.println("1. CUBRID JDBC execution");
        io.println("2. Parser execution");

        while (true) {
            String input = io.readRequired("Select target (1-2): ");
            if ("1".equals(input)) {
                session.setTargetType(AnalyzerTargetType.JDBC);
                promptJdbcTarget(session);
                return;
            }
            if ("2".equals(input)) {
                session.setTargetType(AnalyzerTargetType.PARSER);
                return;
            }
            io.println("Invalid selection.");
        }
    }

    private void promptJdbcTarget(AnalyzerConsoleConfig session) {
        while (true) {
            session.setTargetJdbcUrl(io.readRequired("Target JDBC URL: "));
            session.setTargetUser(io.readRequired("Target user: "));
            session.setTargetPassword(io.readRequired("Target password: "));

            try {
                validateJdbcTargetConnection(session);
                io.println("Target connection validation succeeded.");
                return;
            } catch (RuntimeException ex) {
                io.println(ex.getMessage());
                if (!io.confirm("Retry target connection input? (y/n): ")) {
                    throw ex;
                }
            }
        }
    }

    private void applyExecutionMode(AnalyzerConsoleConfig session) {
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

    private void applyArguments(
            AnalyzerConsoleConfig session, AnalyzerConsoleArguments arguments) {
        session.setSourceType(arguments.getSourceType());
        if (AnalyzerSourceType.ORACLE.equals(arguments.getSourceType())) {
            session.setSourceJdbcUrl(arguments.getSourceJdbcUrl());
            session.setSourceUser(arguments.getSourceUser());
            session.setSourcePassword(arguments.getSourcePassword());
            validateOracleSourceConnection(session);
            io.println("Oracle connection validation succeeded.");
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
            io.println("Target connection validation succeeded.");
        }

        applyExecutionMode(session);
    }

    private void validateOracleSourceConnection(AnalyzerConsoleConfig session) {
        AnalyzerJdbcConnectionInfo profile = AnalyzerJdbcConnectionSupport.parseOracleProfile(
                session.getSourceJdbcUrl(),
                session.getSourceUser(),
                session.getSourcePassword());
        AnalyzerJdbcConnectionSupport.validateConnection(
                SOURCE_CONNECTION_NAME, profile, ORACLE_CONN_PARAMETERS_FACTORY);
    }

    private void validateJdbcTargetConnection(AnalyzerConsoleConfig session) {
        AnalyzerJdbcConnectionInfo profile = AnalyzerJdbcConnectionSupport.parseCubridProfile(
                session.getTargetJdbcUrl(),
                session.getTargetUser(),
                session.getTargetPassword());
        AnalyzerJdbcConnectionSupport.validateConnection(
                TARGET_CONNECTION_NAME, profile, CUBRID_CONN_PARAMETERS_FACTORY);
    }

    private void prepareConfiguration(AnalyzerConsoleConfig session) {
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

    private void loadSourceCatalog(AnalyzerConsoleConfig session) {
        io.println("");
        io.println("Loading source metadata...");

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

        io.println("Oracle catalog loaded.");
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

        io.println("XML query dictionary loaded.");
    }

    private void printObjectCountPage(AnalyzerConsoleConfig session) {
        AnalyzerConfiguration config = session.getConfig();

        io.println("");
        io.println("[3/4] Object count preview");
        io.println("Source      : " + session.getSourceType());
        io.println("Target      : " + session.getTargetType());
        io.println("Mode        : " + session.getExecutionMode());

        if (session.getSourceType() == AnalyzerSourceType.ORACLE) {
            io.println("Oracle URL  : " + session.getSourceJdbcUrl());
        } else {
            io.println("XML dir     : " + session.getXmlDirectory());
            io.println("XML charset : " + session.getXmlCharset());
        }

        if (session.getTargetType() == AnalyzerTargetType.JDBC) {
            io.println("Target URL  : " + session.getTargetJdbcUrl());
        }

        io.println("");
        if (session.getSourceType() == AnalyzerSourceType.ORACLE) {
            long targetPkCount = countTargetPrimaryKeys(config.getTargetTableSchema());
            long targetFkCount = countTargetForeignKeys(config.getTargetTableSchema());

            io.println("Catalog schemas : " + session.getSourceCatalog().getSchemas().size());
            io.println("Target tables   : " + config.getTargetTableSchema().size());
            io.println("Target PKs      : " + targetPkCount);
            io.println("Target FKs      : " + targetFkCount);
            io.println("Target views    : " + config.getTargetViewSchema().size());
            io.println("Target serials  : " + config.getTargetSerialSchema().size());
            io.println("Target synonyms : " + config.getTargetSynonymSchema().size());
            io.println("Target grants   : " + config.getExpGrantCfg().size());
            io.println("Target procs    : " + config.getTargetPlcsqlProcedureSchema().size());
            io.println("Target funcs    : " + config.getTargetPlcsqlFunctionSchema().size());
        } else {
            QueryDictionary dict = config.getQueryDict();
            io.println("SELECT count    : " + dict.getSelectQueryMap().size());
            io.println("INSERT count    : " + dict.getInsertQueryMap().size());
            io.println("UPDATE count    : " + dict.getUpdateQueryMap().size());
            io.println("DELETE count    : " + dict.getDeleteQueryMap().size());
        }
    }

    private void runAnalysis(AnalyzerConsoleConfig session) {
        io.println("");
        io.println("[4/4] Analysis progress");

        AnalyzerExecutionPlan executionPlan = buildExecutionPlan(session);
        costCalculator.analyzeBeforeExecution(executionPlan, session.getConsoleReport());
        if (executionPlan.isEmpty()) {
            session.setAnalyzedStatementCount(0);
            session.setSucceededStatementCount(0);
            session.setFailedStatementCount(0);
            session.clearFailures();
            io.println("No SQL statements were generated for the selected source/mode.");
            return;
        }

        if (session.getTargetType() == AnalyzerTargetType.PARSER) {
            runParserAnalysis(session, executionPlan);
            return;
        }

        if (session.getTargetType() == AnalyzerTargetType.JDBC) {
            runJdbcAnalysis(session, executionPlan);
            return;
        }

        throw new IllegalStateException("Unsupported target type: " + session.getTargetType());
    }

    private void runParserAnalysis(
            AnalyzerConsoleConfig session, AnalyzerExecutionPlan executionPlan) {
        QueryParser queryParser = new QueryParser();
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
                io.println("[OK] " + statement.getType() + " " + statement.getId());
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
                io.println("[FAIL] " + failureMessage);
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
                io.println("[FAIL] " + failureMessage);
            }
        }

        session.setAnalyzedStatementCount(analyzed);
        session.setSucceededStatementCount(succeeded);
        session.setFailedStatementCount(failed);
        costCalculator.analyzeAfterExecution(session.getConsoleReport());

        io.println(
                "Analysis completed. Total="
                        + analyzed
                        + ", Success="
                        + succeeded
                        + ", Failed="
                        + failed);
    }

    private void runJdbcAnalysis(
            AnalyzerConsoleConfig session, AnalyzerExecutionPlan executionPlan) {
        Connection connection = null;
        List<String> cleanupQueries = new ArrayList<String>();
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
                    io.println(
                            "[OK] "
                                    + statement.getType()
                                    + " "
                                    + statement.getId()
                                    + " : "
                                    + resultSummary);

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
                    io.println("[FAIL] " + failureMessage);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("JDBC execution failed to start: " + ex.getMessage(), ex);
        } finally {
            if (!cleanupQueries.isEmpty()) {
                runJdbcCleanup(connection, cleanupQueries, session);
            }
            Closer.close(connection);
        }

        session.setAnalyzedStatementCount(analyzed);
        session.setSucceededStatementCount(succeeded);
        session.setFailedStatementCount(failed);
        costCalculator.analyzeAfterExecution(session.getConsoleReport());

        io.println(
                "Analysis completed. Total="
                        + analyzed
                        + ", Success="
                        + succeeded
                        + ", Failed="
                        + failed);
    }

    private void printResult(AnalyzerConsoleConfig session) {
        AnalyzerConsoleReport report = session.getConsoleReport();

        io.println("");
        io.println("Result summary");
        io.println("Source : " + report.getSourceType());
        io.println("Target : " + report.getTargetType());
        io.println("Mode   : " + report.getExecutionMode());
        io.println("Total  : " + report.getAnalyzedStatementCount());
        io.println("OK     : " + report.getSucceededStatementCount());
        io.println("FAIL   : " + report.getFailedStatementCount());

        if (!report.getFailures().isEmpty()) {
            io.println("");
            io.println("Failed statements");
            for (AnalyzerConsoleFailure failure : report.getFailures()) {
                io.println(
                        "- "
                                + failure.getStatementType()
                                + " "
                                + failure.getStatementId()
                                + " ["
                                + failure.getFailureStage()
                                + "]");
                io.println("  Reason: " + failure.getReason());
                io.println("  SQL   : " + String.valueOf(failure.getSql()));
            }
        } else if (!report.getFailureMessages().isEmpty()) {
            io.println("");
            io.println("Failed statements");
            for (String failureMessage : report.getFailureMessages()) {
                io.println("- " + failureMessage);
            }
        }

        String savedReportPath = report.saveResultReport();
        io.println("");
        io.println("Saved result report: " + savedReportPath);
    }

    private String readLineWithDefault(String prompt, String defaultValue) {
        io.print(prompt);
        String input = io.readLine();
        return input.isEmpty() ? defaultValue : input;
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

    private String buildFailureMessage(String type, String id, String reason) {
        return type + " " + id + " : " + reason;
    }

    private AnalyzerConsoleFailure buildFailure(
            AnalyzerStatement statement, String reason, AnalyzerFailureStage stage) {
        return buildFailure(
                statement.getType(), statement.getId(), statement.getSQL(), reason, stage);
    }

    private AnalyzerConsoleFailure buildFailure(
            String statementType,
            String statementId,
            String sql,
            String reason,
            AnalyzerFailureStage stage) {
        AnalyzerConsoleFailure failure = new AnalyzerConsoleFailure();
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
            Connection connection, List<String> cleanupQueries, AnalyzerConsoleConfig session) {
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
                io.println("[CLEANUP OK] " + cleanupQuery);
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
                io.println("[CLEANUP FAIL] " + failureMessage);
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

        if (TYPE_DDL_TABLE.equals(statement.getType())) {
            int tableIndex = parseStatementIndex(statement.getId(), "TABLE_");
            Table table = config.getTargetTableSchema().get(tableIndex);
            return "DROP TABLE "
                    + helper.getOwnerNameWithDot(table.getOwner(), config.isAddUserSchema())
                    + helper.getQuotedObjName(table.getName())
                    + ";";
        }

        if (TYPE_DDL_VIEW.equals(statement.getType())
                || TYPE_DDL_VIEW_CREATE.equals(statement.getType())) {
            int viewIndex = parseStatementIndex(statement.getId(), "VIEW_");
            View view = config.getTargetViewSchema().get(viewIndex);
            return "DROP VIEW "
                    + helper.getOwnerNameWithDot(view.getOwner(), config.isAddUserSchema())
                    + helper.getQuotedObjName(view.getName())
                    + ";";
        }

        if (TYPE_DDL_SEQUENCE.equals(statement.getType())) {
            int sequenceIndex = parseStatementIndex(statement.getId(), "SEQ_");
            Sequence sequence = config.getTargetSerialSchema().get(sequenceIndex);
            return "DROP SERIAL "
                    + helper.getOwnerNameWithDot(sequence.getOwner(), config.isAddUserSchema())
                    + helper.getQuotedObjName(sequence.getName())
                    + ";";
        }

        if (TYPE_DDL_SYNONYM.equals(statement.getType())) {
            int synonymIndex = parseStatementIndex(statement.getId(), "SYNONYM_");
            Synonym synonym = config.getTargetSynonymSchema().get(synonymIndex);
            return "DROP SYNONYM "
                    + helper.getOwnerNameWithDot(synonym.getOwner(), config.isAddUserSchema())
                    + helper.getQuotedObjName(synonym.getName())
                    + ";";
        }

        if (TYPE_DDL_GRANT.equals(statement.getType())) {
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

        if (TYPE_DDL_PROC_HEADER.equals(statement.getType())) {
            int procIndex = parseStatementIndex(statement.getId(), "PROC_");
            PlcsqlProcedure procedure = config.getTargetPlcsqlProcedureSchema().get(procIndex);
            return helper.getPlcsqlProcedureDropDDL(procedure, config.isAddUserSchema());
        }

        if (TYPE_DDL_FUNC_HEADER.equals(statement.getType())) {
            int functionIndex = parseStatementIndex(statement.getId(), "FUNC_");
            PlcsqlFunction function = config.getTargetPlcsqlFunctionSchema().get(functionIndex);
            return helper.getPlcsqlFunctionDropDDL(function, config.isAddUserSchema());
        }

        if (TYPE_DDL_PK.equals(statement.getType())
                || TYPE_DDL_FK.equals(statement.getType())
                || TYPE_DDL_INDEX.equals(statement.getType())
                || TYPE_DDL_VIEW_ALTER.equals(statement.getType())
                || TYPE_DDL_PROC_BODY.equals(statement.getType())
                || TYPE_DDL_FUNC_BODY.equals(statement.getType())) {
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

    private long countTargetPrimaryKeys(List<Table> tables) {
        long count = 0;

        for (Table table : tables) {
            if (table.getPk() != null && !table.getPk().getPkColumns().isEmpty()) {
                count++;
            }
        }

        return count;
    }

    private long countTargetForeignKeys(List<Table> tables) {
        long count = 0;

        for (Table table : tables) {
            count += table.getFks().size();
        }

        return count;
    }
}
