package com.cubrid.sqlanalyzer.core.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AnalyzerExecutionPlan {
    private final List<AnalyzerStatement> statements = new ArrayList<>();

    public void add(AnalyzerStatement stmt) {
        if (stmt != null) {
            statements.add(stmt);
        }
    }

    public List<AnalyzerStatement> getStatements() {
        statements.sort(Comparator.comparingInt(AnalyzerStatement::getOrder));
        return Collections.unmodifiableList(statements);
    }

    public boolean isEmpty() {
        return statements.isEmpty();
    }
}
