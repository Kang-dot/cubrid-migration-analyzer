package com.cubrid.sqlanalyzer.navigator;

import java.util.Map;

import com.cubrid.cubridmigration.ui.common.navigator.node.DatabaseNode;
import com.cubrid.sqlanalyzer.core.dbobject.AnalyzerCatalog;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.DefaultNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.DeleteNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.InsertNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.SelectNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.UpdateNode;

public class AnalyzerNodeManager {
    private static volatile AnalyzerNodeManager instance = null;
    private static final Object LOCKOBJ = new Object();
    private DatabaseNode dbNode = null;
	
	public static AnalyzerNodeManager getInstance() {
        synchronized (LOCKOBJ) {
            if (instance == null) {
                instance = new AnalyzerNodeManager();
            }
            return instance;
        }
	}
	
	private void addSelectNodes(DefaultNode defaultNode, Map<String, String> selectQueryList) {
		String parentID = defaultNode.getNodeId();
		
		String selectQueriesID = parentID + "/selectQuery";
		String selectQueriesLabel = "SELECT" + "(" + selectQueryList.size() + ")";
		SelectNode rootSelectNode = new SelectNode(selectQueriesID, selectQueriesLabel);
		
		defaultNode.addChildren(rootSelectNode);
		
		selectQueryList.forEach((key, query) ->  {
			String queryID = selectQueriesID + "/" + key;
			String queryLabel = key;
			SelectNode selectNode = new SelectNode(queryID, queryLabel);
			selectNode.setQuery(query);
			rootSelectNode.addChildren(selectNode);
		});
	}
	
	private void addInsertNodes(DefaultNode defaultNode, Map<String, String> insertQueryList) {
		String parentID = defaultNode.getNodeId();
		
		String insertQueriesID = parentID + "/insertQuery";
		String insertQueriesLabel = "INSERT" + "(" + insertQueryList.size() + ")";
		InsertNode rootInsertNode = new InsertNode(insertQueriesID, insertQueriesLabel);
		
		defaultNode.addChildren(rootInsertNode);
		
		insertQueryList.forEach((key, query) ->  {
			String queryID = insertQueriesID + "/" + key;
			String queryLabel = key;
			InsertNode insertNode = new InsertNode(queryID, queryLabel);
			insertNode.setInsertQuery(query);
			rootInsertNode.addChildren(insertNode);
		});
	}
	
	private void addUpdateNodes(DefaultNode defaultNode, Map<String, String> updateQueryList) {
		String parentID = defaultNode.getNodeId();
		
		String updateQueriesID = parentID + "/updateQuery";
		String updateQueriesLabel = "UPDATE" + "(" + updateQueryList.size() + ")";
		UpdateNode rootUpdateNode = new UpdateNode(updateQueriesID, updateQueriesLabel);
		
		defaultNode.addChildren(rootUpdateNode);
		
		updateQueryList.forEach((key, query) ->  {
			String queryID = updateQueriesID + "/" + key;
			String queryLabel = key;
			UpdateNode updateNode = new UpdateNode(queryID, queryLabel);
			updateNode.setUpdateQuery(query);
			rootUpdateNode.addChildren(updateNode);
		});
	}
	
	private void addDeleteNodes(DefaultNode defaultNode, Map<String, String> deleteQueryList) {
		String parentID = defaultNode.getNodeId();
		
		String deleteQueriesID = parentID + "/deleteQuery";
		String deleteQueriesLabel = "DELETE" + "(" + deleteQueryList.size() + ")";
		DeleteNode rootDeleteNode = new DeleteNode(deleteQueriesID, deleteQueriesLabel);
		
		defaultNode.addChildren(rootDeleteNode);
		
		deleteQueryList.forEach((key, query) ->  {
			String queryID = deleteQueriesID + "/" + key;
			String queryLabel = key;
			DeleteNode deleteNode = new DeleteNode(queryID, queryLabel);
			deleteNode.setDeleteQuery(query);
			rootDeleteNode.addChildren(deleteNode);
		});
	}
	
	public DefaultNode createDBNode(AnalyzerCatalog catalog) {
		String hostID = "DML";
		
		QueryDictionary queryDict = catalog.getQueryDictionary();
		
//		dbNode = new DefaultCUBRIDNode(hostID, hostID, hostID)
		
		DefaultNode defaultNode = new DefaultNode("DML", "defaultNode");
		
		addSelectNodes(defaultNode, queryDict.getSelectQueryMap());
		addInsertNodes(defaultNode, queryDict.getInsertQueryMap());
		addUpdateNodes(defaultNode, queryDict.getUpdateQueryMap());
		addDeleteNodes(defaultNode, queryDict.getDeleteQueryMap());
		
		return defaultNode;
	}
}
