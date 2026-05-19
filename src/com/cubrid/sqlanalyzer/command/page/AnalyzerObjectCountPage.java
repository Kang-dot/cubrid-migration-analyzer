package com.cubrid.sqlanalyzer.command.page;

import com.cubrid.sqlanalyzer.command.AnalyzerConsoleConfig;
import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.ConsoleIO;
import com.cubrid.sqlanalyzer.command.service.AnalyzerService;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;

public class AnalyzerObjectCountPage {
    private final ConsoleIO io;
    private final AnalyzerService analyzerService;

    public AnalyzerObjectCountPage(ConsoleIO io, AnalyzerService analyzerService) {
        this.io = io;
        this.analyzerService = analyzerService;
    }

    public void render(AnalyzerConsoleConfig session) {
        AnalyzerConfiguration config = session.getConfig();

        io.println("");
        io.println("Object count preview");
        if (session.getSourceType() == AnalyzerSourceType.ORACLE) {
            long targetPkCount = analyzerService.countTargetPrimaryKeys(config.getTargetTableSchema());
            long targetFkCount = analyzerService.countTargetForeignKeys(config.getTargetTableSchema());

            io.println("Catalog schemas : " + session.getSourceCatalog().getSchemas().size());
            io.println("Target tables   : " + config.getTargetTableSchema().size());
            io.println("Target PKs      : " + targetPkCount);
            io.println("Target FKs      : " + targetFkCount);
            io.println("Target views    : " + config.getTargetViewSchema().size());
            io.println("Target serials  : " + config.getTargetSerialSchema().size());
            io.println("Target synonyms : " + config.getTargetSynonymSchema().size());
            io.println("Target grants   : " + config.getExpGrantCfg().size());
            io.println("Target procs    : " + config.getTargetPlcsqlProcedureSchema().size());
            io.println("Target funcs    : " + config.getTargetPlcsqlFunctionSchema().size());
        } else {
            QueryDictionary dict = config.getQueryDict();
            io.println("SELECT count    : " + dict.getSelectQueryMap().size());
            io.println("INSERT count    : " + dict.getInsertQueryMap().size());
            io.println("UPDATE count    : " + dict.getUpdateQueryMap().size());
            io.println("DELETE count    : " + dict.getDeleteQueryMap().size());
        }
    }
}
