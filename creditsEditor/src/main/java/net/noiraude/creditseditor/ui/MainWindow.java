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
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        doSave();
    }

    private boolean doSave() {
        if (session == null) return false;
        try {
            session.save();
        } catch (Exception ex) {
            ErrorPresenter.show(this, I18n.get("dialog.save.error.title"), ex);
            return false;
        }
        updateTitle();
        return true;
    }

    @Override
    public void onSaveAs() {
        if (session == null) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(I18n.get("filechooser.save_as.title"));
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter zipFilter = new FileNameExtensionFilter(
            I18n.get("filechooser.save_as.filter.zip"),
            "zip");
        FileFilter dirFilter = new FileFilter() {

            @Override
            public boolean accept(File f) {
                return f.isDirectory();
            }

            @Override
            public String getDescription() {
                return I18n.get("filechooser.save_as.filter.dir");
            }
        };
        chooser.addChoosableFileFilter(zipFilter);
        chooser.addChoosableFileFilter(dirFilter);
        chooser.setFileFilter(zipFilter);

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File selected = chooser.getSelectedFile();
        if (
            chooser.getFileFilter() == zipFilter && !selected.getName()
                .toLowerCase()
                .endsWith(".zip")
        ) {
            selected = new File(selected.getAbsolutePath() + ".zip");
        }
        Path target = Paths.get(selected.getAbsolutePath());

        if (isNonEmptyTarget(target) && !confirmOverwrite(target)) return;

        try {
            session.saveAs(target.toString());
        } catch (Exception ex) {
            ErrorPresenter.show(this, I18n.get("dialog.save_as.error.title"), ex);
            return;
        }
        updateTitle();
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
                target.getFileName()
                    .toString()),
            I18n.get("dialog.save_as.confirm.title"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
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
}
