package com.cubrid.sqlanalyzer.core.engine;

import com.cubrid.cubridmigration.core.engine.IMigrationBroker;
import com.cubrid.cubridmigration.core.engine.IMigrationEventHandler;
import com.cubrid.cubridmigration.core.engine.IMigrationMonitor;
import com.cubrid.cubridmigration.core.engine.ThreadUtils;
import com.cubrid.cubridmigration.core.engine.event.MigrationCanceledEvent;
import com.cubrid.cubridmigration.core.engine.event.MigrationErrorEvent;
import com.cubrid.cubridmigration.core.engine.event.MigrationFinishedEvent;
import com.cubrid.cubridmigration.core.engine.event.MigrationStartEvent;
import com.cubrid.cubridmigration.core.engine.exception.BreakMigrationException;
import com.cubrid.cubridmigration.core.engine.exception.NormalMigrationException;
import com.cubrid.cubridmigration.core.engine.exporter.IMigrationExporter;
import com.cubrid.cubridmigration.core.engine.exporter.impl.CUBRIDJDBCExporter;
import com.cubrid.cubridmigration.core.engine.exporter.impl.JDBCExporter;
import com.cubrid.cubridmigration.core.engine.exporter.impl.MYSQLDumpXMLExporter;
import com.cubrid.cubridmigration.core.engine.exporter.impl.PerformMYSQLXMLDataReader;
import com.cubrid.cubridmigration.core.engine.importer.IMigrationImporter;
import com.cubrid.cubridmigration.core.engine.report.IMigrationReporter;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.engine.schedular.AnalyzerTasksScheduler;
import com.cubrid.sqlanalyzer.core.engine.task.AnalyzerTaskFactory;
import com.cubrid.sqlanalyzer.core.runner.JDBCQueryRunner;

public class AnalyzerProcessManager {
	
	private static class AnalyzerBreaker implements IMigrationBroker  {
        private final AnalyzerProcessManager mpm;

        private AnalyzerBreaker(AnalyzerProcessManager mpm) {
            this.mpm = mpm;
        }

        /**
         * Migration stopped
         *
         * @param isBroken or all works were done.
         */
        public void migrationStopped(boolean isBroken) {
            mpm.setMigrationStop(isBroken);
        }
	}
	
	private class AnalyzerMainThread extends Thread {
		AnalyzerMainThread() {
            setName("Migration main thread");
        }

        /** Run */
        public void run() {
            final IMigrationEventHandler eventsHandler = context.getEventsHandler();
            try {
                // Initialize
                if (isRunning()) {
                    // Record start time
                    eventsHandler.handleEvent(new MigrationStartEvent());
                    eventsHandler.handleEvent(new MigrationCanceledEvent());
                    throw new BreakMigrationException("Migration canceled");
                }
                setRunning(true);
                eventsHandler.handleEvent(new MigrationStartEvent());

                AnalyzerTasksScheduler scheduler = buildTaskScheduler();
                scheduler.schedule();

                eventsHandler.handleEvent(new MigrationFinishedEvent(false));
            } catch (NormalMigrationException ex) {
                eventsHandler.handleEvent(new MigrationErrorEvent(ex));
                eventsHandler.handleEvent(new MigrationFinishedEvent(true));
            } catch (Throwable er) {
                eventsHandler.handleEvent(new MigrationErrorEvent(er));
            }
        }
	}
	
	private static boolean isRunning = false;
	private static final Object RUNNING_LOCK = new Object();
	
    public static AnalyzerProcessManager getInstance(
            AnalyzerConfiguration config, IMigrationMonitor monitor, IMigrationReporter reporter) {
        AnalyzerProcessManager mpm = new AnalyzerProcessManager();
        AnalyzerEventHandler eh =
                new AnalyzerEventHandler(monitor, reporter, new AnalyzerBreaker(mpm));
        AnalyzerContext context = AnalyzerContext.buildContext(config, eh);
        mpm.setContext(context);
        return mpm;
    }

