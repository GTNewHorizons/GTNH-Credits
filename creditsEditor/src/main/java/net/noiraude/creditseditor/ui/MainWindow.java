package net.noiraude.creditseditor.ui;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.ui.dialog.AboutDialog;
import net.noiraude.creditseditor.ui.dialog.ProgressDialog;
import net.noiraude.creditseditor.ui.dialog.ShortcutsDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutionException;

/**
 * Main application window for the GTNH Credits Editor.
 *
 * <p>
 * Responsible for: the JFrame lifecycle, the menu bar, file I/O dialogs (open / save / quit),
 * and undo/redo dispatch. Panel layout and selection coordination are delegated to
 * {@link EditorView}; session state is held in {@link EditorSession}. All widgets subscribe
 * to a single application-wide {@link DocumentBus}; refresh is entirely event-driven.
 *
 * <p>
 * Implements {@link EditorActions.Handlers} directly so menu activations dispatch to
 * named methods on this class rather than to an inline anonymous adapter.
 */
public final class MainWindow extends JFrame implements EditorActions.Handlers {

    private @Nullable EditorSession session; // null when no resource is open

    private final @NotNull DocumentBus bus = new DocumentBus();

    public MainWindow(@Nullable String initialPath) {
        super(AppInfo.name());
        setIconImages(AppIcons.load());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(@NotNull WindowEvent e) {
                onQuit();
            }
        });

        CommandExecutor onCommand = cmd -> {
            if (session == null) return;
            session.stack.execute(cmd);
            afterCommand();
        };

        @NotNull
        EditorView editorView = new EditorView(bus, onCommand);

        setLayout(new BorderLayout());
        add(editorView, BorderLayout.CENTER);

        EditorActions actions = new EditorActions(this, bus, () -> session);
        EditorMenuBar menuBar = new EditorMenuBar(actions);
        setJMenuBar(menuBar);

        // Realize the peer so getInsets() returns real chrome sizes, then force the frame's
        // minimum size to whatever the content pane plus the menu bar report as their own
        // minimum. This prevents the user from shrinking the window below the point where
        // inner panels (memberships, roles, etc.) can still render at their pinned minimums.
        pack();
        Insets frameInsets = getInsets();
        Dimension contentMin = getContentPane().getMinimumSize();
        int menuBarHeight = menuBar.getPreferredSize().height;
        setMinimumSize(
            new Dimension(
                contentMin.width + frameInsets.left + frameInsets.right,
                contentMin.height + menuBarHeight + frameInsets.top + frameInsets.bottom));

        setSize(ScaledMetrics.mainWindowWidth, ScaledMetrics.mainWindowHeight);
        setLocationRelativeTo(null);
        ScaledMetrics.attachTo(this);

        if (initialPath != null) {
            loadResource(initialPath);
        } else {
            updateTitle();
        }
    }

    // -----------------------------------------------------------------------
    // Resource loading and saving
    // -----------------------------------------------------------------------

    private void loadResource(@NotNull String path) {
        SwingWorker<EditorSession, Void> worker = new SwingWorker<>() {

            @Override
            protected EditorSession doInBackground() throws Exception {
                return EditorSession.open(path);
            }
        };
        ProgressDialog.runWith(
            this,
            I18n.get("dialog.load.progress.title"),
            I18n.get("dialog.load.progress.message", path),
            worker);

        EditorSession newSession;
        try {
            newSession = worker.get();
        } catch (InterruptedException ex) {
            Thread.currentThread()
                .interrupt();
            ErrorPresenter.show(this, I18n.get("dialog.load.error.title"), ex);
            return;
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            ErrorPresenter.show(this, I18n.get("dialog.load.error.title"), cause);
            return;
        }

        if (session != null) {
            session.close();
        }
        session = newSession;

        bus.setSession(session.creditsDoc(), session.langDoc());
        updateTitle();
    }

    private void doOpen(boolean createNew) {
        if (shouldAbort()) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setDialogTitle(I18n.get(createNew ? "filechooser.new.title" : "filechooser.open.title"));

        int result = createNew ? chooser.showSaveDialog(this) : chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        loadResource(
            chooser.getSelectedFile()
                .getAbsolutePath());
    }

    // -----------------------------------------------------------------------
    // EditorActions.Handlers implementation
    // -----------------------------------------------------------------------

    @Override
    public void onOpen() {
        doOpen(false);
    }

    @Override
    public void onNew() {
        doOpen(true);
    }

    @Override
    public void onSave() {
        if (session == null) return;
        try {
            session.save();
        } catch (Exception ex) {
            ErrorPresenter.show(this, I18n.get("dialog.save.error.title"), ex);
            return;
        }
        updateTitle();
    }

    @Override
    public void onQuit() {
        if (shouldAbort()) return;
        if (session != null) session.close();
        System.exit(0);
    }

    @Override
    public void onUndo() {
        if (session == null || !session.stack.canUndo()) return;
        session.stack.undo();
        afterCommand();
    }

    @Override
    public void onRedo() {
        if (session == null || !session.stack.canRedo()) return;
        session.stack.redo();
        afterCommand();
    }

    @Override
    public void onShortcuts() {
        new ShortcutsDialog(this).setVisible(true);
    }

    @Override
    public void onAbout() {
        new AboutDialog(this).setVisible(true);
    }

    // -----------------------------------------------------------------------
    // UI refresh helpers
    // -----------------------------------------------------------------------

    /**
     * Post-command/undo/redo housekeeping. Document-driven UI updates are published on the
     * bus by the commands themselves; this method forwards command-stack state to the bus
     * (so {@link EditorActions} updates Save/Undo/Redo) and refreshes the window title.
     */
    private void afterCommand() {
        bus.fireCommandStackChanged();
        updateTitle();
    }

    private void updateTitle() {
        String appName = AppInfo.name();
        String title;
        if (session == null) {
            title = appName;
        } else if (session.isDirty()) {
            title = I18n.get("title.session.dirty", appName, session.displayPath());
        } else {
            title = I18n.get("title.session", appName, session.displayPath());
        }
        setTitle(title);
    }

    /**
     * Shows an "unsaved changes" confirmation dialog if needed.
     *
     * @return {@code true} if the caller should abort (no session, dirty and user declined),
     *         {@code false} if the caller may proceed
     */
    private boolean shouldAbort() {
        if (session == null || !session.isDirty()) return false;
        int choice = JOptionPane.showConfirmDialog(
            this,
            I18n.get("dialog.unsaved.message"),
            I18n.get("dialog.unsaved.title"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        return choice != JOptionPane.YES_OPTION;
    }
}
