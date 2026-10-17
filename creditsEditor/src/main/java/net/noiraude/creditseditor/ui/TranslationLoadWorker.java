package net.noiraude.creditseditor.ui;

import java.nio.file.Path;

import javax.swing.SwingWorker;

/** Reads the selected instance off the event dispatch thread. */
final class TranslationLoadWorker extends SwingWorker<Void, Void> {

    private final EditorSession session;
    private final Path instance;

    TranslationLoadWorker(EditorSession session, Path instance) {
        this.session = session;
        this.instance = instance;
    }

    @Override
    protected Void doInBackground() throws Exception {
        session.loadTranslations(instance);
        return null;
    }
}
