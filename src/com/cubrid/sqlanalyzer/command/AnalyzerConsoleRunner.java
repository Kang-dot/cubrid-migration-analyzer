package com.cubrid.sqlanalyzer.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cubrid.sqlanalyzer.command.page.AnalyzerObjectCountPage;
import com.cubrid.sqlanalyzer.command.page.AnalyzerOverviewPage;
import com.cubrid.sqlanalyzer.command.page.AnalyzerResultPage;
import com.cubrid.sqlanalyzer.command.service.AnalyzerService;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressEventViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressStage;

public class AnalyzerConsoleRunner {
    private static final String DEFAULT_XML_CHARSET = "UTF-8";
    private static final Logger LOG = LoggerFactory.getLogger(AnalyzerConsoleRunner.class);

    private final ConsoleIO io;
    private final AnalyzerService analyzerService;
    private final AnalyzerOverviewPage overviewPage;
    private final AnalyzerObjectCountPage objectCountPage;
    private final AnalyzerResultPage resultPage;

    public AnalyzerConsoleRunner(ConsoleIO io) {
        this.io = io;
        this.analyzerService = new AnalyzerService();
        this.overviewPage = new AnalyzerOverviewPage(io);
        this.objectCountPage = new AnalyzerObjectCountPage(io);
        this.resultPage = new AnalyzerResultPage(io);
    }

    public int startAnalyzer() {
        LOG.info("Interactive console analyzer flow started.");
        io.println("======================================");
        io.println("CUBRID SQL Analyzer Console");
        io.println("======================================");

        AnalyzerSession session = new AnalyzerSession();

        try {
            selectSource(session);
            selectTarget(session);
            analyzerService.applyExecutionMode(session);
            analyzerService.prepareConfiguration(session);
            loadSourceCatalog(session);
            renderPreviewPages(session);

            if (!io.confirm("Continue analysis? (y/n): ")) {
                io.println("Analysis canceled.");
                return 0;
            }

            runAnalysis(session);
            printResult(session);
            LOG.info("Interactive console analyzer flow finished successfully.");
            return 0;
        } catch (RuntimeException ex) {
            LOG.error("Interactive console analyzer flow failed.", ex);
            io.println("Analyzer failed: " + ex.getMessage());
            ex.printStackTrace();
            return 1;
        }
    }

    public int startAnalyzer(AnalyzerArgumentsController arguments) {
        LOG.info("Non-interactive console analyzer flow started.");
        io.println("======================================");
        io.println("CUBRID SQL Analyzer Console");
        io.println("======================================");

        AnalyzerSession session = new AnalyzerSession();

        try {
            analyzerService.applyArguments(session, arguments);
            printConnectionValidationMessages(arguments);
            analyzerService.prepareConfiguration(session);
            loadSourceCatalog(session);
            renderPreviewPages(session);
            runAnalysis(session);
            printResult(session);
            LOG.info("Non-interactive console analyzer flow finished successfully.");
            return 0;
        } catch (RuntimeException ex) {
            LOG.error("Non-interactive console analyzer flow failed.", ex);
            io.println("Analyzer failed: " + ex.getMessage());
            ex.printStackTrace();
            return 1;
        }
    }

    private void selectSource(AnalyzerSession session) {
        io.println("");
        io.println("[1/4] Select source");
        io.println("1. Oracle JDBC connection");
        io.println("2. XML directory");

        while (true) {
            String input = io.readRequired("Select source (1-2): ");
            if ("1".equals(input)) {
                session.setSourceType(AnalyzerSourceType.ORACLE);
                promptOracleSource(session);
                return;
            }
            if ("2".equals(input)) {
                session.setSourceType(AnalyzerSourceType.XML);
                promptXmlSource(session);
                return;
            }
            io.println("Invalid selection.");
        }
    }

