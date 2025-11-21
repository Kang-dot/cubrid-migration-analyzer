package com.cubrid.sqlanalyzer.ui.page.view;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;

import com.cubrid.sqlanalyzer.core.dbobject.treenode.IAnalyzerNode;


public class AnalyzerTreeContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		// input is List<IAnalyzerNode>
        List<IAnalyzerNode> list;
        if (inputElement instanceof List) {
            list = (List<IAnalyzerNode>) inputElement;
        } else if (inputElement instanceof IAnalyzerNode) {
            list = ((IAnalyzerNode) inputElement).getChildren();
        } else {
            return new Object[] {};
        }
		
		return list.toArray(new IAnalyzerNode[list.size()]);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		System.out.println("analyzer tree content provider getChild call");
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof IAnalyzerNode) {
			return ((IAnalyzerNode) element).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
        if (element instanceof IAnalyzerNode) {
        	IAnalyzerNode node = (IAnalyzerNode) element;
            return node.hasChild();
        }
		return false;
	}
}
