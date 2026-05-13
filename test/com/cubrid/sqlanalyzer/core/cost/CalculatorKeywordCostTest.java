package com.cubrid.sqlanalyzer.core.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.command.AnalyzerConsoleCostDetail;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleFailure;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes;

class CalculatorKeywordCostTest extends CostTestSupport {
        @Test
        @DisplayName("JOIN adds 0.5 cost for DML statements")
        void shouldAddJoinCostForSelectWithJoin() {
                float cost = estimateCost(
                                "SELECT",
                                "SELECT * FROM emp e\n"
                                                + "JOIN dept d\n"
                                                + "ON e.deptno = d.deptno");

                assertEquals(0.7f, cost, DELTA);
        }

        @Test
        @DisplayName("cost breakdown lists the contributing rules")
        void shouldRecordCostBreakdownDetails() {
                AnalyzerConsoleFailure failure = analyzeFailure(
                                "SELECT",
                                "SELECT * FROM emp e JOIN dept d ON e.deptno = d.deptno");

                assertEquals(0.7f, failure.getEstimatedCost(), DELTA);
                assertTrue(hasCostDetail(failure, "Base DML", 1, 0.2f, 0.2f));
                assertTrue(hasCostDetail(failure, "JOIN detected", 1, 0.5f, 0.5f));
        }

        @Test
        @DisplayName("subquery adds 0.5 cost for DML statements")
        void shouldAddSubqueryCostForSelectWithSubquery() {
                float cost = estimateCost(
                                "SELECT",
                                "SELECT * FROM emp WHERE deptno IN (\n"
                                                + "    SELECT deptno FROM dept\n"
                                                + ")");

                assertEquals(0.7f, cost, DELTA);
        }

        @Test
        @DisplayName("ON DELETE adds 0.5 cost for foreign keys")
        void shouldAddOnDeleteCostForForeignKey() {
                float cost = estimateCost(
                                AnalyzerStatementTypes.TYPE_DDL_FK,
                                "ALTER TABLE child ADD CONSTRAINT fk1 FOREIGN KEY (pid) "
                                                + "REFERENCES parent(id) ON\n"
                                                + "DELETE CASCADE");

                assertEquals(0.6f, cost, DELTA);
        }

        @Test
        @DisplayName("function-based indexes add 0.2 cost")
        void shouldAddFunctionBasedIndexCost() {
                float cost = estimateCost(
                                AnalyzerStatementTypes.TYPE_DDL_INDEX,
                                "CREATE INDEX idx1 ON t1 (UPPER(name))");

                assertEquals(0.3f, cost, DELTA);
        }

        @Test
        @DisplayName("REVERSE indexes add 1.0 cost")
        void shouldAddReverseIndexCost() {
                float cost = estimateCost(
                                AnalyzerStatementTypes.TYPE_DDL_INDEX,
                                "CREATE INDEX idx1 ON t1 (name) REVERSE");

                assertEquals(1.1f, cost, DELTA);
        }

        @Test
        @DisplayName("CHECK constraints add 0.1 cost per constraint")
        void shouldAddCheckConstraintCostForTableDdl() {
                float cost = estimateCost(
                                AnalyzerStatementTypes.TYPE_DDL_TABLE,
                                "CREATE TABLE t1 (\n"
                                                + "    age INT CHECK\n"
                                                + "    (age > 0)\n"
                                                + ")");

                // Current implementation also adds the TABLE keyword weight (+2.0).
                assertEquals(2.2f, cost, DELTA);
        }

        @Test
        @DisplayName("ENCRYPT adds 20.0 cost for table DDL")
        void shouldAddEncryptCostForTableDdl() {
                float cost = estimateCost(
                                AnalyzerStatementTypes.TYPE_DDL_TABLE,
                                "CREATE TABLE t1 (name VARCHAR(20) ENCRYPT)");

                // Current implementation also adds the TABLE keyword weight (+2.0).
                assertEquals(22.1f, cost, DELTA);
        }

        @Test
        @DisplayName("GRANT privilege count adds 0.1 cost per privilege")
        void shouldAddPrivilegeCostForGrantStatement() {
                float cost = estimateCost(
                                AnalyzerStatementTypes.TYPE_DDL_GRANT,
                                "GRANT SELECT, INSERT, UPDATE\n"
                                                + "ON t1\n"
                                                + "TO user1");

                assertEquals(0.4f, cost, DELTA);
        }

        private boolean hasCostDetail(
                        AnalyzerConsoleFailure failure,
                        String itemName,
                        int count,
                        float unitCost,
                        float totalCost) {
                for (AnalyzerConsoleCostDetail costDetail : failure.getCostDetails()) {
                        if (itemName.equals(costDetail.getItemName())
                                        && count == costDetail.getCount()
                                        && Math.abs(unitCost - costDetail.getUnitCost()) < DELTA
                                        && Math.abs(totalCost - costDetail.getTotalCost()) < DELTA) {
                                return true;
                        }
                }
                return false;
        }
}
