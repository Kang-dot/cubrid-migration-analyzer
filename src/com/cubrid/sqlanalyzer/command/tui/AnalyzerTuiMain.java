package com.cubrid.sqlanalyzer.command.tui;

import java.io.IOException;

import com.cubrid.common.log.LogInitializer;
import com.cubrid.cubridmigration.core.common.PathUtils;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleArguments;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleConfig;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleSettingsLoader;
import com.cubrid.sqlanalyzer.command.AnalyzerJdbcConnectionSupport;
import com.cubrid.sqlanalyzer.command.service.AnalyzerService;

public class AnalyzerTuiMain {
    public static void main(String[] args) throws IOException {
        PathUtils.initPaths();
        LogInitializer.initLog(PathUtils.getLogDir());

        AnalyzerConsoleArguments arguments;
        try {
            arguments = AnalyzerConsoleArguments.parse(
                    AnalyzerConsoleSettingsLoader.loadStartupArguments(args));
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
            return;
        }

        if (arguments.isInteractive()) {
            System.err.println("TUI mode requires analyzer options or -conf <settingsFile>.");
            System.exit(1);
            return;
        }

        if (arguments.getJdbcRepositoryDir() != null
                && !arguments.getJdbcRepositoryDir().isEmpty()) {
            AnalyzerJdbcConnectionSupport.configureJdbcRepository(arguments.getJdbcRepositoryDir());
        }
        AnalyzerJdbcConnectionSupport.initializeJdbcDrivers();

        AnalyzerService analyzerService = new AnalyzerService();
        AnalyzerConsoleConfig session = new AnalyzerConsoleConfig();
        analyzerService.applyArguments(session, arguments);
        analyzerService.prepareConfiguration(session);
        new AnalyzerTuiRunner().start(session, analyzerService);
    }
}
