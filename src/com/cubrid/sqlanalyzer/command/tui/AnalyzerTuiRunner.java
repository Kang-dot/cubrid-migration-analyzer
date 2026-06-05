package com.cubrid.sqlanalyzer.command.tui;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.cubrid.sqlanalyzer.command.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.service.AnalyzerService;
import com.cubrid.sqlanalyzer.command.tui.page.AnalyzerTuiObjectCountPage;
import com.cubrid.sqlanalyzer.command.tui.page.AnalyzerTuiOverviewPage;
import com.cubrid.sqlanalyzer.command.tui.page.AnalyzerTuiProgressPage;
import com.cubrid.sqlanalyzer.command.tui.page.AnalyzerTuiProgressPage.ProgressView;
import com.cubrid.sqlanalyzer.command.tui.page.AnalyzerTuiResultPage;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerObjectCountPreviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerResultViewModel;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyzerTuiRunner {
    private static final TerminalSize DEFAULT_TERMINAL_SIZE = new TerminalSize(100, 30);
    private static final int METADATA_LOADING_TICK_MILLIS = 1000;
    private static final Logger LOG = LoggerFactory.getLogger(AnalyzerTuiRunner.class);

    private final AnalyzerTuiOverviewPage overviewPage;
    private final AnalyzerTuiObjectCountPage objectCountPage;
    private final AnalyzerTuiProgressPage progressPage;
    private final AnalyzerTuiResultPage resultPage;

    public AnalyzerTuiRunner() {
        this(
                new AnalyzerTuiOverviewPage(),
                new AnalyzerTuiObjectCountPage(),
                new AnalyzerTuiProgressPage(),
                new AnalyzerTuiResultPage());
    }

    AnalyzerTuiRunner(
            AnalyzerTuiOverviewPage overviewPage,
            AnalyzerTuiObjectCountPage objectCountPage,
            AnalyzerTuiProgressPage progressPage,
            AnalyzerTuiResultPage resultPage) {
        this.overviewPage = overviewPage;
        this.objectCountPage = objectCountPage;
        this.progressPage = progressPage;
        this.resultPage = resultPage;
    }

    public void start(AnalyzerSession session, AnalyzerService analyzerService) throws IOException {
        LOG.info("Starting TUI runner.");
        try (Screen screen = new DefaultTerminalFactory()
                .setInitialTerminalSize(DEFAULT_TERMINAL_SIZE)
                .createScreen()) {
            screen.startScreen();
            MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
            BasicWindow window = new BasicWindow("CUBRID SQL Analyzer");
            NavigationState state = new NavigationState();

            showOverview(window, gui, session, analyzerService, state);
            gui.addWindowAndWait(window);
        }
        LOG.info("TUI runner stopped.");
    }

    public void showOverview(AnalyzerOverviewViewModel overview) throws IOException {
        try (Screen screen = new DefaultTerminalFactory()
                .setInitialTerminalSize(DEFAULT_TERMINAL_SIZE)
                .createScreen()) {
            screen.startScreen();
            MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
            gui.addWindowAndWait(buildOverviewWindow(overview));
        }
    }

    BasicWindow buildOverviewWindow(AnalyzerOverviewViewModel overview) {
        BasicWindow window = new BasicWindow("CUBRID SQL Analyzer");
        Panel content = overviewPage.build(overview);
        content.setLayoutManager(new LinearLayout());
        Button closeButton = new Button("Close", window::close);
        content.addComponent(closeButton);
        setContent(window, content, closeButton);
        return window;
    }

    private void showOverview(
            BasicWindow window,
            MultiWindowTextGUI gui,
            AnalyzerSession session,
            AnalyzerService analyzerService,
            NavigationState state) {
        AnalyzerOverviewViewModel overview = analyzerService.getOverview(session);
        Panel content = withLayout(overviewPage.build(overview));
        Runnable nextAction = () -> showMetadataLoadingAndObjectCount(window, gui, session, analyzerService, state);
        Button nextButton = new Button("Next", nextAction);
        content.addComponent(nextButton);
        content.addComponent(new Button("Close", window::close));
        setContent(window, content, nextButton);
    }

    private void showObjectCount(
            BasicWindow window,
            MultiWindowTextGUI gui,
            AnalyzerSession session,
            AnalyzerService analyzerService,
            NavigationState state) {
        try {
            AnalyzerObjectCountPreviewViewModel preview = analyzerService.getObjectCountPreview(session);
            Panel content = withLayout(objectCountPage.build(preview));
            Runnable analyzeAction = () -> showProgressAndRun(window, gui, session, analyzerService);
            Button analyzeButton = new Button("Analyze", analyzeAction);
            content.addComponent(analyzeButton);
            content.addComponent(new Button("Back", () -> showOverview(window, gui, session, analyzerService, state)));
            content.addComponent(new Button("Close", window::close));
            setContent(window, content, analyzeButton);
        } catch (RuntimeException ex) {
            showError(window, ex);
        }
    }

    private void showMetadataLoadingAndObjectCount(
            BasicWindow window,
            MultiWindowTextGUI gui,
            AnalyzerSession session,
            AnalyzerService analyzerService,
            NavigationState state) {
        if (state.sourceLoaded) {
            LOG.info("Source metadata already loaded. Showing object count page.");
            showObjectCount(window, gui, session, analyzerService, state);
            return;
        }

        LOG.info("Showing metadata loading page.");
        Panel content = new Panel();
        content.setLayoutManager(new LinearLayout());
        Label loadingStatus = new Label("Loading source metadata");
        content.addComponent(loadingStatus);
        content.addComponent(new Label("The object count page will open when metadata is ready."));
        window.setComponent(content);
        AtomicBoolean loading = new AtomicBoolean(true);
        startMetadataLoadingAnimation(gui, loadingStatus, loading);

        Thread worker = new Thread(
                () -> loadMetadataAndShowObjectCount(window, gui, session, analyzerService, state, loading),
                "analyzer-tui-metadata");
        worker.setDaemon(true);
        worker.start();
    }

    private void startMetadataLoadingAnimation(
            MultiWindowTextGUI gui,
            Label loadingStatus,
            AtomicBoolean loading) {
        AtomicInteger tick = new AtomicInteger();
        Thread animator = new Thread(
                () -> {
                    while (loading.get()) {
                        int currentTick = tick.getAndIncrement();
                        String dots = ".".repeat(currentTick % 4);
                        try {
                            gui.getGUIThread().invokeLater(
                                    () -> loadingStatus.setText("Loading source metadata" + dots));
                        } catch (IllegalStateException ex) {
                            return;
                        }
                        try {
                            Thread.sleep(METADATA_LOADING_TICK_MILLIS);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                },
                "analyzer-tui-metadata-animation");
        animator.setDaemon(true);
        animator.start();
    }

    private void loadMetadataAndShowObjectCount(
            BasicWindow window,
            MultiWindowTextGUI gui,
            AnalyzerSession session,
            AnalyzerService analyzerService,
            NavigationState state,
            AtomicBoolean loading) {
        try {
            LOG.info("Loading source metadata in TUI worker.");
            analyzerService.loadSourceCatalog(session);
            state.sourceLoaded = true;
            loading.set(false);
            gui.getGUIThread().invokeLater(
                    () -> showObjectCount(window, gui, session, analyzerService, state));
        } catch (Throwable ex) {
            loading.set(false);
            LOG.error("Failed to load source metadata in TUI worker.", ex);
            gui.getGUIThread().invokeLater(() -> showError(window, ex));
        }
    }

    private void showProgressAndRun(
            BasicWindow window,
            MultiWindowTextGUI gui,
            AnalyzerSession session,
            AnalyzerService analyzerService) {
        LOG.info("Showing TUI progress page and starting analysis worker.");
        ProgressView progressView = progressPage.buildView();
        Panel content = withLayout(progressView.getPanel());
        window.setComponent(content);
        Thread worker = new Thread(
                () -> runAnalysisAndShowResultButton(
                        window, gui, session, analyzerService, content, progressView),
                "analyzer-tui-analysis");
        worker.setDaemon(true);
        worker.start();
    }

    private void runAnalysisAndShowResultButton(
            BasicWindow window,
            MultiWindowTextGUI gui,
            AnalyzerSession session,
            AnalyzerService analyzerService,
            Panel content,
            ProgressView progressView) {
        try {
            LOG.info("Running analysis in TUI worker.");
            analyzerService.runAnalysis(
                    session,
                    event -> gui.getGUIThread().invokeLater(() -> progressView.update(event)));
            AnalyzerResultViewModel result = analyzerService.saveResult(session);
            gui.getGUIThread().invokeLater(
                    () -> {
                        progressView.markCompleted();
                        Button resultButton = new Button("Result", () -> showResult(window, result));
                        content.addComponent(resultButton);
                        window.setFocusedInteractable(resultButton);
                    });
        } catch (Throwable ex) {
            LOG.error("Analysis failed in TUI worker.", ex);
            gui.getGUIThread().invokeLater(() -> showError(window, ex));
        }
    }

    private void showResult(BasicWindow window, AnalyzerResultViewModel result) {
        LOG.info("Showing TUI result page.");
        Panel content = withLayout(resultPage.build(result));
        Button closeButton = new Button("Close", window::close);
        content.addComponent(closeButton);
        setContent(window, content, closeButton);
    }

    private void showError(BasicWindow window, Throwable ex) {
        LOG.error("Showing TUI error page.", ex);
        Panel content = new Panel();
        content.setLayoutManager(new LinearLayout());
        content.addComponent(new Label("Analyzer failed"));
        content.addComponent(new Label(ex.getMessage() == null ? ex.toString() : ex.getMessage()));
        Button closeButton = new Button("Close", window::close);
        content.addComponent(closeButton);
        setContent(window, content, closeButton);
    }

    private Panel withLayout(Panel panel) {
        panel.setLayoutManager(new LinearLayout());
        return panel;
    }

    private void setContent(BasicWindow window, Panel content, Button primaryButton) {
        window.setComponent(content);
        window.setFocusedInteractable(primaryButton);
    }

    private static class NavigationState {
        private boolean sourceLoaded;
    }
}
