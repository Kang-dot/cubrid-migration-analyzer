package com.cubrid.sqlanalyzer.ui.editor;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.engine.event.CreateObjectEvent;
import com.cubrid.cubridmigration.core.engine.event.ExportCSVEvent;
import com.cubrid.cubridmigration.core.engine.event.ExportRecordsEvent;
import com.cubrid.cubridmigration.core.engine.event.ExportSQLEvent;
import com.cubrid.cubridmigration.core.engine.event.ImportCSVEvent;
import com.cubrid.cubridmigration.core.engine.event.ImportRecordsEvent;
import com.cubrid.cubridmigration.core.engine.event.ImportSQLsEvent;
import com.cubrid.cubridmigration.core.engine.event.MigrationErrorEvent;
import com.cubrid.cubridmigration.core.engine.event.MigrationEvent;
import com.cubrid.cubridmigration.cubrid.CUBRIDTimeUtil;
import com.cubrid.cubridmigration.ui.database.SchemaFetcherWithProgress;
import com.cubrid.cubridmigration.ui.message.Messages;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.dbobject.AnalyzerCatalog;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;
import com.cubrid.sqlanalyzer.core.engine.AnalyzerProcessManager;
import com.cubrid.sqlanalyzer.core.event.AnalyzerErrorEvent;
import com.cubrid.sqlanalyzer.core.event.AnalyzerEvent;
import com.cubrid.sqlanalyzer.core.event.AnalyzerExecuteEvent;
import com.cubrid.sqlanalyzer.core.event.IAnalyzerMonitor;
import com.cubrid.sqlanalyzer.ui.reporter.AnalyzerReporter;
//import com.cubrid.common.ui.swt.ProgressMonitorDialogRunner;
import com.cubrid.sqlanalyzer.ui.swt.ProgressMonitorDialogRunner;

public class AnalyzerProgressUIController {
    protected static final String NA_STRING = "--";
    private static final String TYPE_DDL_TABLE = "DDL_TABLE";
    private static final String TYPE_DDL_VIEW = "DDL_VIEW";
    private static final String TYPE_SELECT = "SELECT";
    private static final String TYPE_INSERT = "INSERT";
    private static final String TYPE_UPDATE = "UPDATE";
    private static final String TYPE_DELETE = "DELETE";
    private static final String[] STATEMENT_TYPES = {
        TYPE_DDL_TABLE, TYPE_DDL_VIEW, TYPE_SELECT, TYPE_INSERT, TYPE_UPDATE, TYPE_DELETE
    };
//    protected static final Logger LOG = LogUtil.getLogger(MigrationProgressUIController.class);

    protected ProgressMonitorDialogRunner progressMonitorDialogRunner =
            new ProgressMonitorDialogRunner();

    protected AnalyzerConfiguration config;
    protected AnalyzerReporter reporter;
    protected AnalyzerProcessManager mpm;

    protected String[][] tableItems;
    protected final Map<String, Integer> totalCountMap = new LinkedHashMap<String, Integer>();
    protected final Map<String, Integer> finishedCountMap = new LinkedHashMap<String, Integer>();
    protected int totalQueries;
    protected int finishedQueries;

    protected int expCountCache = 0;

    protected int impCountCache = 0;

    protected String reportEditorPartId;

    /**
     * Add an message to the text area.
     *
     * @param txtProgress Date
     * @param eventDate message
     * @param msg String
     * @param isError whether the message is a error message
     */
    public void addMessage2Text(
            StyledText txtProgress, Date eventDate, String msg, boolean isError) {
        if (!txtProgress.getVisible()) {
            return;
        }
        int length = txtProgress.getText().length();
        txtProgress.append(CUBRIDTimeUtil.defaultFormatMilin(eventDate));
        txtProgress.setStyleRange(
                new StyleRange(
                        length, 23, txtProgress.getDisplay().getSystemColor(SWT.COLOR_BLUE), null));
        txtProgress.append(" ");
        String message = msg == null ? " " : msg;
        txtProgress.append(message);
        txtProgress.append("\n");
        Color color = getLogTextColor(isError);
        txtProgress.setStyleRange(new StyleRange(length + 24, message.length(), color, null));
        txtProgress.setSelection(length);
        if (txtProgress.getLineCount() > 8000) {
            txtProgress
                    .getContent()
                    .replaceTextRange(0, txtProgress.getContent().getOffsetAtLine(1), "");
        }
    }

    /** @return ISaveablePart2.YES if migration is finished */
    public int canBeClosed() {
        return isMigrationRunning() ? ISaveablePart2.CANCEL : ISaveablePart2.YES;
    }

    /**
     * @param startMode started by user or scheduler
     * @return MigrationReporter used in progress monitor
     */
    public AnalyzerReporter createMigrationReporter(int startMode) {
        reporter = new AnalyzerReporter(config, startMode);
        return reporter;
    }

