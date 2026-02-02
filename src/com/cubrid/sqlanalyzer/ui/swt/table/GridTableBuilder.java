package com.cubrid.sqlanalyzer.ui.swt.table;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.nebula.jface.gridviewer.GridTableViewer;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

public class GridTableBuilder {
	
	private GridTableViewer gridTable;
	private IStructuredContentProvider contentProvider;
	private ITableLabelProvider labelProvider;
	
	public GridTableViewer buildGridTable(Composite parent, int style) {
		gridTable = new GridTableViewer(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		gridTable.setContentProvider(contentProvider);
		gridTable.setLabelProvider(labelProvider);
		
		Grid grid = gridTable.getGrid();
		grid.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		grid.setHeaderVisible(true);
		grid.setLinesVisible(true);
		
		String[] columnNames = {"SQL type", "total count", "error count", "query id", "query", "log"};
		int[] columnWidths = {150, 150, 150, 150, 200, 200};
		int[] columnStyles = {SWT.LEFT, SWT.RIGHT, SWT.RIGHT, SWT.LEFT, SWT.LEFT, SWT.LEFT};
		
		// 컬럼 생성 및 설정
		for (int i = 0; i < columnNames.length; i++) {
			GridColumn column = new GridColumn(grid, columnStyles[i]);
			column.setText(columnNames[i]);
			column.setWidth(columnWidths[i]);
		}
		
		return gridTable;
	}
	
	public void setContentProvider(IStructuredContentProvider contentProvider) {
		this.contentProvider = contentProvider;  
	}
	
	public void setLabelProvider(ITableLabelProvider labelProvider) {
		this.labelProvider = labelProvider;
	}
}
