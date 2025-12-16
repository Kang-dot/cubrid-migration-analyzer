package com.cubrid.sqlanalyzer.core.engine.task;

public class AnalyzeQueryTask extends AnalyzeTask {

	private String id;
	private String query;
	
	public AnalyzeQueryTask(String id, String query) {
		this.id = id;
		this.query = query;
	}
 	
	@Override
	protected void executeTask() {
		// TODO Auto-generated method stub
		importer.executeQuery(id, query);
	}
}
