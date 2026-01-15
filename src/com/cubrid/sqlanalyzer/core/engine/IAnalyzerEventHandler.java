package com.cubrid.sqlanalyzer.core.engine;

import com.cubrid.cubridmigration.core.engine.ICanDispose;
import com.cubrid.sqlanalyzer.core.event.AnalyzerEvent;

public interface IAnalyzerEventHandler extends ICanDispose {
	public void handleEvent(AnalyzerEvent event);
	
	public void dispose();
}
