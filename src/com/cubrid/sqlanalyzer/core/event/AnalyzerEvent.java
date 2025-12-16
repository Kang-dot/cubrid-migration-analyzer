package com.cubrid.sqlanalyzer.core.event;

import java.util.Date;

public abstract class AnalyzerEvent {
	private final Date eventTime = new Date();
	
	public Date getEventTime() {
		return (Date) eventTime.clone();
	}
}
