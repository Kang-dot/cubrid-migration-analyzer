package com.cubrid.sqlanalyzer.ui.page;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.cubrid.cubridmigration.core.connection.ConnParameters;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.ui.database.IJDBCConnectionFilter;
import com.cubrid.cubridmigration.ui.database.JDBCConnectionMgrView;
import com.cubrid.cubridmigration.ui.wizard.MigrationWizard;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.ui.AnalyzerWizard;
import com.cubrid.sqlanalyzer.ui.AnalyzerWizardPage;

public class CreateTarConnectionPage extends AnalyzerWizardPage {
	
	private interface AbstractSelectView {
		void createControls(Composite parent);
		void hide();
		void init();
		boolean isInputChanged();
		boolean save();
		void show();
	}
	
	/**
	 * OnlineTargetDBView provides settings exporting to a online CUBRID DB.
	 *
	 * @author Kevin Cao
	 * @version 1.0 - 2012-10-9 created by Kevin Cao
	 */
	private class XMLDirSelectView implements AbstractSelectView {
		private final JDBCConnectionMgrView conMgrView;
		
		private Button btnCreateConstrainsNow;

		private XMLDirSelectView() {
			conMgrView =
					new JDBCConnectionMgrView(
							MigrationWizard.getSupportedTarDBTypes(),
							new IJDBCConnectionFilter() {

								public boolean doFilter(ConnParameters cp) {
									final MigrationConfiguration cfg =
											getMigrationWizard().getMigrationConfig();
									return false;
								}
							});
		}

		/**
		 * Create Controls
		 *
		 * @param parent Composite
		 */
		public void createControls(Composite parent) {
			if (btnCreateConstrainsNow != null) {
				return;
			}
			conMgrView.createControls(parent);

			Composite container2 = new Composite(conMgrView.getComposite(), SWT.NONE);
			container2.setLayout(new GridLayout());
			container2.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		}

		/** initial the page set which option is visiable and updateDialogStatus */
		public void init() {
			setTitle(
					getMigrationWizard().getStepNoMsg(CreateTarConnectionPage.this)
							+ "Select Target Online CUBRID DB");
			setDescription("Select target online CUBRID database");
			final MigrationConfiguration config = getMigrationWizard().getMigrationConfig();
			conMgrView.init(config.getTargetConParams(), null);
		}

		/**
		 * Save UI
		 *
		 * @return true if saving successfully
		 */
		public boolean save() {
			if (conMgrView.getSelectedDCI() == null) {
				MessageDialog.openError(
						getShell(), "Error", "No Selected Item");
				return false;
			}
			final AnalyzerWizard wzd = getMigrationWizard();
			final MigrationConfiguration config = wzd.getAnalyzerConfig();
			ConnParameters connParameters = conMgrView.getSelectedDCI().getConnParameters();
			config.setTargetConParams(connParameters);
            
            Catalog catalog = conMgrView.getCatalog();
            if (catalog == null) {
                return false;
            }
            wzd.setTargetCatalog(catalog);

			return true;
		}

		/** displayOnlineContainer */
		public void show() {
			conMgrView.show();
		}

		/** Hide view */
		@Override
		public void hide() {
			conMgrView.hide();
		}

		@Override
		public boolean isInputChanged() {
			// TODO Auto-generated method stub
			return false;
		}
	}
	
	/**
	 * ParserSelectView provides settings for selecting parser location.
	 *
	 * @author Generated
	 * @version 1.0 - Created
	 */
	private class ParserSelectView implements AbstractSelectView {
		private Text txtParserLocation;
		private Button btnBrowse;
		private Group grpParser;

