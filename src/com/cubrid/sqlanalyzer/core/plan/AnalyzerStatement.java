package com.cubrid.sqlanalyzer.core.plan;

public class AnalyzerStatement {
    private final String type;
    private final String id;
    private final String sql;
    private final int order;

    public AnalyzerStatement(String type, String id, String sql, int order) {
        this.type = type;
        this.id = id;
        this.sql = sql;
        this.order = order;
    }

    public String getType() { return type; }
    public String getId() { return id; }
    public String getSQL() { return sql; }
    public int getOrder() { return order; }
}
