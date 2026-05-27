package com.cubrid.sqlanalyzer.command.tui.page;

import java.util.ArrayList;
import java.util.List;

import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerObjectCountPreviewViewModel;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

public class AnalyzerTuiObjectCountPage {
    public Panel build(AnalyzerObjectCountPreviewViewModel preview) {
        Panel panel = new Panel();
        for (String line : buildLines(preview)) {
            panel.addComponent(new Label(line));
        }
        return panel;
    }

    List<String> buildLines(AnalyzerObjectCountPreviewViewModel preview) {
        List<String> lines = new ArrayList<String>();
        lines.add("[2/4] Object count preview");
        if (preview.sourceType() == AnalyzerSourceType.ORACLE) {
            lines.add("Catalog schemas : " + preview.catalogSchemaCount());
            lines.add("Target tables   : " + preview.targetTableCount());
            lines.add("Target PKs      : " + preview.targetPkCount());
            lines.add("Target FKs      : " + preview.targetFkCount());
            lines.add("Target views    : " + preview.targetViewCount());
            lines.add("Target serials  : " + preview.targetSerialCount());
            lines.add("Target synonyms : " + preview.targetSynonymCount());
            lines.add("Target grants   : " + preview.targetGrantCount());
            lines.add("Target procs    : " + preview.targetProcedureCount());
            lines.add("Target funcs    : " + preview.targetFunctionCount());
            lines.add("Target triggers : " + preview.targetTriggerCount());
            return lines;
        }

        lines.add("SELECT count    : " + preview.selectCount());
        lines.add("INSERT count    : " + preview.insertCount());
        lines.add("UPDATE count    : " + preview.updateCount());
        lines.add("DELETE count    : " + preview.deleteCount());
        return lines;
    }
}
