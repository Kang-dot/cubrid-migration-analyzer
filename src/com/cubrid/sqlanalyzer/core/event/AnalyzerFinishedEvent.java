package com.cubrid.sqlanalyzer.core.event;

public class AnalyzerFinishedEvent extends AnalyzerEvent {
	private final boolean isBroken;
	
	public AnalyzerFinishedEvent(boolean isBroken) {
		this.isBroken = isBroken;
	}
	
	public String toString() {
		return isBroken ? "Analyze interrupted" : "Analyzer is finished";
	}
	
	public boolean isBroken() {
		return isBroken;
	}
}
