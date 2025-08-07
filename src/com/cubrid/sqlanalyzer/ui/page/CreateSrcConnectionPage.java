package com.cubrid.sqlanalyzer.ui.page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

// import com.cubrid.common.log.LogUtil;
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
import com.cubrid.cubridmigration.ui.wizard.MigrationWizard;
import com.cubrid.cubridmigration.ui.wizard.dialog.RenameSchemaDialog;
import com.cubrid.cubridmigration.ui.wizard.page.SelectSourcePage;

public class CreateSrcConnectionPage extends AnalyzerWizardPage {

	/**
	 * AbstractSourceView
	 *
	 * @author Kevin Cao
	 * @version 1.0 - 2013-6-3 created by Kevin Cao
	 */
	private static interface AbstractSourceView {
		/**
		 * Create controls
		 *
		 * @param parent of the controls
		 */
		void createControls(Composite parent);

		/**
		 * Retrieves the catalog
		 *
		 * @return Catalog
		 */
		Catalog getCatalog();

		/** Hide view */
		void hide();

		/** Initialize the view */
		void init();

		/**
		 * check whether the dialog changed
		 *
		 * @return true if content changed
		 */
		boolean isInputChanged();

		/**
		 * Save to wizard
		 *
		 * @return true if successfully
		 */
		boolean save();

		/** Show view */
		void show();
	}

	/**
	 * Select online database as source.
	 *
	 * @author Kevin Cao
	 * @version 1.0 - 2013-6-3 created by Kevin Cao
	 */
	private class SelectOnlineSrcView implements AbstractSourceView {

		private final JDBCConnectionMgrView conMgrView;

		private SelectOnlineSrcView() {
			conMgrView =
					new JDBCConnectionMgrView(
							MigrationWizard.getSupportedSrcDBTypes(),
							new IJDBCConnectionFilter() {

								public boolean doFilter(ConnParameters cp) {
									return getMigrationWizard().getMigrationConfig().getSourceType()
											!= cp.getDatabaseType().getID();
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
			MigrationWizard wzd = getMigrationWizard();
			setTitle(wzd.getStepNoMsg(CreateSrcConnectionPage.this) + "sample title");
			setMessage("sample description");
			List<Integer> dts = new ArrayList<Integer>();
			MigrationConfiguration cfg = wzd.getMigrationConfig();
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
						getShell(), "Error", "No Selected Item");
				return false;
			}
			final MigrationWizard wzd = getMigrationWizard();
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

//	private static final Logger LOG = LogUtil.getLogger(CreateSrcConnectionPage.class);
	private AbstractSourceView onlineView = new SelectOnlineSrcView();
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
		
		// UI 컨트롤들을 즉시 생성
		onlineView.createControls(container);
		onlineView.show();
		container.layout(true);
		onlineView.init();
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
		return onlineView.save();
	}
}
