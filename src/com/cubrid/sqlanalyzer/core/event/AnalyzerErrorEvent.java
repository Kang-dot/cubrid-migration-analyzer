package com.cubrid.sqlanalyzer.core.event;

public class AnalyzerErrorEvent extends AnalyzerEvent {
	private Throwable error;
	
	public AnalyzerErrorEvent(Throwable ex) {
		this.error = ex;
	}
	
	public String toString() {
		error.printStackTrace();
		return error.getMessage();
	}
	
	public Throwable getError() {
		return error;
	}
}
