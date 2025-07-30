package com.cubrid.sqlanalyzer.ui.page;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

import com.cubrid.sqlanalyzer.ui.AnalyzerWizard;

public class AnalyzerWizardPage extends WizardPage implements IPageChangedListener, IPageChangingListener {

	protected AnalyzerWizardPage(String pageName) {
		super(pageName);
		// TODO Auto-generated constructor stub
	}

	protected boolean isFirstVisible = true;

	public AnalyzerWizardPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	/**
	 * Retrieves the migration wizard object.
	 *
	 * @return MigrationWizard
	 */
	public AnalyzerWizard getMigrationWizard() {
		return ((AnalyzerWizard) getWizard());
	}

	/**
	 * Handle migration wizard page changed.
	 *
	 * @param event PageChangedEvent
	 */
	public void pageChanged(PageChangedEvent event) {
		if (event.getSelectedPage() == this) {
			afterShowCurrentPage(event);
		}
	}

	/**
	 * Handle migration wizard page changing.
	 *
	 * @param event PageChangingEvent
	 */
	public void handlePageChanging(PageChangingEvent event) {
		if (!event.doit) {
			return;
		}
		if (event.getCurrentPage() == this) {
			handlePageLeaving(event);
		}
	}

	/**
	 * When migration wizard displayed current page.
	 *
	 * @param event PageChangedEvent
	 */
	protected void afterShowCurrentPage(PageChangedEvent event) {
		// Default is doing nothing.
	}

	/**
	 * When migration wizard will show next page or previous page.
	 *
	 * @param event PageChangingEvent
	 */
	protected void handlePageLeaving(PageChangingEvent event) {
		// Default is doing nothing.
	}

	/**
	 * Retrieves that is in go to next page process.
	 *
	 * @param event the PageChangingEvent that is fired.
	 * @return true:go to next page.false:go to previous page.
	 */
	protected boolean isGotoNextPage(PageChangingEvent event) {
		return getWizard().getNextPage(this) == event.getTargetPage();
	}

	/**
	 * Create the control of wizard page.
	 *
	 * @param parent Composite of control.
	 */
	public void createControl(Composite parent) {
		// Do nothing.
	}

	/**
	 * fire page changed
	 *
	 * @param status IStatus
	 */
	protected void firePageStatusChanged(IStatus status) {
		if (status == null) {
			return;
		}
		if (status.getSeverity() == IStatus.INFO) {
			setErrorMessage(null);
			setMessage(status.getMessage());
			setPageComplete(true);
		} else {
			setErrorMessage(status.getMessage());
			setPageComplete(false);
		}
	}

	/**
	 * Save options.
	 *
	 * @return true if update success.
	 */
	protected boolean updateMigrationConfig() {
		return true;
	}
}
