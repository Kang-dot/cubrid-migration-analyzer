package com.cubrid.sqlanalyzer.core.engine;

import com.cubrid.cubridmigration.core.engine.IMigrationBroker;
import com.cubrid.cubridmigration.core.engine.event.IMigrationErrorEvent;
import com.cubrid.cubridmigration.core.engine.executors.IRunnableExecutor;
import com.cubrid.cubridmigration.core.engine.report.IMigrationReporter;
import com.cubrid.sqlanalyzer.core.engine.executor.SingleQueueExecutor;
import com.cubrid.sqlanalyzer.core.event.AnalyzerCanceledEvent;
import com.cubrid.sqlanalyzer.core.event.AnalyzerErrorEvent;
import com.cubrid.sqlanalyzer.core.event.AnalyzerEvent;
import com.cubrid.sqlanalyzer.core.event.AnalyzerFinishedEvent;
import com.cubrid.sqlanalyzer.core.event.AnalyzerStartEvent;
import com.cubrid.sqlanalyzer.core.event.IAnalyzerMonitor;
import com.cubrid.sqlanalyzer.ui.reporter.AnalyzerReporter;

public class AnalyzerEventHandler implements IAnalyzerEventHandler {

//    private static final Logger LOG = LogUtil.getLogger(MigrationEventHandler.class);
    private final IAnalyzerMonitor monitor;
    private final AnalyzerReporter reporter;
    private final IMigrationBroker breaker;
    private final IRunnableExecutor handlerExecutor = new SingleQueueExecutor(1, false);
    private AnalyzerFinishedEvent mfe = null;

    /**
     * Constructor
     *
     * @param migrationMonitor IMigrationMonitor
     * @param migraionReporter IMigrationReporter
     * @param breaker IMigrationProcessManager
     */
    public AnalyzerEventHandler(
    		IAnalyzerMonitor migrationMonitor,
            IMigrationReporter migrationReporter,
            IMigrationBroker breaker) {
        this.monitor = migrationMonitor;
        this.reporter = (AnalyzerReporter) migrationReporter;
        this.breaker = breaker;
    }

    /**
     * Add event to handle list.
     *
     * @param event MigrationEvent
     */
    public void handleEvent(final AnalyzerEvent event) {
        handlerExecutor.execute(new EventHandlerRunnable(event));
    }

    /** Dispose and release resources */
    public void dispose() {
        monitor.finished();
        reporter.finished();
        handlerExecutor.dispose();
    }

    /**
     * EventHandlerRunnable responses to handle events.
     *
     * @author Kevin Cao
     * @version 1.0 - 2011-8-11 created by Kevin Cao
     */
    protected class EventHandlerRunnable implements Runnable {

        private final AnalyzerEvent event;

        public EventHandlerRunnable(AnalyzerEvent event) {
            this.event = event;
        }

        /** Handle events. */
        public void run() {
            try {
                // After finished event, new event will not be accepted.
                if (mfe != null) {
//                    LOG.info("Migration already finished; ignoring further event: {}", event);
                    return;
                }
                if (event instanceof AnalyzerCanceledEvent) {
                    reporter.addAnalyzerEvent(event);
                    return;
                }
                if (event instanceof IMigrationErrorEvent) {
                    IMigrationErrorEvent evt = (IMigrationErrorEvent) event;
                    // Details of errors will be written into LOG files.
                    if (evt.getError() != null) {
//                        LOG.error("", evt.getError());
                    }
                }
                if (event instanceof AnalyzerErrorEvent) {
                	AnalyzerErrorEvent ee = (AnalyzerErrorEvent) event;
                    monitor.addEvent(event);
                    reporter.addAnalyzerEvent(ee);
//                    if (ee.isFatalError()) {
//                        handleEvent(new MigrationFinishedEvent(true));
//                    }
                    return;
                }
                if (event instanceof AnalyzerFinishedEvent) {
                    // Only receives the first MigrationFinishedEvent.
                    mfe = (AnalyzerFinishedEvent) event;
                    monitor.addEvent(event);
                    reporter.addAnalyzerEvent(event);
                    breaker.migrationStopped(mfe.isBroken());
                    return;
                }
                if (event instanceof AnalyzerStartEvent) {
                    monitor.start();
                    mfe = null;
                    monitor.addEvent(event);
                    reporter.addAnalyzerEvent(event);
                    return;
                }
                // Single record error doesn't be sent to monitor
//                if (!(event instanceof SingleRecordErrorEvent)) {
//                    monitor.addEvent(event);
//                }
                monitor.addEvent(event);
                reporter.addAnalyzerEvent(event);
            } catch (Throwable ex) {
//                LOG.error("", ex);
            	ex.printStackTrace();
            }
        }
    }
}
