package com.cubrid.sqlanalyzer.core.dbobject.treenode;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;

public class DefaultNode implements IAnalyzerNode {
	protected List<IAnalyzerNode> childNodeList = null;
	private IAnalyzerNode parentNode = null;
	private IAnalyzerNode rootNode = null;

	String nodeId = "";
	String label = "";

	boolean isRoot = false;

	public DefaultNode(String id, String label) {
		this.nodeId = id;
		this.label = label;
		childNodeList = new ArrayList<IAnalyzerNode>();
	}
	
	public String getNodeId() {
		return nodeId;
	}
	
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
	
	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		return label;
	}

	@Override
	public IPersistableElement getPersistable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getToolTipText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<IAnalyzerNode> getChildren() {
		return childNodeList;
	}

	@Override
	public void setChildren(List<IAnalyzerNode> childrenNodes) {
		this.childNodeList = childrenNodes;
	}
	
	public void addChildren(IAnalyzerNode node) {
		childNodeList.add(node);
	}

	@Override
	public IAnalyzerNode getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setParent() {
		// TODO Auto-generated method stub

	}

	@Override
	public IAnalyzerNode getRoot() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setRoot() {
		// TODO Auto-generated method stub

	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasChild() {
		// TODO Auto-generated method stub
		return false;
	}

}
