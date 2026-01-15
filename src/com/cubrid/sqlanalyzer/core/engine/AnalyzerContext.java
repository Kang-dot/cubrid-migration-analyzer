package com.cubrid.sqlanalyzer.core.engine;

import java.util.ArrayList;
import java.util.List;

import com.cubrid.cubridmigration.core.engine.ICanDispose;
import com.cubrid.cubridmigration.core.engine.ICanInterrupt;
import com.cubrid.cubridmigration.core.engine.IMigrationEventHandler;
import com.cubrid.cubridmigration.core.engine.JDBCConManager;
import com.cubrid.cubridmigration.core.engine.MigrationDirAndFilesManager;
import com.cubrid.cubridmigration.core.engine.executors.IRunnableExecutor;
import com.cubrid.cubridmigration.core.engine.executors.MultiQueueExecutor;
import com.cubrid.cubridmigration.core.engine.executors.SingleQueueExecutor;
import com.cubrid.cubridmigration.cubrid.stmt.CUBRIDParameterSetter;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.engine.executor.ImmediateExecutor;

public class AnalyzerContext {

    private final List<IRunnableExecutor> executors = new ArrayList<IRunnableExecutor>();
    // private final Map<String, IRunnableExecutor> mergeDataFileExe = new HashMap<String,
    // IRunnableExecutor>();
    private final List<ICanDispose> tobeDisposed = new ArrayList<ICanDispose>();
    private final AnalyzerConfiguration config;
    private final IAnalyzerEventHandler eventsHandler;

    private IRunnableExecutor mergeTaskExe;
    private IRunnableExecutor dbObjectExe;
    private IRunnableExecutor exportRecExe;
    private IRunnableExecutor importRecordExecutor;
    private CUBRIDParameterSetter paramSetter;
    private JDBCConManager connManager;
    private AnalyzerStatusManager statusMgr;
    private MigrationDirAndFilesManager dirAndFilesMgr;

    private AnalyzerContext(AnalyzerConfiguration config, IAnalyzerEventHandler eventsHandler) {
        this.config = config;
        this.eventsHandler = eventsHandler;
        addTobeDisposed(eventsHandler);
    }

    /**
     * Build migration context.
     *
     * @param config MigrationConfiguration
     * @param eventsHandler IMigrationEventHandler
     * @return MigrationContext
     */
    public static AnalyzerContext buildContext(
            AnalyzerConfiguration config, IAnalyzerEventHandler eventsHandler) {
        final AnalyzerContext context = new AnalyzerContext(config, eventsHandler);

        context.setConnManager(new JDBCConManager(config));

        final AnalyzerStatusManager msm = new AnalyzerStatusManager();
        msm.setHasOOMRisk(config.checkOOMRisk());
        // Adjust OOM control parameters
        final long maxMemory = Runtime.getRuntime().maxMemory() * 95 / 100;
        msm.setMaxMemory(maxMemory);
        msm.setWarningFreeMemory(maxMemory / 2);
        msm.setWarningCommitCount(Math.max(config.getCommitCount() / 10, 500));
        msm.setAlertFreeMemory(maxMemory / 5);
        msm.setAlertCommitCount(Math.max(config.getCommitCount() / 100, 100));
        context.setStatusMgr(msm);

        context.setExportRecExe(new SingleQueueExecutor(config.getExportThreadCount(), true));
        context.setImportRecordExecutor(
                new MultiQueueExecutor(config.getImportThreadCount(), true));

        context.setMergeTaskExe(new SingleQueueExecutor(1, false));

        context.setDbObjectExe(new ImmediateExecutor());

        MigrationDirAndFilesManager dirAndFilesMgr = new MigrationDirAndFilesManager(config);
        dirAndFilesMgr.initialize();
        context.setDirAndFilesMgr(dirAndFilesMgr);
        return context;
    }

