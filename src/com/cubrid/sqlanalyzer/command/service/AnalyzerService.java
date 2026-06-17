package com.cubrid.sqlanalyzer.command.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cubrid.cubridmigration.core.dbmetadata.JDBCDBSchemaFetcherFacade;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbtype.DatabaseType;
import com.cubrid.sqlanalyzer.command.AnalyzerArgumentsController;
import com.cubrid.sqlanalyzer.command.AnalyzerConnParametersFactory;
import com.cubrid.sqlanalyzer.command.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.AnalyzerReport;
import com.cubrid.sqlanalyzer.command.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.AnalyzerJdbcConnectionInfo;
import com.cubrid.sqlanalyzer.command.AnalyzerJdbcConnectionSupport;
import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerObjectCountPreviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerResultViewModel;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.dbobject.AnalyzerCatalog;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;
import com.cubrid.sqlanalyzer.xmlmetadata.XMLDirSchemaFetcher;
import com.cubrid.sqlanalyzer.xmlmetadata.XMLDirSource;

public class AnalyzerService {
    private static final Logger LOG = LoggerFactory.getLogger(AnalyzerService.class);
    private static final String SOURCE_CONNECTION_NAME = "console-source";
    private static final String TARGET_CONNECTION_NAME = "console-target";
    private static final AnalyzerConnParametersFactory ORACLE_CONN_PARAMETERS_FACTORY = AnalyzerJdbcConnectionSupport
            .createFactory(DatabaseType.ORACLE);
    private static final AnalyzerConnParametersFactory CUBRID_CONN_PARAMETERS_FACTORY = AnalyzerJdbcConnectionSupport
            .createFactory(DatabaseType.CUBRID);

    private final AnalyzerViewModelBuilder viewModelBuilder;
    private final AnalyzerExecutionRunner executionRunner;
    private final AnalyzerOracleTableSizeFetcher oracleTableSizeFetcher;

    public AnalyzerService() {
        this(
                new AnalyzerViewModelBuilder(),
                new AnalyzerExecutionRunner(),
                new AnalyzerOracleTableSizeFetcher());
    }

    AnalyzerService(
            AnalyzerViewModelBuilder viewModelBuilder,
            AnalyzerExecutionRunner executionRunner,
            AnalyzerOracleTableSizeFetcher oracleTableSizeFetcher) {
        this.viewModelBuilder = viewModelBuilder;
        this.executionRunner = executionRunner;
        this.oracleTableSizeFetcher = oracleTableSizeFetcher;
    }

    public void applyArguments(AnalyzerSession session, AnalyzerArgumentsController arguments) {
        LOG.info(
                "Applying analyzer arguments. sourceType={}, targetType={}, uiMode={}",
                arguments.getSourceType(),
                arguments.getTargetType(),
                arguments.getUiMode());
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
        LOG.info("Analyzer arguments applied. executionMode={}", session.getExecutionMode());
    }

    public void applyExecutionMode(AnalyzerSession session) {
        LOG.info("Resolving execution mode. sourceType={}", session.getSourceType());
        if (AnalyzerSourceType.ORACLE.equals(session.getSourceType())) {
            session.setExecutionMode(AnalyzerExecutionMode.DDL);
            LOG.info("Execution mode resolved. executionMode={}", session.getExecutionMode());
            return;
        }

        if (AnalyzerSourceType.XML.equals(session.getSourceType())) {
            session.setExecutionMode(AnalyzerExecutionMode.DML);
            LOG.info("Execution mode resolved. executionMode={}", session.getExecutionMode());
            return;
        }

        throw new IllegalStateException("Unsupported source type: " + session.getSourceType());
    }

    public void validateOracleSourceConnection(AnalyzerSession session) {
        LOG.info("Validating Oracle source connection.");
        AnalyzerJdbcConnectionInfo profile = AnalyzerJdbcConnectionSupport.parseOracleProfile(
                session.getSourceJdbcUrl(),
                session.getSourceUser(),
                session.getSourcePassword());
        AnalyzerJdbcConnectionSupport.validateConnection(
                SOURCE_CONNECTION_NAME, profile, ORACLE_CONN_PARAMETERS_FACTORY);
        LOG.info("Oracle source connection validation succeeded.");
    }

