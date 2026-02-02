package com.cubrid.sqlanalyzer.core.event;

public interface IAnalyzerMonitor {
	void start();
	void finished();
    void addEvent(AnalyzerEvent event);
}
