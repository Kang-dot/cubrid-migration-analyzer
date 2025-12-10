package com.cubrid.sqlanalyzer.ui.editor;

import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.ui.script.MigrationScript;
import com.cubrid.cubridmigration.ui.wizard.editor.MigrationProgressEditorInput;

// TODO: maybe useless class

public class AnalyzerProgressEditorInput extends MigrationProgressEditorInput {
	public AnalyzerProgressEditorInput(MigrationConfiguration config, MigrationScript migrationScript) {
		super(config, migrationScript);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public Object getAdapter(Class adapter) {
		return this;
	}
}