    public void validateJdbcTargetConnection(AnalyzerSession session) {
        LOG.info("Validating CUBRID target connection.");
        AnalyzerJdbcConnectionInfo profile = AnalyzerJdbcConnectionSupport.parseCubridProfile(
                session.getTargetJdbcUrl(),
                session.getTargetUser(),
                session.getTargetPassword());
        AnalyzerJdbcConnectionSupport.validateConnection(
                TARGET_CONNECTION_NAME, profile, CUBRID_CONN_PARAMETERS_FACTORY);
        LOG.info("CUBRID target connection validation succeeded.");
    }

    public void prepareConfiguration(AnalyzerSession session) {
        LOG.info(
                "Preparing analyzer configuration. sourceType={}, targetType={}, executionMode={}",
                session.getSourceType(),
                session.getTargetType(),
                session.getExecutionMode());
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
        LOG.info("Analyzer configuration prepared.");
    }

    public void loadSourceCatalog(AnalyzerSession session) {
        LOG.info("Loading source catalog. sourceType={}", session.getSourceType());
        if (session.getSourceType() == AnalyzerSourceType.ORACLE) {
            loadOracleSourceCatalog(session);
            LOG.info("Oracle source catalog loaded.");
            return;
        }

        if (session.getSourceType() == AnalyzerSourceType.XML) {
            loadXmlQueryDictionary(session);
            LOG.info("XML query dictionary loaded.");
            return;
        }

        throw new IllegalStateException("Unsupported source type: " + session.getSourceType());
    }

    public AnalyzerOverviewViewModel getOverview(AnalyzerSession session) {
        return viewModelBuilder.buildOverview(session);
    }

    public AnalyzerObjectCountPreviewViewModel getObjectCountPreview(AnalyzerSession session) {
        return viewModelBuilder.buildObjectCountPreview(session);
    }

    public void runAnalysis(
            AnalyzerSession session, AnalyzerProgressListener progressListener) {
        LOG.info(
                "Running analysis. sourceType={}, targetType={}, executionMode={}",
                session.getSourceType(),
                session.getTargetType(),
                session.getExecutionMode());
        executionRunner.run(session, progressListener);
        LOG.info(
                "Analysis finished. total={}, succeeded={}, failed={}",
                session.getReport().getAnalyzedStatementCount(),
                session.getReport().getSucceededStatementCount(),
                session.getReport().getFailedStatementCount());
    }

    public AnalyzerResultViewModel saveResult(AnalyzerSession session) {
        LOG.info("Saving analyzer result report.");
        AnalyzerReport report = session.getReport();
        report.setOverview(viewModelBuilder.buildOverview(session));
        report.setObjectCountPreview(viewModelBuilder.buildObjectCountPreview(session));
        String savedReportPath = report.saveResultReport();
        LOG.info("Analyzer result report saved. path={}", savedReportPath);
        return viewModelBuilder.buildResult(report, savedReportPath);
    }

    private void loadOracleSourceCatalog(AnalyzerSession session) {
        LOG.info("Fetching Oracle source schema.");
        AnalyzerConfiguration config = session.getConfig();
        JDBCDBSchemaFetcherFacade fetcher = new JDBCDBSchemaFetcherFacade();
        Catalog catalog = fetcher.fetchSchema(config.getSourceConParams(), null);

        if (catalog == null) {
            throw new RuntimeException("Failed to fetch Oracle catalog.");
        }

        session.setSourceCatalog(catalog);
        config.setSrcCatalog(catalog, false);
        config.parsingProcedureFunction(true);
        session.setOracleTableSizes(oracleTableSizeFetcher.fetch(config.getSourceConParams()));
        LOG.info("Oracle source schema fetched. schemaCount={}", catalog.getSchemas().size());
    }

    private void loadXmlQueryDictionary(AnalyzerSession session) {
        LOG.info(
                "Building XML query dictionary. directory={}, charset={}",
                session.getXmlDirectory(),
                session.getXmlCharset());
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
        LOG.info(
                "XML query dictionary built. select={}, insert={}, update={}, delete={}",
                queryDictionary.getSelectQueryMap().size(),
                queryDictionary.getInsertQueryMap().size(),
                queryDictionary.getUpdateQueryMap().size(),
                queryDictionary.getDeleteQueryMap().size());
    }
}
