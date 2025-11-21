package com.cubrid.sqlanalyzer.core.dbobject.treenode;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;

public class UpdateNode extends DefaultNode {
	String updateQuery;

	public UpdateNode(String id, String label) {
		super(id, label);
	}

	public String getUpdateQuery() {
		return updateQuery;
	}

	public void setUpdateQuery(String updateQuery) {
		this.updateQuery = updateQuery;
	}
}
