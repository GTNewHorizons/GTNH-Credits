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
    private String instancePath;

    OpenPresenter(@NotNull DocumentBus bus, @NotNull Frame owner, @NotNull SessionHolder holder,
        @NotNull UnsavedChangesGate gate) {
        this.bus = Objects.requireNonNull(bus);
        this.owner = Objects.requireNonNull(owner);
        this.holder = Objects.requireNonNull(holder);
        this.gate = Objects.requireNonNull(gate);
        bus.addListener(DocumentBus.TOPIC_REQUEST_OPEN, e -> request(false));
        bus.addListener(DocumentBus.TOPIC_REQUEST_NEW, e -> request(true));
        bus.addListener(
            DocumentBus.TOPIC_REQUEST_OPEN_PATH,
            e -> {
                if (!gate.shouldAbort()) load(
                    e.getNewValue()
                        .toString(),
                    null);
            });
    }

    /** Loads the path supplied on the command line. */
    void loadInitial(@NotNull String path) {
        load(path, null);
    }

    void setInstancePath(String path) {
        this.instancePath = path;
    }

    private void request(boolean createNew) {
        if (gate.shouldAbort()) return;
        if (createNew) {
            CreditsResourceChooser.chooseForNew(owner)
                .ifPresent(
                    target -> load(
                        target.path()
                            .toString(),
                        target.format()));
        } else {
            CreditsResourceChooser.chooseForOpen(owner)
                .ifPresent(path -> load(path.toString(), null));
        }
    }

    private void load(String path, net.noiraude.libcredits.pack.CreditsLayout.Format format) {
        LoadWorker worker = new LoadWorker(path, format, instancePath);
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
        if (format == null) RecentLocations.user()
            .remember(java.nio.file.Path.of(path));
        bus.setActiveLocale(session.defaultLocale());
        bus.fireSessionChanged();
        bus.fireDirtyChanged(session.isDirty());
        bus.fireCommandStackChanged(session.commandStackSnapshot());
    }

    private static final class LoadWorker extends SwingWorker<EditorSession, Void> {

        private final @NotNull String path;
        private final net.noiraude.libcredits.pack.CreditsLayout.Format format;
        private final String instancePath;

        LoadWorker(String path, net.noiraude.libcredits.pack.CreditsLayout.Format format, String instancePath) {
            this.path = path;
            this.format = format;
            this.instancePath = instancePath;
        }

        @Override
        protected EditorSession doInBackground() throws Exception {
            EditorSession session = format == null ? EditorSession.open(path) : EditorSession.create(path, format);
            try {
                if (instancePath != null) session.loadTranslations(java.nio.file.Path.of(instancePath));
                return session;
            } catch (Exception ex) {
                session.close();
                throw ex;
            }
        }
    }
}
