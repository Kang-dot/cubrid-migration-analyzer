package com.cubrid.sqlanalyzer.ui.page.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import com.cubrid.cubridmigration.ui.wizard.page.view.IRefreshableView;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.DefaultNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.IAnalyzerNode;

public class AnalyzerDMLTreeNodeView {
	private TreeViewer nodeTreeView;
	private IRefreshableView treeView;
    private IRefreshableView refreshableView;
	
	public AnalyzerDMLTreeNodeView(Composite parent, int style) {
		nodeTreeView = new TreeViewer(parent, style);
		nodeTreeView.setLabelProvider(new AnalyzerTreeLabelProvider());
		nodeTreeView.setContentProvider(new AnalyzerTreeContentProvider());

		nodeTreeView.getTree().setLayout(new GridLayout());
		nodeTreeView.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	public void setRefreshableView(IRefreshableView refreshableView) {
		this.refreshableView = refreshableView;
	}
	
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		nodeTreeView.addSelectionChangedListener(listener);
	}
	
	public void setFocus() {
		nodeTreeView.getTree().setFocus();
	}
	
    public void setSelection(Object model) {
        if (model instanceof IAnalyzerNode) {
        	IAnalyzerNode nd = ((IAnalyzerNode) model).getParent();
            if (nodeTreeView.isExpandable(nd)) {
            	nodeTreeView.setExpandedState(nd, true);
            }
            final Widget it = nodeTreeView.testFindItem(model);
            if (it == null) {
                return;
            }
            nodeTreeView.getTree().setSelection((TreeItem) it);
        }
    }
    
    public void setInput(DefaultNode input) {
    	List<IAnalyzerNode> nodeTreeContent = new ArrayList<IAnalyzerNode>();
    	
    	if (input.getChildren().size() == 1) {
    		nodeTreeContent.addAll(input.getChildren().get(0).getChildren());
    	} else {
    		nodeTreeContent.addAll(input.getChildren());
    	}
    	
    	nodeTreeView.setInput(nodeTreeContent);
    }
}
