package com.cubrid.sqlanalyzer.core.engine;

import com.cubrid.cubridmigration.core.engine.IMigrationBroker;
import com.cubrid.cubridmigration.core.engine.IMigrationEventHandler;
import com.cubrid.cubridmigration.core.engine.IMigrationMonitor;
import com.cubrid.cubridmigration.core.engine.event.IMigrationErrorEvent;
import com.cubrid.cubridmigration.core.engine.event.MigrationCanceledEvent;
import com.cubrid.cubridmigration.core.engine.event.MigrationErrorEvent;
import com.cubrid.cubridmigration.core.engine.event.MigrationEvent;
import com.cubrid.cubridmigration.core.engine.event.MigrationFinishedEvent;
import com.cubrid.cubridmigration.core.engine.event.MigrationStartEvent;
import com.cubrid.cubridmigration.core.engine.event.SingleRecordErrorEvent;
import com.cubrid.cubridmigration.core.engine.executors.IRunnableExecutor;
import com.cubrid.cubridmigration.core.engine.report.IMigrationReporter;
import com.cubrid.sqlanalyzer.core.engine.executor.SingleQueueExecutor;

public class AnalyzerEventHandler implements IMigrationEventHandler {

//    private static final Logger LOG = LogUtil.getLogger(MigrationEventHandler.class);
    private final IMigrationMonitor monitor;
    private final IMigrationReporter reporter;
    private final IMigrationBroker breaker;
    private final IRunnableExecutor handlerExecutor = new SingleQueueExecutor(1, false);
    private MigrationFinishedEvent mfe = null;

    /**
     * Constructor
     *
     * @param migrationMonitor IMigrationMonitor
     * @param migraionReporter IMigrationReporter
     * @param breaker IMigrationProcessManager
     */
    public AnalyzerEventHandler(
            IMigrationMonitor migrationMonitor,
            IMigrationReporter migraionReporter,
            IMigrationBroker breaker) {
        this.monitor = migrationMonitor;
        this.reporter = migraionReporter;
        this.breaker = breaker;
    }

    /**
     * Add event to handle list.
     *
     * @param event MigrationEvent
     */
    public void handleEvent(final MigrationEvent event) {
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

        private final MigrationEvent event;

        public EventHandlerRunnable(MigrationEvent event) {
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
                if (event instanceof MigrationCanceledEvent) {
                    reporter.addEvent(event);
                    return;
                }
                if (event instanceof IMigrationErrorEvent) {
                    IMigrationErrorEvent evt = (IMigrationErrorEvent) event;
                    // Details of errors will be written into LOG files.
                    if (evt.getError() != null) {
//                        LOG.error("", evt.getError());
                    }
                }
                if (event instanceof MigrationErrorEvent) {
                    MigrationErrorEvent ee = (MigrationErrorEvent) event;
                    monitor.addEvent(event);
                    reporter.addEvent(event);
                    if (ee.isFatalError()) {
                        handleEvent(new MigrationFinishedEvent(true));
                    }
                    return;
                }
                if (event instanceof MigrationFinishedEvent) {
                    // Only receives the first MigrationFinishedEvent.
                    mfe = (MigrationFinishedEvent) event;
                    monitor.addEvent(event);
                    reporter.addEvent(event);
                    breaker.migrationStopped(mfe.isBroken());
                    return;
                }
                if (event instanceof MigrationStartEvent) {
                    monitor.start();
                    mfe = null;
                    monitor.addEvent(event);
                    reporter.addEvent(event);
                    return;
                }
                // Single record error doesn't be sent to monitor
                if (!(event instanceof SingleRecordErrorEvent)) {
                    monitor.addEvent(event);
                }
                reporter.addEvent(event);
            } catch (Throwable ex) {
//                LOG.error("", ex);
            }
        }
    }
}
