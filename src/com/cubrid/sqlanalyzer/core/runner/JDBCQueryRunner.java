package com.cubrid.sqlanalyzer.core.runner;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.cubrid.cubridmigration.core.common.Closer;
import com.cubrid.cubridmigration.core.dbobject.FK;
import com.cubrid.cubridmigration.core.dbobject.Function;
import com.cubrid.cubridmigration.core.dbobject.Grant;
import com.cubrid.cubridmigration.core.dbobject.Index;
import com.cubrid.cubridmigration.core.dbobject.PK;
import com.cubrid.cubridmigration.core.dbobject.PlcsqlFunction;
import com.cubrid.cubridmigration.core.dbobject.PlcsqlProcedure;
import com.cubrid.cubridmigration.core.dbobject.Procedure;
import com.cubrid.cubridmigration.core.dbobject.Record;
import com.cubrid.cubridmigration.core.dbobject.Schema;
import com.cubrid.cubridmigration.core.dbobject.Sequence;
import com.cubrid.cubridmigration.core.dbobject.Synonym;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbobject.Trigger;
import com.cubrid.cubridmigration.core.dbobject.View;
import com.cubrid.cubridmigration.core.engine.JDBCConManager;
import com.cubrid.cubridmigration.core.engine.config.SourceTableConfig;
import com.cubrid.cubridmigration.core.engine.importer.IMigrationImporter;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.engine.AnalyzerContext;

public class JDBCQueryRunner implements IMigrationImporter {
	
	private final JDBCConManager connManager;
	private final AnalyzerConfiguration config;
	
	public JDBCQueryRunner(AnalyzerContext context) {
		this.connManager = context.getConnManager();
		this.config = context.getConfig();
	}
	
	public void executeQuery(String query) throws SQLException {
		//TODO: Execute query (ddl, dml)
		// make event
		// get result
		
		Connection conn = connManager.getTargetConnection();
		Statement stmt = null;
		
		try {
			stmt = conn.createStatement();
			stmt.execute(query);
		} catch (SQLException ex) {
			throw new SQLException(ex);
		} finally {
			Closer.close(conn);
		}
	}

	@Override
	public void executeDDL(String sql) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createFK(FK fk) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createFunction(Function function) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createPlcsqlFunctionHeader(PlcsqlFunction plcsqlFunction) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createPlcsqlFunctionBody(PlcsqlFunction plcsqlFunction) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createIndex(Index index) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createPK(PK pk) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createProcedure(Procedure procedure) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createPlcsqlProcedureHeader(PlcsqlProcedure plcsqlProcedure) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createPlcsqlProcedureBody(PlcsqlProcedure plcsqlProcedure) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createSequence(Sequence sq) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createSynonym(Synonym sn) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createGrant(Grant gr) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createSchema(Schema schema) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createTable(Table table) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createTriggers(Trigger trigger) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createView(View view) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void alterView(View view) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int importRecords(SourceTableConfig stc, List<Record> records) {
		// TODO Auto-generated method stub
		return 0;
	}
}
