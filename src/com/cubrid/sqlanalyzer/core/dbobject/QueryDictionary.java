package com.cubrid.sqlanalyzer.core.dbobject;

import java.util.ArrayList;
import java.util.List;

public class QueryDictionary {
	private List<String> selectQueryList = new ArrayList<String>();
	private List<String> insertQueryList = new ArrayList<String>();
	private List<String> updateQueryList = new ArrayList<String>();
	private List<String> deleteQueryList = new ArrayList<String>();

	public void addSelectQuery(String query) {
		selectQueryList.add(query);
	}
	
	public void addInsertQuery(String query) {
		insertQueryList.add(query);
	}
	
	public void addUpdateQuery(String query) {
		updateQueryList.add(query);
	}
	
	public void addDeleteQuery(String query) {
		deleteQueryList.add(query);
	}

	public List<String> getSelectQueryList() {
		return selectQueryList;
	}
	
	public List<String> getInsertQueryList() {
		return insertQueryList;
	}
	
	public List<String> getUpdateQueryList() {
		return updateQueryList;
	}
	
	public List<String> getDeleteQueryList() {
		return deleteQueryList;
	}
}
