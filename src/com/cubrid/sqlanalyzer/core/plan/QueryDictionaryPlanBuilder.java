package com.cubrid.sqlanalyzer.core.plan;

import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;

public class QueryDictionaryPlanBuilder implements AnalyzerExecutionPlanBuilder {
    @Override
    public AnalyzerExecutionPlan build(AnalyzerConfiguration config) {
        AnalyzerExecutionPlan plan = new AnalyzerExecutionPlan();
        QueryDictionary dict = config.getQueryDict();
        if (dict == null) {
            return plan;
        }

        dict.getSelectQueryMap().forEach((id, sql) ->
                plan.add(new AnalyzerStatement("SELECT", id, sql, 1000)));
        dict.getInsertQueryMap().forEach((id, sql) ->
                plan.add(new AnalyzerStatement("INSERT", id, sql, 2000)));
        dict.getUpdateQueryMap().forEach((id, sql) ->
                plan.add(new AnalyzerStatement("UPDATE", id, sql, 3000)));
        dict.getDeleteQueryMap().forEach((id, sql) ->
                plan.add(new AnalyzerStatement("DELETE", id, sql, 4000)));

        return plan;
    }
}
