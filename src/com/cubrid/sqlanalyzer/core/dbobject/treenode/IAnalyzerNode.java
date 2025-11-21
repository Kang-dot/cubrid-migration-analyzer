package com.cubrid.sqlanalyzer.core.dbobject.treenode;

import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IEditorInput;

public interface IAnalyzerNode extends IAdaptable, IEditorInput {
	public List<IAnalyzerNode> getChildren();
	public void setChildren(List<IAnalyzerNode> childrenNodes);
	public IAnalyzerNode getParent();
	public void setParent();
	public IAnalyzerNode getRoot();
	public void setRoot();
	public boolean hasChild();
}
