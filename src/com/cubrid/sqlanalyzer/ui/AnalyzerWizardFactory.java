package com.cubrid.sqlanalyzer.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.cubrid.cubridmigration.core.common.PathUtils;
import com.cubrid.cubridmigration.core.engine.MigrationProcessManager;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.core.engine.config.SourceCSVConfig;
import com.cubrid.cubridmigration.core.engine.config.SourceConfig;
import com.cubrid.cubridmigration.core.engine.config.SourceEntryTableConfig;
import com.cubrid.cubridmigration.core.engine.config.SourceSQLTableConfig;
import com.cubrid.cubridmigration.core.engine.config.SourceSequenceConfig;
import com.cubrid.cubridmigration.core.engine.config.SourceViewConfig;
import com.cubrid.cubridmigration.core.engine.report.DBObjMigrationResult;
import com.cubrid.cubridmigration.core.engine.report.DataFileImportResult;
import com.cubrid.cubridmigration.core.engine.report.MigrationReport;
import com.cubrid.cubridmigration.core.engine.report.MigrationReportFileUtils;
import com.cubrid.cubridmigration.core.engine.report.RecordMigrationResult;
import com.cubrid.cubridmigration.core.engine.template.MigrationTemplateParser;
import com.cubrid.cubridmigration.ui.common.UICommonTool;
import com.cubrid.cubridmigration.ui.history.CSVImportReportEditorPart;
import com.cubrid.cubridmigration.ui.history.MigrationReportEditorPart;
import com.cubrid.cubridmigration.ui.history.SQLImportReportEditorPart;
import com.cubrid.cubridmigration.ui.history.dialog.OpenWizardWithHistoryDialog;
import com.cubrid.cubridmigration.ui.message.Messages;
import com.cubrid.cubridmigration.ui.script.MigrationScript;
import com.cubrid.cubridmigration.ui.wizard.MigrationWizard;
import com.cubrid.cubridmigration.ui.wizard.MigrationWizardFactory;
import com.cubrid.cubridmigration.ui.wizard.dialog.MigrationWizardDialog;
import com.cubrid.cubridmigration.ui.wizard.editor.CSVProgressEditorPart;
import com.cubrid.cubridmigration.ui.wizard.editor.MigrationProgressEditorPart;
import com.cubrid.cubridmigration.ui.wizard.editor.SQLProgressEditorPart;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.ui.reporter.AnalyzerReport;
import com.cubrid.sqlanalyzer.ui.reporter.AnalyzerReporter;

public class AnalyzerWizardFactory {

    private AnalyzerWizardFactory() {
        // Do nothing here
    }

    /**
     * Open a migration wizard by migration script object
     *
     * @param script MigrationScript
     */
    public static void openMigrationScript(MigrationScript script) {
        if (MigrationWizardFactory.migrationIsRunning()) {
            return;
        }
        Wizard wizard = new MigrationWizard(script);
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        MigrationWizardDialog dialog = new MigrationWizardDialog(shell, wizard);
        openWizardDlg(dialog);
    }

    /**
     * Open wizard dialog
     *
     * @param dialog MigrationWizardDialog
     */
    private static void openWizardDlg(MigrationWizardDialog dialog) {
        dialog.setBlockOnOpen(true);
        dialog.setPageSize(850, 535);
        dialog.open();
    }

    /**
     * Open a migration wizard dialog by a script file's full name.
     *
     * @param script full name of a script file
     */
    public static void openMigrationScript(String script) {
        if (MigrationWizardFactory.migrationIsRunning()) {
            return;
        }
        Wizard wizard = new MigrationWizard(script);
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        MigrationWizardDialog dialog = new MigrationWizardDialog(shell, wizard);
        openWizardDlg(dialog);
    }

    /**
     * If a migration is running, an error dialog will be popped.
     *
     * @return true if is running
     */
    public static boolean migrationIsRunning() {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        if (MigrationProcessManager.isRunning()) {
            UICommonTool.openErrorBox(shell, Messages.errOtherMigrationRunning);
            return true;
        }
        return false;
    }

    /** Create a new migration wizard */
    public static void newMigrationWizard() {
        if (migrationIsRunning()) {
            return;
        }
        Shell activeShell = PlatformUI.getWorkbench().getDisplay().getActiveShell();

        MigrationWizard wizard = new MigrationWizard();

        MigrationWizardDialog dialog = new MigrationWizardDialog(activeShell, wizard);

        openWizardDlg(dialog);
    }

    /** Create a new SQL migration wizard */
    public static void newSQLWizard() {
        if (migrationIsRunning()) {
            return;
        }
        Shell activeShell = PlatformUI.getWorkbench().getDisplay().getActiveShell();

        MigrationWizard wizard = new MigrationWizard();
        MigrationWizardDialog dialog = new MigrationWizardDialog(activeShell, wizard);
        openWizardDlg(dialog);
    }

