package com.cubrid.sqlanalyzer.command.tui;

import java.io.IOException;

import com.cubrid.sqlanalyzer.command.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.service.AnalyzerService;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;

public class AnalyzerTuiOverviewPreviewMain {
    public static void main(String[] args) throws IOException {
        AnalyzerOverviewViewModel overview = new AnalyzerService().getOverview(createPreviewSession());
        new AnalyzerTuiRunner().showOverview(overview);
    }

    private static AnalyzerSession createPreviewSession() {
        AnalyzerSession session = new AnalyzerSession();
        session.setSourceType(AnalyzerSourceType.XML);
        session.setXmlDirectory("./sqlmap");
        session.setXmlCharset("UTF-8");
        session.setTargetType(AnalyzerTargetType.PARSER);
        session.setExecutionMode(AnalyzerExecutionMode.DML);
        return session;
    }
}
