package net.noiraude.creditseditor.ui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

import net.noiraude.creditseditor.command.CommandExecutor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Main application window for the GTNH Credits Editor.
 *
 * <p>
 * Responsible for: the JFrame lifecycle, the menu bar, file I/O dialogs (open / save / quit),
 * and undo/redo dispatch. Panel layout and selection coordination are delegated to
 * {@link EditorView}; session state is held in {@link EditorSession}.
 */
public final class MainWindow extends JFrame {

    private @Nullable EditorSession session; // null when no resource is open

    private final @NotNull EditorView editorView;
    private final @NotNull EditorMenuBar menuBar;

    public MainWindow(@Nullable String initialPath) {
        super("GTNH Credits Editor");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(@NotNull WindowEvent e) {
                handleQuit();
            }
        });

        CommandExecutor onCommand = cmd -> {
            if (session == null) return;
            boolean light = session.stack.execute(cmd);
            afterCommand(light);
        };

        editorView = new EditorView(onCommand);

        setLayout(new BorderLayout());
        add(editorView, BorderLayout.CENTER);

        menuBar = new EditorMenuBar(
            new EditorMenuBar.FileActions(
                () -> handleOpen(false),
                () -> handleOpen(true),
                this::handleSave,
                this::handleQuit),
            new EditorMenuBar.EditActions(this::handleUndo, this::handleRedo));
        setJMenuBar(menuBar);

        setSize(UiScale.scaled(1100), UiScale.scaled(700));
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
            JOptionPane.showMessageDialog(
                this,
                "Failed to load credits data:\n" + ex.getMessage(),
                "Load error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (session != null) {
            session.close();
        }
        session = newSession;

        editorView.loadSession(session);
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
            JOptionPane.showMessageDialog(
                this,
                "Failed to save:\n" + ex.getMessage(),
                "Save error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        updateTitle();
    }

    // -----------------------------------------------------------------------
    // Undo / redo
    // -----------------------------------------------------------------------

    private void handleUndo() {
        if (session == null || !session.stack.canUndo()) return;
        afterCommand(session.stack.undo());
    }

    private void handleRedo() {
        if (session == null || !session.stack.canRedo()) return;
        afterCommand(session.stack.redo());
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
    // UI refresh helpers
    // -----------------------------------------------------------------------

    /**
     * Post-command/undo/redo refresh. Light edits repaint list cells in place; structural
     * commands trigger a full panel rebuild via {@link EditorView#refreshAll()}.
     */
    private void afterCommand(boolean light) {
        if (light) {
            editorView.repaintLists();
        } else {
            editorView.refreshAll();
        }
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
