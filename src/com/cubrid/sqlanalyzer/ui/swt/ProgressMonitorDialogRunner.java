package com.cubrid.sqlanalyzer.ui.swt;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

public class ProgressMonitorDialogRunner {
    public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) {
        try {
            new ProgressMonitorDialog(null).run(fork, cancelable, runnable);
        } catch (Exception ex) {
            throw new RuntimeException("Run with progress error.", ex);
        }
    }
}