    private void promptOracleSource(AnalyzerSession session) {
        while (true) {
            session.setSourceJdbcUrl(io.readRequired("Oracle JDBC URL: "));
            session.setSourceUser(io.readRequired("Oracle user: "));
            session.setSourcePassword(io.readRequired("Oracle password: "));

            try {
                analyzerService.validateOracleSourceConnection(session);
                io.println("Oracle connection validation succeeded.");
                return;
            } catch (RuntimeException ex) {
                io.println(ex.getMessage());
                if (!io.confirm("Retry Oracle connection input? (y/n): ")) {
                    throw ex;
                }
            }
        }
    }

    private void promptXmlSource(AnalyzerSession session) {
        session.setXmlDirectory(io.readRequired("XML directory path: "));
        String charset = readLineWithDefault("XML charset [UTF-8]: ", DEFAULT_XML_CHARSET);
        session.setXmlCharset(charset.isEmpty() ? DEFAULT_XML_CHARSET : charset);
    }

    private void selectTarget(AnalyzerSession session) {
        io.println("");
        io.println("[2/4] Select target");
        io.println("1. CUBRID JDBC execution");
        io.println("2. Parser execution");

        while (true) {
            String input = io.readRequired("Select target (1-2): ");
            if ("1".equals(input)) {
                session.setTargetType(AnalyzerTargetType.JDBC);
                promptJdbcTarget(session);
                return;
            }
            if ("2".equals(input)) {
                session.setTargetType(AnalyzerTargetType.PARSER);
                return;
            }
            io.println("Invalid selection.");
        }
    }

    private void promptJdbcTarget(AnalyzerSession session) {
        while (true) {
            session.setTargetJdbcUrl(io.readRequired("Target JDBC URL: "));
            session.setTargetUser(io.readRequired("Target user: "));
            session.setTargetPassword(io.readRequired("Target password: "));

            try {
                analyzerService.validateJdbcTargetConnection(session);
                io.println("Target connection validation succeeded.");
                return;
            } catch (RuntimeException ex) {
                io.println(ex.getMessage());
                if (!io.confirm("Retry target connection input? (y/n): ")) {
                    throw ex;
                }
            }
        }
    }

    private void printConnectionValidationMessages(AnalyzerArgumentsController arguments) {
        if (AnalyzerSourceType.ORACLE.equals(arguments.getSourceType())) {
            io.println("Oracle connection validation succeeded.");
        }

        if (AnalyzerTargetType.JDBC.equals(arguments.getTargetType())) {
            io.println("Target connection validation succeeded.");
        }
    }

    private void loadSourceCatalog(AnalyzerSession session) {
        LOG.info("Loading source metadata. sourceType={}", session.getSourceType());
        io.println("");
        io.println("Loading source metadata...");
        analyzerService.loadSourceCatalog(session);
        if (session.getSourceType() == AnalyzerSourceType.ORACLE) {
            io.println("Oracle catalog loaded.");
        } else if (session.getSourceType() == AnalyzerSourceType.XML) {
            io.println("XML query dictionary loaded.");
        }
    }

    private void renderPreviewPages(AnalyzerSession session) {
        overviewPage.render(analyzerService.getOverview(session));
        objectCountPage.render(analyzerService.getObjectCountPreview(session));
    }

    private void runAnalysis(AnalyzerSession session) {
        LOG.info("Console analysis progress started.");
        io.println("");
        io.println("[4/4] Analysis progress");
        analyzerService.runAnalysis(
                session,
                event -> {
                    String message = formatProgressEvent(event);
                    if (message != null && !message.isEmpty()) {
                        io.println(message);
                    }
                });
        LOG.info("Console analysis progress finished.");
    }

    private void printResult(AnalyzerSession session) {
        resultPage.render(analyzerService.saveResult(session));
    }

    private String formatProgressEvent(AnalyzerProgressEventViewModel event) {
        if (event.stage() == AnalyzerProgressStage.PLANNING) {
            return "";
        }
        if (event.message() != null && !event.message().isEmpty()) {
            return event.message();
        }
        return String.valueOf(event.stage());
    }

    private String readLineWithDefault(String prompt, String defaultValue) {
        io.print(prompt);
        String input = io.readLine();
        return input.isEmpty() ? defaultValue : input;
    }
}
