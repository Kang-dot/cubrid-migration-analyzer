package com.cubrid.sqlanalyzer.ui.swt.table;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

import com.cubrid.sqlanalyzer.ui.reporter.AnalyzerOverviewResult;

public class DetailTableLabelProvider implements ITableLabelProvider {

	@Override
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		AnalyzerOverviewResult result = (AnalyzerOverviewResult) element;
		switch (columnIndex) {
		case 0:
			return result.isFirstInGroup() ? result.getQueryType() : "";
		case 1:
			return result.getQueryId();
		case 2:
			return result.getQuery();
		case 3:
			return result.getErrorMessage();
		default:
			return null;
		}
	}

}
