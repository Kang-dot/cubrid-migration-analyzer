package com.cubrid.sqlanalyzer.core.event;

public class AnalyzerExecuteEvent extends AnalyzerEvent {
	
	private String queryType;
	private String id;
	private String query;
	private Throwable error;
	
    public AnalyzerExecuteEvent(String queryType, String id, String query) {
    	this.queryType = queryType;
    	this.id = id;
		this.query = query;
	}

	public AnalyzerExecuteEvent(String queryType, String id, String query, Throwable error) {
		this.queryType = queryType;
		this.id = id;
		this.query = query;
		this.error = error;
	}

	public String getQueryType() {
		return queryType;
	}
	
	public String getId() {
		return id;
	}
	
	public String getQuery() {
		return query;
	}
	
	public Throwable getError() {
		return error;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		
		if (error == null) {
			buffer.append("query [" + id + "] success");
		} else {
			buffer.append("query [" + id + "] failed. error: " + error.getMessage());
		}
		
		return buffer.toString();
	}
}