    /** Create a new CSV migration wizard */
    public static void newCSVWizard() {
        if (migrationIsRunning()) {
            return;
        }
        Shell activeShell = PlatformUI.getWorkbench().getDisplay().getActiveShell();

        MigrationWizardDialog dialog =
                new MigrationWizardDialog(activeShell, new MigrationWizard());
        openWizardDlg(dialog);
    }

    /**
     * Open migration wizard with all error schemas and error data.Only supports tables and records.
     * If only PK/FK/index was failed, the objects will not be auto selected.
     *
     * @param rpt MigrationReport
     * @param config MigrationConfiguration
     */
    private static void openWizardWithAllError(
            final MigrationReport rpt, MigrationConfiguration config) {
        if (config.sourceIsOnline() || config.sourceIsXMLDump()) {
            List<DBObjMigrationResult> list = rpt.getDbObjectsResult();
            for (DBObjMigrationResult rst : list) {
                if (rst.isSucceed()) {
                    SourceEntryTableConfig setc =
                            config.getExpEntryTableCfg(rst.getObjOwner(), rst.getObjName());
                    if (setc != null) {
                        setc.setCreateNewTable(false);
                        continue;
                    }
                    SourceSQLTableConfig sstc = config.getExpSQLCfgByName(rst.getObjName());
                    if (sstc != null) {
                        sstc.setCreateNewTable(false);
                    }
                }
            }
            final List<RecordMigrationResult> recMigResults = rpt.getRecMigResults();
            for (RecordMigrationResult rst : recMigResults) {
                if (rst.getExpCount() != rst.getImpCount()) {
                    continue;
                }
                SourceEntryTableConfig setc =
                        config.getExpEntryTableCfg(rst.getSrcSchema(), rst.getSource());
                if (setc != null) {
                    setc.setMigrateData(false);
                    continue;
                }
                SourceSQLTableConfig sstc = config.getExpSQLCfgByName(rst.getSource());
                if (sstc != null) {
                    sstc.setMigrateData(false);
                }
            }
            removeAllSerialsAndViews(config);
        } else if (config.sourceIsSQL()) {
            final List<DataFileImportResult> dataFileResults = rpt.getDataFileResults();
            List<String> sqlFiles = new ArrayList<String>();
            for (DataFileImportResult rst : dataFileResults) {
                if (rst.getExportCount() == rst.getImportCount()) {
                    continue;
                }
                sqlFiles.add(rst.getFileName());
            }
            config.setSqlFiles(sqlFiles);
        } else if (config.sourceIsCSV()) {
            List<DBObjMigrationResult> list = rpt.getDbObjectsResult();
            for (DBObjMigrationResult rst : list) {
                if (rst.isSucceed()) {
                    SourceCSVConfig setc = config.getCSVConfigByFile(rst.getObjName());
                    if (setc != null) {
                        setc.setCreate(false);
                        continue;
                    }
                }
            }
            final List<DataFileImportResult> dataFileResults = rpt.getDataFileResults();
            for (DataFileImportResult rst : dataFileResults) {
                if (rst.getExportCount() == rst.getImportCount()) {
                    config.removeCSVFile(rst.getFileName());
                    continue;
                }
            }
        }
        final String fileName = PathUtils.getBaseTempDir() + System.currentTimeMillis() + ".xml";
        MigrationTemplateParser.save(config, fileName, false);
        openMigrationScript(fileName);
        new File(fileName).deleteOnExit();
    }

    /**
     * Open migration wizard with error data.
     *
     * @param rpt MigrationReport
     * @param config MigrationConfiguration
     */
    private static void openWizardWithErrorData(
            final MigrationReport rpt, MigrationConfiguration config) {
        if (config.sourceIsOnline() || config.sourceIsXMLDump()) {
            final List<RecordMigrationResult> recMigResults = rpt.getRecMigResults();
            for (RecordMigrationResult rst : recMigResults) {
                SourceEntryTableConfig setc =
                        config.getExpEntryTableCfg(rst.getSrcSchema(), rst.getSource());
                if (setc != null) {
                    if (rst.getExpCount() == rst.getImpCount() && setc.isMigrateData()) {
                        setc.setMigrateData(false);
                        setc.setCreateNewTable(false);
                        setc.setReplace(false);
                        continue;
                    } else {
                        setc.setCreateNewTable(false);
                        setc.setReplace(false);
                        setc.setCreatePK(false);
                    }
                }
                SourceSQLTableConfig sstc = config.getExpSQLCfgByName(rst.getSource());
                if (sstc != null) {
                    if (rst.getExpCount() == rst.getImpCount() && sstc.isMigrateData()) {
                        sstc.setMigrateData(false);
                        sstc.setCreateNewTable(false);
                        sstc.setReplace(false);
                    } else {
                        sstc.setCreateNewTable(false);
                        sstc.setReplace(false);
                    }
                }
                removeAllSerialsAndViews(config);
            }
        } else if (config.sourceIsSQL()) {
            final List<DataFileImportResult> dataFileResults = rpt.getDataFileResults();
            List<String> sqlFiles = new ArrayList<String>();
            for (DataFileImportResult rst : dataFileResults) {
                if (rst.getExportCount() == rst.getImportCount()) {
                    continue;
                }
                sqlFiles.add(rst.getFileName());
            }
            config.setSqlFiles(sqlFiles);
        } else if (config.sourceIsCSV()) {
            final List<DataFileImportResult> dataFileResults = rpt.getDataFileResults();
            for (DataFileImportResult rst : dataFileResults) {
                if (rst.getExportCount() == rst.getImportCount()) {
                    config.removeCSVFile(rst.getFileName());
                    continue;
                }
            }
        }
        final String fileName = PathUtils.getBaseTempDir() + System.currentTimeMillis() + ".xml";
        MigrationTemplateParser.save(config, fileName, false);
        openMigrationScript(fileName);
        new File(fileName).deleteOnExit();
    }

