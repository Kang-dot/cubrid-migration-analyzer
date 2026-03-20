package com.cubrid.sqlanalyzer.ui.page;

import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;

import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.ui.AnalyzerWizard;
import com.cubrid.sqlanalyzer.ui.AnalyzerWizardPage;

/**
 * Page to set up mapping from source DB objects to target DB objects
 *
 * @author caoyilin
 * @version 1.0 - 2012-07-20
 */
public class AnalyzerObjectMappingPage extends AnalyzerWizardPage {
    
    private Composite mainContainer;
    private StackLayout stackLayout;
    
    private IObjectMappingStrategy analyzerStrategy;
    private IObjectMappingStrategy cmtStrategy;
    private IObjectMappingStrategy currentStrategy;

    /** Create the wizard constructor */
    public AnalyzerObjectMappingPage(String pageName) {
        super(pageName);
    }

    /**
     * Create contents of the wizard
     *
     * @param parent Composite
     */
    @Override
    public void createControl(Composite parent) {
        mainContainer = new Composite(parent, SWT.NONE);
        stackLayout = new StackLayout();
        mainContainer.setLayout(stackLayout);
        setControl(mainContainer);

        // 1. Create instances of each strategy
        analyzerStrategy = new AnalyzerObjectMappingStrategy(this);
        cmtStrategy = new CMTObjectMappingStrategy(this);

        // 2. Create UI components for each strategy (inside StackLayout)
        analyzerStrategy.createControl(mainContainer);
        cmtStrategy.createControl(mainContainer);
    }

    @Override
    protected void afterShowCurrentPage(PageChangedEvent event) {
        AnalyzerWizard mw = getMigrationWizard();
        AnalyzerConfiguration config = mw.getAnalyzerConfig();

        // Strategy switching (branching)
        if (config.isSourceXML()) {
            currentStrategy = analyzerStrategy;
        } else {
            currentStrategy = cmtStrategy;
        }

        // Display the selected strategy view
        stackLayout.topControl = currentStrategy.getContainer();
        mainContainer.layout();

        // Delegate data binding
        currentStrategy.afterShowCurrentPage();
    }

    @Override
    protected void handlePageLeaving(PageChangingEvent event) {
        if (!isGotoNextPage(event)) {
            return;
        }
        if (currentStrategy != null) {
            event.doit = currentStrategy.handlePageLeaving(event);
        }
    }
    
    public AnalyzerWizard getMigrationWizard() {
        return (AnalyzerWizard) super.getWizard();
    }
}
