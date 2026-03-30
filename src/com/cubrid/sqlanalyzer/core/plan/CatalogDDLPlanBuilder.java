package com.cubrid.sqlanalyzer.core.plan;

import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbobject.View;
import com.cubrid.cubridmigration.cubrid.CUBRIDSQLHelper;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;

public class CatalogDDLPlanBuilder implements AnalyzerExecutionPlanBuilder {

    @Override
    public AnalyzerExecutionPlan build(AnalyzerConfiguration config) {
        AnalyzerExecutionPlan plan = new AnalyzerExecutionPlan();
        CUBRIDSQLHelper helper = CUBRIDSQLHelper.getInstance(null);

        int seq = 0;
        for (Table table : config.getTargetTableSchema()) {
            String sql = helper.getTableDDL(table, config.isAddUserSchema());
            plan.add(new AnalyzerStatement("DDL_TABLE", "TABLE_" + (++seq), sql, 1000 + seq));
        }

        seq = 0;
        for (View view : config.getTargetViewSchema()) {
            String sql = helper.getViewDDL(view, config.isAddUserSchema());
            plan.add(new AnalyzerStatement("DDL_VIEW", "VIEW_" + (++seq), sql, 2000 + seq));
        }

        return plan;
    }
}
