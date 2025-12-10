package com.cubrid.sqlanalyzer.core.engine.schedular;

import com.cubrid.sqlanalyzer.core.engine.AnalyzerContext;
import com.cubrid.sqlanalyzer.core.engine.task.AnalyzerTaskFactory;

public class AnalyzerTasksScheduler {
	
	protected AnalyzerTaskFactory taskFactory;
	protected AnalyzerContext context;
	
	public AnalyzerTasksScheduler() {}
	
	public void schedule() {
		
	}

	public void setTaskFactory(AnalyzerTaskFactory taskFactory) {
        this.taskFactory = taskFactory;
    }

    public void setContext(AnalyzerContext context) {
        this.context = context;
    }
}
