package com.cubrid.sqlanalyzer.ui.page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.cubrid.cubridmigration.core.connection.ConnParameters;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbobject.Grant;
import com.cubrid.cubridmigration.core.dbobject.Schema;
import com.cubrid.cubridmigration.core.dbobject.Sequence;
import com.cubrid.cubridmigration.core.dbobject.Synonym;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbobject.View;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.ui.database.DatabaseConnectionInfo;
import com.cubrid.cubridmigration.ui.database.IJDBCConnectionFilter;
import com.cubrid.cubridmigration.ui.database.JDBCConnectionMgrView;
import com.cubrid.cubridmigration.ui.message.Messages;
import com.cubrid.cubridmigration.ui.wizard.MigrationWizard;
import com.cubrid.cubridmigration.ui.wizard.dialog.RenameSchemaDialog;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.ui.AnalyzerWizard;
import com.cubrid.sqlanalyzer.ui.AnalyzerWizardPage;
import com.cubrid.sqlanalyzer.ui.page.view.AbstractSourceView;
import com.cubrid.sqlanalyzer.ui.page.view.AnalyzerSrcFileSelectView;

public class CreateSrcConnectionPage extends AnalyzerWizardPage {
	
	private AbstractSourceView crtView;
	
	private class AnalyzerSrcConnSelectView implements AbstractSourceView {

	    private final JDBCConnectionMgrView conMgrView;
	    
	    private AnalyzerSrcConnSelectView() {
	        conMgrView =
	                new JDBCConnectionMgrView(
	                        MigrationWizard.getSupportedSrcDBTypes(),
	                        new IJDBCConnectionFilter() {

	                            public boolean doFilter(ConnParameters cp) {
	                                return MigrationConfiguration.SOURCE_TYPE_ORACLE != cp.getDatabaseType().getID();
	                            }
	                        });
	    }

	    /**
	     * Create controls
	     *
	     * @param parent of the controls
	     */
	    public void createControls(Composite parent) {
	        conMgrView.createControls(parent);
	    }

	    /**
	     * get Catalog
	     *
	     * @return Catalog
	     */
	    public Catalog getCatalog() {
	        return conMgrView.getCatalog();
	    }

	    /** Hide */
	    public void hide() {
	        conMgrView.hide();
	    }

	    /** Initialize with script's source connection */
	    public void init() {
	        AnalyzerWizard wzd = getMigrationWizard();
	        setTitle(wzd.getStepNoMsg(CreateSrcConnectionPage.this) + Messages.msgSrcSelectOnlineDB);
	        setMessage(Messages.msgSrcSelectOnlineDBDes);
	        List<Integer> dts = new ArrayList<Integer>();
	        AnalyzerConfiguration cfg = wzd.getAnalyzerConfig();
	        dts.add(cfg.getSourceType());
	        conMgrView.setSupportedDBType(dts);
	        // Add catalog to cache.
	        Catalog offlineSrcCatalog = cfg.getOfflineSrcCatalog();
	        ConnParameters srcConParams = cfg.getSourceConParams();
	        conMgrView.init(srcConParams, offlineSrcCatalog);
	    }

	    /**
	     * check whether the dialog changed
	     *
	     * @return true if content changed
	     */
	    public boolean isInputChanged() {
	        boolean srcDBChanged = false;
	        MigrationConfiguration config = getMigrationWizard().getMigrationConfig();
	        // if online is saved but not selected or dumpfile is saved but not selected
	        ConnParameters oldCP = config.getSourceConParams();
	        // the first time set it changed
	        DatabaseConnectionInfo dci = conMgrView.getSelectedDCI();
	        if (oldCP == null && dci != null) {
	            srcDBChanged = true;
	        } else if (oldCP != null) {
	            srcDBChanged = !oldCP.isSameDB(dci.getConnParameters());
	        }
	        return srcDBChanged;
	    }

