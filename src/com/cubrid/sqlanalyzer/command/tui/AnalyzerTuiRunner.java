package com.cubrid.sqlanalyzer.command.tui;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.cubrid.sqlanalyzer.command.AnalyzerConsoleConfig;
import com.cubrid.sqlanalyzer.command.service.AnalyzerService;
import com.cubrid.sqlanalyzer.command.tui.page.AnalyzerTuiObjectCountPage;
import com.cubrid.sqlanalyzer.command.tui.page.AnalyzerTuiOverviewPage;
import com.cubrid.sqlanalyzer.command.tui.page.AnalyzerTuiProgressPage;
import com.cubrid.sqlanalyzer.command.tui.page.AnalyzerTuiProgressPage.ProgressView;
import com.cubrid.sqlanalyzer.command.tui.page.AnalyzerTuiResultPage;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerObjectCountPreviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerResultViewModel;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowListener;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

public class AnalyzerTuiRunner {
    private static final TerminalSize DEFAULT_TERMINAL_SIZE = new TerminalSize(100, 30);

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

    public void start(AnalyzerConsoleConfig session, AnalyzerService analyzerService) throws IOException {
        try (Screen screen = new DefaultTerminalFactory()
                .setInitialTerminalSize(DEFAULT_TERMINAL_SIZE)
                .createScreen()) {
            screen.startScreen();
            MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
            BasicWindow window = new BasicWindow("CUBRID SQL Analyzer");
            NavigationState state = new NavigationState();
            window.addWindowListener(new EnterKeyListener(state));

            showOverview(window, gui, session, analyzerService, state);
            gui.addWindowAndWait(window);
        }
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
            AnalyzerConsoleConfig session,
            AnalyzerService analyzerService,
            NavigationState state) {
        AnalyzerOverviewViewModel overview = analyzerService.getOverview(session);
        Panel content = withLayout(overviewPage.build(overview));
        Runnable nextAction = () -> showMetadataLoadingAndObjectCount(window, gui, session, analyzerService, state);
        Button nextButton = new Button("Next (Enter)", nextAction);
        content.addComponent(nextButton);
        content.addComponent(new Button("Close", window::close));
        setContent(window, content, nextButton, state, nextAction);
    }

    private void showObjectCount(
            BasicWindow window,
            MultiWindowTextGUI gui,
            AnalyzerConsoleConfig session,
            AnalyzerService analyzerService,
            NavigationState state) {
        try {
            AnalyzerObjectCountPreviewViewModel preview = analyzerService.getObjectCountPreview(session);
            Panel content = withLayout(objectCountPage.build(preview));
            Runnable analyzeAction = () -> showProgressAndRun(window, gui, session, analyzerService, state);
            Button analyzeButton = new Button("Analyze (Enter)", analyzeAction);
            content.addComponent(analyzeButton);
            content.addComponent(new Button("Back", () -> showOverview(window, gui, session, analyzerService, state)));
            content.addComponent(new Button("Close", window::close));
            setContent(window, content, analyzeButton, state, analyzeAction);
        } catch (RuntimeException ex) {
            showError(window, state, ex);
        }
    }

    private void showMetadataLoadingAndObjectCount(
            BasicWindow window,
            MultiWindowTextGUI gui,
            AnalyzerConsoleConfig session,
            AnalyzerService analyzerService,
            NavigationState state) {
        if (state.sourceLoaded) {
            showObjectCount(window, gui, session, analyzerService, state);
            return;
        }

        state.enterAction = null;
        Panel content = new Panel();
        content.setLayoutManager(new LinearLayout());
        content.addComponent(new Label("Loading source metadata..."));
        content.addComponent(new Label("The object count page will open when metadata is ready."));
        window.setComponent(content);

        Thread worker = new Thread(
                () -> loadMetadataAndShowObjectCount(window, gui, session, analyzerService, state),
                "analyzer-tui-metadata");
        worker.setDaemon(true);
        worker.start();
    }