    /**
     * Get table cell long value
     *
     * @param svalue "-" or a long value
     * @return long value, 0 if "-"
     */
    protected long getCellValue(String svalue) {
        if (StringUtils.isBlank(svalue)) {
            return 0;
        }
        return Long.parseLong(NA_STRING.equals(svalue) ? "0" : svalue);
    }

    /** @return Migration Configuration's commit count. */
    public int getCommitCount() {
        return config.getCommitCount();
    }

    /**
     * @param isError red will be returned.
     * @return red or green
     */
    protected Color getLogTextColor(boolean isError) {
        Color color;
        if (isError) {
            color = Display.getDefault().getSystemColor(SWT.COLOR_RED);
        } else {
            color = Display.getDefault().getSystemColor(SWT.COLOR_GREEN);
        }
        return color;
    }

    /**
     * @param event MigrationEvent
     * @return how much the progress bar should grow up when the event received.
     */
    public int getProgressBarProgressValue(MigrationEvent event) {
        if (event instanceof CreateObjectEvent) {
            CreateObjectEvent ev = (CreateObjectEvent) event;
            return ev.isSuccess() ? 1 : 0;
        }
        int commitCount = getCommitCount();
        if (event instanceof ExportRecordsEvent) {
            ExportRecordsEvent ere = (ExportRecordsEvent) event;
            if (ere.getRecordCount() <= commitCount) {
                expCountCache = expCountCache + ere.getRecordCount();
            }
            if (expCountCache >= commitCount) {
                int factor = expCountCache / commitCount;
                expCountCache = expCountCache % commitCount;
                return factor;
            }
        } else if (event instanceof ImportRecordsEvent) {
            ImportRecordsEvent ire = (ImportRecordsEvent) event;
            if (ire.isSuccess()) {
                if (ire.getRecordCount() <= commitCount) {
                    impCountCache = impCountCache + ire.getRecordCount();
                }
                if (impCountCache >= commitCount) {
                    int factor = impCountCache / commitCount;
                    impCountCache = impCountCache % commitCount;
                    return factor;
                }
            }
        }
        return 0;
    }

    /**
     * AnalyzerExecuteEvent progress bar controller
     * 
     * @param event AnalyzerEvent
     * @return how much the progress bar should grow up when the event received.
     */
    public int getProgressBarProgressValue(AnalyzerEvent event) {
        if (!(event instanceof AnalyzerExecuteEvent)) {
            return 0;
        }
        AnalyzerExecuteEvent aev = (AnalyzerExecuteEvent) event;

        // Count is updated even if the execution failed.
        String statementType = normalizeStatementType(aev.getQueryType());
        if (statementType == null) {
            return 0;
        }

        incrementFinished(statementType);

        return 1;
    }

    /** @return the progress bar's style according to the config.isImplicitEstimate */
    public int getProgressBarStyle() {
        return config.isImplicitEstimate() ? SWT.INDETERMINATE : SWT.NONE;
    }

    /** @return the progress table viewer's input data */
    public String[][] getProgressTableInput() {
        tableItems = new String[STATEMENT_TYPES.length][4];
        totalCountMap.clear();
        finishedCountMap.clear();

        QueryDictionary dict = config.getQueryDict();
        totalCountMap.put(TYPE_DDL_TABLE, config.getTargetTableSchema().size());
        totalCountMap.put(TYPE_DDL_VIEW, config.getTargetViewSchema().size());
        totalCountMap.put(
                TYPE_SELECT,
                dict != null && dict.getSelectQueryMap() != null ? dict.getSelectQueryMap().size() : 0);
        totalCountMap.put(
                TYPE_INSERT,
                dict != null && dict.getInsertQueryMap() != null ? dict.getInsertQueryMap().size() : 0);
        totalCountMap.put(
                TYPE_UPDATE,
                dict != null && dict.getUpdateQueryMap() != null ? dict.getUpdateQueryMap().size() : 0);
        totalCountMap.put(
                TYPE_DELETE,
                dict != null && dict.getDeleteQueryMap() != null ? dict.getDeleteQueryMap().size() : 0);

        totalQueries = 0;
        for (String type : STATEMENT_TYPES) {
            finishedCountMap.put(type, 0);
            totalQueries += totalCountMap.get(type);
        }
        finishedQueries = 0;

        int row = 0;
        for (String type : STATEMENT_TYPES) {
            int total = totalCountMap.get(type);
            tableItems[row++] =
                    new String[] {getDisplayType(type), String.valueOf(total), "0", "0%"};
        }

        return tableItems;
    }

    /** @return the progress bar's total progress value */
    public int getTotalProgress() {
        return Math.max(totalQueries, 0);
    }

