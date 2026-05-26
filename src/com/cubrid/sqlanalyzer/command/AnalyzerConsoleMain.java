package com.cubrid.sqlanalyzer.command;

import java.io.IOException;

import com.cubrid.cubridmigration.core.common.PathUtils;
import com.cubrid.sqlanalyzer.command.service.AnalyzerService;
import com.cubrid.sqlanalyzer.command.tui.AnalyzerTuiRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyzerConsoleMain {
    private static final Logger LOG = LoggerFactory.getLogger(AnalyzerConsoleMain.class);

    public static void main(String[] args) {
        PathUtils.initPaths();
        AnalyzerLogInitializer.initLog(AnalyzerConsoleSettingsLoader.loadLogDirectory(args));
        LOG.info("SQL Analyzer command started. argsCount={}", args == null ? 0 : args.length);

        AnalyzerConsoleArguments arguments;
        try {
            arguments =
                    AnalyzerConsoleArguments.parse(
                            AnalyzerConsoleSettingsLoader.loadStartupArguments(args));
        } catch (IllegalArgumentException ex) {
            LOG.error("Failed to parse analyzer startup arguments.", ex);
            System.err.println(ex.getMessage());
            System.exit(1);
            return;
        }

        if (arguments.getJdbcRepositoryDir() != null
                && !arguments.getJdbcRepositoryDir().isEmpty()) {
            LOG.info("Configuring JDBC repository. path={}", arguments.getJdbcRepositoryDir());
            AnalyzerJdbcConnectionSupport.configureJdbcRepository(arguments.getJdbcRepositoryDir());
        }

        LOG.info("Initializing JDBC drivers.");
        AnalyzerJdbcConnectionSupport.initializeJdbcDrivers();

        if (arguments.isTuiMode()) {
            LOG.info("Starting analyzer in TUI mode.");
            System.exit(startTuiAnalyzer(arguments));
            return;
        }

        LOG.info("Starting analyzer in console mode. interactive={}", arguments.isInteractive());
        ConsoleIO io = new AnalyzerConsoleIOController(System.in, System.out);
        AnalyzerConsoleRunner runner = new AnalyzerConsoleRunner(io);
        int exitCode =
                arguments.isInteractive()
                        ? runner.startAnalyzer()
                        : runner.startAnalyzer(arguments);
        LOG.info("SQL Analyzer command finished. exitCode={}", exitCode);
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
            LOG.info("TUI analyzer finished successfully.");
            return 0;
        } catch (IOException | RuntimeException ex) {
            LOG.error("TUI analyzer failed.", ex);
            System.err.println("Analyzer failed: " + ex.getMessage());
            ex.printStackTrace();
            return 1;
        }
    }
}
