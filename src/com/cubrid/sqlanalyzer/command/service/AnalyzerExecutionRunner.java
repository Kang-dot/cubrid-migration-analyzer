package com.cubrid.sqlanalyzer.command.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.cubrid.cubridmigration.core.dbobject.PlcsqlFunction;
import com.cubrid.cubridmigration.core.dbobject.PlcsqlProcedure;
import com.cubrid.cubridmigration.core.dbobject.Sequence;
import com.cubrid.cubridmigration.core.dbobject.Synonym;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbobject.View;
import com.cubrid.cubridmigration.core.engine.config.SourceGrantConfig;
import com.cubrid.cubridmigration.cubrid.CUBRIDSQLHelper;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.model.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.model.AnalyzerFailure;
import com.cubrid.sqlanalyzer.command.model.AnalyzerFailureStage;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressEventViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressCounts;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressObjectCount;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.cost.AnalyzerCostCalculator;
import com.cubrid.sqlanalyzer.core.cost.FailureCostCalculator;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPlan;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatement;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes;
import com.cubrid.sqlanalyzer.core.plan.CatalogDDLPlanBuilder;
import com.cubrid.sqlanalyzer.core.plan.QueryDictionaryPlanBuilder;
import com.cubrid.sqlanalyzer.core.runner.PlcsqlChecker;
import com.cubrid.sqlanalyzer.core.runner.PlcsqlChecker.PlcsqlCheckResult;
import com.cubrid.sqlanalyzer.core.runner.PlcsqlChecker.StaticSql;
import com.cubrid.sqlanalyzer.core.runner.QueryParser;
import com.cubrid.sqlanalyzer.core.runner.SQLParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyzerExecutionRunner {
    private static final Logger LOG = LoggerFactory.getLogger(AnalyzerExecutionRunner.class);

    private final AnalyzerCostCalculator costCalculator = new FailureCostCalculator();

    public void run(
            AnalyzerSession session, AnalyzerProgressListener progressListener) {
        LOG.info(
                "Analysis execution started. sourceType={}, targetType={}, executionMode={}",
                session.getSourceType(),
                session.getTargetType(),
                session.getExecutionMode());
        AnalyzerExecutionPlan executionPlan = buildExecutionPlan(session);
        int totalCount = executionPlan.getStatements().size();
        ObjectProgressTracker objectProgressTracker = ObjectProgressTracker.from(executionPlan);
        LOG.info("Analysis execution plan built. statementCount={}", totalCount);
        notifyProgress(
                progressListener,
                AnalyzerProgressEventViewModel.planning(
                        objectProgressTracker.snapshot(0, 0, 0)));
        costCalculator.analyzeBeforeExecution(executionPlan, session.getReport());
        if (executionPlan.isEmpty()) {
            LOG.info("Analysis execution ended without statements.");
            session.setAnalyzedStatementCount(0);
            session.setSucceededStatementCount(0);
            session.setFailedStatementCount(0);
            session.clearFailures();
            notifyProgress(
                    progressListener,
                    AnalyzerProgressEventViewModel.empty(
                            objectProgressTracker.snapshot(0, 0, 0)));
            return;
        }

        if (session.getTargetType() == AnalyzerTargetType.PARSER) {
            LOG.info("Running parser analysis.");
            runParserAnalysis(session, executionPlan, objectProgressTracker, progressListener);
            return;
        }

        if (session.getTargetType() == AnalyzerTargetType.JDBC) {
            LOG.info("Running JDBC analysis.");
            runJdbcAnalysis(session, executionPlan, objectProgressTracker, progressListener);
            return;
        }

        throw new IllegalStateException("Unsupported target type: " + session.getTargetType());
    }

    private AnalyzerExecutionPlan buildExecutionPlan(AnalyzerSession session) {
        LOG.info(
                "Building execution plan. sourceType={}, executionMode={}",
                session.getSourceType(),
                session.getExecutionMode());
        AnalyzerExecutionPlan plan = new AnalyzerExecutionPlan();
        if (session.isOracleSourceLoaded()) {
            plan.addAll(new CatalogDDLPlanBuilder().build(session.getConfig()));
        }
        if (session.isXmlSourceLoaded()) {
            plan.addAll(new QueryDictionaryPlanBuilder().build(session.getConfig()));
        }
        return plan;
    }

    private void runParserAnalysis(
            AnalyzerSession session,
            AnalyzerExecutionPlan executionPlan,
            ObjectProgressTracker objectProgressTracker,
            AnalyzerProgressListener progressListener) {
        QueryParser queryParser = new QueryParser();
        int analyzed = 0;
        int succeeded = 0;
        int failed = 0;

        session.clearFailures();

        try (PlcsqlChecker plcsqlChecker = new PlcsqlChecker()) {
            for (AnalyzerStatement statement : executionPlan.getStatements()) {
                analyzed++;
                if (isUnsupportedStatement(statement)) {
                    failed++;
                    objectProgressTracker.record(statement, false);
                    objectUnsupportedFailure(
                            session, progressListener,
                            objectProgressTracker.snapshot(analyzed, succeeded, failed),
                            statement,
                            AnalyzerUnsupportedStatementPolicy.getUnsupportedReason(statement));
                    continue;
                }
                try {
                    PlcsqlCheckResult plcsqlCheckResult =
                            checkStatement(queryParser, plcsqlChecker, statement);
                    succeeded++;
                    objectProgressTracker.record(statement, true);
                    session.getReport()
                            .addStatementResult(
                                    statement.getType(),
                                    statement.getId(),
                                    statement.getObjectName(),
                                    statement.getSQL(),
                                    true,
                                    "parsed",
                                    null);
                    notifyProgress(
                            progressListener,
                            AnalyzerProgressEventViewModel.statementSucceeded(
                                    statement,
                                    "parsed",
                                    objectProgressTracker.snapshot(analyzed, succeeded, failed)));
                    if (plcsqlCheckResult != null) {
                        AnalysisCounters counters =
                                analyzeStaticSqls(
                                        queryParser,
                                        session,
                                        progressListener,
                                        objectProgressTracker,
                                        statement,
                                        plcsqlCheckResult,
                                        analyzed,
                                        succeeded,
                                        failed);
                        analyzed = counters.analyzed;
                        succeeded = counters.succeeded;
                        failed = counters.failed;
                    }
                } catch (SQLParserException ex) {
                    failed++;
                    objectProgressTracker.record(statement, false);
                    String failureMessage = buildFailureMessage(statement.getType(), statement.getId(), ex.getMessage());
                    LOG.warn(
                            "Parser analysis failed for statement. statementType={}, statementId={}, reason={}",
                            statement.getType(),
                            statement.getId(),
                            ex.getMessage(),
                            ex);
                    session.addFailureMessage(failureMessage);
                    session.addFailure(
                            buildFailure(statement, ex.getMessage(), AnalyzerFailureStage.PARSER));
                    session.getReport()
                            .addStatementResult(
                                    statement.getType(),
                                    statement.getId(),
                                    statement.getObjectName(),
                                    statement.getSQL(),
                                    false,
                                    ex.getMessage(),
                                    AnalyzerFailureStage.PARSER);
                    notifyProgress(
                            progressListener,
                            AnalyzerProgressEventViewModel.statementFailed(
                                    statement,
                                    ex.getMessage(),
                                    AnalyzerFailureStage.PARSER,
                                    objectProgressTracker.snapshot(analyzed, succeeded, failed)));
                } catch (Exception ex) {
                    failed++;
                    objectProgressTracker.record(statement, false);
                    String failureMessage = buildFailureMessage(statement.getType(), statement.getId(), ex.toString());
                    LOG.warn(
                            "Unexpected parser analysis exception for statement. statementType={}, statementId={}",
                            statement.getType(),
                            statement.getId(),
                            ex);
                    session.addFailureMessage(failureMessage);
                    session.addFailure(
                            buildFailure(statement, ex.toString(), AnalyzerFailureStage.PARSER));
                    session.getReport()
                            .addStatementResult(
                                    statement.getType(),
                                    statement.getId(),
                                    statement.getObjectName(),
                                    statement.getSQL(),
                                    false,
                                    ex.toString(),
                                    AnalyzerFailureStage.PARSER);
                    notifyProgress(
                            progressListener,
                            AnalyzerProgressEventViewModel.statementFailed(
                                    statement,
                                    ex.toString(),
                                    AnalyzerFailureStage.PARSER,
                                    objectProgressTracker.snapshot(analyzed, succeeded, failed)));
                }
            }
        }

        session.setAnalyzedStatementCount(analyzed);
        session.setSucceededStatementCount(succeeded);
        session.setFailedStatementCount(failed);
        costCalculator.analyzeAfterExecution(session.getReport());

        LOG.info(
                "Parser analysis completed. total={}, succeeded={}, failed={}",
                analyzed,
                succeeded,
                failed);
        notifyAnalysisCompleted(progressListener,
                objectProgressTracker.snapshot(analyzed, succeeded, failed));
    }

    private PlcsqlCheckResult checkStatement(
            QueryParser queryParser,
            PlcsqlChecker plcsqlChecker,
            AnalyzerStatement statement) throws SQLParserException {
        if (isPlcsqlStatement(statement)) {
            return plcsqlChecker.checkSQLAndGetStaticSqls(statement.getSQL());
        }
        queryParser.checkSQL(statement.getSQL());
        return null;
    }

    private void runJdbcAnalysis(
            AnalyzerSession session,
            AnalyzerExecutionPlan executionPlan,
            ObjectProgressTracker objectProgressTracker,
            AnalyzerProgressListener progressListener) {
        QueryParser queryParser = new QueryParser();
        List<String> cleanupQueries = new ArrayList<String>();
        int analyzed = 0;
        int succeeded = 0;
        int failed = 0;

        session.clearFailures();

        try (PlcsqlChecker plcsqlChecker = new PlcsqlChecker();
                Connection connection = session.getConfig().getTargetConParams().createConnection()) {
            for (AnalyzerStatement statement : executionPlan.getStatements()) {
                analyzed++;
                if (isUnsupportedStatement(statement)) {
                    failed++;
                    objectProgressTracker.record(statement, false);
                    objectUnsupportedFailure(
                            session, progressListener,
                            objectProgressTracker.snapshot(analyzed, succeeded, failed),
                            statement,
                            AnalyzerUnsupportedStatementPolicy.getUnsupportedReason(statement));
                    continue;
                }
                try {
                    if (isPlcsqlStatement(statement)) {
                        PlcsqlCheckResult plcsqlCheckResult =
                                plcsqlChecker.checkSQLAndGetStaticSqls(statement.getSQL());
                        succeeded++;
                        objectProgressTracker.record(statement, true);
                        recordParsedStatement(session, progressListener, statement,
                                objectProgressTracker.snapshot(analyzed, succeeded, failed));
                        AnalysisCounters counters =
                                analyzeStaticSqls(
                                        queryParser,
                                        session,
                                        progressListener,
                                        objectProgressTracker,
                                        statement,
                                        plcsqlCheckResult,
                                        analyzed,
                                        succeeded,
                                        failed);
                        analyzed = counters.analyzed;
                        succeeded = counters.succeeded;
                        failed = counters.failed;
                        continue;
                    }

                    String resultSummary = executeJdbcStatement(connection, statement);
                    succeeded++;
                    objectProgressTracker.record(statement, true);
                    session.getReport()
                            .addStatementResult(
                                    statement.getType(),
                                    statement.getId(),
                                    statement.getObjectName(),
                                    statement.getSQL(),
                                    true,
                                    resultSummary,
                                    null);
                    notifyProgress(
                            progressListener,
                            AnalyzerProgressEventViewModel.statementSucceeded(
                                    statement,
                                    resultSummary,
                                    objectProgressTracker.snapshot(analyzed, succeeded, failed)));

                    String cleanupQuery = buildCleanupQuery(session, statement);
                    if (cleanupQuery != null) {
                        cleanupQueries.add(cleanupQuery);
                    }
                } catch (SQLParserException ex) {
                    failed++;
                    objectProgressTracker.record(statement, false);
                    LOG.warn(
                            "PL/CSQL parser analysis failed for JDBC statement. statementType={}, statementId={}, reason={}",
                            statement.getType(),
                            statement.getId(),
                            ex.getMessage(),
                            ex);
                    recordFailedStatement(
                            session,
                            progressListener,
                            statement,
                            ex.getMessage(),
                            AnalyzerFailureStage.PARSER,
                            objectProgressTracker.snapshot(analyzed, succeeded, failed));
                } catch (Exception ex) {
                    failed++;
                    objectProgressTracker.record(statement, false);
                    LOG.warn(
                            "JDBC analysis failed for statement. statementType={}, statementId={}",
                            statement.getType(),
                            statement.getId(),
                            ex);
                    recordFailedStatement(
                            session,
                            progressListener,
                            statement,
                            ex.toString(),
                            AnalyzerFailureStage.JDBC,
                            objectProgressTracker.snapshot(analyzed, succeeded, failed));
                }
            }
            if (!cleanupQueries.isEmpty()) {
                runJdbcCleanup(
                        connection,
                        cleanupQueries,
                        session,
                        progressListener,
                        objectProgressTracker.snapshot(analyzed, succeeded, failed));
            }
        } catch (Exception ex) {
            throw new RuntimeException("JDBC execution failed to start: " + ex.getMessage(), ex);
        }

        session.setAnalyzedStatementCount(analyzed);
        session.setSucceededStatementCount(succeeded);
        session.setFailedStatementCount(failed);
        costCalculator.analyzeAfterExecution(session.getReport());

        LOG.info(
                "JDBC analysis completed. total={}, succeeded={}, failed={}",
                analyzed,
                succeeded,
                failed);
        notifyAnalysisCompleted(progressListener,
                objectProgressTracker.snapshot(analyzed, succeeded, failed));
    }

    private void recordParsedStatement(
            AnalyzerSession session,
            AnalyzerProgressListener progressListener,
            AnalyzerStatement statement,
            AnalyzerProgressCounts counts) {
        session.getReport()
                .addStatementResult(
                        statement.getType(),
                        statement.getId(),
                        statement.getObjectName(),
                        statement.getSQL(),
                        true,
                        "parsed",
                        null);
        notifyProgress(
                progressListener,
                AnalyzerProgressEventViewModel.statementSucceeded(statement, "parsed", counts));
    }

    private void recordFailedStatement(
            AnalyzerSession session,
            AnalyzerProgressListener progressListener,
            AnalyzerStatement statement,
            String reason,
            AnalyzerFailureStage stage,
            AnalyzerProgressCounts counts) {
        session.addFailureMessage(buildFailureMessage(statement.getType(), statement.getId(), reason));
        session.addFailure(buildFailure(statement, reason, stage));
        session.getReport()
                .addStatementResult(
                        statement.getType(),
                        statement.getId(),
                        statement.getObjectName(),
                        statement.getSQL(),
                        false,
                        reason,
                        stage);
        notifyProgress(
                progressListener,
                AnalyzerProgressEventViewModel.statementFailed(statement, reason, stage, counts));
    }

    private AnalysisCounters analyzeStaticSqls(
            QueryParser queryParser,
            AnalyzerSession session,
            AnalyzerProgressListener progressListener,
            ObjectProgressTracker objectProgressTracker,
            AnalyzerStatement parentStatement,
            PlcsqlCheckResult plcsqlCheckResult,
            int analyzed,
            int succeeded,
            int failed) {
        if (plcsqlCheckResult == null || plcsqlCheckResult.getStaticSqls().isEmpty()) {
            return new AnalysisCounters(analyzed, succeeded, failed);
        }

        int staticSqlIndex = 0;
        for (StaticSql staticSql : plcsqlCheckResult.getStaticSqls()) {
            if (staticSql == null || staticSql.getCode() == null || staticSql.getCode().isBlank()) {
                continue;
            }

            AnalyzerStatement staticStatement =
                    buildStaticSqlStatement(parentStatement, staticSql, ++staticSqlIndex);
            objectProgressTracker.addDiscovered(staticStatement);
            analyzed++;
            try {
                queryParser.checkSQL(staticStatement.getSQL());
                succeeded++;
                objectProgressTracker.record(staticStatement, true);
                session.getReport()
                        .addStatementResult(
                                staticStatement.getType(),
                                staticStatement.getId(),
                                staticStatement.getObjectName(),
                                staticStatement.getSQL(),
                                true,
                                "parsed",
                                null);
                notifyProgress(
                        progressListener,
                        AnalyzerProgressEventViewModel.statementSucceeded(
                                staticStatement,
                                "parsed",
                                objectProgressTracker.snapshot(analyzed, succeeded, failed)));
            } catch (SQLParserException ex) {
                failed++;
                objectProgressTracker.record(staticStatement, false);
                LOG.warn(
                        "Static SQL parser analysis failed. parentType={}, parentId={}, staticId={}, reason={}",
                        parentStatement.getType(),
                        parentStatement.getId(),
                        staticStatement.getId(),
                        ex.getMessage(),
                        ex);
                recordFailedStatement(
                        session,
                        progressListener,
                        staticStatement,
                        ex.getMessage(),
                        AnalyzerFailureStage.PARSER,
                        objectProgressTracker.snapshot(analyzed, succeeded, failed));
            } catch (Exception ex) {
                failed++;
                objectProgressTracker.record(staticStatement, false);
                LOG.warn(
                        "Unexpected static SQL parser analysis exception. parentType={}, parentId={}, staticId={}",
                        parentStatement.getType(),
                        parentStatement.getId(),
                        staticStatement.getId(),
                        ex);
                recordFailedStatement(
                        session,
                        progressListener,
                        staticStatement,
                        ex.toString(),
                        AnalyzerFailureStage.PARSER,
                        objectProgressTracker.snapshot(analyzed, succeeded, failed));
            }
        }

        return new AnalysisCounters(analyzed, succeeded, failed);
    }

    private AnalyzerStatement buildStaticSqlStatement(
            AnalyzerStatement parentStatement,
            StaticSql staticSql,
            int staticSqlIndex) {
        String parentId = parentStatement.getId();
        if (parentId == null || parentId.isBlank()) {
            parentId = "PLCSQL";
        }

        StringBuilder id = new StringBuilder(parentId)
                .append("_STATIC_")
                .append(staticSqlIndex);
        if (staticSql.getRow() > 0) {
            id.append("_L").append(staticSql.getRow());
        }
        if (staticSql.getColumn() > 0) {
            id.append("_C").append(staticSql.getColumn());
        }

        return new AnalyzerStatement(
                inferStaticSqlType(staticSql.getCode()),
                id.toString(),
                staticSql.getCode(),
                staticSqlObjectName(parentStatement, staticSqlIndex));
    }

    private String staticSqlObjectName(AnalyzerStatement parentStatement, int staticSqlIndex) {
        String parentObjectName = parentStatement.getObjectName();
        if (parentObjectName == null || parentObjectName.isBlank()) {
            return "";
        }
        return parentObjectName + " / static SQL #" + staticSqlIndex;
    }

    private String inferStaticSqlType(String sql) {
        String normalizedSql = stripLeadingSqlComments(sql).stripLeading().toUpperCase(Locale.ENGLISH);
        if (startsWithKeyword(normalizedSql, "SELECT")) {
            return "SELECT";
        }
        if (startsWithKeyword(normalizedSql, "INSERT")) {
            return "INSERT";
        }
        if (startsWithKeyword(normalizedSql, "UPDATE")) {
            return "UPDATE";
        }
        if (startsWithKeyword(normalizedSql, "DELETE")) {
            return "DELETE";
        }
        return AnalyzerStatementTypes.TYPE_STATIC_SQL;
    }

    private String stripLeadingSqlComments(String sql) {
        String remaining = sql == null ? "" : sql;
        while (true) {
            remaining = remaining.stripLeading();
            if (remaining.startsWith("--")) {
                int lineEnd = remaining.indexOf('\n');
                if (lineEnd < 0) {
                    return "";
                }
                remaining = remaining.substring(lineEnd + 1);
                continue;
            }
            if (remaining.startsWith("/*")) {
                int commentEnd = remaining.indexOf("*/");
                if (commentEnd < 0) {
                    return "";
                }
                remaining = remaining.substring(commentEnd + 2);
                continue;
            }
            return remaining;
        }
    }

    private boolean startsWithKeyword(String sql, String keyword) {
        if (!sql.startsWith(keyword)) {
            return false;
        }
        if (sql.length() == keyword.length()) {
            return true;
        }

        char nextChar = sql.charAt(keyword.length());
        return !Character.isLetterOrDigit(nextChar) && nextChar != '_';
    }

    private String executeJdbcStatement(Connection connection, AnalyzerStatement statement)
            throws SQLException {
        try (Statement jdbcStatement = connection.createStatement()) {
            boolean hasResultSet = jdbcStatement.execute(statement.getSQL());

            if (hasResultSet) {
                try (ResultSet resultSet = jdbcStatement.getResultSet()) {
                    int rowCount = 0;
                    while (resultSet.next()) {
                        rowCount++;
                    }
                    return "rows=" + rowCount;
                }
            }

            int updateCount = jdbcStatement.getUpdateCount();
            if (shouldCommit(statement)) {
                connection.commit();
            }

            if (isDDL(statement)) {
                return "ddl executed";
            }

            return "updated=" + updateCount;
        }
    }

    private void runJdbcCleanup(
            Connection connection,
            List<String> cleanupQueries,
            AnalyzerSession session,
            AnalyzerProgressListener progressListener,
            AnalyzerProgressCounts counts) {
        for (int i = cleanupQueries.size() - 1; i >= 0; i--) {
            String cleanupQuery = cleanupQueries.get(i);
            String cleanupId = "CLEANUP_" + (cleanupQueries.size() - i);
            try (Statement statement = connection.createStatement()) {
                statement.execute(cleanupQuery);
                connection.commit();
                session.getReport()
                        .addStatementResult(
                                "CLEANUP",
                                cleanupId,
                                cleanupQuery,
                                true,
                                "cleanup executed",
                                null);
                notifyProgress(
                        progressListener,
                        AnalyzerProgressEventViewModel.cleanupSucceeded(cleanupId, cleanupQuery, counts));
            } catch (Exception ex) {
                String failureMessage = "CLEANUP : " + cleanupQuery + " : " + ex.toString();
                LOG.warn("JDBC cleanup failed. cleanupId={}", cleanupId, ex);
                session.addFailureMessage(failureMessage);
                session.addFailure(
                        buildFailure(
                                "CLEANUP",
                                cleanupId,
                                cleanupQuery,
                                ex.toString(),
                                AnalyzerFailureStage.CLEANUP));
                session.getReport()
                        .addStatementResult(
                                "CLEANUP",
                                cleanupId,
                                cleanupQuery,
                                false,
                                ex.toString(),
                                AnalyzerFailureStage.CLEANUP);
                notifyProgress(
                        progressListener,
                        AnalyzerProgressEventViewModel.cleanupFailed(
                                "[CLEANUP FAIL] " + failureMessage,
                                cleanupId,
                                cleanupQuery,
                                ex.toString(),
                                counts));
            }
        }
    }

    private AnalyzerFailure buildFailure(
            AnalyzerStatement statement, String reason, AnalyzerFailureStage stage) {
        return buildFailure(
                statement.getType(),
                statement.getId(),
                statement.getObjectName(),
                statement.getSQL(),
                reason,
                stage);
    }

    private AnalyzerFailure buildFailure(
            String statementType,
            String statementId,
            String sql,
            String reason,
            AnalyzerFailureStage stage) {
        return buildFailure(statementType, statementId, null, sql, reason, stage);
    }

    private AnalyzerFailure buildFailure(
            String statementType,
            String statementId,
            String objectName,
            String sql,
            String reason,
            AnalyzerFailureStage stage) {
        AnalyzerFailure failure = new AnalyzerFailure();
        failure.setFailureStage(stage);
        failure.setStatementType(statementType);
        failure.setStatementId(statementId);
        failure.setObjectName(objectName);
        failure.setSql(sql);
        failure.setReason(reason);
        return failure;
    }

    private String buildFailureMessage(String type, String id, String reason) {
        return type + " " + id + " : " + reason;
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

    private String buildCleanupQuery(AnalyzerSession session, AnalyzerStatement statement) {
        if (!isDDL(statement)) {
            return null;
        }

        AnalyzerConfiguration config = session.getConfig();
        CUBRIDSQLHelper helper = CUBRIDSQLHelper.getInstance(null);

        if (AnalyzerStatementTypes.TYPE_DDL_TABLE.equals(statement.getType())) {
            Table table = getStatementObject(
                    config.getTargetTableSchema(), statement.getId(), "TABLE_", "table");
            return "DROP TABLE "
                    + helper.getOwnerNameWithDot(table.getOwner(), config.isAddUserSchema())
                    + helper.getQuotedObjName(table.getName())
                    + ";";
        }

        if (AnalyzerStatementTypes.TYPE_DDL_VIEW.equals(statement.getType())
                || AnalyzerStatementTypes.TYPE_DDL_VIEW_CREATE.equals(statement.getType())) {
            View view = getStatementObject(
                    config.getTargetViewSchema(), statement.getId(), "VIEW_", "view");
            return "DROP VIEW "
                    + helper.getOwnerNameWithDot(view.getOwner(), config.isAddUserSchema())
                    + helper.getQuotedObjName(view.getName())
                    + ";";
        }

        if (AnalyzerStatementTypes.TYPE_DDL_SEQUENCE.equals(statement.getType())) {
            Sequence sequence = getStatementObject(
                    config.getTargetSerialSchema(), statement.getId(), "SEQ_", "sequence");
            return "DROP SERIAL "
                    + helper.getOwnerNameWithDot(sequence.getOwner(), config.isAddUserSchema())
                    + helper.getQuotedObjName(sequence.getName())
                    + ";";
        }

        if (AnalyzerStatementTypes.TYPE_DDL_SYNONYM.equals(statement.getType())) {
            Synonym synonym = getStatementObject(
                    config.getTargetSynonymSchema(), statement.getId(), "SYNONYM_", "synonym");
            return "DROP SYNONYM "
                    + helper.getOwnerNameWithDot(synonym.getOwner(), config.isAddUserSchema())
                    + helper.getQuotedObjName(synonym.getName())
                    + ";";
        }

        if (AnalyzerStatementTypes.TYPE_DDL_GRANT.equals(statement.getType())) {
            SourceGrantConfig grant = getStatementObject(
                    config.getExpGrantCfg(), statement.getId(), "GRANT_", "grant");
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
            PlcsqlProcedure procedure = getStatementObject(
                    config.getTargetPlcsqlProcedureSchema(), statement.getId(), "PROC_", "procedure");
            return helper.getPlcsqlProcedureDropDDL(procedure, config.isAddUserSchema());
        }

        if (AnalyzerStatementTypes.TYPE_DDL_FUNC_HEADER.equals(statement.getType())) {
            PlcsqlFunction function = getStatementObject(
                    config.getTargetPlcsqlFunctionSchema(), statement.getId(), "FUNC_", "function");
            return helper.getPlcsqlFunctionDropDDL(function, config.isAddUserSchema());
        }

        // Remaining DDL types do not require an independent cleanup query.
        return null;
    }

    private boolean isUnsupportedStatement(AnalyzerStatement statement) {
        return AnalyzerUnsupportedStatementPolicy.getUnsupportedReason(statement) != null;
    }

    private boolean isPlcsqlStatement(AnalyzerStatement statement) {
        return AnalyzerStatementTypes.TYPE_DDL_PROC_HEADER.equals(statement.getType())
                || AnalyzerStatementTypes.TYPE_DDL_PROC_BODY.equals(statement.getType())
                || AnalyzerStatementTypes.TYPE_DDL_FUNC_HEADER.equals(statement.getType())
                || AnalyzerStatementTypes.TYPE_DDL_FUNC_BODY.equals(statement.getType());
    }

    private void objectUnsupportedFailure(
            AnalyzerSession session,
            AnalyzerProgressListener progressListener,
            AnalyzerProgressCounts counts,
            AnalyzerStatement statement,
            String reason) {
        String failureMessage = buildFailureMessage(
                statement.getType(), statement.getId(), reason);
        LOG.warn(
                "Analysis skipped unsupported statement. statementType={}, statementId={}, reason={}",
                statement.getType(),
                statement.getId(),
                reason);
        session.addFailureMessage(failureMessage);
        session.addFailure(
                buildFailure(
                        statement,
                        reason,
                        AnalyzerFailureStage.UNSUPPORTED));
        session.getReport()
                .addStatementResult(
                        statement.getType(),
                        statement.getId(),
                        statement.getObjectName(),
                        statement.getSQL(),
                        false,
                        reason,
                        AnalyzerFailureStage.UNSUPPORTED);
        notifyProgress(
                progressListener,
                AnalyzerProgressEventViewModel.statementFailed(
                        statement,
                        reason,
                        AnalyzerFailureStage.UNSUPPORTED,
                        counts));
    }

    private <T> T getStatementObject(List<T> objects, String id, String prefix, String objectType) {
        int index = parseStatementIndex(id, prefix);
        int size = objects == null ? 0 : objects.size();
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException(
                    "Unexpected "
                            + objectType
                            + " statement id: "
                            + id
                            + " (index="
                            + index
                            + ", size="
                            + size
                            + ")");
        }
        return objects.get(index);
    }

    private int parseStatementIndex(String id, String prefix) {
        if (id == null || !id.startsWith(prefix)) {
            throw new IllegalArgumentException("Unexpected statement id: " + id);
        }

        try {
            return Integer.parseInt(id.substring(prefix.length())) - 1;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Unexpected statement id: " + id, ex);
        }
    }

    private void notifyAnalysisCompleted(
            AnalyzerProgressListener progressListener, AnalyzerProgressCounts counts) {
        notifyProgress(progressListener, AnalyzerProgressEventViewModel.completed(counts));
    }

    private void notifyProgress(AnalyzerProgressListener progressListener, AnalyzerProgressEventViewModel event) {
        LOG.info(
                "Analysis progress. stage={}, total={}, completed={}, succeeded={}, failed={}, statementType={}, statementId={}, message={}",
                event.stage(),
                event.totalCount(),
                event.completedCount(),
                event.succeededCount(),
                event.failedCount(),
                event.statementType(),
                event.statementId(),
                event.message());
        if (progressListener != null) {
            progressListener.onProgress(event);
        }
    }

    private static class ObjectProgressTracker {
        private int totalCount;
        private final Map<String, MutableObjectProgressCount> objectCounts =
                new LinkedHashMap<String, MutableObjectProgressCount>();

        private ObjectProgressTracker(int totalCount) {
            this.totalCount = totalCount;
        }

        static ObjectProgressTracker from(AnalyzerExecutionPlan executionPlan) {
            ObjectProgressTracker tracker =
                    new ObjectProgressTracker(executionPlan.getStatements().size());
            for (AnalyzerStatement statement : executionPlan.getStatements()) {
                tracker.getOrCreate(displayObjectType(statement)).totalCount++;
            }
            return tracker;
        }

        void addDiscovered(AnalyzerStatement statement) {
            totalCount++;
            getOrCreate(displayObjectType(statement)).totalCount++;
        }

        void record(AnalyzerStatement statement, boolean success) {
            MutableObjectProgressCount count = getOrCreate(displayObjectType(statement));
            if (success) {
                count.succeededCount++;
                return;
            }
            count.failedCount++;
        }

        AnalyzerProgressCounts snapshot(int completedCount, int succeededCount, int failedCount) {
            List<AnalyzerProgressObjectCount> snapshots =
                    new ArrayList<AnalyzerProgressObjectCount>();
            for (Map.Entry<String, MutableObjectProgressCount> entry : objectCounts.entrySet()) {
                MutableObjectProgressCount count = entry.getValue();
                snapshots.add(
                        new AnalyzerProgressObjectCount(
                                entry.getKey(),
                                count.totalCount,
                                count.succeededCount,
                                count.failedCount));
            }
            return new AnalyzerProgressCounts(
                    totalCount,
                    completedCount,
                    succeededCount,
                    failedCount,
                    snapshots);
        }

        private MutableObjectProgressCount getOrCreate(String objectType) {
            MutableObjectProgressCount count = objectCounts.get(objectType);
            if (count == null) {
                count = new MutableObjectProgressCount();
                objectCounts.put(objectType, count);
            }
            return count;
        }

        private static String displayObjectType(AnalyzerStatement statement) {
            if (statement == null || statement.getType() == null || statement.getType().isEmpty()) {
                return "UNKNOWN";
            }

            String type = statement.getType();
            if (type.startsWith("DDL_")) {
                return type.substring("DDL_".length());
            }
            return type;
        }
    }

    private static class MutableObjectProgressCount {
        private int totalCount;
        private int succeededCount;
        private int failedCount;
    }

    private static class AnalysisCounters {
        private final int analyzed;
        private final int succeeded;
        private final int failed;

        private AnalysisCounters(int analyzed, int succeeded, int failed) {
            this.analyzed = analyzed;
            this.succeeded = succeeded;
            this.failed = failed;
        }
    }
}
