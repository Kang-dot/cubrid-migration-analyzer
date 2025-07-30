package com.cubrid.sqlanalyzer.ui;

import org.eclipse.jface.wizard.Wizard;

import com.cubrid.sqlanalyzer.ui.page.CreateSrcConnectionPage;
import com.cubrid.sqlanalyzer.ui.page.CreateTarConnectionPage;

public class AnalyzerWizard extends Wizard {

	@Override
	public boolean performFinish() {
		// TODO Auto-generated method stub
		return false;
	}
	
	/**addPages
	 * canFinish
	 * getNextPage
	 * getPageNum
	 * getPreviousPage
	 * performCancel
	 * performFinish
	 * checkConnectionStatus
	 * 
	 */
	
	public void addPages() {
		addPage(new CreateSrcConnectionPage("0"));
		addPage(new CreateTarConnectionPage("1"));
	}
}