package com.cubrid.sqlanalyzer.core.engine.executor;

import com.cubrid.cubridmigration.core.engine.executors.IRunnableExecutor;

public class ImmediateExecutor implements IRunnableExecutor {
    /**
     * Execute task in the same thread.
     *
     * @param task is the migration task to be executed.
     */
    public void execute(Runnable task) {
        task.run();
    }

    /**
     * Retrieves whether the executor is busy now.
     *
     * @return false always
     */
    public boolean isBusy() {
        return false;
    }

    /** Interrupt */
    public void interrupt() {
        // Do nothing

    }

    /** Dispose */
    public void dispose() {
        // Do nothing
    }
}
