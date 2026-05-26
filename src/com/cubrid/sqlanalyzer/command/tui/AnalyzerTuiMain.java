package com.cubrid.sqlanalyzer.command.tui;

import java.io.IOException;

import com.cubrid.cubridmigration.core.common.PathUtils;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleArguments;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleConfig;
import com.cubrid.sqlanalyzer.command.AnalyzerConsoleSettingsLoader;
import com.cubrid.sqlanalyzer.command.AnalyzerJdbcConnectionSupport;
import com.cubrid.sqlanalyzer.command.AnalyzerLogInitializer;
import com.cubrid.sqlanalyzer.command.service.AnalyzerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyzerTuiMain {
    private static final Logger LOG = LoggerFactory.getLogger(AnalyzerTuiMain.class);

    public static void main(String[] args) throws IOException {
        PathUtils.initPaths();
        AnalyzerLogInitializer.initLog(AnalyzerConsoleSettingsLoader.loadLogDirectory(args));
        LOG.info("SQL Analyzer TUI command started. argsCount={}", args == null ? 0 : args.length);

        AnalyzerConsoleArguments arguments;
        try {
            arguments = AnalyzerConsoleArguments.parse(
                    AnalyzerConsoleSettingsLoader.loadStartupArguments(args));
        } catch (IllegalArgumentException ex) {
            LOG.error("Failed to parse TUI startup arguments.", ex);
            System.err.println(ex.getMessage());
            System.exit(1);
            return;
        }

        if (arguments.isInteractive()) {
            LOG.error("TUI mode was requested without analyzer options.");
            System.err.println("TUI mode requires analyzer options or -conf <settingsFile>.");
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

        AnalyzerService analyzerService = new AnalyzerService();
        AnalyzerConsoleConfig session = new AnalyzerConsoleConfig();
        try {
            analyzerService.applyArguments(session, arguments);
            analyzerService.prepareConfiguration(session);
            new AnalyzerTuiRunner().start(session, analyzerService);
            LOG.info("SQL Analyzer TUI command finished successfully.");
        } catch (IOException | RuntimeException ex) {
            LOG.error("SQL Analyzer TUI command failed.", ex);
            System.err.println("Analyzer failed: " + ex.getMessage());
            System.exit(1);
        }
    }
}
