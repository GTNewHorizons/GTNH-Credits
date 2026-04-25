package net.noiraude.creditseditor.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.ui.dialog.AboutDialog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Main application window for the GTNH Credits Editor.
 *
 * <p>
 * Responsible for: the JFrame lifecycle, the menu bar, file I/O dialogs (open / save / quit),
 * and undo/redo dispatch. Panel layout and selection coordination are delegated to
 * {@link EditorView}; session state is held in {@link EditorSession}. All widgets subscribe
 * to a single application-wide {@link DocumentBus}; refresh is entirely event-driven.
 */
public final class MainWindow extends JFrame {

    private @Nullable EditorSession session; // null when no resource is open

    private final @NotNull DocumentBus bus = new DocumentBus();
    private final @NotNull EditorMenuBar menuBar;

    public MainWindow(@Nullable String initialPath) {
        super("GTNH Credits Editor");
        setIconImages(AppIcons.load());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(@NotNull WindowEvent e) {
                handleQuit();
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

        menuBar = new EditorMenuBar(
            new EditorMenuBar.FileActions(
                () -> handleOpen(false),
                () -> handleOpen(true),
                this::handleSave,
                this::handleQuit),
            new EditorMenuBar.EditActions(this::handleUndo, this::handleRedo),
            new EditorMenuBar.HelpActions(this::handleAbout));
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

        setSize(UiScale.scaled(UiMetrics.MAIN_WINDOW_WIDTH), UiScale.scaled(UiMetrics.MAIN_WINDOW_HEIGHT));
        setLocationRelativeTo(null);

        if (initialPath != null) {
            loadResource(initialPath);
        } else {
            menuBar.refresh(null);
            updateTitle();
        }
    }

    // -----------------------------------------------------------------------
    // Resource loading and saving
    // -----------------------------------------------------------------------

    private void loadResource(@NotNull String path) {
        EditorSession newSession;
        try {
            newSession = EditorSession.open(path);
        } catch (Exception ex) {
            ErrorPresenter.show(this, "Load error", ex);
            return;
        }

        if (session != null) {
            session.close();
        }
        session = newSession;

        bus.setSession(session.creditsDoc(), session.langDoc());
        menuBar.refresh(session);
        updateTitle();
    }

    private void handleOpen(boolean createNew) {
        if (shouldAbort()) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setDialogTitle(createNew ? "New resource location" : "Open resource");

        int result = createNew ? chooser.showSaveDialog(this) : chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        loadResource(
            chooser.getSelectedFile()
                .getAbsolutePath());
    }

    private void handleSave() {
        if (session == null) return;
        try {
            session.save();
        } catch (Exception ex) {
            ErrorPresenter.show(this, "Save error", ex);
            return;
        }
        updateTitle();
    }

    // -----------------------------------------------------------------------
    // Undo / redo
    // -----------------------------------------------------------------------

    private void handleUndo() {
        if (session == null || !session.stack.canUndo()) return;
        session.stack.undo();
        afterCommand();
    }

    private void handleRedo() {
        if (session == null || !session.stack.canRedo()) return;
        session.stack.redo();
        afterCommand();
    }

    // -----------------------------------------------------------------------
    // Quit
    // -----------------------------------------------------------------------

    private void handleQuit() {
        if (shouldAbort()) return;
        if (session != null) session.close();
        System.exit(0);
    }

    // -----------------------------------------------------------------------
    // Help
    // -----------------------------------------------------------------------

    private void handleAbout() {
        new AboutDialog(this).setVisible(true);
    }

    // -----------------------------------------------------------------------
    // UI refresh helpers
    // -----------------------------------------------------------------------

    /**
     * Post-command/undo/redo housekeeping. Document-driven UI updates are published on the
     * bus by the commands themselves; this method only touches frame-level state (menu
     * enablement and window title).
     */
    private void afterCommand() {
        menuBar.refresh(session);
        updateTitle();
    }

    private void updateTitle() {
        String base = "GTNH Credits Editor";
        if (session != null) {
            base += " " + session.displayPath();
            if (session.isDirty()) base += " *";
        }
        setTitle(base);
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
            "There are unsaved changes. Discard them?",
            "Unsaved changes",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        return choice != JOptionPane.YES_OPTION;
    }
}
