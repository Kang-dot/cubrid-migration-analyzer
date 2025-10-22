package com.cubrid.sqlanalyzer.ui;

import java.util.Date;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.wizard.IWizardPage;

import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.cubrid.CUBRIDTimeUtil;
import com.cubrid.cubridmigration.ui.common.navigator.event.CubridNodeManager;
import com.cubrid.cubridmigration.ui.wizard.MigrationWizard;
import com.cubrid.cubridmigration.ui.wizard.page.CSVImportConfirmPage;
import com.cubrid.cubridmigration.ui.wizard.page.ConfirmationPage;
import com.cubrid.cubridmigration.ui.wizard.page.SQLMigrationConfirmPage;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.ui.page.AnalyzerObjectMappingPage;
import com.cubrid.sqlanalyzer.ui.page.CreateSrcConnectionPage;
import com.cubrid.sqlanalyzer.ui.page.CreateTarConnectionPage;

public class AnalyzerWizard extends MigrationWizard {

	private static final int[] IDX_ONLINE = new int[] {0, 1};
	
	AnalyzerConfiguration analyzerConfig;
	
    public AnalyzerWizard() {
        setWindowTitle("Analyzer wizard");
        setNeedsProgressMonitor(true);
        setDialogSettings(new DialogSettings("migration information"));
        analyzerConfig = new AnalyzerConfiguration();
        analyzerConfig.setWizardStartDateTime(
                CUBRIDTimeUtil.wizardStarDateTimeFormat(new Date(System.currentTimeMillis())));
    }
	
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
//        addPage(new SelectSrcTarTypesPage("0"));
//
        addPage(new CreateSrcConnectionPage("0"));
        addPage(new CreateTarConnectionPage("1"));
        addPage(new AnalyzerObjectMappingPage("2"));
//		addPage(new SelectSourcePage("0"));
//		addPage(new SelectDestinationPage("1"));
	}
	
    public boolean canFinish() {
        final IWizardPage currentPage = getContainer().getCurrentPage();
        if (currentPage instanceof SQLMigrationConfirmPage
                || currentPage instanceof ConfirmationPage
                || currentPage instanceof CSVImportConfirmPage) {
            return true;
        }
        return false;
    }
    
    public IWizardPage getNextPage(IWizardPage page) {
        int[] indexes = getPageNOs();
        IWizardPage currentPage = null;
        IWizardPage nextPage = null;
        for (int i : indexes) {
            if (currentPage != null) {
                nextPage = getPage(String.valueOf(i));
                break;
            }
            if (getPage(String.valueOf(i)) == page) {
                currentPage = page;
            }
        }
        return nextPage;
    }
    
    public IWizardPage getPreviousPage(IWizardPage page) {
        int[] indexes = getPageNOs();
        IWizardPage currentPage = null;
        IWizardPage nextPage = null;
        for (int i = indexes.length - 1; i >= 0; i--) {
            if (currentPage != null) {
                nextPage = getPage(String.valueOf(indexes[i]));
                break;
            }
            if (getPage(String.valueOf(indexes[i])) == page) {
                currentPage = page;
            }
        }
        return nextPage;
    }
    
    public AnalyzerConfiguration getMigrationConfig() {
        return analyzerConfig;
    }
    
    private int[] getPageNOs() {
        return IDX_ONLINE;
    }
    
    /**
     * setSourceDBNode
     *
     * @param sourceCatalog
     */
    public void setSourceDBNode(Catalog sourceCatalog) {
        if (sourceCatalog == null) {
            sourceDBNode = null;
        } else {
            sourceDBNode =
                    CubridNodeManager.getInstance()
                            .createDbNode(
                                    sourceCatalog,
                                    migrationConfig.sourceIsXMLDump()
                                            ? "MySQL dump file"
                                            : "Online");
        }
    }
}