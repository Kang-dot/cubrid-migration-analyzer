package com.cubrid.sqlanalyzer.command.page;

import com.cubrid.sqlanalyzer.command.AnalyzerConsoleConfig;
import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.ConsoleIO;
import com.cubrid.sqlanalyzer.command.service.AnalyzerService;

public class AnalyzerOverviewPage {
    private final ConsoleIO io;
    private final AnalyzerService analyzerService;

    public AnalyzerOverviewPage(ConsoleIO io, AnalyzerService analyzerService) {
        this.io = io;
        this.analyzerService = analyzerService;
    }

    public void render(AnalyzerConsoleConfig session) {
        io.println("");
        io.println("[3/4] Overview");
        io.println("Source      : " + session.getSourceType());
        if (session.getSourceType() == AnalyzerSourceType.ORACLE) {
            io.println(
                    "Oracle URL  : "
                            + session.getSourceJdbcUrl()
                            + analyzerService.buildOracleVersionSuffix(session));
            io.println("Oracle User : " + session.getSourceUser());
        } else {
            io.println("XML dir     : " + session.getXmlDirectory());
            io.println("XML charset : " + session.getXmlCharset());
        }

        io.println("Target      : " + session.getTargetType());
        if (session.getTargetType() == AnalyzerTargetType.JDBC) {
            io.println(
                    "Target URL  : "
                            + session.getTargetJdbcUrl()
                            + analyzerService.buildTargetVersionSuffix(session));
        } else if (session.getTargetType() == AnalyzerTargetType.PARSER
                && session.getXmlDirectory() != null
                && !session.getXmlDirectory().isEmpty()) {
            io.println("XML dir     : " + session.getXmlDirectory());
        }
        io.println("Mode        : " + session.getExecutionMode());
    }
}
