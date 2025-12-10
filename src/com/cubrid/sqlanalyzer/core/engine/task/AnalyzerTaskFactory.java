package com.cubrid.sqlanalyzer.core.engine.task;

import com.cubrid.cubridmigration.core.engine.exporter.IMigrationExporter;
import com.cubrid.cubridmigration.core.engine.importer.IMigrationImporter;
import com.cubrid.sqlanalyzer.core.engine.AnalyzerContext;

public class AnalyzerTaskFactory {
    private AnalyzerContext context;
    private IMigrationExporter exporter;
    private IMigrationImporter importer;

    public AnalyzerTaskFactory() {}

    /**
     * initialize ExportTask
     *
     * @param task ExportTask
     * @param isExportRecords boolean
     */
    private void initExportTask(AnalyzeTask task, boolean isExportRecords) {
        if (isExportRecords) {
            task.setImportTaskExecutor(context.getImportRecordExecutor());
        } else {
            task.setImportTaskExecutor(context.getDbObjectExe());
        }

        task.setMigrationEventHandler(context.getEventsHandler());
        task.setMigrationExporter(exporter);
        task.setTaskFactory(this);
    }
    
    public void setContext(AnalyzerContext context) {
        this.context = context;
    }

    public void setExporter(IMigrationExporter exporter) {
        this.exporter = exporter;
    }

    public void setImporter(IMigrationImporter importer) {
        this.importer = importer;
    }

    
    
    // TODO: Execute query and receive result
//    public initExecuteTask(AnalyzerTask task) {
//    	
//    }
}
