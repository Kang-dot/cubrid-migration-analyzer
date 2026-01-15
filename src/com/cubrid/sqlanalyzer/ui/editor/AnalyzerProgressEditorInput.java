package com.cubrid.sqlanalyzer.ui.editor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.cubrid.cubridmigration.core.engine.report.MigrationBriefReport;
import com.cubrid.cubridmigration.ui.script.MigrationScript;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;

// TODO: maybe useless class

public class AnalyzerProgressEditorInput implements IEditorInput {

    private final AnalyzerConfiguration config;
    private final MigrationScript migrationScript;
    private final int startMode;

    public AnalyzerProgressEditorInput(
            AnalyzerConfiguration config, MigrationScript migrationScript) {
        this(config, migrationScript, MigrationBriefReport.SM_USER);
    }

    public AnalyzerProgressEditorInput(
            AnalyzerConfiguration config, MigrationScript migrationScript, int startMode) {
        this.config = config;
        this.migrationScript = migrationScript;
        this.startMode = startMode;
    }

    /**
     * @param adapter support AnalyzerConfiguration and AnalyzerProgressEditorInput
     * @return AnalyzerConfiguration and AnalyzerProgressEditorInput
     */
    @SuppressWarnings("rawtypes")
    public Object getAdapter(Class adapter) {
        if (adapter.equals(AnalyzerConfiguration.class)) {
            return config;
        } else if (adapter.equals(AnalyzerProgressEditorInput.class)) {
            return this;
        } else if (adapter.equals(MigrationScript.class)) {
            return migrationScript;
        }
        return null;
    }

    /** @return false */
    public boolean exists() {
        return false;
    }

    /**
     * no image
     *
     * @return null
     */
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    /**
     * name
     *
     * @return string
     */
    public String getName() {
        return "Migration from " + config.getSourceTypeName();
    }

    /** @return null */
    public IPersistableElement getPersistable() {
        return null;
    }

    /** @return tool tip text */
    public String getToolTipText() {
        return this.getName();
    }

    public int getStartMode() {
        return startMode;
    }

    /**
     * Retrieves if the migration was started by user
     *
     * @return true if by user, false if by scheduler.
     */
    public boolean isStartedByUser() {
        return startMode == MigrationBriefReport.SM_USER;
    }
}
