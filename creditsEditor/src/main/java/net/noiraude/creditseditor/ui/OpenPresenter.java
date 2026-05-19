package net.noiraude.creditseditor.ui;

import java.awt.Frame;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.ui.dialog.ProgressDialog;

import org.jetbrains.annotations.NotNull;

/** Loads an existing or freshly created credits resource into the session. */
final class OpenPresenter {

    private final @NotNull DocumentBus bus;
    private final @NotNull Frame owner;
    private final @NotNull SessionHolder holder;
    private final @NotNull UnsavedChangesGate gate;

    OpenPresenter(@NotNull DocumentBus bus, @NotNull Frame owner, @NotNull SessionHolder holder,
        @NotNull UnsavedChangesGate gate) {
        this.bus = Objects.requireNonNull(bus);
        this.owner = Objects.requireNonNull(owner);
        this.holder = Objects.requireNonNull(holder);
        this.gate = Objects.requireNonNull(gate);
        bus.addListener(DocumentBus.TOPIC_REQUEST_OPEN, e -> request(false));
        bus.addListener(DocumentBus.TOPIC_REQUEST_NEW, e -> request(true));
    }

    /** Loads the path supplied on the command line. */
    void loadInitial(@NotNull String path) {
        load(path);
    }

    private void request(boolean createNew) {
        if (gate.shouldAbort()) return;
        (createNew ? CreditsResourceChooser.chooseForNew(owner) : CreditsResourceChooser.chooseForOpen(owner))
            .ifPresent(p -> load(p.toString()));
    }

    private void load(@NotNull String path) {
        LoadWorker worker = new LoadWorker(path);
        ProgressDialog.runWith(
            owner,
            I18n.get("dialog.load.progress.title"),
            I18n.get("dialog.load.progress.message", MsgArg.text(path)),
            worker);

        EditorSession session;
        try {
            session = worker.get();
        } catch (InterruptedException ex) {
            Thread.currentThread()
                .interrupt();
            ErrorPresenter.show(owner, I18n.get("dialog.load.error.title"), ex);
            return;
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            ErrorPresenter.show(owner, I18n.get("dialog.load.error.title"), cause);
            return;
        }

        holder.set(session);
        bus.setActiveLocale(session.defaultLocale());
        bus.fireSessionChanged();
        bus.fireDirtyChanged(session.isDirty());
        bus.fireCommandStackChanged(session.commandStackSnapshot());
    }

    private static final class LoadWorker extends SwingWorker<EditorSession, Void> {

        private final @NotNull String path;

        LoadWorker(@NotNull String path) {
            this.path = path;
        }

        @Override
        protected EditorSession doInBackground() throws Exception {
            return EditorSession.open(path);
        }
    }
}
