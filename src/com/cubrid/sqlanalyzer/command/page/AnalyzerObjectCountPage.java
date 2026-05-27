package com.cubrid.sqlanalyzer.command.page;

import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.ConsoleIO;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerObjectCountPreviewViewModel;

public class AnalyzerObjectCountPage {
    private final ConsoleIO io;

    public AnalyzerObjectCountPage(ConsoleIO io) {
        this.io = io;
    }

    public void render(AnalyzerObjectCountPreviewViewModel preview) {
        io.println("");
        io.println("Object count preview");
        if (preview.sourceType() == AnalyzerSourceType.ORACLE) {
            io.println("Catalog schemas : " + preview.catalogSchemaCount());
            io.println("Target tables   : " + preview.targetTableCount());
            io.println("Target PKs      : " + preview.targetPkCount());
            io.println("Target FKs      : " + preview.targetFkCount());
            io.println("Target views    : " + preview.targetViewCount());
            io.println("Target serials  : " + preview.targetSerialCount());
            io.println("Target synonyms : " + preview.targetSynonymCount());
            io.println("Target grants   : " + preview.targetGrantCount());
            io.println("Target procs    : " + preview.targetProcedureCount());
            io.println("Target funcs    : " + preview.targetFunctionCount());
            io.println("Target triggers : " + preview.targetTriggerCount());
        } else {
            io.println("SELECT count    : " + preview.selectCount());
            io.println("INSERT count    : " + preview.insertCount());
            io.println("UPDATE count    : " + preview.updateCount());
            io.println("DELETE count    : " + preview.deleteCount());
        }
    }
}
