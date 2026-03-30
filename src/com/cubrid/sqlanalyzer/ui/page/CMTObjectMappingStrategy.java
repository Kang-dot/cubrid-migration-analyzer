package com.cubrid.sqlanalyzer.ui.page;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
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
import com.cubrid.cubridmigration.ui.common.navigator.event.CubridNodeManager;
import com.cubrid.cubridmigration.ui.common.navigator.node.ColumnNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.DatabaseNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.FKNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.FKsNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.FunctionNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.FunctionsNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.GrantAuthNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.GrantGrantorNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.GrantsNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.IndexNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.IndexesNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.PKNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.PartitionNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.PartitionsNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.ProcedureNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.ProceduresNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.SQLTableNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.SQLTablesNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.SchemaNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.SequenceNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.SequencesNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.SynonymNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.SynonymsNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.TableNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.TablesNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.ViewNode;
import com.cubrid.cubridmigration.ui.common.navigator.node.ViewsNode;
import com.cubrid.cubridmigration.ui.message.Messages;
import com.cubrid.cubridmigration.ui.wizard.dialog.AdjustCharColumnDialog;
import com.cubrid.cubridmigration.ui.wizard.dialog.TableIndexSelectorDialog;
import com.cubrid.cubridmigration.ui.wizard.page.view.AbstractMappingView;
import com.cubrid.cubridmigration.ui.wizard.page.view.ColumnMappingView;
import com.cubrid.cubridmigration.ui.wizard.page.view.FKMappingView;
import com.cubrid.cubridmigration.ui.wizard.page.view.FunctionMappingView;
import com.cubrid.cubridmigration.ui.wizard.page.view.GeneralObjMappingView;
import com.cubrid.cubridmigration.ui.wizard.page.view.IRefreshableView;
import com.cubrid.cubridmigration.ui.wizard.page.view.IndexMappingView;
import com.cubrid.cubridmigration.ui.wizard.page.view.ProcedureMappingView;
import com.cubrid.cubridmigration.ui.wizard.page.view.SQLTableMappingView;
import com.cubrid.cubridmigration.ui.wizard.page.view.SequenceMappingView;
import com.cubrid.cubridmigration.ui.wizard.page.view.SourceDBExploreView;
import com.cubrid.cubridmigration.ui.wizard.page.view.SynonymMappingView;
import com.cubrid.cubridmigration.ui.wizard.page.view.TableMappingView;
import com.cubrid.cubridmigration.ui.wizard.page.view.ViewMappingView;
import com.cubrid.cubridmigration.ui.wizard.utils.MigrationCfgUtils;
import com.cubrid.cubridmigration.ui.wizard.utils.VerifyResultMessages;
import com.cubrid.sqlanalyzer.ui.AnalyzerWizard;

public class CMTObjectMappingStrategy implements IObjectMappingStrategy, IRefreshableView {

    private AnalyzerObjectMappingPage page;
    private Composite container;

    private SourceDBExploreView tvSourceDBObjects;
    private final Map<String, AbstractMappingView> node2ViewMapping = new HashMap<String, AbstractMappingView>();
    private AbstractMappingView currentView;
    private final MigrationCfgUtils util = new MigrationCfgUtils();
    private boolean isFirstVisible = true;

