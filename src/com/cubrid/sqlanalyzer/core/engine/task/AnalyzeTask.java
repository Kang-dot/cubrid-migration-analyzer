package com.cubrid.sqlanalyzer.core.engine.task;

import com.cubrid.cubridmigration.core.engine.executors.IRunnableExecutor;
import com.cubrid.sqlanalyzer.core.engine.IAnalyzerEventHandler;
import com.cubrid.sqlanalyzer.core.event.AnalyzerErrorEvent;
import com.cubrid.sqlanalyzer.core.runner.IAnalyzerRunner;

public abstract class AnalyzeTask implements Runnable {
    protected IRunnableExecutor importTaskExecutor;
    protected IAnalyzerRunner importer;
    protected IAnalyzerEventHandler eventHandler;
    protected AnalyzerTaskFactory taskFactory;

    /** Run */
    public void run() {
        try {
            // Ensure that the import is not null.
            if (importer == null || eventHandler == null) {
                return;
            }
            executeTask();
        } catch (Throwable e) {
            eventHandler.handleEvent(new AnalyzerErrorEvent(e));
        }
    }

    /** Export objects or records from source database,subclasses should implement this method. */
    protected abstract void executeTask();

    public void setImportTaskExecutor(IRunnableExecutor importTaskExector) {
        this.importTaskExecutor = importTaskExector;
    }

    public void setMigrationImporter(IAnalyzerRunner importer) {
        this.importer = importer;
    }

    public void setMigrationEventHandler(IAnalyzerEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    public void setTaskFactory(AnalyzerTaskFactory taskFactory) {
        this.taskFactory = taskFactory;
    }
}