    /**
     * Remove serials and views in order to re-migrate error tables
     *
     * @param config MigrationConfiguration
     */
    private static void removeAllSerialsAndViews(MigrationConfiguration config) {
        List<SourceSequenceConfig> scList = config.getExpSerialCfg();
        if (scList != null) {
            for (SourceConfig sc : scList) {
                sc.setCreate(false);
                sc.setReplace(false);
            }
        }
        List<SourceViewConfig> scLists = config.getExpViewCfg();
        if (scLists != null) {
            for (SourceConfig sc : scLists) {
                sc.setCreate(false);
                sc.setReplace(false);
            }
        }
        config.cleanNoUsedConfigForStart();
    }

    /**
     * Open migration wizard with selected report
     *
     * @param reporter MigrationReporter
     */
    public static void openWizardWithReport(AnalyzerReporter reporter) {
        if (migrationIsRunning()) {
            return;
        }
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        if (reporter == null) {
            throw new RuntimeException(Messages.msgErrNoMigrationHistorySelected);
        }
        final AnalyzerReport rpt = reporter.getReport();
        if (rpt == null) {
            throw new RuntimeException(Messages.msgErrInvalidMigrationHistory);
        }
        String scriptFile;
        try {
            scriptFile = MigrationReportFileUtils.extractScript(reporter.getFileName());
            if (StringUtils.isBlank(scriptFile)) {
                throw new RuntimeException(Messages.msgErrMigrationHistoryTooOld);
            }
        } catch (IOException e) {
            throw new RuntimeException(Messages.msgErrMigrationHistoryTooOld);
        }
        MigrationConfiguration config = MigrationTemplateParser.parse(scriptFile);
        // Get open mode
        int handlingMode = 0;
        if (rpt.hasError()) {
            OpenWizardWithHistoryDialog dlg =
                    new OpenWizardWithHistoryDialog(shell, config.targetIsOnline());
            if (dlg.open() != IDialogConstants.OK_ID) {
                return;
            }
            handlingMode = dlg.getHandlingMode();
        }
    }

    /**
     * Open migration wizard with re-migrating error files.
     *
     * @param rpt MigrationReport
     * @param config MigrationConfiguration
     */
    private static void openWizardWithErrorFiles(
            MigrationReport rpt, MigrationConfiguration config) {
        config.setSourceType(AnalyzerConfiguration.SOURCE_TYPE_SQL);
        // Clear old settings
        config.setSqlFiles(null);
        List<String> files = rpt.getErrorSQLFiles();
        for (String file : files) {
            try {
                final File ff = new File(file);
                if (!ff.exists()) {
                    continue;
                }
                config.addSQLFile(ff.getCanonicalPath());
            } catch (IOException e) {
                // DO nothing here
            }
        }
        if (config.getSqlFiles().isEmpty()) {
            MessageDialog.openError(
                    Display.getCurrent().getActiveShell(),
                    Messages.msgError,
                    Messages.errErrorFileNotFound);
        }
        final String fileName = PathUtils.getBaseTempDir() + System.currentTimeMillis() + ".xml";
        MigrationTemplateParser.save(config, fileName, false);
        openMigrationScript(fileName);
        new File(fileName).deleteOnExit();
    }

    /**
     * get Report Editor Part ID by source type.
     *
     * @param sourceType @see MigrationConfiguration.SOURCE_TYPE
     * @return editor part ID
     */
    public static String getReportEditorPartID(int sourceType) {
        if (sourceType == AnalyzerConfiguration.SOURCE_TYPE_CSV) {
            return CSVImportReportEditorPart.ID;
        } else if (sourceType == AnalyzerConfiguration.SOURCE_TYPE_SQL) {
            return SQLImportReportEditorPart.ID;
        }
        return MigrationReportEditorPart.ID;
    }

    /**
     * get Report Editor Part ID by source type.
     *
     * @param sourceType @see MigrationConfiguration.SOURCE_TYPE
     * @return editor part ID
     */
    public static String getProgressEditorPartID(int sourceType) {
        if (sourceType == AnalyzerConfiguration.SOURCE_TYPE_CSV) {
            return CSVProgressEditorPart.ID;
        } else if (sourceType == AnalyzerConfiguration.SOURCE_TYPE_SQL) {
            return SQLProgressEditorPart.ID;
        }
        return MigrationProgressEditorPart.ID;
    }
}