    public CMTObjectMappingStrategy(AnalyzerObjectMappingPage page) {
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

            // 각종 중복/경고 다이얼로그 로직
            if (isFirstVisible) {
                int tarSchemaSize = mw.getTarCatalogSchemaCount();
                if (util.checkMultipleSchema(sourceCatalog, cfg)
                        && util.createAllObjectsMap(sourceCatalog, targetCatalog, cfg)
                        && util.hasDuplicatedObjects(sourceCatalog)
                        && (tarSchemaSize <= 1 || cfg.isTarSchemaDuplicate())) {
                    // showDetailMessageDialog(sourceCatalog);
                }
            }

            for (AbstractMappingView amv : node2ViewMapping.values()) {
                amv.setMigrationConfig(cfg);
                amv.setWizardStatus(mw);
            }
            
            cfg.setSrcCatalog(sourceCatalog, isFirstVisible && !mw.isLoadMigrationScript());
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

        GeneralObjMappingView generalObjMappingView = new GeneralObjMappingView(detailContainer);
        generalObjMappingView.addDoubleClickListener(event -> handleDoubleClick(event));

        TableMappingView tableMappingView = new TableMappingView(detailContainer);
        tableMappingView.addDoubleClickListener(event -> handleDoubleClick(event));

        ColumnMappingView columnMappingView = new ColumnMappingView(detailContainer);
        IndexMappingView indexMappingView = new IndexMappingView(detailContainer);
        FKMappingView fkMappingView = new FKMappingView(detailContainer);
        SequenceMappingView sequenceMappingView = new SequenceMappingView(detailContainer);
        ViewMappingView viewMappingView = new ViewMappingView(detailContainer);
        SynonymMappingView synonymMappingView = new SynonymMappingView(detailContainer);
        ProcedureMappingView procedureMappingView = new ProcedureMappingView(detailContainer);
        FunctionMappingView functionMappingView = new FunctionMappingView(detailContainer);

        generalObjMappingView.addSQLChangedListener(tvSourceDBObjects);
        
        // Node to View Mapping
        node2ViewMapping.put(DatabaseNode.class.getName(), generalObjMappingView);
        node2ViewMapping.put(SchemaNode.class.getName(), generalObjMappingView);
        node2ViewMapping.put(TablesNode.class.getName(), generalObjMappingView);
        node2ViewMapping.put(ViewsNode.class.getName(), generalObjMappingView);
        node2ViewMapping.put(SequencesNode.class.getName(), generalObjMappingView);
        node2ViewMapping.put(TableNode.class.getName(), tableMappingView);
        node2ViewMapping.put(ViewNode.class.getName(), viewMappingView);
        node2ViewMapping.put(SequenceNode.class.getName(), sequenceMappingView);
        node2ViewMapping.put(SynonymsNode.class.getName(), generalObjMappingView);
        node2ViewMapping.put(SynonymNode.class.getName(), synonymMappingView);
        node2ViewMapping.put(GrantsNode.class.getName(), generalObjMappingView);
        node2ViewMapping.put(GrantGrantorNode.class.getName(), generalObjMappingView);
        node2ViewMapping.put(GrantAuthNode.class.getName(), generalObjMappingView);
        node2ViewMapping.put(ProceduresNode.class.getName(), generalObjMappingView);
        node2ViewMapping.put(ProcedureNode.class.getName(), procedureMappingView);
        node2ViewMapping.put(FunctionsNode.class.getName(), generalObjMappingView);
        node2ViewMapping.put(FunctionNode.class.getName(), functionMappingView);
        node2ViewMapping.put(PKNode.class.getName(), tableMappingView);
        node2ViewMapping.put(FKsNode.class.getName(), tableMappingView);
        node2ViewMapping.put(IndexesNode.class.getName(), tableMappingView);
        node2ViewMapping.put(PartitionsNode.class.getName(), tableMappingView);
        node2ViewMapping.put(PartitionNode.class.getName(), tableMappingView);
        node2ViewMapping.put(ColumnNode.class.getName(), columnMappingView);
        node2ViewMapping.put(FKNode.class.getName(), fkMappingView);
        node2ViewMapping.put(IndexNode.class.getName(), indexMappingView);
        node2ViewMapping.put(SQLTableNode.class.getName(), new SQLTableMappingView(detailContainer));
        node2ViewMapping.put(SQLTablesNode.class.getName(), generalObjMappingView);
    }

    private void handleDoubleClick(DoubleClickEvent event) {
        if (event.getSource() == null || event.getSelection().isEmpty()) return;
        TableViewer tv = (TableViewer) event.getSource();
        Object[] obj = (Object[]) ((StructuredSelection) event.getSelection()).getFirstElement();
        ICUBRIDNode cn = (ICUBRIDNode) currentView.getModel();
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

        tvSourceDBObjects = new SourceDBExploreView(srcDBContainer, SWT.BORDER);
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
        final MigrationConfiguration cfg = mw.getMigrationConfig();

        if (cfg.targetIsOnline() && !cfg.isTargetDBAGroup()) {
            CubridNodeManager.getInstance().changeGrantsNodeLabel();
        }

        tvSourceDBObjects.setInput(mw.getSelectSourceDB(), cfg);
        
        if (currentView != null) {
            currentView.hide();
            currentView = null;
        }

        ICUBRIDNode node = mw.getSelectSourceDB();
        if (node != null) {
            List<ICUBRIDNode> schemaNodes = node.getChildren();
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
//        if (!saveCurrentView()) return false;
//        VerifyResultMessages result = util.checkAll(page.getMigrationWizard().getMigrationConfig());
//        if (result.hasError()) {
//            page.setErrorMessage(result.getErrorMessage());
//            MessageDialog.openError(page.getShell(), Messages.msgError, result.getErrorMessage());
//            return false;
//        }
//        page.setErrorMessage(null);
        return true;
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
