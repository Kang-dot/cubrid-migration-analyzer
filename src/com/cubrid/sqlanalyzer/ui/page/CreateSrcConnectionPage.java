package com.cubrid.sqlanalyzer.ui.page;

import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.cubrid.sqlanalyzer.ui.AnalyzerWizardPage;
import com.cubrid.sqlanalyzer.ui.page.view.AbstractSourceView;
import com.cubrid.sqlanalyzer.ui.page.view.AnalyzerSrcFileSelectView;

public class CreateSrcConnectionPage extends AnalyzerWizardPage {

	private AbstractSourceView xmlView = new AnalyzerSrcFileSelectView(this);
	private Composite container;

	public CreateSrcConnectionPage(String pageName) {
		super(pageName);
	}

	/**
	 * Create contents of the wizard
	 *
	 * @param parent Composite
	 */
	@Override
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		final GridLayout gridLayoutRoot = new GridLayout();
		container.setLayout(gridLayoutRoot);
		setControl(container);
		
		xmlView.createControls(container);
		xmlView.show();
		container.layout(true);
		xmlView.init();
	}
	
    /**
     * When migration wizard will show next page or previous page.
     *
     * @param event PageChangingEvent
     */
    protected void handlePageLeaving(PageChangingEvent event) {
        // If page is not complete, it should be go to previous page.
        if (!isPageComplete()) {
            return;
        }
        if (!isGotoNextPage(event)) {
            return;
        }
        event.doit = updateMigrationConfig();
    }


	/**
	 * Save user input (source database connection information) to export options.
	 *
	 * @return true if update success.
	 */
	@Override
	protected boolean updateMigrationConfig() {
		return xmlView.save();
	}
}
