package com.cubrid.sqlanalyzer.core.plan;

public class AnalyzerStatement {
    private final String type;
    private final String id;
    private final String sql;

    public AnalyzerStatement(String type, String id, String sql) {
        this.type = type;
        this.id = id;
        this.sql = sql;
    }

    public String getType() { return type; }
    public String getId() { return id; }
    public String getSQL() { return sql; }
}
