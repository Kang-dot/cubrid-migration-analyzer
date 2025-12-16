package com.cubrid.sqlanalyzer.core.engine.schedular;

import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.sqlanalyzer.core.dbobject.AnalyzerCatalog;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;
import com.cubrid.sqlanalyzer.core.engine.AnalyzerContext;
import com.cubrid.sqlanalyzer.core.engine.task.AnalyzeTask;
import com.cubrid.sqlanalyzer.core.engine.task.AnalyzerTaskFactory;

public class AnalyzerTasksScheduler {
	
	protected AnalyzerTaskFactory taskFactory;
	protected AnalyzerContext context;
	protected QueryDictionary queryDict;
	
	public AnalyzerTasksScheduler() {

	}
	
	public void schedule() {
		// TODO: schedule
		executeDDL();
		executeDML();
	}
	
	public void setQueryDictionary() {
    	Catalog catalog = context.getConfig().getSrcCatalog();
    	if (catalog instanceof AnalyzerCatalog) {
    	    AnalyzerCatalog analyzerCatalog = (AnalyzerCatalog) catalog;
    	    queryDict = analyzerCatalog.getQueryDictionary();
    	}
	}
		

	public void setTaskFactory(AnalyzerTaskFactory taskFactory) {
        this.taskFactory = taskFactory;
    }

    public void setContext(AnalyzerContext context) {
        this.context = context;
    }
    
    public void executeDDL() {
    	
    }
    
    public void executeDML() {
    	queryDict.getSelectQueryMap().forEach((id, query) -> {
    		executeTask(taskFactory.executeQuery(id, query));
    	});
    	
    	
    }
    
    protected void executeTask(AnalyzeTask task) {
    	context.getDbObjectExe().execute((Runnable) task);
    }
}
