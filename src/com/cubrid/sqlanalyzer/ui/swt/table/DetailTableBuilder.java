package com.cubrid.sqlanalyzer.ui.swt.table;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class DetailTableBuilder {

    private TableViewer tableViewer;
    private IStructuredContentProvider contentProvider;
    private ITableLabelProvider labelProvider;

    public TableViewer buildTable(Composite parent, int style) {
        tableViewer = new TableViewer(parent, style | SWT.BORDER | SWT.FULL_SELECTION);
        tableViewer.setContentProvider(contentProvider);
        tableViewer.setLabelProvider(labelProvider);

        Table table = tableViewer.getTable();
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        String[] columnNames = {"SQL type", "query id", "query", "log"};
        int[] columnWidths = {100, 100, 400, 300};
        int[] columnStyles = {SWT.LEFT, SWT.LEFT, SWT.LEFT, SWT.LEFT};

        for (int i = 0; i < columnNames.length; i++) {
            TableColumn column = new TableColumn(table, columnStyles[i]);
            column.setText(columnNames[i]);
            column.setWidth(columnWidths[i]);
        }

        return tableViewer;
    }

    public void setContentProvider(IStructuredContentProvider contentProvider) {
        this.contentProvider = contentProvider;
    }

    public void setLabelProvider(ITableLabelProvider labelProvider) {
        this.labelProvider = labelProvider;
    }
}
