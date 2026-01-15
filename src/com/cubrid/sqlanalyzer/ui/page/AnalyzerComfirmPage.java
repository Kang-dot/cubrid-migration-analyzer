package com.cubrid.sqlanalyzer.ui.page;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;

import com.cubrid.cubridmigration.ui.SWTResourceConstents;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.dbobject.AnalyzerCatalog;
import com.cubrid.sqlanalyzer.ui.AnalyzerWizard;
import com.cubrid.sqlanalyzer.ui.AnalyzerWizardPage;
import com.cubrid.sqlanalyzer.ui.page.service.AnalyzerConfirmTextBuilder;
import com.cubrid.sqlanalyzer.ui.page.service.IAnalyzerConfirmTextBuilder;

import jakarta.inject.Inject;

public class AnalyzerComfirmPage extends AnalyzerWizardPage {
	private boolean isScriptSaved;
	private Button btnSaveSchema;
	protected StyledText txtContent;
	protected Composite comRoot;
	protected ToolBar tbTools;
	
	/**
	 * Service responsible for string processing logic.
	 * Injected using Eclipse 4.x DI, or can be replaced with mock objects for testing.
	 */
	@Inject
	private IAnalyzerConfirmTextBuilder textBuilder;

	public AnalyzerComfirmPage(String pageName) {
		super(pageName);
	}
	
	/**
	 * Initializes the text builder.
	 * Uses Eclipse 4.x DI context if available, otherwise creates instance directly.
	 * Called from createControl to handle cases where @Inject injection fails.
	 */
	private void initializeTextBuilder() {
		if (textBuilder != null) {
			// Already injected via @Inject
			return;
		}
		
		try {
			// Attempts injection via Eclipse 4.x DI context
			IEclipseContext context = getEclipseContext();
			if (context != null) {
				// Injects @Inject fields using ContextInjectionFactory.inject()
				ContextInjectionFactory.inject(this, context);
				// Creates instance directly if still null after injection
				if (textBuilder == null) {
					textBuilder = ContextInjectionFactory.make(AnalyzerConfirmTextBuilder.class, context);
				}
			} else {
				// Creates instance directly if context is unavailable (legacy compatibility)
				textBuilder = new AnalyzerConfirmTextBuilder();
			}
		} catch (Exception e) {
			// Creates instance directly on DI failure (legacy compatibility)
			if (textBuilder == null) {
				textBuilder = new AnalyzerConfirmTextBuilder();
			}
		}
	}
	
	/**
	 * Retrieves Eclipse 4.x context.
	 * May return null in Eclipse 3.x environments.
	 *
	 * @return IEclipseContext or null
	 */
	private IEclipseContext getEclipseContext() {
		// Logic to retrieve Eclipse 4.x context
		// Currently returns null to operate in legacy mode
		// Can add logic to retrieve Eclipse 4.x context if needed
		// Example: PlatformUI.getWorkbench().getService(IEclipseContext.class)
		return null;
	}
	
	/**
	 * Sets the text builder. (Method for testing convenience)
	 *
	 * @param textBuilder IAnalyzerConfirmTextBuilder
	 */
	public void setTextBuilder(IAnalyzerConfirmTextBuilder textBuilder) {
		this.textBuilder = textBuilder;
	}

