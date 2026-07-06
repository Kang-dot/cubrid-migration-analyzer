package com.cubrid.sqlanalyzer.command.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.command.model.AnalyzerFailureStage;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressEventViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressStage;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;

/**
 * Exercises the JDBC-target branch of {@link AnalyzerExecutionRunner} through the
 * {@link AnalyzerJdbcExecutor} seam, so commit/DDL/failure-stage decisions are
 * verified without a real CUBRID connection or CMT's {@code ConnParameters}.
 */
class AnalyzerExecutionRunnerJdbcTest {
    @Test
    void shouldCommitForDmlStatementsButNotForSelect() {
        ScriptedJdbcExecutor executor = new ScriptedJdbcExecutor();
        executor.whenExecuting("SELECT * FROM emp", new AnalyzerJdbcExecutionResult(true, 3, 0));
        executor.whenExecuting("INSERT INTO emp VALUES (1)", new AnalyzerJdbcExecutionResult(false, 0, 1));
        AnalyzerExecutionRunner executionRunner = new AnalyzerExecutionRunner(config -> executor);

        AnalyzerSession session = new AnalyzerSession();
        session.setTargetType(AnalyzerTargetType.JDBC);
        session.setXmlSourceLoaded(true);
        QueryDictionary queryDictionary = new QueryDictionary();
        queryDictionary.addSelectQuery("Q_SELECT", "SELECT * FROM emp");
        queryDictionary.addInsertQuery("Q_INSERT", "INSERT INTO emp VALUES (1)");
        session.getConfig().setQueryDict(queryDictionary);

        List<AnalyzerProgressEventViewModel> events = new ArrayList<>();
        executionRunner.run(session, events::add);

        assertEquals(2, session.getSucceededStatementCount());
        assertEquals(0, session.getFailedStatementCount());
        assertEquals(1, executor.commitCount());
        assertTrue(executor.isClosed());
        assertTrue(events.stream().anyMatch(e ->
                "Q_SELECT".equals(e.statementId()) && "rows=3".equals(e.detail())));
        assertTrue(events.stream().anyMatch(e ->
                "Q_INSERT".equals(e.statementId()) && "updated=1".equals(e.detail())));
    }

    @Test
    void shouldTagJdbcFailureStageWhenExecutionThrows() {
        ScriptedJdbcExecutor executor = new ScriptedJdbcExecutor();
        executor.whenExecutingThrow(
                "INSERT INTO emp VALUES (1)", new SQLException("constraint violation"));
        AnalyzerExecutionRunner executionRunner = new AnalyzerExecutionRunner(config -> executor);

        AnalyzerSession session = new AnalyzerSession();
        session.setTargetType(AnalyzerTargetType.JDBC);
        session.setXmlSourceLoaded(true);
        QueryDictionary queryDictionary = new QueryDictionary();
        queryDictionary.addInsertQuery("Q_INSERT", "INSERT INTO emp VALUES (1)");
        session.getConfig().setQueryDict(queryDictionary);

        List<AnalyzerProgressEventViewModel> events = new ArrayList<>();
        executionRunner.run(session, events::add);

        assertEquals(1, session.getFailedStatementCount());
        assertEquals(0, executor.commitCount());
        assertEquals(1, session.getReport().getFailures().size());
        assertEquals(AnalyzerFailureStage.JDBC, session.getReport().getFailures().get(0).getFailureStage());
        assertTrue(events.stream().anyMatch(e ->
                e.stage() == AnalyzerProgressStage.STATEMENT_FAILED
                        && "Q_INSERT".equals(e.statementId())
                        && e.failureStage() == AnalyzerFailureStage.JDBC));
    }
}
