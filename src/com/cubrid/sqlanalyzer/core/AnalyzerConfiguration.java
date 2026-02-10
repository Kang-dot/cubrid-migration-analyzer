package com.cubrid.sqlanalyzer.core;

import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;

public class AnalyzerConfiguration extends MigrationConfiguration {

//    private static final Logger LOG = LogUtil.getLogger(MigrationConfiguration.class);

    public static int SOURCE_TYPE_DB = 1;
    public static int SOURCE_TYPE_MYBATIS = 2;
    
    public static int TARGET_TYPE_CUBRID = 3;
    public static int TARGET_TYPE_PARSER = 4;
	
	private QueryDictionary queryDict = null;
    
    public QueryDictionary getQueryDict() {
		return queryDict;
	}

	public void setQueryDict(QueryDictionary queryDict) {
		this.queryDict = queryDict;
	}
}