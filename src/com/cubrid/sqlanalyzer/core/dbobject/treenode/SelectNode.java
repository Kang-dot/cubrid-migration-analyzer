package com.cubrid.sqlanalyzer.core.dbobject.treenode;

public class SelectNode extends DefaultNode {
	String selectQuery;
	
	public SelectNode(String id, String label) {
		super(id, label);
	}
	
	public void setQuery(String selectQuery) {
		this.selectQuery = selectQuery;
	}
	
	public String getQuery() {
		return selectQuery;
	}
}
