package net.noiraude.creditseditor.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.CommandStackSnapshot;
import net.noiraude.creditseditor.command.EditAbortedException;
import net.noiraude.creditseditor.ui.dialog.AboutDialog;
import net.noiraude.creditseditor.ui.dialog.ProgressDialog;
import net.noiraude.creditseditor.ui.dialog.ShortcutsDialog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Main application window for the GTNH Credits Editor. */
public final class MainWindow extends JFrame {

    private @Nullable EditorSession session; // null when no resource is open

    private final @NotNull DocumentBus bus = new DocumentBus();

    public MainWindow(@Nullable String initialPath) {
        super(AppInfo.name());
        setIconImages(AppIcons.load());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(@NotNull WindowEvent e) {
                doQuit();
            }
        });

        CommandExecutor onCommand = cmd -> {
            if (session == null) return;
            try {
                session.stack.execute(cmd);
            } catch (EditAbortedException ex) {
                showEditAborted(ex);
            }
            afterCommand();
        };

        EditorActions actions = new EditorActions(bus);

        @NotNull
        EditorView editorView = new EditorView(bus, onCommand);

        setLayout(new BorderLayout());
        add(new EditorToolBar(bus, actions), BorderLayout.NORTH);
        add(editorView, BorderLayout.CENTER);

        EditorMenuBar menuBar = new EditorMenuBar(actions);
        setJMenuBar(menuBar);

        bus.addListener(DocumentBus.TOPIC_REQUEST_OPEN, e -> doOpen(false));
        bus.addListener(DocumentBus.TOPIC_REQUEST_NEW, e -> doOpen(true));
        bus.addListener(DocumentBus.TOPIC_REQUEST_SAVE, e -> doSave());
        bus.addListener(DocumentBus.TOPIC_REQUEST_SAVE_AS, e -> doSaveAs());
        bus.addListener(DocumentBus.TOPIC_REQUEST_QUIT, e -> doQuit());
        bus.addListener(DocumentBus.TOPIC_REQUEST_UNDO, e -> doUndo());
        bus.addListener(DocumentBus.TOPIC_REQUEST_REDO, e -> doRedo());
        bus.addListener(DocumentBus.TOPIC_REQUEST_SHORTCUTS, e -> doShortcuts());
        bus.addListener(DocumentBus.TOPIC_REQUEST_ABOUT, e -> doAbout());
        bus.addListener(DocumentBus.TOPIC_DIRTY, e -> updateTitle());
        bus.addListener(DocumentBus.TOPIC_SESSION, e -> updateTitle());

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
            I18n.get("dialog.load.progress.message", MsgArg.text(path)),
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

        bus.setActiveLocale(session.defaultLocale());
        bus.setSession(session.creditsDoc(), session.langDocs());
        bus.fireDirtyChanged(session.isDirty());
        bus.fireCommandStackChanged(CommandStackSnapshot.of(session.stack));
    }

    private void doOpen(boolean createNew) {
        if (shouldAbort()) return;
        Optional<Path> chosen = createNew ? CreditsResourceChooser.chooseForNew(this)
            : CreditsResourceChooser.chooseForOpen(this);
        chosen.ifPresent(p -> loadResource(p.toString()));
    }

    private boolean doSave() {
        if (session == null) return false;
        try {
            session.save();
        } catch (Exception ex) {
            ErrorPresenter.show(this, I18n.get("dialog.save.error.title"), ex);
            return false;
        }
        bus.fireDirtyChanged(session.isDirty());
        return true;
    }

    private void doSaveAs() {
        if (session == null) return;

        Optional<Path> chosen = CreditsResourceChooser.chooseForSaveAs(this);
        if (chosen.isEmpty()) return;
        Path target = chosen.get();

        if (isNonEmptyTarget(target) && !confirmOverwrite(target)) return;

        try {
            session.saveAs(target.toString());
        } catch (Exception ex) {
            ErrorPresenter.show(this, I18n.get("dialog.save_as.error.title"), ex);
            return;
        }
        bus.fireDirtyChanged(session.isDirty());
    }

    private static boolean isNonEmptyTarget(@NotNull Path target) {
        if (!Files.exists(target)) return false;
        if (Files.isRegularFile(target)) return true;
        if (!Files.isDirectory(target)) return false;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(target)) {
            return entries.iterator()
                .hasNext();
        } catch (IOException ex) {
            return true;
        }
    }

    private boolean confirmOverwrite(@NotNull Path target) {
        int choice = JOptionPane.showConfirmDialog(
            this,
            I18n.get(
                "dialog.save_as.confirm.message",
                MsgArg.text(
                    target.getFileName()
                        .toString())),
            I18n.get("dialog.save_as.confirm.title"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    private void doQuit() {
        if (shouldAbort()) return;
        if (session != null) session.close();
        System.exit(0);
    }

    private void doUndo() {
        if (session == null || !session.stack.canUndo()) return;
        try {
            session.stack.undo();
        } catch (EditAbortedException ex) {
            showEditAborted(ex);
        }
        afterCommand();
    }

    private void doRedo() {
        if (session == null || !session.stack.canRedo()) return;
        try {
            session.stack.redo();
        } catch (EditAbortedException ex) {
            showEditAborted(ex);
        }
        afterCommand();
    }

    private void doShortcuts() {
        new ShortcutsDialog(this).setVisible(true);
    }

    private void doAbout() {
        new AboutDialog(this).setVisible(true);
    }

    // -----------------------------------------------------------------------
    // UI refresh helpers
    // -----------------------------------------------------------------------

    /** Forwards command-stack state to the bus after every command execution, undo, or redo. */
    private void afterCommand() {
        if (session != null) {
            bus.fireCommandStackChanged(CommandStackSnapshot.of(session.stack));
            bus.fireDirtyChanged(session.isDirty());
        }
    }

    private void updateTitle() {
        String appName = AppInfo.name();
        String title;
        if (session == null) {
            title = appName;
        } else if (session.isDirty()) {
            title = I18n.get("title.session.dirty", MsgArg.text(appName), MsgArg.text(session.displayPath()));
        } else {
            title = I18n.get("title.session", MsgArg.text(appName), MsgArg.text(session.displayPath()));
        }
        setTitle(title);
    }

    /**
     * Shows an "unsaved changes" confirmation dialog if needed.
     *
     * <p>
     * Offers Save, Discard, and Cancel. Save runs {@link #doSave()} and only allows the
     * caller to proceed when the save succeeds; Discard always allows it; Cancel and any
     * other dismissal abort the caller.
     *
     * @return {@code true} if the caller should abort, {@code false} if it may proceed
     */
    private boolean shouldAbort() {
        if (session == null || !session.isDirty()) return false;
        Object[] options = { I18n.get("dialog.unsaved.button.save"), I18n.get("dialog.unsaved.button.discard"),
            I18n.get("button.cancel") };
        int choice = JOptionPane.showOptionDialog(
            this,
            I18n.get("dialog.unsaved.message"),
            I18n.get("dialog.unsaved.title"),
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[0]);
        return switch (choice) {
            case 0 -> !doSave();
            case 1 -> false;
            default -> true;
        };
    }

    /**
     * Surfaces an {@link EditAbortedException} as a non-blocking warning dialog. The command
     * stack has already refused to record the failed attempt; this method only informs the
     * user that the action did not take effect.
     */
    private void showEditAborted(@NotNull EditAbortedException ex) {
        JOptionPane.showMessageDialog(
            this,
            I18n.get("dialog.edit_aborted.message", MsgArg.text(ex.getMessage())),
            I18n.get("dialog.edit_aborted.title"),
            JOptionPane.WARNING_MESSAGE);
    }
}
