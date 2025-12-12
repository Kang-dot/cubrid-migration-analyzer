package com.cubrid.sqlanalyzer.ui.page;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.cubrid.cubridmigration.core.connection.ConnParameters;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.ui.common.UICommonTool;
import com.cubrid.cubridmigration.ui.database.IJDBCConnectionFilter;
import com.cubrid.cubridmigration.ui.database.JDBCConnectionMgrView;
import com.cubrid.cubridmigration.ui.wizard.MigrationWizard;
import com.cubrid.cubridmigration.ui.wizard.utils.MigrationCfgUtils;
import com.cubrid.sqlanalyzer.ui.AnalyzerWizardPage;

public class CreateTarConnectionPage extends AnalyzerWizardPage {
	
	/**
	 * OnlineTargetDBView provides settings exporting to a online CUBRID DB.
	 *
	 * @author Kevin Cao
	 * @version 1.0 - 2012-10-9 created by Kevin Cao
	 */
	private class OnlineTargetDBView {
		private final int USERSCHEMA_VERSION = 112;
		private final JDBCConnectionMgrView conMgrView;

		private Button btnWriteErrorRecords;
		private Button btnCreateConstrainsNow;
		private Button btnUpdateStatistics;

		private OnlineTargetDBView() {
			conMgrView =
					new JDBCConnectionMgrView(
							MigrationWizard.getSupportedTarDBTypes(),
							new IJDBCConnectionFilter() {

								public boolean doFilter(ConnParameters cp) {
									final MigrationConfiguration cfg =
											getMigrationWizard().getMigrationConfig();
//									if (cfg.sourceIsOnline()) {
//										return cfg.getSourceConParams().isSameDB(cp);
//									}
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
			final MigrationWizard wzd = getMigrationWizard();
			final MigrationConfiguration config = wzd.getMigrationConfig();
			ConnParameters connParameters = conMgrView.getSelectedDCI().getConnParameters();
			// connParameters.setTimeZone(onLineTimezoneCombo.getItem(onLineTimezoneCombo.getSelectionIndex()));
			config.setTargetConParams(connParameters);
			config.setWriteErrorRecords(btnWriteErrorRecords.getSelection());
			config.setUpdateStatistics(btnUpdateStatistics.getSelection());

			Catalog catalog = conMgrView.getCatalog();

			if (catalog == null) {
				return false;
			}

			int targetCubridVersion =
					(catalog.getVersion().getDbMajorVersion() * 10)
							+ (catalog.getVersion().getDbMinorVersion());
			config.setTargetDBVersion(String.valueOf(targetCubridVersion));
			config.setAddUserSchema(targetCubridVersion >= USERSCHEMA_VERSION);

			if (null != catalog) {
				wzd.setTargetCatalog(catalog);
				config.setTarSchemaSize(catalog.getSchemas().size());

				if (!btnCreateConstrainsNow.getSelection()
						&& MigrationCfgUtils.isHACUBRID(config)
						&& UICommonTool.openConfirmBox("PK HA database alert")) {
					btnCreateConstrainsNow.setSelection(true);
				}
			}
			config.setTargetDBAGroup(catalog.isDBAGroup());
			config.setCreateConstrainsBeforeData(btnCreateConstrainsNow.getSelection());
			return true;
		}

		/** displayOnlineContainer */
		public void show() {
			conMgrView.show();
		}
	}

	private OnlineTargetDBView onlineTargetDBView;
	private Composite container;

	public CreateTarConnectionPage(String pageName) {
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
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		setControl(container);

		onlineTargetDBView = new OnlineTargetDBView();
		
		// UI 컨트롤들을 즉시 생성
		onlineTargetDBView.createControls(container);
		onlineTargetDBView.init();
		onlineTargetDBView.show();
		container.layout();
	}

	/**
	 * Save user input (target database connection information) to export options.
	 *
	 * @return true if update success.
	 */
	@Override
	protected boolean updateMigrationConfig() {
		return onlineTargetDBView.save();
	}
}
