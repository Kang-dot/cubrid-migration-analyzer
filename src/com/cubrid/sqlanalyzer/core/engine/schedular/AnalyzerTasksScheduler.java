package com.cubrid.sqlanalyzer.core.engine.schedular;

import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;
import com.cubrid.sqlanalyzer.core.engine.AnalyzerContext;
import com.cubrid.sqlanalyzer.core.engine.task.AnalyzeTask;
import com.cubrid.sqlanalyzer.core.engine.task.AnalyzerTaskFactory;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPlan;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPlanBuilder;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatement;
import com.cubrid.sqlanalyzer.core.plan.CatalogDDLPlanBuilder;
import com.cubrid.sqlanalyzer.core.plan.QueryDictionaryPlanBuilder;

public class AnalyzerTasksScheduler {
	
	protected AnalyzerTaskFactory taskFactory;
	protected AnalyzerContext context;
	protected QueryDictionary queryDict;
	
	public AnalyzerTasksScheduler() {

	}
	
	public void schedule() {
		// TODO: schedule
		AnalyzerExecutionPlan plan = buildExecutionPlan();
		
		for (AnalyzerStatement stmt : plan.getStatements()) {
			executeTask(taskFactory.executeQuery(stmt.getType(), stmt.getId(), stmt.getSQL()));
		}
		
//		setQueryDictionary();
//		executeDDL();
//		executeDML();
	}
	
	public AnalyzerExecutionPlan buildExecutionPlan() {
	    AnalyzerConfiguration config = context.getConfig();
	    AnalyzerExecutionPlanBuilder builder;

	    if (config.isSourceXML()) {
	        builder = new QueryDictionaryPlanBuilder();
	    } else {
	    	builder = new CatalogDDLPlanBuilder();
	    }

	    return builder.build(config);
	}
	
	public void setQueryDictionary() {
    	AnalyzerConfiguration config = context.getConfig();
    	queryDict = config.getQueryDict();
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
    		executeTask(taskFactory.executeQuery("SELECT", id, query));
    	});
    	
    	queryDict.getInsertQueryMap().forEach((id, query) -> {
    		executeTask(taskFactory.executeQuery("INSERT", id, query));
    	});
    	
    	queryDict.getDeleteQueryMap().forEach((id, query) -> {
    		executeTask(taskFactory.executeQuery("DELETE", id, query));
    	});
    	
    	queryDict.getUpdateQueryMap().forEach((id, query) -> {
    		executeTask(taskFactory.executeQuery("UPDATE", id, query));
    	});
    	
    }
    
    protected void executeTask(AnalyzeTask task) {
    	context.getDbObjectExe().execute((Runnable) task);
    }
}
