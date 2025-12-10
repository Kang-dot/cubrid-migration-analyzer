package com.cubrid.sqlanalyzer.core.engine.task;

import com.cubrid.cubridmigration.core.engine.IMigrationEventHandler;
import com.cubrid.cubridmigration.core.engine.event.MigrationErrorEvent;
import com.cubrid.cubridmigration.core.engine.executors.IRunnableExecutor;
import com.cubrid.cubridmigration.core.engine.exporter.IMigrationExporter;

public abstract class AnalyzeTask {
    protected IRunnableExecutor importTaskExecutor;
    protected IMigrationExporter exporter;
    protected IMigrationEventHandler eventHandler;
    protected AnalyzerTaskFactory taskFactory;

    /** Run */
    public void run() {
        if (null == importTaskExecutor || null == exporter || null == eventHandler) {
            return;
        }
        try {
            executeExportTask();
        } catch (Throwable ex) {
            eventHandler.handleEvent(new MigrationErrorEvent(ex));
        }
    }

    /** Export objects or records from source database,subclasses should implement this method. */
    protected abstract void executeExportTask();

    public void setImportTaskExecutor(IRunnableExecutor importTaskExector) {
        this.importTaskExecutor = importTaskExector;
    }

    public void setMigrationExporter(IMigrationExporter exporter) {
        this.exporter = exporter;
    }

    public void setMigrationEventHandler(IMigrationEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    public void setTaskFactory(AnalyzerTaskFactory taskFactory) {
        this.taskFactory = taskFactory;
    }
}
