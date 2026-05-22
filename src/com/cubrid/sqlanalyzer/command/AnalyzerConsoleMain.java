package com.cubrid.sqlanalyzer.command;

import java.io.IOException;

import com.cubrid.common.log.LogInitializer;
import com.cubrid.cubridmigration.core.common.PathUtils;
import com.cubrid.sqlanalyzer.command.service.AnalyzerService;
import com.cubrid.sqlanalyzer.command.tui.AnalyzerTuiRunner;

public class AnalyzerConsoleMain {
    public static void main(String[] args) {
        PathUtils.initPaths();
        LogInitializer.initLog(PathUtils.getLogDir());

        AnalyzerConsoleArguments arguments;
        try {
            arguments =
                    AnalyzerConsoleArguments.parse(
                            AnalyzerConsoleSettingsLoader.loadStartupArguments(args));
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
            return;
        }

        if (arguments.getJdbcRepositoryDir() != null
                && !arguments.getJdbcRepositoryDir().isEmpty()) {
            AnalyzerJdbcConnectionSupport.configureJdbcRepository(arguments.getJdbcRepositoryDir());
        }

        AnalyzerJdbcConnectionSupport.initializeJdbcDrivers();

        if (arguments.isTuiMode()) {
            System.exit(startTuiAnalyzer(arguments));
            return;
        }

        ConsoleIO io = new AnalyzerConsoleIOController(System.in, System.out);
        AnalyzerConsoleRunner runner = new AnalyzerConsoleRunner(io);
        int exitCode =
                arguments.isInteractive()
                        ? runner.startAnalyzer()
                        : runner.startAnalyzer(arguments);
        System.exit(exitCode);
    }

    private static int startTuiAnalyzer(AnalyzerConsoleArguments arguments) {
        if (arguments.isInteractive()) {
            System.err.println("TUI mode requires analyzer options or -conf <settingsFile>.");
            return 1;
        }

        AnalyzerService analyzerService = new AnalyzerService();
        AnalyzerConsoleConfig session = new AnalyzerConsoleConfig();
        try {
            analyzerService.applyArguments(session, arguments);
            analyzerService.prepareConfiguration(session);
            new AnalyzerTuiRunner().start(session, analyzerService);
            return 0;
        } catch (IOException | RuntimeException ex) {
            System.err.println("Analyzer failed: " + ex.getMessage());
            ex.printStackTrace();
            return 1;
        }
    }
}
