package com.cubrid.sqlanalyzer.core.engine;

import com.cubrid.sqlanalyzer.core.event.AnalyzerEvent;

public interface IAnalyzerEventHandler {
	public void handleEvent(AnalyzerEvent event);
}
