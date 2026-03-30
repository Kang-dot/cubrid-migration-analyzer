package com.cubrid.sqlanalyzer.ui.page.view;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
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
import com.cubrid.sqlanalyzer.ui.AnalyzerWizard;
import com.cubrid.sqlanalyzer.ui.page.CreateSrcConnectionPage;
import com.cubrid.sqlanalyzer.xmlmetadata.XMLDirSchemaProgressFetcher;
import com.cubrid.sqlanalyzer.xmlmetadata.XMLDirSource;

/**
 * AnalyzerSrcFileSelectView
 *
 * @author Kevin Cao
 * @version 1.0 - 2013-6-3 created by Kevin Cao
 */
public class AnalyzerSrcFileSelectView implements AbstractSourceView {
	private CreateSrcConnectionPage parentPage;
	private Text txtXMLFileDir;
	private Button btnBrowse;
	private Button btnAnalyz;
	private Combo cboFileCharset;
	private Combo cobTimezone;
	private Group grpXML;
	private XMLDirSource parsedSource;
	private Catalog xmlCatalog = null;

	public AnalyzerSrcFileSelectView(CreateSrcConnectionPage parentPage) {
		this.parentPage = parentPage;
	}

	/**
	 * Validate the char-set input.
	 *
	 * @return true if all are valid.
	 */
	private boolean checkCharset() {
		if (StringUtils.isBlank(cboFileCharset.getItem(cboFileCharset.getSelectionIndex()))) {
			MessageDialog.openError(parentPage.getShell(), Messages.msgError,
					Messages.sourceDBPageErrNoSetXMLFileCharset);
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
			MessageDialog.openError(parentPage.getShell(), Messages.msgError,
					Messages.sourceDBPageErrNoSelectedXMLFile);
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
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (!checkCharset()) {
					return;
				}
				DirectoryDialog dlg = new DirectoryDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(),
						SWT.SINGLE | SWT.OPEN);
				dlg.setFilterPath(".");
				final String fileDirPath = dlg.open();
				if (fileDirPath == null) {
					return;
				}
				txtXMLFileDir.setText(fileDirPath);
				btnAnalyz.setEnabled(true);
				try {
					xmlCatalog = getXmlCatalog(true);
				} catch (Exception e) {
					// LOG.error(e.getMessage());
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
		btnAnalyz.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (!checkInput()) {
					return;
				}
				try {
					xmlCatalog = getXmlCatalog(false);
					if (null != xmlCatalog) {
						parentPage.getMigrationWizard().resetBySourceDBChanged();
					}
				} catch (Exception e) {
					// LOG.error(e.getMessage());
				}
			}
		});

		Label charsetLabel = new Label(grpXML, SWT.NONE);
		charsetLabel.setText(Messages.lblXMLFileCharset);
		charsetLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		cboFileCharset = new Combo(grpXML, SWT.READ_ONLY);
		final GridData gdCharsetCombo = new GridData(SWT.FILL, SWT.CENTER, true, false);
		cboFileCharset.setLayoutData(gdCharsetCombo);
		cboFileCharset.setItems(com.cubrid.cubridmigration.core.common.CharsetUtils.getCharsets());
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
		if ((parsedSource != null && parsedSource.getFilePath().equals(txtXMLFileDir.getText().trim())
				&& parsedSource.getCharset().equalsIgnoreCase(cboFileCharset.getText())) && xmlCatalog != null) {
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
			MessageDialog.openError(parentPage.getShell(), Messages.msgError, Messages.errInvalidMysqlDumpFile);
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
		parentPage.setTitle(parentPage.getMigrationWizard().getStepNoMsg(parentPage) + Messages.msgSrcSelectMySQLDump);
		parentPage.setMessage(Messages.msgSrcSelectMySQLDumpDes);

		final AnalyzerWizard wizard = parentPage.getMigrationWizard();
		final MigrationConfiguration config = wizard.getMigrationConfig();
		// if load script and use MYSQL dump, load the XML catalog
		if (wizard.isLoadMigrationScript()) {
			parsedSource = new XMLDirSource(config.getSourceFileName(), config.getSourceFileEncoding());
			txtXMLFileDir.setText(config.getSourceFileName() == null ? "" : config.getSourceFileName());
			cboFileCharset.setText(config.getSourceFileEncoding() == null ? "" : config.getSourceFileEncoding());
			cobTimezone.setText(config.getSourceFileTimeZone() == null ? UIConstant.DEFAULT_TIME_ZONE
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
		MigrationConfiguration config = parentPage.getMigrationWizard().getMigrationConfig();
		// if online is saved but not selected or dumpfile is saved but not selected
		String xmlFile = config.getSourceFileName();
		String xmlFileCharset = config.getSourceFileEncoding();
		if (!(txtXMLFileDir.getText().equals(xmlFile)
				&& xmlFileCharset.equals(cboFileCharset.getItem(cboFileCharset.getSelectionIndex())))) {
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
		final AnalyzerWizard wzd = parentPage.getMigrationWizard();
		// if (isInputChanged()) {
		// // If it is a new migration, initialize the configuration
		// wzd.resetBySourceDBChanged();
		// }
		wzd.setTempCatalog(catalog);
		// MigrationConfiguration cfg = wzd.getMigrationConfig();
		MigrationConfiguration cfg = wzd.getMigrationConfig();

		if (cfg.getName() == null) {
			cfg.setName(catalog.getDatabaseType().getName() + "-XML", catalog.getName(), cfg.getWizardStartDateTime());
		}

//		cfg.setSourceType(MigrationConfiguration.XML);
		cfg.setSourceFileName(txtXMLFileDir.getText());
		cfg.setSourceFileEncoding(cboFileCharset.getItem(cboFileCharset.getSelectionIndex()));
		cfg.setSourceFileTimeZone(cobTimezone.getItem(cobTimezone.getSelectionIndex()));
		cfg.clearAllSQLTables();
		xmlCatalog = catalog;
		xmlCatalog.setCharset(cboFileCharset.getItem(cboFileCharset.getSelectionIndex()));
		xmlCatalog.setTimezone(cobTimezone.getItem(cobTimezone.getSelectionIndex()));
		cfg.setSrcCatalog(catalog, isInputChanged());

		wzd.setSourceDBNode(xmlCatalog);

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