    /**
     * @param event MigrationEvent
     * @return true if the event has error
     */
    public boolean ifEventHasError(MigrationEvent event) {
        if (event instanceof CreateObjectEvent) {
            CreateObjectEvent ev = (CreateObjectEvent) event;
            return !ev.isSuccess();
        } else if (event instanceof ImportRecordsEvent) {
            ImportRecordsEvent ire = (ImportRecordsEvent) event;
            return !ire.isSuccess();
        } else if (event instanceof ImportCSVEvent) {
            ImportCSVEvent ire = (ImportCSVEvent) event;
            return !ire.isSuccess();
        } else if (event instanceof ImportSQLsEvent) {
            ImportSQLsEvent ire = (ImportSQLsEvent) event;
            return !ire.isSuccess();
        }
        return event instanceof MigrationErrorEvent;
    }

    /**
     * @param event AnalyzerEvent
     * @return true if the event has error
     */
    public boolean ifEventHasError(AnalyzerEvent event) {
        if (event instanceof AnalyzerExecuteEvent) {
            return ((AnalyzerExecuteEvent) event).getError() != null;
        }
        return event instanceof AnalyzerErrorEvent;
    }

    /**
     * When the monitor received migration event, judge if the monitor should update the export
     * status
     *
     * @param event MigrationEvent
     * @return true if export status should be updated.
     */
    public boolean ifShouldUpdateExportStatus(MigrationEvent event) {
        return (event instanceof ExportRecordsEvent)
                || (event instanceof ExportCSVEvent)
                || (event instanceof ExportSQLEvent);
    }

    /**
     * When the monitor received migration event, judge if the monitor should update the import
     * status
     *
     * @param event MigrationEvent
     * @return true if import status should be updated.
     */
    public boolean ifShouldUpdateImportStatus(MigrationEvent event) {
        if (event instanceof ImportCSVEvent) {
            return ((ImportCSVEvent) event).isSuccess();
        }
        if (event instanceof ImportSQLsEvent) {
            return ((ImportSQLsEvent) event).isSuccess();
        }
        if (event instanceof ImportRecordsEvent) {
            return ((ImportRecordsEvent) event).isSuccess();
        }
        return false;
    }

    /** @return if the migration is running. */
    public boolean isMigrationRunning() {
        return mpm != null;
    }

    /** */
    public void migrationFinished() {
        mpm = null;
        if (!config.targetIsOnline()) {
            return;
        }
        // auto refresh target schema
        try {
            Catalog catalog = SchemaFetcherWithProgress.fetch(config.getTargetConParams());
            if (catalog == null) {
//                LOG.error("Refresh target DB schema failed.");
            }
        } catch (Exception ignored) {
//            LOG.error("", ignored);
        }
    }

    /** @param oldEditorPart to be closed. */
    public void openMigrationReport(EditorPart oldEditorPart) {
        try {
            PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow()
                    .getActivePage()
                    .openEditor(reporter, reportEditorPartId);
            oldEditorPart.getSite().getPage().closeEditor(oldEditorPart, false);
        } catch (PartInitException e1) {
            MessageDialog.openError(
                    PlatformUI.getWorkbench().getDisplay().getActiveShell(),
                    Messages.msgError,
                    e1.getMessage());
        }
    }

    public void setConfig(AnalyzerConfiguration config) {
        this.config = config;
    }

    public void setProgressMonitorDialogRunner(
            ProgressMonitorDialogRunner progressMonitorDialogRunner) {
        this.progressMonitorDialogRunner = progressMonitorDialogRunner;
    }

    public void setReportEditorPartId(String reportEditorPartId) {
        this.reportEditorPartId = reportEditorPartId;
    }

    /**
     * Start migration process
     *
     * @param monitor migration monitor
     * @param startMode start by user or scheduler
     */
    public void startMigration(IAnalyzerMonitor monitor, int startMode) {
        expCountCache = 0;
        impCountCache = 0;
        config.cleanNoUsedConfigForStart();
        final AnalyzerReporter reporter = createMigrationReporter(startMode);
        mpm = AnalyzerProcessManager.getInstance(config, monitor, reporter);
        mpm.startMigration();
    }

    /** Stop migration immediately. */
    public void stopMigrationNow() {
        if (mpm == null) {
            return;
        }
        if (!MessageDialog.openConfirm(
                Display.getDefault().getActiveShell(),
                Messages.msgConfirmation,
                Messages.msgConfirmStopMigration)) {
            return;
        }
        // Stop migration with progress dialog.
        try {
            progressMonitorDialogRunner.run(
                    true,
                    false,
                    new IRunnableWithProgress() {

                        public void run(IProgressMonitor monitor)
                                throws InvocationTargetException, InterruptedException {
                            monitor.beginTask(
                                    Messages.msgStoppingMigration, IProgressMonitor.UNKNOWN);
                            mpm.interruptMigration();
                            monitor.done();
                        }
                    });
        } catch (Exception e) {
//            LOG.error("", e);
        }
    }

