package com.cubrid.sqlanalyzer.command.page;

import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.ConsoleIO;
import com.cubrid.sqlanalyzer.command.dto.AnalyzerOverview;
import com.cubrid.sqlanalyzer.command.dto.AnalyzerSourceOverview;
import com.cubrid.sqlanalyzer.command.dto.AnalyzerTargetOverview;

public class AnalyzerOverviewPage {
    private final ConsoleIO io;

    public AnalyzerOverviewPage(ConsoleIO io) {
        this.io = io;
    }

    public void render(AnalyzerOverview overview) {
        io.println("");
        io.println("[3/4] Overview");
        io.println("Program     : " + formatText(overview.programVersion()));
        renderSource(overview.source());
        renderTarget(overview.source(), overview.target());
        io.println("Mode        : " + overview.executionMode());
    }

    private void renderSource(AnalyzerSourceOverview source) {
        io.println("Source      : " + source.type());
        if (source.type() == AnalyzerSourceType.ORACLE) {
            io.println("Oracle URL  : " + formatText(source.jdbcUrl()) + formatVersionSuffix(source.version()));
            io.println("Oracle Host : " + formatHost(source.host(), source.port()));
            io.println("Oracle DB   : " + formatText(source.databaseName()));
            io.println("Oracle User : " + formatText(source.user()));
        } else {
            io.println("XML dir     : " + formatText(source.xmlDirectory()));
            io.println("XML charset : " + formatText(source.xmlCharset()));
            io.println("XML files   : " + source.xmlFileCount());
        }
    }

    private void renderTarget(AnalyzerSourceOverview source, AnalyzerTargetOverview target) {
        io.println("Target      : " + target.type());
        if (target.type() == AnalyzerTargetType.JDBC) {
            io.println("Target URL  : " + formatText(target.jdbcUrl()) + formatVersionSuffix(target.version()));
            io.println("Target Host : " + formatHost(target.host(), target.port()));
            io.println("Target DB   : " + formatText(target.databaseName()));
            io.println("Target User : " + formatText(target.user()));
        } else if (target.type() == AnalyzerTargetType.PARSER
                && source.xmlDirectory() != null
                && !source.xmlDirectory().isEmpty()) {
            io.println("Parser      : " + formatText(target.parserVersion()));
            io.println("XML dir     : " + formatText(source.xmlDirectory()));
        } else if (target.type() == AnalyzerTargetType.PARSER) {
            io.println("Parser      : " + formatText(target.parserVersion()));
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
