package com.cubrid.sqlanalyzer.command.tui.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerObjectCountPreviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTableSizeViewModel;
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
            lines.add("");
            lines.add("Oracle table size total : " + formatBytes(preview.totalTableBytes()));
            lines.add("Oracle table sizes");
            if (preview.tableSizes().isEmpty()) {
                lines.add("  (none)");
            } else {
                lines.add("  Table                         Size");
                for (AnalyzerTableSizeViewModel tableSize : preview.tableSizes()) {
                    lines.add(
                            String.format(
                                    Locale.US,
                                    "  %-28s %10s",
                                    fitText(tableSize.tableName(), 28),
                                    formatBytes(tableSize.bytes())));
                }
            }
            return lines;
        }

        lines.add("SELECT count    : " + preview.selectCount());
        lines.add("INSERT count    : " + preview.insertCount());
        lines.add("UPDATE count    : " + preview.updateCount());
        lines.add("DELETE count    : " + preview.deleteCount());
        return lines;
    }

    private String formatBytes(long bytes) {
        long safeBytes = Math.max(0L, bytes);
        if (safeBytes < 1024L) {
            return safeBytes + " B";
        }

        double value = safeBytes;
        String[] units = { "B", "KB", "MB", "GB", "TB", "PB" };
        int unitIndex = 0;
        while (value >= 1024.0 && unitIndex < units.length - 1) {
            value /= 1024.0;
            unitIndex++;
        }
        return String.format(Locale.US, "%.2f %s", value, units[unitIndex]);
    }

    private String fitText(String value, int maxLength) {
        String text = value == null ? "" : value;
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 1) + ".";
    }
}