	    /**
	     * Save to configurations
	     *
	     * @return true if successfully
	     */
	    public boolean save() {
	        if (this.conMgrView.getSelectedDCI() == null) {
	            MessageDialog.openError(
	                    getShell(), Messages.msgError, Messages.sourceDBPageErrNoSelectedItem);
	            return false;
	        }
	        final AnalyzerWizard wzd = getMigrationWizard();
	        Catalog catalog = getCatalog();
	        if (catalog == null) {
	            return false;
	        }

	        if (catalog.getDatabaseType().getID() == 1) {
	            removeEmptySchema(catalog);
	        }

	        List<String> errorSchemas = new ArrayList<String>();
	        Map<String, String> old2NewSchemaMapping = new HashMap<String, String>();
	        MigrationConfiguration cfg = wzd.getMigrationConfig();
	        cfg.resetSchemaInfo();
	        if (catalog.getDatabaseType().isSupportMultiSchema()
	                && !cfg.getExpEntryTableCfg().isEmpty()) {
	            List<String> expSchemas = cfg.getExpSchemaNames();
	            for (String schema : expSchemas) {
	                if (catalog.getSchemaByName(schema) != null) {
	                    continue;
	                }
	                errorSchemas.add(schema);
	            }
	            if (!errorSchemas.isEmpty()) {
	                List<String> newSchemas = new ArrayList<String>();
	                for (Schema newSchema : catalog.getSchemas()) {
	                    newSchemas.add(newSchema.getName());
	                }
	                old2NewSchemaMapping =
	                        RenameSchemaDialog.renameSchemas(errorSchemas, newSchemas);
	                // Dialog canceled, user maybe want to choose another source.
	                if (old2NewSchemaMapping == null) {
	                    return false;
	                }
	            }
	        }

	        // create configuration name
	        if (cfg.getName() == null) {
	            cfg.setName(
	                    catalog.getDatabaseType().getName(),
	                    catalog.getName(),
	                    cfg.getWizardStartDateTime());
	        }

	        if (isInputChanged() || wzd.getOriginalSourceCatalog() != catalog) {
	            // If it is a new migration, initialize the configuration
	            wzd.resetBySourceDBChanged();
	            cfg = wzd.getMigrationConfig();
	        }
	        wzd.setOriginalSourceCatalog(catalog);
	        cfg.setSourceConParams(catalog.getConnectionParameters());
	        // Set the invalid schema to right schema or remove them.
	        for (String es : errorSchemas) {
	            String newSchema = old2NewSchemaMapping.get(es);
	            if (StringUtils.isBlank(newSchema)) {
	                cfg.removeExpSchema(es);
	            } else {
	                cfg.renameExpSchema(es, newSchema);
	            }
	        }
	        return true;
	    }

	    /**
	     * Remove empty Schema
	     *
	     * @param catalog Catalog
	     */
	    private void removeEmptySchema(Catalog catalog) {
	        List<Schema> schemaList = catalog.getSchemas();
	        List<Schema> removeSchema = new ArrayList<Schema>();

	        for (Schema schema : schemaList) {
	            List<Table> tableList = schema.getTables();
	            List<View> viewList = schema.getViews();
	            List<Sequence> sequenceList = schema.getSequenceList();
	            List<Synonym> synonymList = schema.getSynonymList();
	            List<Grant> grantList = schema.getGrantList();

	            if (tableList.isEmpty()
	                    && viewList.isEmpty()
	                    && sequenceList.isEmpty()
	                    && synonymList.isEmpty()
	                    && grantList.isEmpty()) {
	                removeSchema.add(schema);
	            }
	        }

	        catalog.removeSchema(removeSchema);
	    }

	    /** Show */
	    public void show() {
	        conMgrView.show();
	    }
	}
	

	private AbstractSourceView xmlView;
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
		
		
//		crtView.createControls(container);
//		crtView.show();
//		container.layout(true);
//		crtView.init();
	}
	
	protected void afterShowCurrentPage(PageChangedEvent event) {
		try {
			crtView = getCrtView();
			crtView.createControls(container);
			crtView.init();
			crtView.show();
			container.layout();
		} catch (Exception ex) {
			MessageDialog.openError(getShell(), getTitle(), getErrorMessage());
		}
	}
	
	private AbstractSourceView getCrtView() {
		AnalyzerWizard wizard = getMigrationWizard();
		AnalyzerConfiguration config = wizard.getAnalyzerConfig();
		int destType = config.getSourceType();
		if (destType == AnalyzerConfiguration.SOURCE_TYPE_DB) {
			return new AnalyzerSrcConnSelectView();
		} else if (destType == AnalyzerConfiguration.SOURCE_TYPE_XML) {
			return new AnalyzerSrcFileSelectView(this);
		}
		throw new RuntimeException("Error source configuration.");
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
		return crtView.save();
	}
}
