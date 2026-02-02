package com.cubrid.sqlanalyzer.core.engine.task;

import com.cubrid.cubridmigration.core.engine.exporter.IMigrationExporter;
import com.cubrid.cubridmigration.core.engine.importer.IMigrationImporter;
import com.cubrid.sqlanalyzer.core.engine.AnalyzerContext;
import com.cubrid.sqlanalyzer.core.runner.IAnalyzerRunner;

public class AnalyzerTaskFactory {
    private AnalyzerContext context;
    private IMigrationExporter exporter;
    private IAnalyzerRunner importer;

    public AnalyzerTaskFactory() {}

    /**
     * initialize ExportTask
     *
     * @param task ExportTask
     * @param isExportRecords boolean
     */
    private void initAnalyzeTask(AnalyzeTask task, boolean isExportRecords) {
        if (isExportRecords) {
            task.setImportTaskExecutor(context.getImportRecordExecutor());
        } else {
            task.setImportTaskExecutor(context.getDbObjectExe());
        }

        task.setMigrationEventHandler(context.getEventsHandler());
        task.setMigrationImporter(importer);
        task.setTaskFactory(this);
    }
    
    public void initAnalyzeTask(AnalyzeTask task) {
    	task.setMigrationEventHandler(context.getEventsHandler());
    	task.setMigrationImporter(importer);
    	task.setImportTaskExecutor(null);
    }
    
    public void setContext(AnalyzerContext context) {
        this.context = context;
    }

    public void setExporter(IMigrationExporter exporter) {
        this.exporter = exporter;
    }

    public void setImporter(IAnalyzerRunner importer) {
        this.importer = importer;
    }

    public AnalyzeTask executeQuery(String queryType, String id, String query) {
    	AnalyzeQueryTask task = new AnalyzeQueryTask(queryType, id, query);
    	initAnalyzeTask(task);
    	return task;
    }
}

