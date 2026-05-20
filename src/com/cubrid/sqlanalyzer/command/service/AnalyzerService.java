package com.cubrid.sqlanalyzer.command.service;

import com.cubrid.cubridmigration.core.dbmetadata.JDBCDBSchemaFetcherFacade;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbtype.DatabaseType;
import com.cubrid.sqlanalyzer.command.AnalyzerConnParametersFactory;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleArguments;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleConfig;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleReport;
import com.cubrid.sqlanalyzer.command.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.AnalyzerJdbcConnectionInfo;
import com.cubrid.sqlanalyzer.command.AnalyzerJdbcConnectionSupport;
import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.dto.AnalyzerObjectCountPreview;
import com.cubrid.sqlanalyzer.command.dto.AnalyzerOverview;
import com.cubrid.sqlanalyzer.command.dto.AnalyzerResult;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.dbobject.AnalyzerCatalog;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;
import com.cubrid.sqlanalyzer.xmlmetadata.XMLDirSchemaFetcher;
import com.cubrid.sqlanalyzer.xmlmetadata.XMLDirSource;

public class AnalyzerService {
    private static final String SOURCE_CONNECTION_NAME = "console-source";
    private static final String TARGET_CONNECTION_NAME = "console-target";
    private static final AnalyzerConnParametersFactory ORACLE_CONN_PARAMETERS_FACTORY = AnalyzerJdbcConnectionSupport
            .createFactory(DatabaseType.ORACLE);
    private static final AnalyzerConnParametersFactory CUBRID_CONN_PARAMETERS_FACTORY = AnalyzerJdbcConnectionSupport
            .createFactory(DatabaseType.CUBRID);

    private final AnalyzerDTOBuilder dtoBuilder;
    private final AnalyzerExecutionRunner executionRunner;

    public AnalyzerService() {
        this(new AnalyzerDTOBuilder(), new AnalyzerExecutionRunner());
    }

    AnalyzerService(AnalyzerDTOBuilder dtoBuilder, AnalyzerExecutionRunner executionRunner) {
        this.dtoBuilder = dtoBuilder;
        this.executionRunner = executionRunner;
    }

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
        return dtoBuilder.buildOverview(session);
    }

    public AnalyzerObjectCountPreview getObjectCountPreview(AnalyzerConsoleConfig session) {
        return dtoBuilder.buildObjectCountPreview(session);
    }

    public void runAnalysis(
            AnalyzerConsoleConfig session, AnalyzerProgressListener progressListener) {
        executionRunner.run(session, progressListener);
    }

    public AnalyzerResult saveResult(AnalyzerConsoleConfig session) {
        AnalyzerConsoleReport report = session.getConsoleReport();
        String savedReportPath = report.saveResultReport();
        return dtoBuilder.buildResult(report, savedReportPath);
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
}
