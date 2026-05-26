package com.cubrid.sqlanalyzer.ui.reporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.cubrid.cubridmigration.core.common.Closer;
import com.cubrid.cubridmigration.core.common.PathUtils;
import com.cubrid.cubridmigration.core.engine.event.MigrationEvent;
import com.cubrid.cubridmigration.core.engine.report.IMigrationReporter;
import com.cubrid.cubridmigration.cubrid.CUBRIDTimeUtil;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.event.AnalyzerEvent;
import com.cubrid.sqlanalyzer.core.event.AnalyzerExecuteEvent;
import com.cubrid.sqlanalyzer.core.event.AnalyzerFinishedEvent;
import com.cubrid.sqlanalyzer.core.event.AnalyzerStartEvent;

/**
 * Analyzer Reporter - Minimal implementation
 * Processes only AnalyzerExecuteEvent to store query execution success status
 * 
 * @author Generated
 */
public class AnalyzerReporter implements IMigrationReporter, IEditorInput {
    protected final String fileName; // Only file name, without directory
    protected final AnalyzerConfiguration config;
    protected AnalyzerReport report;
    protected File reportFile;
    protected PrintWriter pwLog;
    protected String logFileName;
    
    /**
     * Constructor that loads report from file
     * 
     * @param file Report file
     */
    public AnalyzerReporter(File file) {
        config = null;
        fileName = file.getName();
    }
    
    /**
     * Constructor using AnalyzerConfiguration
     * 
     * @param config AnalyzerConfiguration
     * @param startMode Start mode
     */
    public AnalyzerReporter(AnalyzerConfiguration config, int startMode) {
        this.config = config;
        long timeTag = System.currentTimeMillis();
        fileName = timeTag + HIS_FILE_EX;
        
        report = new AnalyzerReport();
        
        try {
            final String reportDir = PathUtils.getReportDir();
            // Log file
            File logFile = new File(reportDir + "analyzer_log_" + timeTag + LOG_FILE_EX);
            logFileName = logFile.getName();
            PathUtils.createFile(logFile);
            pwLog = new PrintWriter(new OutputStreamWriter(new FileOutputStream(logFile), UTF_8));
            // Report file
            reportFile = new File(reportDir + "analyzer_report_" + timeTag + REPORT_FILE_EX);
            PathUtils.createFile(reportFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Process AnalyzerEvent
     * Processes only AnalyzerExecuteEvent and stores it in the report
     * 
     * @param event AnalyzerEvent
     */
    public void addAnalyzerEvent(AnalyzerEvent event) {
        if (config != null && pwLog != null) {
            // Write to log file
            pwLog.append(CUBRIDTimeUtil.defaultFormatMilin(event.getEventTime())).append(" ");
            pwLog.append(event.toString());
            pwLog.append(" \r\n");
            pwLog.flush();
        }
        
        long eventTime = event.getEventTime().getTime();
        
        // Process AnalyzerExecuteEvent
        if (event instanceof AnalyzerExecuteEvent) {
            AnalyzerExecuteEvent executeEvent = (AnalyzerExecuteEvent) event;
            String queryType = executeEvent.getQueryType();
            String queryId = executeEvent.getId();
            String query = executeEvent.getQuery();
            boolean success = (executeEvent.getError() == null);
            String errorMessage = executeEvent.getError() != null 
                    ? executeEvent.getError().getMessage() 
                    : null;
            
            report.addQueryResult(queryType, queryId, query, success, errorMessage, eventTime);
        } else if (event instanceof AnalyzerStartEvent) {
            report.setTotalStartTime(eventTime);
        } else if (event instanceof AnalyzerFinishedEvent) {
            report.setTotalEndTime(eventTime);
        }
    }
    
    /**
     * Process MigrationEvent (IMigrationReporter interface implementation)
     * Convert to AnalyzerEvent and process
     * 
     * @param event MigrationEvent
     */
    @Override
    public void addEvent(MigrationEvent event) {
        // MigrationEvent is not AnalyzerEvent, so it is not processed
        // AnalyzerEventHandler should directly call addAnalyzerEvent
//        LOG.warn("addEvent(MigrationEvent) called, but AnalyzerReporter expects AnalyzerEvent");
    }
    
    /**
     * Called when migration is completed
     */
    @Override
    public void finished() {
        try {
            if (pwLog != null) {
                Closer.close(pwLog);
            }

            // Save report file
            if (reportFile != null && report != null) {
                saveReportToFile(reportFile.getCanonicalPath());
            }
        } catch (Exception e) {
//            LOG.error("Failed to finish reporter", e);
        }
    }
    
    /**
     * Save report to file
     * 
     * @param reportFilePath Report file path
     */
    private void saveReportToFile(String reportFilePath) {
        try {
            java.beans.XMLEncoder encoder = new java.beans.XMLEncoder(
                    new java.io.FileOutputStream(reportFilePath));
            encoder.writeObject(report);
            encoder.close();
        } catch (IOException e) {
//            LOG.error("Failed to save report to file: " + reportFilePath, e);
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Load report from report file
     */
    public void loadReportFromFile() {
        try {
            String reportFilePath = PathUtils.getReportDir() + fileName;
            // Use MigrationReportFileUtils if report file needs to be extracted from .mh file
            // Here, we simply assume the report file path
            report = (AnalyzerReport) com.cubrid.cubridmigration.core.common.CUBRIDIOUtils
                    .loadObjectFromXML(reportFilePath);
        } catch (Exception e) {
//            LOG.error("Failed to load report from file", e);
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Return report
     * 
     * @return AnalyzerReport
     */
    public AnalyzerReport getReport() {
        return report;
    }
    
    /**
     * Return file name
     * 
     * @return File name
     */
    public String getFileName() {
        return fileName;
    }
    
    // IEditorInput implementation methods
    
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.equals(AnalyzerConfiguration.class)) {
            return adapter.cast(config);
        } else if (adapter.equals(AnalyzerReport.class)) {
            return adapter.cast(report);
        }
        return null;
    }
    
    @Override
    public boolean exists() {
        return false;
    }
    
    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }
    
    @Override
    public String getName() {
        return "Analyzer Report";
    }
    
    @Override
    public IPersistableElement getPersistable() {
        return null;
    }
    
    @Override
    public String getToolTipText() {
        return getName();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AnalyzerReporter) {
            AnalyzerReporter other = (AnalyzerReporter) obj;
            return fileName != null && fileName.equalsIgnoreCase(other.fileName);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return fileName != null ? fileName.hashCode() : 0;
    }
}
