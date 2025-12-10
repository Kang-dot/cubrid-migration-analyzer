package com.cubrid.sqlanalyzer.core.engine.executor;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.cubrid.cubridmigration.core.engine.ThreadUtils;
import com.cubrid.cubridmigration.core.engine.executors.IRunnableExecutor;

public class SingleQueueExecutor implements IRunnableExecutor {

    protected final ThreadPoolExecutor executor;

    protected int poolSize;

    protected final boolean limitTaskCount;

    protected long taskCount;

    protected Object lockObj = new Object();

    protected boolean interrupted;

    public SingleQueueExecutor(int threadSize, boolean limitTaskCount) {
        poolSize = threadSize + Math.max(2, threadSize / 2);
        this.limitTaskCount = limitTaskCount;
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadSize);
    }

    /** Release threads pool. */
    public void dispose() {
        executor.shutdown();
    }

    /**
     * Add task to scheduler.
     *
     * @param tk is the migration task to be executed.
     */
    public void execute(Runnable tk) {
        synchronized (lockObj) {
            taskCount++;
            while (limitTaskCount && tooManyTasks()) {
                if (interrupted) {
                    throw new RuntimeException("Interrupted.");
                }
                ThreadUtils.threadSleep(200, null);
            }
            if (interrupted) {
                throw new RuntimeException("Interrupted.");
            }
            executor.execute(tk);
        }
    }

    /**
     * True if there are too many tasks in queue.
     *
     * @return True if there are too many tasks in queue.
     */
    protected boolean tooManyTasks() {
        return (taskCount - executor.getCompletedTaskCount()) > poolSize;
    }

    /** Interrupted */
    public void interrupt() {
        interrupted = true;
        executor.shutdownNow();
    }

    /**
     * The executor is busy
     *
     * @return true if has tasks not executed
     */
    public boolean isBusy() {
        if (interrupted) {
            return false;
        }
        return ((taskCount - executor.getCompletedTaskCount()) != 0);
    }
}
