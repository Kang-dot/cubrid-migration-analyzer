package com.cubrid.sqlanalyzer.ui.page.view;

import com.cubrid.cubridmigration.ui.common.CompositeUtils;
import com.cubrid.cubridmigration.ui.wizard.page.view.AbstractMappingView;
import com.cubrid.cubridmigration.ui.wizard.utils.VerifyResultMessages;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.DefaultNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.DeleteNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.IAnalyzerNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.InsertNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.SelectNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.UpdateNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

public class AnalyzerObjectTableMappingView extends AbstractMappingView {
    private static final String TAB_ICON = "icon/db/sql.png";
    private static final String CT_SELECT = "select";
    private static final String CT_INSERT = "insert";
    private static final String CT_UPDATE = "update";
    private static final String CT_DELETE = "delete";

//    private enum DMLType {
//        SELECT,
//        INSERT,
//        UPDATE,
//        DELETE
//    }

    private CTabFolder dmlFolder;
    private TableViewer tvSelect;
    private TableViewer tvInsert;
    private TableViewer tvUpdate;
    private TableViewer tvDelete;

    public AnalyzerObjectTableMappingView(Composite parent) {
        super(parent);
    }

    @Override
    protected void createControl(Composite parent) {
        dmlFolder = new CTabFolder(parent, SWT.BORDER);
        GridData gdTab = new GridData(SWT.FILL, SWT.FILL, true, true);
        dmlFolder.setLayoutData(gdTab);
        dmlFolder.setSimple(false);
        dmlFolder.setUnselectedCloseVisible(false);
        dmlFolder.setTabHeight(24);

        tvSelect = createDmlTab(dmlFolder, "SELECT", CT_SELECT);
        tvInsert = createDmlTab(dmlFolder, "INSERT", CT_INSERT);
        tvUpdate = createDmlTab(dmlFolder, "UPDATE", CT_UPDATE);
        tvDelete = createDmlTab(dmlFolder, "DELETE", CT_DELETE);
    }

    private TableViewer createDmlTab(CTabFolder parent, String title, String contentType) {
        Composite container = CompositeUtils.createTabItem(parent, title, TAB_ICON);
        TableViewer viewer =
                new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        Table table = viewer.getTable();
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        createColumn(viewer, "Query ID", 180, false);
        createColumn(viewer, "SQL", 560, true);

        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setData(CONTENT_TYPE, contentType);
        return viewer;
    }

    private void createColumn(TableViewer viewer, String title, int width, boolean sqlColumn) {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);
        column.setLabelProvider(
                new ColumnLabelProvider() {
                    @Override
                    public String getText(Object element) {
                        if (!(element instanceof DmlRow)) {
                            return "";
                        }
                        DmlRow row = (DmlRow) element;
                        return sqlColumn ? row.getSql() : row.getName();
                    }
                });
    }

    @Override
    public void hide() {
        CompositeUtils.hideOrShowComposite(dmlFolder, true);
    }

    @Override
    public void show() {
        CompositeUtils.hideOrShowComposite(dmlFolder, false);
    }

    @Override
    public VerifyResultMessages save() {
        return super.save();
    }

    @Override
    public void showData(Object obj) {
        super.showData(obj);
        if (obj instanceof DefaultNode) {
            showDefaultNode((DefaultNode) obj);
            return;
        }
        if (obj instanceof SelectNode) {
            showSelectNode((SelectNode) obj);
        } else if (obj instanceof InsertNode) {
            showInsertNode((InsertNode) obj);
        } else if (obj instanceof UpdateNode) {
            showUpdateNode((UpdateNode) obj);
        } else if (obj instanceof DeleteNode) {
            showDeleteNode((DeleteNode) obj);
        }
    }

    public void addDoubleClickListener(IDoubleClickListener listener) {
        tvSelect.addDoubleClickListener(listener);
        tvInsert.addDoubleClickListener(listener);
        tvUpdate.addDoubleClickListener(listener);
        tvDelete.addDoubleClickListener(listener);
    }

    private void showDefaultNode(DefaultNode node) {
        tvSelect.setInput(
                buildRows(findChildNodes(node, SelectNode.class)));
        tvInsert.setInput(
                buildRows(findChildNodes(node, InsertNode.class)));
        tvUpdate.setInput(
                buildRows(findChildNodes(node, UpdateNode.class)));
        tvDelete.setInput(
                buildRows(findChildNodes(node, DeleteNode.class)));
        dmlFolder.setSelection(0);
    }

    private void showSelectNode(SelectNode node) {
        tvSelect.setInput(buildRows(flattenNode(node, SelectNode.class)));
        dmlFolder.setSelection(0);
    }

    private void showInsertNode(InsertNode node) {
        tvInsert.setInput(buildRows(flattenNode(node, InsertNode.class)));
        dmlFolder.setSelection(1);
    }

    private void showUpdateNode(UpdateNode node) {
        tvUpdate.setInput(buildRows(flattenNode(node, UpdateNode.class)));
        dmlFolder.setSelection(2);
    }

    private void showDeleteNode(DeleteNode node) {
        tvDelete.setInput(buildRows(flattenNode(node, DeleteNode.class)));
        dmlFolder.setSelection(3);
    }

    private <T extends IAnalyzerNode> List<T> findChildNodes(DefaultNode parent, Class<T> type) {
        for (IAnalyzerNode child : parent.getChildren()) {
            if (type.isInstance(child)) {
                return flattenNode(child, type);
            }
        }
        return Collections.emptyList();
    }

    private <T extends IAnalyzerNode> List<T> flattenNode(IAnalyzerNode node, Class<T> type) {
        List<T> result = new ArrayList<T>();
        if (node == null) {
            return result;
        }
        List<IAnalyzerNode> children = node.getChildren();
        if (children == null || children.isEmpty()) {
            if (type.isInstance(node)) {
                result.add(type.cast(node));
            }
            return result;
        }
        for (IAnalyzerNode child : children) {
            if (type.isInstance(child)) {
                result.add(type.cast(child));
            }
        }
        if (result.isEmpty() && type.isInstance(node)) {
            result.add(type.cast(node));
        }
        return result;
    }

    private List<DmlRow> buildRows(List<? extends IAnalyzerNode> nodes) {
        List<DmlRow> rows = new ArrayList<DmlRow>();
        for (IAnalyzerNode node : nodes) {
            rows.add(new DmlRow(node.getName(), extractQuery(node)));
        }
        return rows;
    }

    private String extractQuery(IAnalyzerNode node) {
        if (node instanceof SelectNode) {
            return safe(((SelectNode) node).getQuery());
        }
        if (node instanceof InsertNode) {
            return safe(((InsertNode) node).getInsertQuery());
        }
        if (node instanceof UpdateNode) {
            return safe(((UpdateNode) node).getUpdateQuery());
        }
        if (node instanceof DeleteNode) {
            return safe(((DeleteNode) node).getDeleteQuery());
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static class DmlRow {
        private final String name;
        private final String sql;

        private DmlRow(String name, String sql) {
            this.name = name;
            this.sql = sql;
        }

        public String getName() {
            return name;
        }

        public String getSql() {
            return sql;
        }
    }
}
