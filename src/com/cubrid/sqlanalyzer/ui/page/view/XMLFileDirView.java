package com.cubrid.sqlanalyzer.ui.page.view;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.cubrid.cubridmigration.core.connection.ConnParameters;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.ui.database.IJDBCConnectionFilter;
import com.cubrid.cubridmigration.ui.database.JDBCConnectionMgrView;
import com.cubrid.cubridmigration.ui.wizard.MigrationWizard;
import com.cubrid.sqlanalyzer.ui.AnalyzerWizard;
import com.cubrid.sqlanalyzer.ui.page.CreateTarConnectionPage;

public class XMLFileDirView {
//	private final JDBCConnectionMgrView conMgrView;
//
//	private Button btnCreateConstrainsNow;
//	
//	private AnalyzerWizard analyzerWizard;
//
//	private XMLFileDirView(AnalyzerWizard analyzerWizard) {
//		this.analyzerWizard = analyzerWizard;
//		
//		conMgrView = new JDBCConnectionMgrView(MigrationWizard.getSupportedTarDBTypes(), new IJDBCConnectionFilter() {
//
//			public boolean doFilter(ConnParameters cp) {
//				final MigrationConfiguration cfg = analyzerWizard.getMigrationConfig();
//				return false;
//			}
//		});
//	}
//
//	/**
//	 * Create Controls
//	 *
//	 * @param parent Composite
//	 */
//	public void createControls(Composite parent) {
//		if (btnCreateConstrainsNow != null) {
//			return;
//		}
//		conMgrView.createControls(parent);
//
//		Composite container2 = new Composite(conMgrView.getComposite(), SWT.NONE);
//		container2.setLayout(new GridLayout());
//		container2.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
//	}
//
//	/**
//	 * Save UI
//	 *
//	 * @return true if saving successfully
//	 */
//	public boolean save() {
//		if (conMgrView.getSelectedDCI() == null) {
//			MessageDialog.openError(getShell(), "Error", "No Selected Item");
//			return false;
//		}
//		final AnalyzerWizard wzd = getMigrationWizard();
//		final MigrationConfiguration config = wzd.getAnalyzerConfig();
//		ConnParameters connParameters = conMgrView.getSelectedDCI().getConnParameters();
//		config.setTargetConParams(connParameters);
//
//		Catalog catalog = conMgrView.getCatalog();
//		if (catalog == null) {
//			return false;
//		}
//		wzd.setTargetCatalog(catalog);
//
//		return true;
//	}
//
//	/** displayOnlineContainer */
//	public void show() {
//		conMgrView.show();
//	}
}