    /**
     * Retrieve whether the migration process is running.
     *
     * @return true if it is running.
     */
    public static boolean isRunning() {
        synchronized (RUNNING_LOCK) {
            return isRunning;
        }
    }

    /**
     * Set migration process status
     *
     * @param running true if is running
     */
    private static void setRunning(boolean running) {
        synchronized (RUNNING_LOCK) {
            isRunning = running;
        }
    }

    private AnalyzerContext context;

    private Thread mainThread;

    private final Object threadLock = new Object();

    private AnalyzerProcessManager() {
        // Private constructor
    }

    /**
     * buildTaskFactory
     *
     * @return MigrationTaskFactory
     */
    private AnalyzerTaskFactory buildTaskFactory() {
    	AnalyzerTaskFactory taskFactory = new AnalyzerTaskFactory();
        taskFactory.setContext(context);
        // Exporter
        AnalyzerConfiguration config = context.getConfig();
        IMigrationExporter exporter;
        if (config.sourceIsOnline()) {
            JDBCExporter exp =
                    config.getSourceType() == AnalyzerConfiguration.SOURCE_TYPE_CUBRID
                            ? new CUBRIDJDBCExporter()
                            : new JDBCExporter();
            exp.setConfig(config);
            exp.setConnManager(context.getConnManager());
            exp.setEventHandler(context.getEventsHandler());
            exp.setStatusManager(context.getStatusMgr());
            exporter = exp;
        } else if (config.sourceIsXMLDump()) {
            MYSQLDumpXMLExporter exp = new MYSQLDumpXMLExporter();
            exp.setConfig(config);
            exp.setEventHandler(context.getEventsHandler());

            PerformMYSQLXMLDataReader handler = new PerformMYSQLXMLDataReader();
            handler.setConfig(config);
            handler.setExecutor(context.getExportRecExe());
            handler.setStatusManager(context.getStatusMgr());
            exp.setHandler(handler);
            exporter = exp;
        } else {
            exporter = null;
        }
        taskFactory.setExporter(exporter);
        // Importer
        IMigrationImporter importer;
        
        
        //TODO: need connect jdbc version importer and load .dll file and parse version
//        if (config.targetIsFile()) {
//            importer = new LoadFileImporter(context);
//        } else if (config.targetIsOnline()) {
//            importer = new JDBCQueryRunner(context);
//        } else {
//            // importer = new LoadDBImporter(mrManager);
//            throw new BreakMigrationException("Offline migration is not supported any more.");
//        }
        
        importer = new JDBCQueryRunner(context);
        
        taskFactory.setImporter(importer);
        return taskFactory;
    }

    /**
     * createTaskScheduler
     *
     * @return MigrationTasksScheduler
     */
    private AnalyzerTasksScheduler buildTaskScheduler() {
        AnalyzerTaskFactory taskFactory = buildTaskFactory();
        AnalyzerTasksScheduler scheduler = new AnalyzerTasksScheduler();
        scheduler.setTaskFactory(taskFactory);
        scheduler.setContext(context);
        return scheduler;
    }

    /** Interrupt the migration process by Users. It should be called in a progress dialog. */
    public void interruptMigration() {
        context.getEventsHandler().handleEvent(new MigrationFinishedEvent(true));
        // waiting for stopping.
        while (mainThread != null) {
            ThreadUtils.threadSleep(1000, null);
        }
    }

    /**
     * It should be called by object factory
     *
     * @param context MigrationContext
     */
    protected void setContext(AnalyzerContext context) {
        this.context = context;
    }

    /**
     * Stop migration process and release resources.
     *
     * @param isBroken true if migration is broken.
     */
    private void setMigrationStop(boolean isBroken) {
        setRunning(false);
        synchronized (threadLock) {
            if (mainThread == null) {
                return;
            }
            try {
                context.dispose(isBroken);
                mainThread.interrupt();
            } finally {
                mainThread = null;
            }
        }
    }

    /** Start migration process. */
    public void startMigration() {
        synchronized (threadLock) {
            if (mainThread != null) {
                return;
            }
            mainThread = new AnalyzerMainThread();
            mainThread.start();
        }
    }
	
}