	/**
	 * Create contents of the wizard
	 *
	 * @param parent Composite
	 */
	@Override
	public void createControl(Composite parent) {
		// Initializes to handle cases where @Inject injection fails
		initializeTextBuilder();
		
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		setControl(container);

		Composite container2 = new Composite(container, SWT.BORDER);
		container2.setLayout(new GridLayout());
		container2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		txtContent =
				new StyledText(
						container2,
						SWT.LEFT | SWT.BORDER | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
		txtContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txtContent.setBackground(SWTResourceConstents.COLOR_WHITE);
//		createButtons(container);
	}

	/**
	 * Create buttons in this page
	 *
	 * @param parent of the buttons
	 */
//	protected void createButtons(Composite parent) {
//		comRoot = new Composite(parent, SWT.BORDER);
//		comRoot.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
//		comRoot.setLayout(new GridLayout(2, false));
//
//		tbTools = new ToolBar(comRoot, SWT.WRAP | SWT.RIGHT | SWT.FLAT);
//		tbTools.setLayout(new GridLayout());
//		tbTools.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, true, false));
//
//		new ToolItem(tbTools, SWT.SEPARATOR);
//		btnPreviewDDL = new ToolItem(tbTools, SWT.CHECK);
//		btnPreviewDDL.setSelection(false);
//		btnPreviewDDL.setText("Preview DDL");
//		btnPreviewDDL.setToolTipText("Preview DDL");
//		btnPreviewDDL.addSelectionListener(
//				new SelectionAdapter() {
//
//					public void widgetSelected(final SelectionEvent event) {
//						boolean flag = btnPreviewDDL.getSelection();
//						switchText(flag);
//					}
//				});
//		new ToolItem(tbTools, SWT.SEPARATOR);
//		btnSaveSchema = new Button(comRoot, SWT.CHECK);
//		btnSaveSchema.setText("Save Source Catalog");
//		btnSaveSchema.setToolTipText("Save Source Catalog to Script");
//		btnSaveSchema.addSelectionListener(
//				new SelectionAdapter() {
//
//					public void widgetSelected(SelectionEvent ev) {
//						AnalyzerWizard wzd = getMigrationWizard();
//						if (wzd != null) {
//							wzd.setSaveSchema(btnSaveSchema.getSelection());
//						}
//					}
//				});
//	}

	/**
	 * When migration wizard displayed current page.
	 *
	 * @param event PageChangedEvent
	 */
	@Override
	protected void afterShowCurrentPage(PageChangedEvent event) {
		try {
			AnalyzerWizard wzd = getMigrationWizard();
			if (wzd != null) {
				setTitle("Confirm Settings");
				setDescription("Confirm the settings before proceeding");
				isScriptSaved = false;
				if (btnSaveSchema != null) {
					// Add initialization logic for btnSaveSchema if needed
					wzd.setSaveSchema(btnSaveSchema.getSelection());
				}
				
				// Calls string processing logic and updates view
				updateViewWithText(wzd);
			}
		} catch (RuntimeException e) {
			// Add logging if needed
			throw e;
		} finally {
			isFirstVisible = false;
		}
	}
	
	/**
	 * Calls buildText to generate text and update view.
	 * String processing logic and view update logic are separated.
	 *
	 * @param wizard AnalyzerWizard
	 */
	protected void updateViewWithText(AnalyzerWizard wizard) {
		if (textBuilder == null) {
			initializeTextBuilder();
		}
		
		if (textBuilder != null && wizard != null) {
			// Calls string processing logic (business logic)
			String text = textBuilder.buildText(wizard);
			
			// Updates view (display logic)
			updateTextView(text);
		}
	}
	
	/**
	 * Displays generated text result in view.
	 * Only responsible for view update logic.
	 *
	 * @param text rendered confirmation text
	 */
	protected void updateTextView(String text) {
		if (text == null) {
			return;
		}
		
		if (txtContent != null && !txtContent.isDisposed()) {
			txtContent.setText(text);
		}
	}

	/**
	 * Handle page leaving
	 *
	 * @param event PageChangingEvent
	 */
	@Override
	protected void handlePageLeaving(PageChangingEvent event) {
		if (!isGotoNextPage(event) && isScriptSaved) {
			// Add script saving logic if needed
		}
		saveDictionaryInConfig();
		super.handlePageLeaving(event);
	}

	/** Prepare for saving migration script. */
	protected void prepare4SaveScript() {
		isScriptSaved = true;
		// Add script saving preparation logic if needed
	}

	protected boolean isSaveSchema() {
		return btnSaveSchema != null && btnSaveSchema.getSelection();
	}
	
	private void saveDictionaryInConfig() {
		AnalyzerCatalog analyzerCatalog =
				((AnalyzerCatalog) getMigrationWizard().getOriginalSourceCatalog());
		
		AnalyzerConfiguration analyzerConfig = getMigrationWizard().getAnalyzerConfig();
		
		analyzerConfig.setQueryDict(analyzerCatalog.getQueryDictionary());
	}
}
