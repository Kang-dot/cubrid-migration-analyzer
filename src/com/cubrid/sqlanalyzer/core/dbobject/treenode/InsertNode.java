package com.cubrid.sqlanalyzer.core.dbobject.treenode;

public class InsertNode extends DefaultNode {
	String insertQuery;
	
	public InsertNode(String id, String label) {
		super(id, label);
		// TODO Auto-generated constructor stub
	}

	public String getInsertQuery() {
		return insertQuery;
	}

	public void setInsertQuery(String insertQuery) {
		this.insertQuery = insertQuery;
	}
}
