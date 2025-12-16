package com.cubrid.sqlanalyzer.core.runner;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.cubrid.cubridmigration.core.common.Closer;
import com.cubrid.cubridmigration.core.engine.JDBCConManager;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.engine.AnalyzerContext;
import com.cubrid.sqlanalyzer.core.engine.AnalyzerEventHandler;
import com.cubrid.sqlanalyzer.core.event.AnalyzerExecuteEvent;

public class JDBCQueryRunner implements IAnalyzerRunner {
	
	private final JDBCConManager connManager;
	private final AnalyzerConfiguration config;
	private final AnalyzerEventHandler eventHandler;
	
	public JDBCQueryRunner(AnalyzerContext context) {
		this.connManager = context.getConnManager();
		this.config = context.getConfig();
		//TODO: remove cast in future
		this.eventHandler = (AnalyzerEventHandler) context.getEventsHandler();
	}
	
	public void executeQuery(String id, String query) {
		//TODO: Execute query (ddl, dml)
		// make event
		// get result
		
		Connection conn = connManager.getTargetConnection();
		Statement stmt = null;
		
		try {
			stmt = conn.createStatement();
			stmt.execute(query);
			runSuccess(id, query);
		} catch (SQLException ex) {
			ex.printStackTrace();
			runFailed(id, query, ex);
		} finally {
			Closer.close(conn);
		}
	}
	
	protected void runSuccess(String id, String query) {
		eventHandler.handleEvent(new AnalyzerExecuteEvent(id, query));
	}
	
	protected void runFailed(String id, String query, Throwable ex) {
		eventHandler.handleEvent(new AnalyzerExecuteEvent(id, query, ex));
	}
}
