package com.cubrid.sqlanalyzer.navigator;

import java.util.Map;

import com.cubrid.cubridmigration.ui.common.navigator.node.DatabaseNode;
import com.cubrid.sqlanalyzer.core.dbobject.AnalyzerCatalog;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.DefaultNode;
import com.cubrid.sqlanalyzer.core.dbobject.treenode.SelectNode;

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
		
		String selectQueriesID = parentID + "/viewQuery";
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
	
	private void addInsertNodes() {
		
	}
	
	private void addUpdateNodes() {
		
	}
	
	private void addDeleteNodes() {
		
	}
	
	public DatabaseNode createDBNode(AnalyzerCatalog catalog) {
		String hostID = "DML";
		
		QueryDictionary queryDict = catalog.getQueryDictionary();
		
//		dbNode = new DefaultCUBRIDNode(hostID, hostID, hostID)
		
		DefaultNode defaultNode = new DefaultNode("DML", "defaultNode");
		
		addSelectNodes(defaultNode, queryDict.getSelectQueryMap());
		return null;
	}
}
