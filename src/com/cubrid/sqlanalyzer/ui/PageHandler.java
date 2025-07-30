package com.cubrid.sqlanalyzer.ui;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class PageHandler {
    public static void newMigrationWizard() {
        Shell activeShell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
        AnalyzerUIPlugin uiPlugin = new AnalyzerUIPlugin();
        AnalyzerWizard wizard = new AnalyzerWizard();
        AnalyzerWizardDialog dialog = new AnalyzerWizardDialog(activeShell, wizard);

        openWizardDlg(dialog);
    }
    
    private static void openWizardDlg(AnalyzerWizardDialog dialog) {
        dialog.setBlockOnOpen(true);
        dialog.setPageSize(850, 535);
        dialog.open();
    }
}
