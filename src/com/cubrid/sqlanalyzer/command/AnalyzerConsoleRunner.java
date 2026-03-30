package com.cubrid.sqlanalyzer.command;

import com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPlan;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatement;
import com.cubrid.sqlanalyzer.core.plan.CatalogDDLPlanBuilder;
import com.cubrid.sqlanalyzer.core.plan.QueryDictionaryPlanBuilder;
import com.cubrid.cubridmigration.core.dbmetadata.JDBCDBSchemaFetcherFacade;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbtype.DatabaseType;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
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
    private static final AnalyzerConnParametersFactory ORACLE_CONN_PARAMETERS_FACTORY =
            AnalyzerJdbcConnectionSupport.createFactory(DatabaseType.ORACLE);
    private static final AnalyzerConnParametersFactory CUBRID_CONN_PARAMETERS_FACTORY =
            AnalyzerJdbcConnectionSupport.createFactory(DatabaseType.CUBRID);

    private final ConsoleIO io;

    public AnalyzerConsoleRunner(ConsoleIO io) {
        this.io = io;
    }

    public int startAnalyzer() {
        io.println("======================================");
        io.println("CUBRID SQL Analyzer Console");
        io.println("======================================");

        AnalyzerConsoleSession session = new AnalyzerConsoleSession();

        try {
            selectSource(session);
            selectTarget(session);
            selectExecutionMode(session);
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

    private void selectSource(AnalyzerConsoleSession session) {
        io.println("");
        io.println("[1/5] Select source");
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

    private void promptOracleSource(AnalyzerConsoleSession session) {
        while (true) {
            session.setSourceJdbcUrl(io.readRequired("Oracle JDBC URL: "));
            session.setSourceUser(io.readRequired("Oracle user: "));
            session.setSourcePassword(io.readRequired("Oracle password: "));

            try {
                AnalyzerJdbcConnectionProfile profile =
                        AnalyzerJdbcConnectionSupport.parseOracleProfile(
                                session.getSourceJdbcUrl(),
                                session.getSourceUser(),
                                session.getSourcePassword());
                AnalyzerJdbcConnectionSupport.validateConnection(
                        SOURCE_CONNECTION_NAME, profile, ORACLE_CONN_PARAMETERS_FACTORY);
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

    private void promptXmlSource(AnalyzerConsoleSession session) {
        session.setXmlDirectory(io.readRequired("XML directory path: "));
        String charset = readLineWithDefault("XML charset [UTF-8]: ", DEFAULT_XML_CHARSET);
        session.setXmlCharset(charset.isEmpty() ? DEFAULT_XML_CHARSET : charset);
    }

    private void selectTarget(AnalyzerConsoleSession session) {
        io.println("");
        io.println("[2/5] Select target");
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

    private void promptJdbcTarget(AnalyzerConsoleSession session) {
        while (true) {
            session.setTargetJdbcUrl(io.readRequired("Target JDBC URL: "));
            session.setTargetUser(io.readRequired("Target user: "));
            session.setTargetPassword(io.readRequired("Target password: "));

            try {
                AnalyzerJdbcConnectionProfile profile =
                        AnalyzerJdbcConnectionSupport.parseCubridProfile(
                                session.getTargetJdbcUrl(),
                                session.getTargetUser(),
                                session.getTargetPassword());
                AnalyzerJdbcConnectionSupport.validateConnection(
                        TARGET_CONNECTION_NAME, profile, CUBRID_CONN_PARAMETERS_FACTORY);
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

    private void selectExecutionMode(AnalyzerConsoleSession session) {
        io.println("");
        io.println("[3/5] Select execution mode");
        io.println("1. DDL only");
        io.println("2. DML only");
        io.println("3. DDL + DML");

        while (true) {
            String input = io.readRequired("Select mode (1-3): ");
            if ("1".equals(input)) {
                session.setExecutionMode(AnalyzerExecutionMode.DDL);
                return;
            }
            if ("2".equals(input)) {
                session.setExecutionMode(AnalyzerExecutionMode.DML);
                return;
            }
            if ("3".equals(input)) {
                session.setExecutionMode(AnalyzerExecutionMode.ALL);
                return;
            }
            io.println("Invalid selection.");
        }
    }

    private void prepareConfiguration(AnalyzerConsoleSession session) {
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
    
    private void loadSourceCatalog(AnalyzerConsoleSession session) {
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

    private void loadOracleSourceCatalog(AnalyzerConsoleSession session) {
        AnalyzerConfiguration config = session.getConfig();
        JDBCDBSchemaFetcherFacade fetcher = new JDBCDBSchemaFetcherFacade();
        Catalog catalog = fetcher.fetchSchema(config.getSourceConParams(), null);

        if (catalog == null) {
            throw new RuntimeException("Failed to fetch Oracle catalog.");
        }

        session.setSourceCatalog(catalog);
        config.setSrcCatalog(catalog, false);

        io.println("Oracle catalog loaded.");
    }

    private void loadXmlQueryDictionary(AnalyzerConsoleSession session) {
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

    private void printObjectCountPage(AnalyzerConsoleSession session) {
        AnalyzerConfiguration config = session.getConfig();

        io.println("");
        io.println("[4/5] Object count preview");
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
            io.println("Catalog schemas : " + session.getSourceCatalog().getSchemas().size());
            io.println("Target tables   : " + config.getTargetTableSchema().size());
            io.println("Target views    : " + config.getTargetViewSchema().size());
        } else {
            QueryDictionary dict = config.getQueryDict();
            io.println("SELECT count    : " + dict.getSelectQueryMap().size());
            io.println("INSERT count    : " + dict.getInsertQueryMap().size());
            io.println("UPDATE count    : " + dict.getUpdateQueryMap().size());
            io.println("DELETE count    : " + dict.getDeleteQueryMap().size());
        }
    }

    private void runAnalysis(AnalyzerConsoleSession session) {
        io.println("");
        io.println("[5/5] Analysis progress");

        if (session.getTargetType() != AnalyzerTargetType.PARSER) {
            throw new UnsupportedOperationException(
                    "Console analysis currently supports parser execution only.");
        }

        AnalyzerExecutionPlan executionPlan = buildExecutionPlan(session);
        if (executionPlan.isEmpty()) {
            session.setAnalyzedStatementCount(0);
            session.setSucceededStatementCount(0);
            session.setFailedStatementCount(0);
            session.clearFailures();
            io.println("No SQL statements were generated for the selected source/mode.");
            return;
        }

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
                io.println("[OK] " + statement.getType() + " " + statement.getId());
            } catch (SQLParserException ex) {
                failed++;
                String failureMessage =
                        buildFailureMessage(statement.getType(), statement.getId(), ex.getMessage());
                session.addFailureMessage(failureMessage);
                io.println("[FAIL] " + failureMessage);
            } catch (Exception ex) {
                failed++;
                String failureMessage =
                        buildFailureMessage(statement.getType(), statement.getId(), ex.toString());
                session.addFailureMessage(failureMessage);
                io.println("[FAIL] " + failureMessage);
            }
        }

        session.setAnalyzedStatementCount(analyzed);
        session.setSucceededStatementCount(succeeded);
        session.setFailedStatementCount(failed);

        io.println(
                "Analysis completed. Total="
                        + analyzed
                        + ", Success="
                        + succeeded
                        + ", Failed="
                        + failed);
    }

    private void printResult(AnalyzerConsoleSession session) {
        io.println("");
        io.println("Result summary");
        io.println("Source : " + session.getSourceType());
        io.println("Target : " + session.getTargetType());
        io.println("Mode   : " + session.getExecutionMode());
        io.println("Total  : " + session.getAnalyzedStatementCount());
        io.println("OK     : " + session.getSucceededStatementCount());
        io.println("FAIL   : " + session.getFailedStatementCount());

        if (!session.getFailureMessages().isEmpty()) {
            io.println("");
            io.println("Failed statements");
            for (String failureMessage : session.getFailureMessages()) {
                io.println("- " + failureMessage);
            }
        }
    }

    private String readLineWithDefault(String prompt, String defaultValue) {
        io.print(prompt);
        String input = io.readLine();
        return input.isEmpty() ? defaultValue : input;
    }

    private AnalyzerExecutionPlan buildExecutionPlan(AnalyzerConsoleSession session) {
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
}
