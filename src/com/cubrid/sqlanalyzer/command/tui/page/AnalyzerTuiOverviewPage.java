package com.cubrid.sqlanalyzer.command.tui.page;

import java.util.ArrayList;
import java.util.List;

import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerSourceOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTargetOverviewViewModel;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

public class AnalyzerTuiOverviewPage {
    public Panel build(AnalyzerOverviewViewModel overview) {
        Panel panel = new Panel();
        for (String line : buildLines(overview)) {
            panel.addComponent(new Label(line));
        }
        return panel;
    }

    List<String> buildLines(AnalyzerOverviewViewModel overview) {
        List<String> lines = new ArrayList<String>();
        lines.add("CUBRID SQL Analyzer");
        lines.add("[1/4] Overview");
        lines.add("Program     : " + formatText(overview.programVersion()));
        appendSource(lines, overview.source());
        appendTarget(lines, overview.source(), overview.target());
        lines.add("Mode        : " + overview.executionMode());
        return lines;
    }

    private void appendSource(List<String> lines, AnalyzerSourceOverviewViewModel source) {
        lines.add("Source      : " + source.type());
        if (source.type() == AnalyzerSourceType.ORACLE) {
            lines.add("Oracle URL  : " + formatText(source.jdbcUrl()) + formatVersionSuffix(source.version()));
            lines.add("Oracle Host : " + formatHost(source.host(), source.port()));
            lines.add("Oracle DB   : " + formatText(source.databaseName()));
            lines.add("Oracle User : " + formatText(source.user()));
            return;
        }

        lines.add("XML dir     : " + formatText(source.xmlDirectory()));
        lines.add("XML charset : " + formatText(source.xmlCharset()));
        lines.add("XML files   : " + source.xmlFileCount());
    }

    private void appendTarget(
            List<String> lines,
            AnalyzerSourceOverviewViewModel source,
            AnalyzerTargetOverviewViewModel target) {
        lines.add("Target      : " + target.type());
        if (target.type() == AnalyzerTargetType.JDBC) {
            lines.add("Target URL  : " + formatText(target.jdbcUrl()) + formatVersionSuffix(target.version()));
            lines.add("Target Host : " + formatHost(target.host(), target.port()));
            lines.add("Target DB   : " + formatText(target.databaseName()));
            lines.add("Target User : " + formatText(target.user()));
            return;
        }

        lines.add("Parser      : " + formatText(target.parserVersion()));
        if (source != null
                && source.xmlDirectory() != null
                && !source.xmlDirectory().isEmpty()) {
            lines.add("XML dir     : " + formatText(source.xmlDirectory()));
            lines.add("XML files   : " + source.xmlFileCount());
        }
    }

    private String formatVersionSuffix(String version) {
        return version == null || version.isEmpty() ? "" : " (" + version + ")";
    }

    private String formatHost(String host, int port) {
        if (host == null || host.isEmpty()) {
            return "";
        }

        return port > 0 ? host + ":" + port : host;
    }

    private String formatText(String value) {
        return value == null ? "" : value;
    }
}
