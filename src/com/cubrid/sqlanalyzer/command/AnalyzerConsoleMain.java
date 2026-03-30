package com.cubrid.sqlanalyzer.command;

import com.cubrid.common.log.LogInitializer;
import com.cubrid.cubridmigration.core.common.PathUtils;

public class AnalyzerConsoleMain {
    public static void main(String[] args) {
        PathUtils.initPaths();
        LogInitializer.initLog(PathUtils.getLogDir());

        if (args.length > 0 && args[0] != null && !args[0].isEmpty()) {
            AnalyzerJdbcConnectionSupport.configureJdbcRepository(args[0]);
        }

        AnalyzerJdbcConnectionSupport.initializeJdbcDrivers();

        ConsoleIO io = new AnalyzerConsoleIOController(System.in, System.out);
        AnalyzerConsoleRunner runner = new AnalyzerConsoleRunner(io);
        int exitCode = runner.startAnalyzer();
        System.exit(exitCode);
    }
}
