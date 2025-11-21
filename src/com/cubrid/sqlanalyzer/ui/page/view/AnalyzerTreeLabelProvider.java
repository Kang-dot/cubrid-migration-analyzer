package com.cubrid.sqlanalyzer.ui.page.view;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

import com.cubrid.sqlanalyzer.core.dbobject.treenode.DeleteNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.IAnalyzerNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.InsertNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.SelectNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.UpdateNode;

public class AnalyzerTreeLabelProvider extends CellLabelProvider {

	@Override
	public void update(ViewerCell cell) {
		Object cellElement = cell.getElement();
		
        if (!(cellElement instanceof IAnalyzerNode)) {
            return;
        }
        
        IAnalyzerNode analyzerNode = (IAnalyzerNode) cellElement;
        String text = analyzerNode.getName();
        
		if (cellElement instanceof SelectNode) {
			SelectNode selectNode = (SelectNode) cellElement;
			text = selectNode.getName();
			
		} else if (cellElement instanceof InsertNode) {
			InsertNode insertNode = (InsertNode) cellElement;
			text = insertNode.getName();
			
		} else if (cellElement instanceof DeleteNode) {
			DeleteNode deleteNode = (DeleteNode) cellElement;
			text = deleteNode.getName();
			
		} else if (cellElement instanceof UpdateNode) {
			UpdateNode updateNode = (UpdateNode) cellElement;
			text = updateNode.getName();
		}
		
		cell.setText(text);
	}
}