    /**
     * Update the export count
     *
     * @param tableName to be updated
     * @param exp count
     * @return row data updated
     */
    public String[] updateTableExpData(String tableName, long exp) {
        if (exp <= 0) {
            return new String[] {};
        }
        for (String[] item : tableItems) {
            if (item[0].equals(tableName)) {
                return getItemForExpData(exp, item);
            }
        }
        return new String[] {};
    }

    public String[] updateTableExpData(String owner, String tableName, long exp) {
        if (exp <= 0) {
            return new String[] {};
        }
        for (String[] item : tableItems) {

            // for Single Schema
            if (item[5] == null || "null".equalsIgnoreCase(item[5])) {
                return updateTableExpData(tableName, exp);
            }

            if (item[0].equals(tableName) && item[5].equalsIgnoreCase(owner)) {
                return getItemForExpData(exp, item);
            }
        }
        return new String[] {};
    }

    private String[] getItemForExpData(long exp, String[] item) {
        long newExp = getCellValue(item[2]) + exp;
        item[2] = String.valueOf(newExp);
        if (!config.isImplicitEstimate()) {
            long oldimp = getCellValue(item[3]);
            try {
                item[4] =
                        String.valueOf(
                                        Math.round(
                                                100
                                                        * (newExp + oldimp)
                                                        / (2 * getCellValue(item[1]))))
                                + "%";
            } catch (Exception e) {
                // Do nothing
            }
        }
        return item;
    }

    /**
     * Update import count of table
     *
     * @param tableName to be updated
     * @param imp count
     * @return row data updated
     */
    public String[] updateTableImpData(String owner, String tableName, long imp) {

        for (String[] item : tableItems) {
            // for Single Schema
            if (item[5] == null || "null".equalsIgnoreCase(item[5])) {
                return updateTableImpData(tableName, imp);
            }
            if (item[0].equals(tableName) && item[5].equalsIgnoreCase(owner)) {
                return getItemForImpData(imp, item);
            }
        }
        return new String[] {};
    }

    public String[] updateTableImpData(String tableName, long imp) {
        for (String[] item : tableItems) {
            if (item[0].equals(tableName)) {
                return getItemForImpData(imp, item);
            }
        }
        return new String[] {};
    }

    private String[] getItemForImpData(long imp, String[] item) {
        long newImp = getCellValue(item[3]) + imp;
        item[3] = String.valueOf(newImp);
        if (!config.isImplicitEstimate()) {
            long oldexp = getCellValue(item[2]);
            try {
                item[4] =
                        String.valueOf(
                                        Math.round(
                                                100
                                                        * (oldexp + newImp)
                                                        / (2 * getCellValue(item[1]))))
                                + "%";
            } catch (Exception e) {
                // Do nothing
            }
        }
        return item;
    }

    /** Update the table's row count in a progress dialog. */
    public void updateTableRowCount() {
        progressMonitorDialogRunner.run(
                true,
                false,
                new IRunnableWithProgress() {

                    public void run(IProgressMonitor monitor)
                            throws InvocationTargetException, InterruptedException {
                        monitor.beginTask(Messages.msgPrepare4Start, IProgressMonitor.UNKNOWN);
                        try {
//                            config.getSourceDBType().getExportHelper().fillTablesRowCount(config);
                        } finally {
                            monitor.done();
                        }
                    }
                });
    }

    private String normalizeStatementType(String queryType) {
        if (queryType == null) {
            return null;
        }
        for (String type : STATEMENT_TYPES) {
            if (type.equals(queryType)) {
                return type;
            }
        }
        return null;
    }

    private String getDisplayType(String statementType) {
        if (TYPE_DDL_TABLE.equals(statementType)) {
            return "TABLE";
        }
        if (TYPE_DDL_VIEW.equals(statementType)) {
            return "VIEW";
        }
        return statementType;
    }

    private int getRowIndex(String statementType) {
        for (int i = 0; i < STATEMENT_TYPES.length; i++) {
            if (STATEMENT_TYPES[i].equals(statementType)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * update table finish count
     * 
     * @param statementType statement type
     */
    private void incrementFinished(String statementType) {
        int rowIndex = getRowIndex(statementType);
        if (rowIndex < 0) {
            return;
        }

        int finished = finishedCountMap.get(statementType) + 1;
        finishedCountMap.put(statementType, finished);
        int total = totalCountMap.containsKey(statementType) ? totalCountMap.get(statementType) : 0;

        if (tableItems != null && rowIndex < tableItems.length) {
            tableItems[rowIndex][2] = String.valueOf(finished);
            String percent = (total <= 0) ? "0%" : (Math.round(100.0 * finished / total) + "%");
            tableItems[rowIndex][3] = percent;
        }

        finishedQueries++;
    }
}
