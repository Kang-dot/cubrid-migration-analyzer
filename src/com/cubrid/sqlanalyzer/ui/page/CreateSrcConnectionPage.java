package com.cubrid.sqlanalyzer.ui.page;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.cubrid.cubridmigration.core.common.TimeZoneUtils;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.ui.common.UIConstant;
import com.cubrid.cubridmigration.ui.message.Messages;
import com.cubrid.cubridmigration.ui.wizard.MigrationWizard;
import com.cubrid.sqlanalyzer.xmlmetadata.XMLDirSchemaProgressFetcher;
import com.cubrid.sqlanalyzer.xmlmetadata.XMLDirSource;

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
	
    private class SelectXMLSrcView implements AbstractSourceView {
        private Text txtXMLFileDir;
        private Button btnBrowse;
        private Button btnAnalyz;
        private Combo cboFileCharset;
        private Combo cobTimezone;
        private Group grpXML;
        private XMLDirSource parsedSource;
        private Catalog xmlCatalog = null;

        /**
         * Validate the char-set input.
         *
         * @return true if all are valid.
         */
        private boolean checkCharset() {
            if (StringUtils.isBlank(cboFileCharset.getItem(cboFileCharset.getSelectionIndex()))) {
                MessageDialog.openError(
                        getShell(), Messages.msgError, Messages.sourceDBPageErrNoSetXMLFileCharset);
                return false;
            }
            return true;
        }

        /**
         * Validate the input.
         *
         * @return true if all are valid.
         */
        private boolean checkInput() {
            if (StringUtils.isBlank(txtXMLFileDir.getText())) {
                MessageDialog.openError(
                        getShell(), Messages.msgError, Messages.sourceDBPageErrNoSelectedXMLFile);
                return false;
            }
            return true;
        }

        /**
         * Create controls
         *
         * @param parent of the controls
         */
        public void createControls(Composite parent) {
            if (grpXML != null) {
                return;
            }
            grpXML = new Group(parent, SWT.SHADOW_ETCHED_IN);
            grpXML.setLayout(new GridLayout(4, false));
            GridData groupGridData2 = new GridData(SWT.FILL, SWT.FILL, true, true);
            groupGridData2.heightHint = 98;
            grpXML.setLayoutData(groupGridData2);

            Label xmlFilePathLabel = new Label(grpXML, SWT.NONE);
            xmlFilePathLabel.setText(Messages.lblXMLFilePath);
            xmlFilePathLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

            txtXMLFileDir = new Text(grpXML, SWT.BORDER | SWT.READ_ONLY);
            GridData gdXmlFilePathTxt = new GridData(SWT.FILL, SWT.CENTER, true, false);
            gdXmlFilePathTxt.grabExcessHorizontalSpace = true;
            txtXMLFileDir.setLayoutData(gdXmlFilePathTxt);

            btnBrowse = new Button(grpXML, SWT.NONE);
            GridData xmlButtonGd = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
            xmlButtonGd.minimumWidth = 70;
            btnBrowse.setLayoutData(xmlButtonGd);
            btnBrowse.setText(Messages.btnBrowse);
            btnBrowse.setToolTipText(Messages.ttSelectXMLDumpFile);
            btnBrowse.addSelectionListener(
                    new SelectionAdapter() {
                        public void widgetSelected(final SelectionEvent event) {
                            if (!checkCharset()) {
                                return;
                            }
                            DirectoryDialog dlg =
                                    new DirectoryDialog(
                                            PlatformUI.getWorkbench().getDisplay().getActiveShell(),
                                            SWT.SINGLE | SWT.OPEN);
                            dlg.setFilterPath(".");
//                            dlg.setFilterExtensions(new String[] {"*.xml"});
//                            dlg.setFilterNames(new String[] {"*.xml"});
                            final String fileDirPath = dlg.open();
                            if (fileDirPath == null) {
                                return;
                            }
                            txtXMLFileDir.setText(fileDirPath);
                            btnAnalyz.setEnabled(true);
                            try {
                                xmlCatalog = getXmlCatalog(true);
                            } catch (Exception e) {
//                                LOG.error(e.getMessage());
                            }
                        }
                    });

            btnAnalyz = new Button(grpXML, SWT.NONE);
            GridData gdAnalyzXMLFile = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
            gdAnalyzXMLFile.minimumWidth = 70;
            btnAnalyz.setLayoutData(gdAnalyzXMLFile);
            btnAnalyz.setText(Messages.btnAnalyze);
            btnAnalyz.setToolTipText(Messages.ttAnalyzXMLFile);
            btnAnalyz.setEnabled(false);
            btnAnalyz.addSelectionListener(
                    new SelectionAdapter() {
                        public void widgetSelected(final SelectionEvent event) {
                            if (!checkInput()) {
                                return;
                            }
                            try {
                                xmlCatalog = getXmlCatalog(false);
                                if (null != xmlCatalog) {
                                    getMigrationWizard().resetBySourceDBChanged();
                                }
                            } catch (Exception e) {
//                                LOG.error(e.getMessage());
                            }
                        }
                    });

            Label charsetLabel = new Label(grpXML, SWT.NONE);
            charsetLabel.setText(Messages.lblXMLFileCharset);
            charsetLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
            cboFileCharset = new Combo(grpXML, SWT.READ_ONLY);
            final GridData gdCharsetCombo = new GridData(SWT.FILL, SWT.CENTER, true, false);
            cboFileCharset.setLayoutData(gdCharsetCombo);
            cboFileCharset.setItems(
                    com.cubrid.cubridmigration.core.common.CharsetUtils.getCharsets());
            cboFileCharset.select(1);

            new Label(grpXML, SWT.NONE);
            new Label(grpXML, SWT.NONE);

            Label lblTZ = new Label(grpXML, SWT.NONE);
            lblTZ.setText(Messages.lblXMLFileTimezone);
            lblTZ.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

            cobTimezone = new Combo(grpXML, SWT.READ_ONLY);
            final GridData gdTimezoneCombo = new GridData(SWT.FILL, SWT.TOP, true, false);
            gdTimezoneCombo.widthHint = 600;
            cobTimezone.setLayoutData(gdTimezoneCombo);
            cobTimezone.setVisibleItemCount(20);
            List<String> allTimeZones = TimeZoneUtils.getTimeZonesList();
            cobTimezone.setItems(allTimeZones.toArray(new String[allTimeZones.size()]));
            cobTimezone.add(Messages.msgDefault, 0);
            cobTimezone.select(0);
        }

        /**
         * Retrieves the catalog.
         *
         * @return catalog
         */
        public Catalog getCatalog() {
            if ((parsedSource != null
                            && parsedSource.getFilePath().equals(txtXMLFileDir.getText().trim())
                            && parsedSource.getCharset().equalsIgnoreCase(cboFileCharset.getText()))
                    && xmlCatalog != null) {
                return xmlCatalog;
            } else {
                return getXmlCatalog(true);
            }
        }

        /**
         * return xml catalog
         *
         * @param hisFirst reading the parsing history firstly.
         * @return Catalog
         */
        private Catalog getXmlCatalog(boolean hisFirst) {
            // Reading parsing history firstly.
            String xmlFileCharset = cboFileCharset.getItem(cboFileCharset.getSelectionIndex());
            String xmlDir = txtXMLFileDir.getText();
            final File fileDir = new File(xmlDir);
            File[] files = fileDir.listFiles();
            
            if (!fileDir.isDirectory() || !(files != null && files.length > 0)) {
                MessageDialog.openError(
                        getShell(), Messages.msgError, Messages.errInvalidMysqlDumpFile);
                return null;
            }
            parsedSource = new XMLDirSource(xmlDir, xmlFileCharset);
            Catalog catalog = XMLDirSchemaProgressFetcher.fetch(parsedSource, hisFirst);
            if (catalog == null) {
                parsedSource = null;
                return null;
            }
            // Update timezone.
            String newTimezoneTxt = cobTimezone.getText();
            String timezone;
            if (newTimezoneTxt.equalsIgnoreCase(UIConstant.DEFAULT_TIME_ZONE)) {
                timezone = TimeZoneUtils.getDefaultID2GMT();
            } else {
                timezone = TimeZoneUtils.getGMTByDisplay(newTimezoneTxt);
            }
            catalog.setTimezone(timezone);
            return catalog;
        }

        /** Hide */
        public void hide() {
            if (grpXML == null) {
                return;
            }
            grpXML.setVisible(false);
            ((GridData) grpXML.getLayoutData()).exclude = true;
        }

        /** Initialize */
        public void init() {
            setTitle(
                    getMigrationWizard().getStepNoMsg(CreateSrcConnectionPage.this)
                            + Messages.msgSrcSelectMySQLDump);
            setMessage(Messages.msgSrcSelectMySQLDumpDes);

            final MigrationWizard wizard = getMigrationWizard();
            final MigrationConfiguration config = wizard.getMigrationConfig();
            // if load script and use MYSQL dump, load the XML catalog
            if (wizard.isLoadMigrationScript()) {
                parsedSource =
                        new XMLDirSource(
                                config.getSourceFileName(), config.getSourceFileEncoding());
                txtXMLFileDir.setText(
                        config.getSourceFileName() == null ? "" : config.getSourceFileName());
                cboFileCharset.setText(
                        config.getSourceFileEncoding() == null
                                ? ""
                                : config.getSourceFileEncoding());
                cobTimezone.setText(
                        config.getSourceFileTimeZone() == null
                                ? UIConstant.DEFAULT_TIME_ZONE
                                : config.getSourceFileTimeZone());
            }
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
            String xmlFile = config.getSourceFileName();
            String xmlFileCharset = config.getSourceFileEncoding();
            if (!(txtXMLFileDir.getText().equals(xmlFile)
                    && xmlFileCharset.equals(
                            cboFileCharset.getItem(cboFileCharset.getSelectionIndex())))) {
                srcDBChanged = true;
            }
            // online saved but but some of the parameters changed
            return srcDBChanged;
        }

        /**
         * Check and save
         *
         * @return true if saved.
         */
        public boolean save() {
            if (!checkInput()) {
                return false;
            }
            Catalog catalog = getCatalog();
            if (null == catalog) {
                return false;
            }
            final MigrationWizard wzd = getMigrationWizard();
            if (isInputChanged()) {
                // If it is a new migration, initialize the configuration
                wzd.resetBySourceDBChanged();
            }
            wzd.setOriginalSourceCatalog(catalog);
            MigrationConfiguration cfg = wzd.getMigrationConfig();

            if (cfg.getName() == null) {
                cfg.setName(
                        catalog.getDatabaseType().getName() + "-XML",
                        catalog.getName(),
                        cfg.getWizardStartDateTime());
            }

            cfg.setSourceType(MigrationConfiguration.XML);
            cfg.setSourceFileName(txtXMLFileDir.getText());
            cfg.setSourceFileEncoding(cboFileCharset.getItem(cboFileCharset.getSelectionIndex()));
            cfg.setSourceFileTimeZone(cobTimezone.getItem(cobTimezone.getSelectionIndex()));
            cfg.clearAllSQLTables();
            xmlCatalog = catalog;
            xmlCatalog.setCharset(cboFileCharset.getItem(cboFileCharset.getSelectionIndex()));
            xmlCatalog.setTimezone(cobTimezone.getItem(cobTimezone.getSelectionIndex()));
            cfg.setSrcCatalog(catalog, isInputChanged());
            return true;
        }

        /** Show */
        public void show() {
            if (grpXML == null) {
                return;
            }
            grpXML.setVisible(true);
            ((GridData) grpXML.getLayoutData()).exclude = false;
        }
    }

	/**
	 * Select online database as source.
	 *
	 * @author Kevin Cao
	 * @version 1.0 - 2013-6-3 created by Kevin Cao
	 */
