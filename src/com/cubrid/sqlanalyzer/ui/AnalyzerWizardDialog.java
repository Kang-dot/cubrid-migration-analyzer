package com.cubrid.sqlanalyzer.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.cubrid.cubridmigration.ui.common.UICommonTool;

public class AnalyzerWizardDialog extends WizardDialog {

	public AnalyzerWizardDialog(Shell parentShell, IWizard newWizard) {
		super(parentShell, newWizard);
		// TODO Auto-generated constructor stub
        setHelpAvailable(false);
        setShellStyle(
                SWT.CLOSE
                        | SWT.MAX
                        | SWT.TITLE
                        | SWT.BORDER
                        | SWT.APPLICATION_MODAL
                        | SWT.RESIZE
                        | getDefaultOrientation());
	}
	
    /**
     * Overwrite the method. disable the ProgressMonitorPart which take up place on bottom of page
     *
     * @param parent Composite
     * @return Control
     */
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        for (Control control : composite.getChildren()) {
            if (control instanceof ProgressMonitorPart) {
                GridData gd = (GridData) control.getLayoutData();
                gd.exclude = true;
            }
        }

        return composite;
    }
    
    /**
     * rename finish button text
     *
     * @param parent Composite
     */
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        Button finishButton = super.getButton(IDialogConstants.FINISH_ID);
        finishButton.setText("Start");
        Button btnBack = super.getButton(IDialogConstants.BACK_ID);
        btnBack.setText("< Back");
        Button btnNext = super.getButton(IDialogConstants.NEXT_ID);
        btnNext.setText("Next >");
        Button btnCancel = super.getButton(IDialogConstants.CANCEL_ID);
        btnCancel.setText("Cancel");
    }
    
    protected void constrainShellSize() {
        super.constrainShellSize();
        getShell().setMinimumSize(750, 450);
        UICommonTool.centerShell(getShell());
    }
    
    protected Control createContents(Composite parent) {
        Control result = super.createContents(parent);
        IWizardPage[] pages = this.getWizard().getPages();
        for (IWizardPage page : pages) {
            if (page instanceof IPageChangingListener) {
                this.addPageChangingListener((IPageChangingListener) page);
            }
            if (page instanceof IPageChangedListener) {
                this.addPageChangedListener((IPageChangedListener) page);
            }
        }
        return result;
    }

    protected void backPressed() {
//        IWizardPage prePage = getCurrentPage().getPreviousPage();
//        if (prePage instanceof SelectSrcTarTypesPage) {
//            if (!MessageDialog.openConfirm(
//                    getShell(), Messages.msgConfirmation, Messages.msgConfirmationChangedType)) {
//                return;
//            }
//        }
//        super.backPressed();
    }

}
