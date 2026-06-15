package com.cubrid.sqlanalyzer.core.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class AnalyzerExecutionPlanTest {
    @Test
    void shouldKeepPhaseOrderRegardlessOfObjectCountAndInsertionOrder() {
        AnalyzerExecutionPlan plan = new AnalyzerExecutionPlan();

        plan.add(
                AnalyzerExecutionPhase.DDL_PRIMARY_KEY,
                new AnalyzerStatement(AnalyzerStatementTypes.TYPE_DDL_PK, "PK_1", "pk"));

        for (int i = 1; i <= 1500; i++) {
            plan.add(
                    AnalyzerExecutionPhase.DDL_TABLE,
                    new AnalyzerStatement(
                            AnalyzerStatementTypes.TYPE_DDL_TABLE,
                            "TABLE_" + i,
                            "table " + i));
        }

        plan.add(
                AnalyzerExecutionPhase.DDL_FOREIGN_KEY,
                new AnalyzerStatement(AnalyzerStatementTypes.TYPE_DDL_FK, "FK_1", "fk"));

        List<AnalyzerStatement> statements = plan.getStatements();

        assertEquals(1502, statements.size());
        assertEquals("TABLE_1", statements.get(0).getId());
        assertEquals("TABLE_1500", statements.get(1499).getId());
        assertEquals("PK_1", statements.get(1500).getId());
        assertEquals("FK_1", statements.get(1501).getId());
    }

    @Test
    void shouldPreserveInsertionOrderWithinPhaseAndRefreshCachedView() {
        AnalyzerExecutionPlan plan = new AnalyzerExecutionPlan();

        plan.add(
                AnalyzerExecutionPhase.DML_SELECT,
                new AnalyzerStatement("SELECT", "SELECT_1", "select 1"));

        assertEquals(List.of("SELECT_1"), statementIds(plan.getStatements()));

        plan.add(
                AnalyzerExecutionPhase.DML_SELECT,
                new AnalyzerStatement("SELECT", "SELECT_2", "select 2"));

        assertEquals(
                List.of("SELECT_1", "SELECT_2"),
                statementIds(plan.getStatements()));
    }

    private List<String> statementIds(List<AnalyzerStatement> statements) {
        return statements.stream()
                .map(AnalyzerStatement::getId)
                .toList();
    }
}