//	private class SelectOnlineSrcView implements AbstractSourceView {
//
//		private final JDBCConnectionMgrView conMgrView;
//
//		private SelectOnlineSrcView() {
//			conMgrView =
//					new JDBCConnectionMgrView(
//							MigrationWizard.getSupportedSrcDBTypes(),
//							new IJDBCConnectionFilter() {
//
//								public boolean doFilter(ConnParameters cp) {
//									return getMigrationWizard().getMigrationConfig().getSourceType()
//											!= cp.getDatabaseType().getID();
//								}
//							});
//		}
//
//		/**
//		 * Create controls
//		 *
//		 * @param parent of the controls
//		 */
//		public void createControls(Composite parent) {
//			conMgrView.createControls(parent);
//		}
//
//		/**
//		 * get Catalog
//		 *
//		 * @return Catalog
//		 */
//		public Catalog getCatalog() {
//			return conMgrView.getCatalog();
//		}
//
//		/** Hide */
//		public void hide() {
//			conMgrView.hide();
//		}
//
//		/** Initialize with script's source connection */
//		public void init() {
//			MigrationWizard wzd = getMigrationWizard();
//			setTitle(wzd.getStepNoMsg(CreateSrcConnectionPage.this) + "sample title");
//			setMessage("sample description");
//			List<Integer> dts = new ArrayList<Integer>();
//			MigrationConfiguration cfg = wzd.getMigrationConfig();
//			dts.add(cfg.getSourceType());
//			conMgrView.setSupportedDBType(dts);
//			// Add catalog to cache.
//			Catalog offlineSrcCatalog = cfg.getOfflineSrcCatalog();
//			ConnParameters srcConParams = cfg.getSourceConParams();
//			conMgrView.init(srcConParams, offlineSrcCatalog);
//		}
//
//		/**
//		 * check whether the dialog changed
//		 *
//		 * @return true if content changed
//		 */
//		public boolean isInputChanged() {
//			boolean srcDBChanged = false;
//			MigrationConfiguration config = getMigrationWizard().getMigrationConfig();
//			// if online is saved but not selected or dumpfile is saved but not selected
//			ConnParameters oldCP = config.getSourceConParams();
//			// the first time set it changed
//			DatabaseConnectionInfo dci = conMgrView.getSelectedDCI();
//			if (oldCP == null && dci != null) {
//				srcDBChanged = true;
//			} else if (oldCP != null) {
//				srcDBChanged = !oldCP.isSameDB(dci.getConnParameters());
//			}
//			return srcDBChanged;
//		}
//
//		/**
//		 * Save to configurations
//		 *
//		 * @return true if successfully
//		 */
//		public boolean save() {
//			if (this.conMgrView.getSelectedDCI() == null) {
//				MessageDialog.openError(
//						getShell(), "Error", "No Selected Item");
//				return false;
//			}
//			final MigrationWizard wzd = getMigrationWizard();
//			Catalog catalog = getCatalog();
//			if (catalog == null) {
//				return false;
//			}
//
//			if (catalog.getDatabaseType().getID() == 1) {
//				removeEmptySchema(catalog);
//			}
//
//			List<String> errorSchemas = new ArrayList<String>();
//			Map<String, String> old2NewSchemaMapping = new HashMap<String, String>();
//			MigrationConfiguration cfg = wzd.getMigrationConfig();
//			cfg.resetSchemaInfo();
//			if (catalog.getDatabaseType().isSupportMultiSchema()
//					&& !cfg.getExpEntryTableCfg().isEmpty()) {
//				List<String> expSchemas = cfg.getExpSchemaNames();
//				for (String schema : expSchemas) {
//					if (catalog.getSchemaByName(schema) != null) {
//						continue;
//					}
//					errorSchemas.add(schema);
//				}
//				if (!errorSchemas.isEmpty()) {
//					List<String> newSchemas = new ArrayList<String>();
//					for (Schema newSchema : catalog.getSchemas()) {
//						newSchemas.add(newSchema.getName());
//					}
//					old2NewSchemaMapping =
//							RenameSchemaDialog.renameSchemas(errorSchemas, newSchemas);
//					// Dialog canceled, user maybe want to choose another source.
//					if (old2NewSchemaMapping == null) {
//						return false;
//					}
//				}
//			}
//
//			// create configuration name
//			if (cfg.getName() == null) {
//				cfg.setName(
//						catalog.getDatabaseType().getName(),
//						catalog.getName(),
//						cfg.getWizardStartDateTime());
//			}
//
//			if (isInputChanged() || wzd.getOriginalSourceCatalog() != catalog) {
//				// If it is a new migration, initialize the configuration
//				wzd.resetBySourceDBChanged();
//				cfg = wzd.getMigrationConfig();
//			}
//			wzd.setOriginalSourceCatalog(catalog);
//			cfg.setSourceConParams(catalog.getConnectionParameters());
//			// Set the invalid schema to right schema or remove them.
//			for (String es : errorSchemas) {
//				String newSchema = old2NewSchemaMapping.get(es);
//				if (StringUtils.isBlank(newSchema)) {
//					cfg.removeExpSchema(es);
//				} else {
//					cfg.renameExpSchema(es, newSchema);
//				}
//			}
//			return true;
//		}
//
//		/**
//		 * Remove empty Schema
//		 *
//		 * @param catalog Catalog
//		 */
//		private void removeEmptySchema(Catalog catalog) {
//			List<Schema> schemaList = catalog.getSchemas();
//			List<Schema> removeSchema = new ArrayList<Schema>();
//
//			for (Schema schema : schemaList) {
//				List<Table> tableList = schema.getTables();
//				List<View> viewList = schema.getViews();
//				List<Sequence> sequenceList = schema.getSequenceList();
//				List<Synonym> synonymList = schema.getSynonymList();
//				List<Grant> grantList = schema.getGrantList();
//
//				if (tableList.isEmpty()
//						&& viewList.isEmpty()
//						&& sequenceList.isEmpty()
//						&& synonymList.isEmpty()
//						&& grantList.isEmpty()) {
//					removeSchema.add(schema);
//				}
//			}
//
//			catalog.removeSchema(removeSchema);
//		}
//
//		/** Show */
//		public void show() {
//			conMgrView.show();
//		}
//	}
//
//	private static final Logger LOG = LogUtil.getLogger(CreateSrcConnectionPage.class);
//	private AbstractSourceView onlineView = new SelectOnlineSrcView();
	private AbstractSourceView xmlView = new SelectXMLSrcView();
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
//		onlineView.createControls(container);
//		onlineView.show();
//		container.layout(true);
//		onlineView.init();
		
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
