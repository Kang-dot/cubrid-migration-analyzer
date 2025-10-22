package com.cubrid.sqlanalyzer.core.dbobject.treenode;

public class DeleteNode extends DefaultNode {
	String deleteQuery;
	
	public DeleteNode(String id, String label) {
		super(id, label);
	}

	public String getDeleteQuery() {
		return deleteQuery;
	}

	public void setDeleteQuery(String deleteQuery) {
		this.deleteQuery = deleteQuery;
	}
}
