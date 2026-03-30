package com.cubrid.sqlanalyzer.core.plan;

import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;

public interface AnalyzerExecutionPlanBuilder {
    AnalyzerExecutionPlan build(AnalyzerConfiguration config);
}
