package com.cubrid.sqlanalyzer.command.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.cubrid.cubridmigration.core.dbobject.PlcsqlFunction;
import com.cubrid.cubridmigration.core.dbobject.PlcsqlProcedure;
import com.cubrid.cubridmigration.core.dbobject.Sequence;
import com.cubrid.cubridmigration.core.dbobject.Synonym;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbobject.View;
import com.cubrid.cubridmigration.core.engine.config.SourceGrantConfig;
import com.cubrid.cubridmigration.cubrid.CUBRIDSQLHelper;
import com.cubrid.sqlanalyzer.command.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.AnalyzerFailure;
import com.cubrid.sqlanalyzer.command.AnalyzerFailureStage;
import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressEventViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressStage;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.cost.AnalyzerCostCalculator;
import com.cubrid.sqlanalyzer.core.cost.FailureCostCalculator;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPlan;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatement;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes;
import com.cubrid.sqlanalyzer.core.plan.CatalogDDLPlanBuilder;
import com.cubrid.sqlanalyzer.core.plan.QueryDictionaryPlanBuilder;
import com.cubrid.sqlanalyzer.core.runner.QueryParser;
import com.cubrid.sqlanalyzer.core.runner.SQLParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyzerExecutionRunner {
    private static final Logger LOG = LoggerFactory.getLogger(AnalyzerExecutionRunner.class);
    private static final String TRIGGER_UNSUPPORTED_REASON = "Trigger migration is not supported.";

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
        LOG.info("Analysis execution plan built. statementCount={}", totalCount);
        notifyProgress(
                progressListener,
                new AnalyzerProgressEventViewModel(
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
        costCalculator.analyzeBeforeExecution(executionPlan, session.getReport());
        if (executionPlan.isEmpty()) {
            LOG.info("Analysis execution ended without statements.");
            session.setAnalyzedStatementCount(0);
            session.setSucceededStatementCount(0);
            session.setFailedStatementCount(0);
            session.clearFailures();
            notifyProgress(
                    progressListener,
                    new AnalyzerProgressEventViewModel(
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
            LOG.info("Running parser analysis.");
            runParserAnalysis(session, executionPlan, progressListener);
            return;
        }

        if (session.getTargetType() == AnalyzerTargetType.JDBC) {
            LOG.info("Running JDBC analysis.");
            runJdbcAnalysis(session, executionPlan, progressListener);
            return;
        }

        throw new IllegalStateException("Unsupported target type: " + session.getTargetType());
    }

    private AnalyzerExecutionPlan buildExecutionPlan(AnalyzerSession session) {
        LOG.info(
                "Building execution plan. sourceType={}, executionMode={}",
                session.getSourceType(),
                session.getExecutionMode());
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
            AnalyzerSession session,
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
            if (isUnsupportedStatement(statement)) {
                failed++;
                objectUnsupportedFailure(
                        session, progressListener, totalCount, analyzed, succeeded, failed,
                        statement);
                continue;
            }
            try {
                queryParser.checkSQL(statement.getSQL());
                succeeded++;
                session.getReport()
                        .addStatementResult(
                                statement.getType(),
                                statement.getId(),
                                statement.getSQL(),
                                true,
                                "parsed",
                                null);
                notifyProgress(
                        progressListener,
                        new AnalyzerProgressEventViewModel(
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
                                statement.getSQL(),
                                false,
                                ex.getMessage(),
                                AnalyzerFailureStage.PARSER);
                notifyProgress(
                        progressListener,
                        new AnalyzerProgressEventViewModel(
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
                                statement.getSQL(),
                                false,
                                ex.toString(),
                                AnalyzerFailureStage.PARSER);
                notifyProgress(
                        progressListener,
                        new AnalyzerProgressEventViewModel(
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
        costCalculator.analyzeAfterExecution(session.getReport());

        LOG.info(
                "Parser analysis completed. total={}, succeeded={}, failed={}",
                analyzed,
                succeeded,
                failed);
        notifyAnalysisCompleted(progressListener, analyzed, succeeded, failed);
    }

    private void runJdbcAnalysis(
            AnalyzerSession session,
            AnalyzerExecutionPlan executionPlan,
            AnalyzerProgressListener progressListener) {
        List<String> cleanupQueries = new ArrayList<String>();
        int totalCount = executionPlan.getStatements().size();
        int analyzed = 0;
        int succeeded = 0;
        int failed = 0;

        session.clearFailures();

        try (Connection connection = session.getConfig().getTargetConParams().createConnection()) {
            for (AnalyzerStatement statement : executionPlan.getStatements()) {
                analyzed++;
                if (isUnsupportedStatement(statement)) {
                    failed++;
                    objectUnsupportedFailure(
                            session, progressListener, totalCount, analyzed, succeeded, failed,
                            statement);
                    continue;
                }
                try {
                    String resultSummary = executeJdbcStatement(connection, statement);
                    succeeded++;
                    session.getReport()
                            .addStatementResult(
                                    statement.getType(),
                                    statement.getId(),
                                    statement.getSQL(),
                                    true,
                                    resultSummary,
                                    null);
                    notifyProgress(
                            progressListener,
                            new AnalyzerProgressEventViewModel(
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
                    LOG.warn(
                            "JDBC analysis failed for statement. statementType={}, statementId={}",
                            statement.getType(),
                            statement.getId(),
                            ex);
                    session.addFailureMessage(failureMessage);
                    session.addFailure(
                            buildFailure(statement, ex.toString(), AnalyzerFailureStage.JDBC));
                    session.getReport()
                            .addStatementResult(
                                    statement.getType(),
                                    statement.getId(),
                                    statement.getSQL(),
                                    false,
                                    ex.toString(),
                                    AnalyzerFailureStage.JDBC);
                    notifyProgress(
                            progressListener,
                            new AnalyzerProgressEventViewModel(
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
        notifyAnalysisCompleted(progressListener, analyzed, succeeded, failed);
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
            int totalCount,
            int completedCount,
            int succeededCount,
            int failedCount) {
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
                        new AnalyzerProgressEventViewModel(
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
                        new AnalyzerProgressEventViewModel(
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
            }
        }
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

    private boolean isUnsupportedStatement(AnalyzerStatement statement) {
        return AnalyzerStatementTypes.TYPE_DDL_TRIGGER.equals(statement.getType());
    }

    private void objectUnsupportedFailure(
            AnalyzerSession session,
            AnalyzerProgressListener progressListener,
            int totalCount,
            int analyzed,
            int succeeded,
            int failed,
            AnalyzerStatement statement) {
        String failureMessage = buildFailureMessage(
                statement.getType(), statement.getId(), TRIGGER_UNSUPPORTED_REASON);
        LOG.warn(
                "Analysis skipped unsupported statement. statementType={}, statementId={}, reason={}",
                statement.getType(),
                statement.getId(),
                TRIGGER_UNSUPPORTED_REASON);
        session.addFailureMessage(failureMessage);
        session.addFailure(
                buildFailure(
                        statement,
                        TRIGGER_UNSUPPORTED_REASON,
                        AnalyzerFailureStage.UNSUPPORTED));
        session.getReport()
                .addStatementResult(
                        statement.getType(),
                        statement.getId(),
                        statement.getSQL(),
                        false,
                        TRIGGER_UNSUPPORTED_REASON,
                        AnalyzerFailureStage.UNSUPPORTED);
        notifyProgress(
                progressListener,
                new AnalyzerProgressEventViewModel(
                        AnalyzerProgressStage.STATEMENT_FAILED,
                        "[FAIL] " + failureMessage,
                        statement.getType(),
                        statement.getId(),
                        statement.getSQL(),
                        TRIGGER_UNSUPPORTED_REASON,
                        AnalyzerFailureStage.UNSUPPORTED,
                        totalCount,
                        analyzed,
                        succeeded,
                        failed));
    }

    private int parseStatementIndex(String id, String prefix) {
        if (id == null || !id.startsWith(prefix)) {
            throw new IllegalArgumentException("Unexpected statement id: " + id);
        }

        return Integer.parseInt(id.substring(prefix.length())) - 1;
    }

    private void notifyAnalysisCompleted(
            AnalyzerProgressListener progressListener, int analyzed, int succeeded, int failed) {
        notifyProgress(
                progressListener,
                new AnalyzerProgressEventViewModel(
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
}
