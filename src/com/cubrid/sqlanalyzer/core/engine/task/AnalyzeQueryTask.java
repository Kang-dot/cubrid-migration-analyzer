package com.cubrid.sqlanalyzer.core.engine.task;

public class AnalyzeQueryTask extends AnalyzeTask {
	private String queryType;
	private String id;
	private String query;
	
	public AnalyzeQueryTask(String queryType,String id, String query) {
		this.queryType = queryType;
		this.id = id;
		this.query = query;
	}
 	
	@Override
	protected void executeTask() {
		importer.executeQuery(queryType, id, query);
	}
}
