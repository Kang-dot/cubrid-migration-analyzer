package com.cubrid.sqlanalyzer.command.service;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbobject.Version;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleConfig;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleReport;
import com.cubrid.sqlanalyzer.command.AnalyzerJdbcConnectionInfo;
import com.cubrid.sqlanalyzer.command.AnalyzerJdbcConnectionSupport;
import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerObjectCountPreviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerResultViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerSourceOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTargetOverviewViewModel;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;

public class AnalyzerViewModelBuilder {
    private static final Pattern ORACLE_VERSION_NAME_PATTERN = Pattern.compile("\\b\\d+(?:ai|c|g)\\b",
            Pattern.CASE_INSENSITIVE);

    public AnalyzerOverviewViewModel buildOverview(AnalyzerConsoleConfig session) {
        return new AnalyzerOverviewViewModel(
                getProgramVersion(),
                buildSourceOverview(session),
                buildTargetOverview(session),
                session.getExecutionMode());
    }

    public AnalyzerObjectCountPreviewViewModel buildObjectCountPreview(AnalyzerConsoleConfig session) {
        AnalyzerConfiguration config = session.getConfig();
        if (session.getSourceType() == AnalyzerSourceType.ORACLE) {
            return new AnalyzerObjectCountPreviewViewModel(
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
        return new AnalyzerObjectCountPreviewViewModel(
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

    public AnalyzerResultViewModel buildResult(AnalyzerConsoleReport report, String savedReportPath) {
        return new AnalyzerResultViewModel(
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

    private AnalyzerSourceOverviewViewModel buildSourceOverview(AnalyzerConsoleConfig session) {
        if (session.getSourceType() == AnalyzerSourceType.ORACLE) {
            AnalyzerJdbcConnectionInfo profile = AnalyzerJdbcConnectionSupport.parseOracleProfile(
                    session.getSourceJdbcUrl(),
                    session.getSourceUser(),
                    session.getSourcePassword());
            Catalog catalog = session.getSourceCatalog();
            String version = catalog == null || catalog.getVersion() == null
                    ? null
                    : getOracleVersionName(catalog.getVersion());
            return new AnalyzerSourceOverviewViewModel(
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

        return new AnalyzerSourceOverviewViewModel(
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

    private AnalyzerTargetOverviewViewModel buildTargetOverview(AnalyzerConsoleConfig session) {
        if (session.getTargetType() == AnalyzerTargetType.JDBC) {
            AnalyzerJdbcConnectionInfo profile = AnalyzerJdbcConnectionSupport.parseCubridProfile(
                    session.getTargetJdbcUrl(),
                    session.getTargetUser(),
                    session.getTargetPassword());
            return new AnalyzerTargetOverviewViewModel(
                    session.getTargetType(),
                    session.getTargetJdbcUrl(),
                    profile.getHost(),
                    profile.getPort(),
                    profile.getDatabaseName(),
                    session.getTargetUser(),
                    getTargetVersionName(session),
                    null);
        }

        return new AnalyzerTargetOverviewViewModel(
                session.getTargetType(),
                null,
                null,
                0,
                null,
                null,
                null,
                getParserVersion());
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

    private String getProgramVersion() {
        Package packageInfo = AnalyzerViewModelBuilder.class.getPackage();
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
}
