package com.cubrid.sqlanalyzer.core.event;

public class AnalyzerExecuteEvent extends AnalyzerEvent {
	
	private String id;
	private String query;
	private Throwable error;
	
    public AnalyzerExecuteEvent(String id, String query) {
    	this.id = id;
		this.query = query;
	}

	public AnalyzerExecuteEvent(String id, String query, Throwable error) {
		this.id = id;
		this.query = query;
		this.error = error;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		
		if (error == null) {
			buffer.append("query [" + id + "] success");
		} else {
			buffer.append("query [" + id + "] failed. error: " + error.getStackTrace());
		}
		
		return buffer.toString();
	}
}
