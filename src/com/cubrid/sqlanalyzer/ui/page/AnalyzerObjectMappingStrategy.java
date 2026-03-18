package com.cubrid.sqlanalyzer.ui.page;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.cubrid.common.ui.navigator.ICUBRIDNode;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.ui.common.navigator.node.ColumnsNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.FKsNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.IndexesNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.TableNode;
import com.cubrid.cubridmigration.ui.message.Messages;
import com.cubrid.cubridmigration.ui.wizard.dialog.AdjustCharColumnDialog;
import com.cubrid.cubridmigration.ui.wizard.dialog.TableIndexSelectorDialog;
import com.cubrid.cubridmigration.ui.wizard.page.view.AbstractMappingView;
import com.cubrid.cubridmigration.ui.wizard.page.view.IRefreshableView;
import com.cubrid.cubridmigration.ui.wizard.page.view.TableMappingView;
import com.cubrid.cubridmigration.ui.wizard.utils.MigrationCfgUtils;
import com.cubrid.cubridmigration.ui.wizard.utils.VerifyResultMessages;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.DefaultNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.DeleteNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.IAnalyzerNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.InsertNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.SelectNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.UpdateNode;
import com.cubrid.sqlanalyzer.ui.AnalyzerWizard;
import com.cubrid.sqlanalyzer.ui.page.view.AnalyzerDMLTreeNodeView;
import com.cubrid.sqlanalyzer.ui.page.view.AnalyzerObjectTableMappingView;

public class AnalyzerObjectMappingStrategy implements IObjectMappingStrategy, IRefreshableView {

    private AnalyzerObjectMappingPage page;
    private Composite container;

    private AnalyzerDMLTreeNodeView tvSourceDBObjects;
    private final Map<String, AbstractMappingView> node2ViewMapping = new HashMap<String, AbstractMappingView>();
    private AbstractMappingView currentView;
    private final MigrationCfgUtils util = new MigrationCfgUtils();
    private boolean isFirstVisible = true;

    public AnalyzerObjectMappingStrategy(AnalyzerObjectMappingPage page) {
        this.page = page;
    }

