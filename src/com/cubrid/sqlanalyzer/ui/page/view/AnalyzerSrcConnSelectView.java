package com.cubrid.sqlanalyzer.ui.page.view;

public class AnalyzerSrcConnSelectView /* extends AnalyzerWizardPage */{
//
//    private final JDBCConnectionMgrView conMgrView;
//    
//    private AnalyzerSrcConnSelectView(AnalyzerConfiguration config) {
//        conMgrView =
//                new JDBCConnectionMgrView(
//                        MigrationWizard.getSupportedSrcDBTypes(),
//                        new IJDBCConnectionFilter() {
//
//                            public boolean doFilter(ConnParameters cp) {
//                                return getMigrationWizard().getMigrationConfig().getSourceType()
//                                        != cp.getDatabaseType().getID();
//                            }
//                        });
//    }
//
//    /**
//     * Create controls
//     *
//     * @param parent of the controls
//     */
//    public void createControls(Composite parent) {
//        conMgrView.createControls(parent);
//    }
//
//    /**
//     * get Catalog
//     *
//     * @return Catalog
//     */
//    public Catalog getCatalog() {
//        return conMgrView.getCatalog();
//    }
//
//    /** Hide */
//    public void hide() {
//        conMgrView.hide();
//    }
//
//    /** Initialize with script's source connection */
//    public void init() {
//        MigrationWizard wzd = getMigrationWizard();
//        setTitle(wzd.getStepNoMsg(SelectSourcePage.this) + Messages.msgSrcSelectOnlineDB);
//        setMessage(Messages.msgSrcSelectOnlineDBDes);
//        List<Integer> dts = new ArrayList<Integer>();
//        MigrationConfiguration cfg = wzd.getMigrationConfig();
//        dts.add(cfg.getSourceType());
//        conMgrView.setSupportedDBType(dts);
//        // Add catalog to cache.
//        Catalog offlineSrcCatalog = cfg.getOfflineSrcCatalog();
//        ConnParameters srcConParams = cfg.getSourceConParams();
//        conMgrView.init(srcConParams, offlineSrcCatalog);
//    }
//
//    /**
//     * check whether the dialog changed
//     *
//     * @return true if content changed
//     */
//    public boolean isInputChanged() {
//        boolean srcDBChanged = false;
//        MigrationConfiguration config = getMigrationWizard().getMigrationConfig();
//        // if online is saved but not selected or dumpfile is saved but not selected
//        ConnParameters oldCP = config.getSourceConParams();
//        // the first time set it changed
//        DatabaseConnectionInfo dci = conMgrView.getSelectedDCI();
//        if (oldCP == null && dci != null) {
//            srcDBChanged = true;
//        } else if (oldCP != null) {
//            srcDBChanged = !oldCP.isSameDB(dci.getConnParameters());
//        }
//        return srcDBChanged;
//    }
//
//    /**
//     * Save to configurations
//     *
//     * @return true if successfully
//     */
//    public boolean save() {
//        if (this.conMgrView.getSelectedDCI() == null) {
//            MessageDialog.openError(
//                    getShell(), Messages.msgError, Messages.sourceDBPageErrNoSelectedItem);
//            return false;
//        }
//        final MigrationWizard wzd = getMigrationWizard();
//        Catalog catalog = getCatalog();
//        if (catalog == null) {
//            return false;
//        }
//
//        if (catalog.getDatabaseType().getID() == 1) {
//            removeEmptySchema(catalog);
//        }
//
//        List<String> errorSchemas = new ArrayList<String>();
//        Map<String, String> old2NewSchemaMapping = new HashMap<String, String>();
//        MigrationConfiguration cfg = wzd.getMigrationConfig();
//        cfg.resetSchemaInfo();
//        if (catalog.getDatabaseType().isSupportMultiSchema()
//                && !cfg.getExpEntryTableCfg().isEmpty()) {
//            List<String> expSchemas = cfg.getExpSchemaNames();
//            for (String schema : expSchemas) {
//                if (catalog.getSchemaByName(schema) != null) {
//                    continue;
//                }
//                errorSchemas.add(schema);
//            }
//            if (!errorSchemas.isEmpty()) {
//                List<String> newSchemas = new ArrayList<String>();
//                for (Schema newSchema : catalog.getSchemas()) {
//                    newSchemas.add(newSchema.getName());
//                }
//                old2NewSchemaMapping =
//                        RenameSchemaDialog.renameSchemas(errorSchemas, newSchemas);
//                // Dialog canceled, user maybe want to choose another source.
//                if (old2NewSchemaMapping == null) {
//                    return false;
//                }
//            }
//        }
//
//        // create configuration name
//        if (cfg.getName() == null) {
//            cfg.setName(
//                    catalog.getDatabaseType().getName(),
//                    catalog.getName(),
//                    cfg.getWizardStartDateTime());
//        }
//
//        if (isInputChanged() || wzd.getOriginalSourceCatalog() != catalog) {
//            // If it is a new migration, initialize the configuration
//            wzd.resetBySourceDBChanged();
//            cfg = wzd.getMigrationConfig();
//        }
//        wzd.setOriginalSourceCatalog(catalog);
//        cfg.setSourceConParams(catalog.getConnectionParameters());
//        // Set the invalid schema to right schema or remove them.
//        for (String es : errorSchemas) {
//            String newSchema = old2NewSchemaMapping.get(es);
//            if (StringUtils.isBlank(newSchema)) {
//                cfg.removeExpSchema(es);
//            } else {
//                cfg.renameExpSchema(es, newSchema);
//            }
//        }
//        return true;
//    }
//
//    /**
//     * Remove empty Schema
//     *
//     * @param catalog Catalog
//     */
//    private void removeEmptySchema(Catalog catalog) {
//        List<Schema> schemaList = catalog.getSchemas();
//        List<Schema> removeSchema = new ArrayList<Schema>();
//
//        for (Schema schema : schemaList) {
//            List<Table> tableList = schema.getTables();
//            List<View> viewList = schema.getViews();
//            List<Sequence> sequenceList = schema.getSequenceList();
//            List<Synonym> synonymList = schema.getSynonymList();
//            List<Grant> grantList = schema.getGrantList();
//
//            if (tableList.isEmpty()
//                    && viewList.isEmpty()
//                    && sequenceList.isEmpty()
//                    && synonymList.isEmpty()
//                    && grantList.isEmpty()) {
//                removeSchema.add(schema);
//            }
//        }
//
//        catalog.removeSchema(removeSchema);
//    }
//
//    /** Show */
//    public void show() {
//        conMgrView.show();
//    }
}
