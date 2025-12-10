package com.cubrid.sqlanalyzer.ui.reporter;

import java.io.File;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.core.engine.report.DefaultMigrationReporter;

public class AnalyzerReporter extends DefaultMigrationReporter implements IEditorInput {

    public AnalyzerReporter(MigrationConfiguration config, int startMode) {
        super(config, startMode);
    }
	
	public AnalyzerReporter(File file) {
		super(file);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void getSummary() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean exists() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPersistableElement getPersistable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getToolTipText() {
		// TODO Auto-generated method stub
		return null;
	}

}