    @Override
    public void createControl(Composite parent) {
        container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout());
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        SashForm sashForm = new SashForm(container, SWT.HORIZONTAL);
        sashForm.setLayout(new FillLayout());
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        createTreeView(sashForm);
        createDetailPanel(sashForm);
        sashForm.setWeights(new int[] {1, 3});
        createToolButtons(container);
    }

    @Override
    public Composite getContainer() {
        return container;
    }

    @Override
    public void afterShowCurrentPage() {
        final AnalyzerWizard mw = page.getMigrationWizard();
        page.setTitle(mw.getStepNoMsg(page) + Messages.objectMapPageTitle);
        page.setDescription(Messages.objectMapPageDescription);
        setSourceTableNoPKWarningMessage();
        page.setErrorMessage(null);
        mw.refreshWizardStatus();
        util.setTargetCatalog(mw.getTargetCatalog(), mw);
        
        try {
            Catalog sourceCatalog = mw.getSourceCatalog();
            Catalog targetCatalog = mw.getTargetCatalog();
            final MigrationConfiguration cfg = mw.getMigrationConfig();

            for (AbstractMappingView amv : node2ViewMapping.values()) {
                amv.setMigrationConfig(cfg);
                amv.setWizardStatus(mw);
            }
            util.setMigrationConfiguration(cfg);

            refreshTreeView();
            page.getShell().setMaximized(true);
            isFirstVisible = false;

            if (!cfg.hasObjects2Export()) {
                cfg.setAll(true);
                refreshCurrentView();
            }
        } catch (RuntimeException ex) {
            throw ex;
        }
    }

    @Override
    public boolean handlePageLeaving(PageChangingEvent event) {
        return validateConfig();
    }

    protected void createDetailPanel(Composite parent) {
        Group detailContainer = new Group(parent, SWT.NONE);
        detailContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        detailContainer.setLayout(new GridLayout());
        detailContainer.setText(Messages.dbObjectSelectMapping);

        AnalyzerObjectTableMappingView analyzerObjMappingView = new AnalyzerObjectTableMappingView(detailContainer);
        analyzerObjMappingView.addTabSelectionListener(node -> tvSourceDBObjects.setSelection(node));
        analyzerObjMappingView.addDoubleClickListener(event -> {
            if (event.getSelection().isEmpty()) return;
            Object first = ((StructuredSelection) event.getSelection()).getFirstElement();
            if (!(first instanceof AnalyzerObjectTableMappingView.DmlRow)) return;
            AnalyzerObjectTableMappingView.DmlRow row = (AnalyzerObjectTableMappingView.DmlRow) first;
            IAnalyzerNode targetNode = row.getNode();
            if (targetNode == null) return;
            tvSourceDBObjects.setSelection(targetNode);
            showRightView(targetNode, true);
        });

        TableMappingView tableMappingView = new TableMappingView(detailContainer);
        tableMappingView.addDoubleClickListener(event -> {
            if (event.getSource() == null || event.getSelection().isEmpty()) return;
            Object[] obj = (Object[]) ((StructuredSelection) event.getSelection()).getFirstElement();
            TableViewer tv = (TableViewer) event.getSource();
            String ct = tv.getData(AbstractMappingView.CONTENT_TYPE).toString();
            ICUBRIDNode cn = (ICUBRIDNode) currentView.getModel();
            while (cn != null) {
                if (cn instanceof TableNode) break;
                cn = cn.getParent();
            }
            if (cn == null) return;
            ICUBRIDNode selectionParent = cn;
            if (AbstractMappingView.CT_COLUMN.equals(ct)) {
                for (ICUBRIDNode chn : cn.getChildren()) {
                    if (chn instanceof ColumnsNode) {
                        selectionParent = chn;
                        break;
                    }
                }
            } else if (AbstractMappingView.CT_FK.equals(ct)) {
                for (ICUBRIDNode chn : cn.getChildren()) {
                    if (chn instanceof FKsNode) {
                        selectionParent = chn;
                        break;
                    }
                }
            } else if (AbstractMappingView.CT_INDEX.equals(ct)) {
                for (ICUBRIDNode chn : cn.getChildren()) {
                    if (chn instanceof IndexesNode) {
                        selectionParent = chn;
                        break;
                    }
                }
            }
            for (ICUBRIDNode col : selectionParent.getChildren()) {
                if (col.getName().equals((String) obj[1])) {
                    tvSourceDBObjects.setSelection(col);
                    showRightView(col, true);
                    return;
                }
            }
        });

        node2ViewMapping.put(DefaultNode.class.getName(), analyzerObjMappingView);
        node2ViewMapping.put(SelectNode.class.getName(), analyzerObjMappingView);
        node2ViewMapping.put(InsertNode.class.getName(), analyzerObjMappingView);
        node2ViewMapping.put(UpdateNode.class.getName(), analyzerObjMappingView);
        node2ViewMapping.put(DeleteNode.class.getName(), analyzerObjMappingView);
    }

    private void createToolButtons(Composite parent) {
        final Composite bottomComposite = new Composite(parent, SWT.NONE);
        bottomComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 2, 1));
        bottomComposite.setLayout(new GridLayout());

        Composite grpChangeSize = new Composite(bottomComposite, SWT.BORDER);
        grpChangeSize.setLayout(new GridLayout());
        grpChangeSize.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

        ToolBar tbTools = new ToolBar(grpChangeSize, SWT.WRAP | SWT.RIGHT | SWT.FLAT);
        tbTools.setLayout(new GridLayout());
        tbTools.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        final ToolItem btnReuseOID = new ToolItem(tbTools, SWT.CHECK);
        btnReuseOID.setText(Messages.lblReuseOID);
        btnReuseOID.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                if (!saveCurrentView()) return;
                final MigrationConfiguration cfg = page.getMigrationWizard().getMigrationConfig();
                for (Table tt : cfg.getTargetTableSchema()) {
                    tt.setReuseOID(btnReuseOID.getSelection());
                }
                refreshCurrentView();
            }
        });
        new ToolItem(tbTools, SWT.SEPARATOR);

        ToolItem btnSelectAll = new ToolItem(tbTools, SWT.PUSH);
        btnSelectAll.setText(Messages.lblSelectAll);
        btnSelectAll.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                if (!saveCurrentView()) return;
                page.getMigrationWizard().getMigrationConfig().setAll(true);
                refreshCurrentView();
                page.setErrorMessage(null);
            }
        });
        new ToolItem(tbTools, SWT.SEPARATOR);
        
        ToolItem btnClearAll = new ToolItem(tbTools, SWT.PUSH);
        btnClearAll.setText(Messages.lblClearAll);
        btnClearAll.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                if (MessageDialog.openConfirm(null, Messages.lblClearAll, Messages.msgCfmClearALL)) {
                    final MigrationConfiguration cfg = page.getMigrationWizard().getMigrationConfig();
                    cfg.setAll(false);
                    refreshTreeView();
                    if (cfg.getExpSQLCfg().isEmpty()) {
                        page.setErrorMessage(Messages.errNoDBObject);
                    }
                }
            }
        });
        new ToolItem(tbTools, SWT.SEPARATOR);
        
        ToolItem btnConstaintSelector = new ToolItem(tbTools, SWT.PUSH);
        btnConstaintSelector.setText(Messages.lblIndexQuickSetting);
        btnConstaintSelector.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent ev) {
                if (!saveCurrentView()) return;
                final MigrationConfiguration cfg = page.getMigrationWizard().getMigrationConfig();
                TableIndexSelectorDialog dialog = new TableIndexSelectorDialog(page.getShell(), cfg);
                if (dialog.open() != Dialog.OK) return;
                refreshCurrentView();
            }
        });
        new ToolItem(tbTools, SWT.SEPARATOR);
        
        ToolItem btnChangeCharColumns = new ToolItem(tbTools, SWT.NONE);
        btnChangeCharColumns.setText(Messages.btnChangeCharColumns);
        btnChangeCharColumns.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                if (!saveCurrentView()) return;
                openAdjustCharColumnDialog();
            }
        });
    }

    protected void createTreeView(SashForm parent) {
        Group srcDBContainer = new Group(parent, SWT.NONE);
        srcDBContainer.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
        srcDBContainer.setLayout(new GridLayout());
        srcDBContainer.setText(Messages.lblSourceDBPart);

        tvSourceDBObjects = new AnalyzerDMLTreeNodeView(srcDBContainer, SWT.BORDER);
        tvSourceDBObjects.setRefreshableView(this);
        tvSourceDBObjects.addSelectionChangedListener(event -> {
            if (event.getSelection().isEmpty()) return;
            IStructuredSelection ss = (IStructuredSelection) event.getSelection();
            showRightView(ss.getFirstElement(), true);
        });
    }

    private void setSourceTableNoPKWarningMessage() {
        final AnalyzerWizard mw = page.getMigrationWizard();
        Catalog sourceCatalog = mw.getSourceCatalog();
        final MigrationConfiguration cfg = mw.getMigrationConfig();
        if (cfg.isCreateConstrainsBeforeData()) {
            StringBuffer descriptionMessage = new StringBuffer();
            List<String> noPKTables = util.getNoPKTables(sourceCatalog);
            if (CollectionUtils.isNotEmpty(noPKTables)) {
                descriptionMessage.append(" Source tables without primary key: ");
                for (String noPKTableName : noPKTables) {
                    descriptionMessage.append(noPKTableName).append(", ");
                }
                descriptionMessage.deleteCharAt(descriptionMessage.length() - 1);
                descriptionMessage.deleteCharAt(descriptionMessage.length() - 1);
            }
            page.setMessage(descriptionMessage.toString(), IMessageProvider.WARNING);
        }
    }

    private void openAdjustCharColumnDialog() {
        AdjustCharColumnDialog dialog = new AdjustCharColumnDialog(page.getShell(), util);
        dialog.open();
        refreshCurrentView();
    }

    @Override
    public void refreshCurrentView() {
        if (currentView != null) {
            currentView.showData(currentView.getModel());
        }
    }

    private void refreshTreeView() {
        final AnalyzerWizard mw = page.getMigrationWizard();
        tvSourceDBObjects.setInput(mw.getSourceDBNode());
        
        if (currentView != null) {
            currentView.hide();
            currentView = null;
        }

        IAnalyzerNode node = mw.getSourceDBNode();
        if (node != null) {
            List<IAnalyzerNode> schemaNodes = node.getChildren();
            if (schemaNodes.size() == 1) {
                node = schemaNodes.get(0).getChildren().get(0);
            } else if (schemaNodes.size() > 1) {
                node = schemaNodes.get(0);
            }
            showRightView(node, true);
        }
        tvSourceDBObjects.setFocus();
    }

    private void showRightView(Object selection, boolean autoSave) {
        if (selection == null) return;
        
        AbstractMappingView view = node2ViewMapping.get(selection.getClass().getName());
        if (view == null) {
            showRightView(((ICUBRIDNode) selection).getParent(), autoSave);
            return;
        }
        
        if (autoSave && currentView != null) {
            try {
                VerifyResultMessages msg = currentView.save();
                if (msg.hasError()) {
                    tvSourceDBObjects.setSelection(currentView.getModel());
                    page.setErrorMessage(msg.getErrorMessage());
                    return;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                if (!MessageDialog.openConfirm(page.getShell(), Messages.lblSaveConfig, Messages.msgCfmErrorSave)) {
                    tvSourceDBObjects.setSelection(currentView.getModel());
                    return;
                }
            }
        }
        if (currentView != null && !currentView.equals(view)) {
            currentView.hide();
        }
        page.setErrorMessage(null);
        currentView = view;
        currentView.show();
        currentView.showData(selection);
    }

    private boolean validateConfig() {
        return saveCurrentView();
    }

    protected boolean saveCurrentView() {
        if (currentView != null) {
            VerifyResultMessages msg = currentView.save();
            if (msg.hasError()) {
                page.setErrorMessage(msg.getErrorMessage());
                MessageDialog.openError(page.getShell(), Messages.msgError, msg.getErrorMessage());
                return false;
            }
        }
        return true;
    }
}
