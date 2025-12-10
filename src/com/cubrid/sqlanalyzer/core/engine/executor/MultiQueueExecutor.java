package com.cubrid.sqlanalyzer.core.engine.executor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.cubrid.cubridmigration.core.engine.executors.IRunnableExecutor;
import com.cubrid.cubridmigration.core.engine.executors.SingleQueueExecutor;

public class MultiQueueExecutor {
    private final Map<Long, IRunnableExecutor> executors =
            Collections.synchronizedMap(new HashMap<Long, IRunnableExecutor>());

    private boolean interrupted;

    private final boolean limitTaskCount;

    private long totalTask = 0;

    private Object lockObj = new Object();

    private int executorPoolSize;

    public MultiQueueExecutor(int poolSize, boolean limitTaskCount) {
        this.limitTaskCount = limitTaskCount;
        this.executorPoolSize = poolSize;
    }

    /**
     * Add task to scheduler.
     *
     * @param tk is the migration task to be executed.
     */
    public void execute(Runnable tk) {
        IRunnableExecutor es = getExecutor();
        if (interrupted) {
            throw new RuntimeException("Interrupted");
        }
        totalTask++;
        es.execute((Runnable) tk);
    }

    /**
     * Retrieves executor of current thread
     *
     * @return IRunnableExecutor
     */
    private IRunnableExecutor getExecutor() {
        synchronized (lockObj) {
            final long id = Thread.currentThread().getId();
            IRunnableExecutor es = executors.get(id);
            if (es == null) {
                es = new SingleQueueExecutor(executorPoolSize, limitTaskCount);
                executors.put(id, es);
            }
            return es;
        }
    }

    /** Release thread pools */
    public void dispose() {
        for (IRunnableExecutor et : executors.values()) {
            et.dispose();
        }
    }

    /** Release thread pools */
    public void interrupt() {
        interrupted = true;
        for (IRunnableExecutor et : executors.values()) {
            et.interrupt();
        }
    }

    /** @return true if executors are busy. */
    public boolean isBusy() {
        if (interrupted) {
            return false;
        }
        for (IRunnableExecutor et : executors.values()) {
            if (et.isBusy()) {
                return true;
            }
        }
        return false;
    }
}
