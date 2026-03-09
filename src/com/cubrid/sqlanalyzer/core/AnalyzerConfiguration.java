package com.cubrid.sqlanalyzer.core;

import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;

public class AnalyzerConfiguration extends MigrationConfiguration {

//    private static final Logger LOG = LogUtil.getLogger(MigrationConfiguration.class);

	// TODO: ora2cubrid
	// TODO: need to change oracle ID
    public static int SOURCE_TYPE_DB = 3;
    
    // TODO: mybatis xml
    public static int SOURCE_TYPE_XML = 2;
    
    public static int TARGET_TYPE_CUBRID = 4;
    public static int TARGET_TYPE_PARSER = 5;
	
	private QueryDictionary queryDict = null;
    
    public QueryDictionary getQueryDict() {
		return queryDict;
	}

	public void setQueryDict(QueryDictionary queryDict) {
		this.queryDict = queryDict;
	}
	
    public boolean isTargetParser() {
    	return getDestType() == AnalyzerConfiguration.TARGET_TYPE_PARSER;
    }
}