		/**
		 * Validate the input.
		 *
		 * @return true if all are valid.
		 */
		private boolean checkInput() {
			if (StringUtils.isBlank(txtParserLocation.getText())) {
				MessageDialog.openError(
						getShell(), "Error", "No parser location selected");
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
			if (grpParser != null) {
				return;
			}
			grpParser = new Group(parent, SWT.SHADOW_ETCHED_IN);
			grpParser.setLayout(new GridLayout(3, false));
			GridData groupGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
			groupGridData.heightHint = 98;
			grpParser.setLayoutData(groupGridData);

			Label parserLocationLabel = new Label(grpParser, SWT.NONE);
			parserLocationLabel.setText("Parser Location:");
			parserLocationLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

			txtParserLocation = new Text(grpParser, SWT.BORDER | SWT.READ_ONLY);
			GridData gdParserLocationTxt = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gdParserLocationTxt.grabExcessHorizontalSpace = true;
			txtParserLocation.setLayoutData(gdParserLocationTxt);

			btnBrowse = new Button(grpParser, SWT.NONE);
			GridData parserButtonGd = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
			parserButtonGd.minimumWidth = 70;
			btnBrowse.setLayoutData(parserButtonGd);
			btnBrowse.setText("Browse_file_location");
			btnBrowse.addSelectionListener(
					new SelectionAdapter() {
						public void widgetSelected(final SelectionEvent event) {
							if (!checkInput()) {
								return;
							}
						}
					});
		}

		/** Hide */
		public void hide() {
			if (grpParser == null) {
				return;
			}
			grpParser.setVisible(false);
			((GridData) grpParser.getLayoutData()).exclude = true;
		}

		/** Initialize */
		public void init() {
			setTitle(
					getMigrationWizard().getStepNoMsg(CreateTarConnectionPage.this)
							+ "Select Parser Location");
			setDescription("Select parser location");
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
			// TODO: Implement save logic
			return true;
		}

		/** Show */
		public void show() {
			if (grpParser == null) {
				return;
			}
			grpParser.setVisible(true);
			((GridData) grpParser.getLayoutData()).exclude = false;
		}

		@Override
		public boolean isInputChanged() {
			// TODO Auto-generated method stub
			return false;
		}
	}

	private XMLDirSelectView XMLDirSelectView = new XMLDirSelectView();
	private ParserSelectView parserSelectView = new ParserSelectView();
	
	private Composite container;

	public CreateTarConnectionPage(String pageName) {
		super(pageName);
	}
	
	/**
	 * When migration wizard displayed current page.
	 *
	 * @param event PageChangedEvent
	 */
	protected void afterShowCurrentPage(PageChangedEvent event) {
		try {
			final AbstractSelectView crtView = getCrtView();
			crtView.createControls(container);
			parserSelectView.hide();
			XMLDirSelectView.hide();
			crtView.init();
			crtView.show();
			container.layout();
		} catch (Exception ex) {
			MessageDialog.openError(getShell(), getTitle(), getErrorMessage());
		}
	}

	/**
	 * Retrieves current target view based on configuration
	 *
	 * @return AbstractSelectView
	 */
	private AbstractSelectView getCrtView() {
		AnalyzerWizard wizard = getMigrationWizard();
		AnalyzerConfiguration config = wizard.getAnalyzerConfig();
		int destType = config.getDestType();
		if (destType == AnalyzerConfiguration.TARGET_TYPE_CUBRID) {
			return XMLDirSelectView;
		} else if (destType == AnalyzerConfiguration.TARGET_TYPE_PARSER) {
			return parserSelectView;
		}
		throw new RuntimeException("Error destination configuration.");
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
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		setControl(container);

		XMLDirSelectView = new XMLDirSelectView();
		parserSelectView = new ParserSelectView();
	}
	
	protected void handlePageLeaving(PageChangingEvent event) {
        if (!isPageComplete()) {
            return;
        }
        if (isGotoNextPage(event)) {
            event.doit = updateMigrationConfig();
        }
	}

	/**
	 * Save user input (target database connection information) to export options.
	 *
	 * @return true if update success.
	 */
	@Override
	protected boolean updateMigrationConfig() {
		return getCrtView().save();
	}
}