    private void loadMetadataAndShowObjectCount(
            BasicWindow window,
            MultiWindowTextGUI gui,
            AnalyzerConsoleConfig session,
            AnalyzerService analyzerService,
            NavigationState state) {
        try {
            analyzerService.loadSourceCatalog(session);
            state.sourceLoaded = true;
            gui.getGUIThread().invokeLater(
                    () -> showObjectCount(window, gui, session, analyzerService, state));
        } catch (RuntimeException ex) {
            gui.getGUIThread().invokeLater(() -> showError(window, state, ex));
        }
    }

    private void showProgressAndRun(
            BasicWindow window,
            MultiWindowTextGUI gui,
            AnalyzerConsoleConfig session,
            AnalyzerService analyzerService,
            NavigationState state) {
        state.enterAction = null;
        ProgressView progressView = progressPage.buildView();
        Panel content = withLayout(progressView.getPanel());
        window.setComponent(content);
        Thread worker = new Thread(
                () -> runAnalysisAndWaitForResultEnter(
                        window, gui, session, analyzerService, state, progressView),
                "analyzer-tui-analysis");
        worker.setDaemon(true);
        worker.start();
    }

    private void runAnalysisAndWaitForResultEnter(
            BasicWindow window,
            MultiWindowTextGUI gui,
            AnalyzerConsoleConfig session,
            AnalyzerService analyzerService,
            NavigationState state,
            ProgressView progressView) {
        try {
            analyzerService.runAnalysis(
                    session,
                    event -> gui.getGUIThread().invokeLater(() -> progressView.update(event)));
            AnalyzerResultViewModel result = analyzerService.saveResult(session);
            gui.getGUIThread().invokeLater(
                    () -> {
                        progressView.markCompleted();
                        state.enterAction = () -> showResult(window, state, result);
                    });
        } catch (RuntimeException ex) {
            gui.getGUIThread().invokeLater(() -> showError(window, state, ex));
        }
    }

    private void showResult(BasicWindow window, NavigationState state, AnalyzerResultViewModel result) {
        Panel content = withLayout(resultPage.build(result));
        Button closeButton = new Button("Close", window::close);
        content.addComponent(closeButton);
        setContent(window, content, closeButton, state, window::close);
    }

    private void showError(BasicWindow window, NavigationState state, RuntimeException ex) {
        Panel content = new Panel();
        content.setLayoutManager(new LinearLayout());
        content.addComponent(new Label("Analyzer failed"));
        content.addComponent(new Label(ex.getMessage() == null ? ex.toString() : ex.getMessage()));
        Button closeButton = new Button("Close", window::close);
        content.addComponent(closeButton);
        setContent(window, content, closeButton);
        if (state != null) {
            state.enterAction = window::close;
        }
    }

    private Panel withLayout(Panel panel) {
        panel.setLayoutManager(new LinearLayout());
        return panel;
    }

    private void setContent(BasicWindow window, Panel content, Button primaryButton) {
        window.setComponent(content);
        window.setFocusedInteractable(primaryButton);
    }

    private void setContent(
            BasicWindow window,
            Panel content,
            Button primaryButton,
            NavigationState state,
            Runnable enterAction) {
        setContent(window, content, primaryButton);
        state.enterAction = enterAction;
    }

    private static class NavigationState {
        private boolean sourceLoaded;
        private Runnable enterAction;
    }

    private static class EnterKeyListener implements WindowListener {
        private final NavigationState state;

        private EnterKeyListener(NavigationState state) {
            this.state = state;
        }

        public void onInput(Window basePane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
            if (keyStroke.getKeyType() == KeyType.Enter && state.enterAction != null) {
                deliverEvent.set(false);
                state.enterAction.run();
            }
        }

        public void onUnhandledInput(Window basePane, KeyStroke keyStroke, AtomicBoolean hasBeenHandled) {
            if (keyStroke.getKeyType() == KeyType.Enter && state.enterAction != null) {
                hasBeenHandled.set(true);
                state.enterAction.run();
            }
        }

        public void onResized(Window window, TerminalSize oldSize, TerminalSize newSize) {
        }

        public void onMoved(Window window, TerminalPosition oldPosition, TerminalPosition newPosition) {
        }
    }
}