    /**
     * Add a resource should be disposed by resource manager
     *
     * @param cd is a resource which will be disposed by this manager
     */
    public void addTobeDisposed(ICanDispose cd) {
        if (!tobeDisposed.contains(cd)) {
            tobeDisposed.add(cd);
            if (cd instanceof IRunnableExecutor) {
                executors.add((IRunnableExecutor) cd);
            }
        }
    }

    /**
     * Release resources
     *
     * @param isBroken if true means the migration is stopped by exception or user
     */
    public void dispose(boolean isBroken) {

        for (ICanDispose cd : tobeDisposed) {
            if (isBroken && (cd instanceof ICanInterrupt)) {
                ((ICanInterrupt) cd).interrupt();
            } else {
                cd.dispose();
            }
        }
    }

    public IRunnableExecutor getMergeTaskExe() {
        return mergeTaskExe;
    }

    public AnalyzerConfiguration getConfig() {
        return config;
    }

    public JDBCConManager getConnManager() {
        return connManager;
    }

    public IRunnableExecutor getDbObjectExe() {
        return dbObjectExe;
    }

    public MigrationDirAndFilesManager getDirAndFilesMgr() {
        return dirAndFilesMgr;
    }

    public IAnalyzerEventHandler getEventsHandler() {
        return eventsHandler;
    }

    public IRunnableExecutor getExportRecExe() {
        return exportRecExe;
    }

    public IRunnableExecutor getImportRecordExecutor() {
        return importRecordExecutor;
    }

    public CUBRIDParameterSetter getParamSetter() {
        return paramSetter;
    }

    public AnalyzerStatusManager getStatusMgr() {
        return statusMgr;
    }

    /**
     * Retrieves whether the executors are busy now.
     *
     * @return true means busy now.
     */
    public boolean isExecutorsBusy() {
        for (IRunnableExecutor re : executors) {
            if (re.isBusy()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set cm task service
     *
     * @param cmTaskService IRunnableExecutor
     */
    protected void setMergeTaskExe(IRunnableExecutor cmTaskService) {
        this.mergeTaskExe = cmTaskService;
        addTobeDisposed(cmTaskService);
    }

    /**
     * Set DB objects executor.
     *
     * @param dbObjectExecutor IRunnableExecutor
     */
    protected void setDbObjectExe(IRunnableExecutor dbObjectExecutor) {
        this.dbObjectExe = dbObjectExecutor;
    }

    /**
     * Set export records executor
     *
     * @param exportRecordsExecutor IRunnableExecutor
     */
    protected void setExportRecExe(IRunnableExecutor exportRecordsExecutor) {
        this.exportRecExe = exportRecordsExecutor;
        addTobeDisposed(exportRecordsExecutor);
    }

    /**
     * Set importing records executor 2
     *
     * @param impRecExecutor2 IRunnableExecutor
     */
    protected void setImportRecordExecutor(IRunnableExecutor impRecExecutor2) {
        this.importRecordExecutor = impRecExecutor2;
        addTobeDisposed(impRecExecutor2);
    }

    protected void setParamSetter(CUBRIDParameterSetter parameterSetter) {
        this.paramSetter = parameterSetter;
    }

    /**
     * Set JDBC connection manager
     *
     * @param connectionManager JDBCConManager
     */
    protected void setConnManager(JDBCConManager connectionManager) {
        this.connManager = connectionManager;
        addTobeDisposed(connectionManager);
    }

    /**
     * Set status manager
     *
     * @param statusManager MigrationStatusManager
     */
    protected void setStatusMgr(AnalyzerStatusManager statusManager) {
        this.statusMgr = statusManager;
    }

    /**
     * Set dir and file manager
     *
     * @param dirAndFilesMgr MigrationDirAndFilesManager
     */
    protected void setDirAndFilesMgr(MigrationDirAndFilesManager dirAndFilesMgr) {
        this.dirAndFilesMgr = dirAndFilesMgr;
        addTobeDisposed(dirAndFilesMgr);
    }

}
