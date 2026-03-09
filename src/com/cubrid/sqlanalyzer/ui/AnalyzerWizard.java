package com.cubrid.sqlanalyzer.ui;

import java.util.Date;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.cubrid.CUBRIDTimeUtil;
import com.cubrid.cubridmigration.ui.wizard.MigrationWizard;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.dbobject.AnalyzerCatalog;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.DefaultNode;
import com.cubrid.sqlanalyzer.navigator.AnalyzerNodeManager;
import com.cubrid.sqlanalyzer.ui.editor.AnalyzerProgressEditorInput;
import com.cubrid.sqlanalyzer.ui.editor.AnalyzerProgressEditorPart;
import com.cubrid.sqlanalyzer.ui.page.AnalyzerComfirmPage;
import com.cubrid.sqlanalyzer.ui.page.AnalyzerObjectMappingPage;
import com.cubrid.sqlanalyzer.ui.page.AnalyzerSelectSrcTarTypePage;
import com.cubrid.sqlanalyzer.ui.page.CreateSrcConnectionPage;
import com.cubrid.sqlanalyzer.ui.page.CreateTarConnectionPage;

public class AnalyzerWizard extends MigrationWizard {

	private static final int[] IDX_ONLINE = new int[] {0, 1, 2, 3, 4};
	private static final int[] IDX_PARSER = new int[] {0, 1, 3, 4};
	
	DefaultNode defaultTreeNode;
	AnalyzerConfiguration analyzerConfig;
    private Catalog targetCatalog;
	
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
		AnalyzerCatalog analyzerCatalog = ((AnalyzerCatalog) getOriginalSourceCatalog());
		analyzerConfig.setQueryDict(analyzerCatalog.getQueryDictionary());
        
		startAnalyze();
		
		return true;
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
		addPage(new AnalyzerSelectSrcTarTypePage("0"));
        addPage(new CreateSrcConnectionPage("1"));
        addPage(new CreateTarConnectionPage("2"));
        addPage(new AnalyzerObjectMappingPage("3"));
        addPage(new AnalyzerComfirmPage("4"));
//		addPage(new SelectDestinationPage("1"));
	}
	
    public boolean canFinish() {
        final IWizardPage currentPage = getContainer().getCurrentPage();
        if (currentPage instanceof AnalyzerComfirmPage) {
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
    
    protected void startAnalyze() {
		  try {
		      analyzerConfig.cleanNoUsedConfigForStart();
		      
		      PlatformUI.getWorkbench()
              .getActiveWorkbenchWindow()
              .getActivePage()
              .openEditor(
                      new AnalyzerProgressEditorInput(getAnalyzerConfig(), migrationScript),
                      AnalyzerProgressEditorPart.ID);
		  } catch (PartInitException e) {
			  e.printStackTrace();
		  }
    }
    
    public AnalyzerConfiguration getAnalyzerConfig() {
        return analyzerConfig;
    }
    
    public MigrationConfiguration getMigrationConfig() {
    	return super.getMigrationConfig();
    }
    
    private int[] getPageNOs() {
    	if (analyzerConfig.isTargetParser()) {
    		return IDX_PARSER;
    	}
    	
        return IDX_ONLINE;
    }
    
    /**
     * setSourceDBNode
     *
     * @param sourceCatalog
     */
    public void setSourceDBNode(Catalog sourceCatalog) {
        if (sourceCatalog == null) {
        	defaultTreeNode = null;
        } else {
        	defaultTreeNode =
                    AnalyzerNodeManager.getInstance()
                            .createDBNode(
                                    (AnalyzerCatalog) sourceCatalog);
        }
    }

    public boolean updateSrcTarType(int srcType, int tarType) {
        // Warning message : type changing will cause settings reset
        AnalyzerConfiguration cfg = analyzerConfig;

        cfg.setSourceType(srcType);
        cfg.setDestType(tarType);
        return true;
    }
    

        
    public DefaultNode getSourceDBNode() {
    	return defaultTreeNode;
    }

    public void setTargetCatalog(Catalog catalog) {
        this.targetCatalog = catalog;
    }

    public Catalog getTargetCatalog() {
        return targetCatalog;
    }
}