package com.cubrid.sqlanalyzer.command;

import com.cubrid.common.log.LogInitializer;
import com.cubrid.cubridmigration.core.common.PathUtils;

public class AnalyzerConsoleMain {
    public static void main(String[] args) {
        PathUtils.initPaths();
        LogInitializer.initLog(PathUtils.getLogDir());

        AnalyzerConsoleArguments arguments;
        try {
            arguments = AnalyzerConsoleArguments.parse(args);
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

        ConsoleIO io = new AnalyzerConsoleIOController(System.in, System.out);
        AnalyzerConsoleRunner runner = new AnalyzerConsoleRunner(io);
        int exitCode =
                arguments.isInteractive()
                        ? runner.startAnalyzer()
                        : runner.startAnalyzer(arguments);
        System.exit(exitCode);
    }
}
