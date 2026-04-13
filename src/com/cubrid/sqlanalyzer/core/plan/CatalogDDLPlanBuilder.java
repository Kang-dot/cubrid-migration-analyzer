package com.cubrid.sqlanalyzer.core.plan;

import com.cubrid.cubridmigration.core.dbobject.FK;
import com.cubrid.cubridmigration.core.dbobject.Grant;
import com.cubrid.cubridmigration.core.dbobject.Index;
import com.cubrid.cubridmigration.core.dbobject.PK;
import com.cubrid.cubridmigration.core.dbobject.PlcsqlFunction;
import com.cubrid.cubridmigration.core.dbobject.PlcsqlProcedure;
import com.cubrid.cubridmigration.core.dbobject.Sequence;
import com.cubrid.cubridmigration.core.dbobject.Synonym;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbobject.View;
import com.cubrid.cubridmigration.core.engine.config.SourceGrantConfig;
import com.cubrid.cubridmigration.cubrid.CUBRIDSQLHelper;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;

public class CatalogDDLPlanBuilder implements AnalyzerExecutionPlanBuilder {
    private static final String TYPE_DDL_TABLE = "DDL_TABLE";
    private static final String TYPE_DDL_PK = "DDL_PK";
    private static final String TYPE_DDL_FK = "DDL_FK";
    private static final String TYPE_DDL_INDEX = "DDL_INDEX";
    private static final String TYPE_DDL_SEQUENCE = "DDL_SEQUENCE";
    private static final String TYPE_DDL_VIEW_CREATE = "DDL_VIEW_CREATE";
    private static final String TYPE_DDL_VIEW_ALTER = "DDL_VIEW_ALTER";
    private static final String TYPE_DDL_SYNONYM = "DDL_SYNONYM";
    private static final String TYPE_DDL_GRANT = "DDL_GRANT";
    private static final String TYPE_DDL_PROC_HEADER = "DDL_PROC_HEADER";
    private static final String TYPE_DDL_PROC_BODY = "DDL_PROC_BODY";
    private static final String TYPE_DDL_FUNC_HEADER = "DDL_FUNC_HEADER";
    private static final String TYPE_DDL_FUNC_BODY = "DDL_FUNC_BODY";

    @Override
    public AnalyzerExecutionPlan build(AnalyzerConfiguration config) {
        AnalyzerExecutionPlan plan = new AnalyzerExecutionPlan();
        CUBRIDSQLHelper helper = CUBRIDSQLHelper.getInstance(null);

        int seq = 0;
        for (Table table : config.getTargetTableSchema()) {
            String sql = helper.getTableDDL(table, config.isAddUserSchema());
            plan.add(new AnalyzerStatement(TYPE_DDL_TABLE, "TABLE_" + (++seq), sql, 1000 + seq));
        }

        seq = 0;
        for (Table table : config.getTargetTableSchema()) {
            PK pk = table.getPk();
            if (pk == null) {
                continue;
            }
            String sql =
                    helper.getPKDDL(
                            table.getOwner(),
                            table.getName(),
                            pk.getName(),
                            pk.getPkColumns(),
                            config.isAddUserSchema());
            plan.add(new AnalyzerStatement(TYPE_DDL_PK, "PK_" + (++seq), sql, 1100 + seq));
        }

        seq = 0;
        for (Table table : config.getTargetTableSchema()) {
            for (FK fk : table.getFks()) {
                String sql =
                        helper.getFKDDL(
                                table.getOwner(), table.getName(), fk, config.isAddUserSchema());
                plan.add(new AnalyzerStatement(TYPE_DDL_FK, "FK_" + (++seq), sql, 1200 + seq));
            }
        }

        seq = 0;
        for (Table table : config.getTargetTableSchema()) {
            for (Index index : table.getIndexes()) {
                String sql =
                        helper.getIndexDDL(
                                table.getOwner(),
                                table.getName(),
                                index,
                                "",
                                config.isAddUserSchema());
                plan.add(
                        new AnalyzerStatement(TYPE_DDL_INDEX, "INDEX_" + (++seq), sql, 1300 + seq));
            }
        }

        seq = 0;
        for (Sequence sequence : config.getTargetSerialSchema()) {
            String sql = helper.getSequenceDDL(sequence, config.isAddUserSchema());
            plan.add(
                    new AnalyzerStatement(TYPE_DDL_SEQUENCE, "SEQ_" + (++seq), sql, 1400 + seq));
        }

        seq = 0;
        for (View view : config.getTargetViewSchema()) {
            String sql = helper.getViewDDL(view, config.isAddUserSchema());
            plan.add(
                    new AnalyzerStatement(TYPE_DDL_VIEW_CREATE, "VIEW_" + (++seq), sql, 2000 + seq));
        }

        seq = 0;
        for (View view : config.getTargetViewSchema()) {
            String sql = helper.getViewAlterDDL(view, config.isAddUserSchema());
            if (CUBRIDSQLHelper.SQL_NULL.equals(sql)) {
                continue;
            }
            plan.add(
                    new AnalyzerStatement(
                            TYPE_DDL_VIEW_ALTER, "VIEW_ALTER_" + (++seq), sql, 2100 + seq));
        }

        seq = 0;
        for (Synonym synonym : config.getTargetSynonymSchema()) {
            String sql = helper.getSynonymDDL(synonym, config.isAddUserSchema());
            plan.add(
                    new AnalyzerStatement(
                            TYPE_DDL_SYNONYM, "SYNONYM_" + (++seq), sql, 3000 + seq));
        }

        seq = 0;
        for (SourceGrantConfig grantConfig : config.getExpGrantCfg()) {
            Grant grant = config.getTargetGrantSchema(grantConfig.getTarget());
            if (grant == null) {
                continue;
            }
            String sql = helper.getGrantDDL(grant, config.isAddUserSchema());
            plan.add(new AnalyzerStatement(TYPE_DDL_GRANT, "GRANT_" + (++seq), sql, 3100 + seq));
        }

        seq = 0;
        for (PlcsqlProcedure procedure : config.getTargetPlcsqlProcedureSchema()) {
            String sql = helper.getPlcsqlProcedureHeaderDDL(procedure, config.isAddUserSchema());
            plan.add(
                    new AnalyzerStatement(
                            TYPE_DDL_PROC_HEADER, "PROC_" + (++seq), sql, 4000 + seq));
        }

        seq = 0;
        for (PlcsqlProcedure procedure : config.getTargetPlcsqlProcedureSchema()) {
            String sql = helper.getPlcsqlProcedureDDL(procedure, config.isAddUserSchema());
            plan.add(
                    new AnalyzerStatement(
                            TYPE_DDL_PROC_BODY, "PROC_BODY_" + (++seq), sql, 4100 + seq));
        }

        seq = 0;
        for (PlcsqlFunction function : config.getTargetPlcsqlFunctionSchema()) {
            String sql = helper.getPlcsqlFunctionHeaderDDL(function, config.isAddUserSchema());
            plan.add(
                    new AnalyzerStatement(
                            TYPE_DDL_FUNC_HEADER, "FUNC_" + (++seq), sql, 4200 + seq));
        }

        seq = 0;
        for (PlcsqlFunction function : config.getTargetPlcsqlFunctionSchema()) {
            String sql = helper.getPlcsqlFunctionDDL(function, config.isAddUserSchema());
            plan.add(
                    new AnalyzerStatement(
                            TYPE_DDL_FUNC_BODY, "FUNC_BODY_" + (++seq), sql, 4300 + seq));
        }

        return plan;
    }
}
