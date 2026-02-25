package com.cubrid.sqlanalyzer.core.runner;

import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.engine.AnalyzerContext;
import com.cubrid.sqlanalyzer.core.engine.AnalyzerEventHandler;
import com.cubrid.sqlanalyzer.core.event.AnalyzerExecuteEvent;

public class ParserQueryRunner implements IAnalyzerRunner {
	
	private final AnalyzerConfiguration config;
	private final AnalyzerEventHandler eventHandler;
	private final QueryParser queryParser;
	
	public ParserQueryRunner(AnalyzerContext context) {
		this.config = context.getConfig();
		this.eventHandler = (AnalyzerEventHandler) context.getEventsHandler();
		this.queryParser = new QueryParser();
	}
	
	public void executeQuery(String queryType, String id, String query) {
		try {
			queryParser.checkSQL(query);
			runSuccess(queryType, id, query);
		} catch (SQLParserException ex) {
			ex.printStackTrace();
			runFailed(queryType, id, query, ex);
		} catch (Exception ex) {
			// for jni exception
			ex.printStackTrace();
			runFailed(queryType, id, query, ex);
		}
	}
	
	protected void runSuccess(String queryType, String id, String query) {
		eventHandler.handleEvent(new AnalyzerExecuteEvent(queryType, id, query));
	}
	
	protected void runFailed(String queryType, String id, String query, Throwable ex) {
		eventHandler.handleEvent(new AnalyzerExecuteEvent(queryType, id, query, ex));
	}
}
