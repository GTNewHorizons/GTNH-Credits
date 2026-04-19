package net.noiraude.creditseditor.ui;

import java.io.IOException;

import net.noiraude.creditseditor.ResourceManager;
import net.noiraude.creditseditor.command.CommandStack;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.model.CreditsDocument;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Facade for one editing session: encapsulates the {@link ResourceManager}, loaded
 * documents, and the undo/redo stack.
 *
 * <p>
 * Instances are created via {@link #open(String)}. The resource they point to is
 * immutable; the document data inside is mutable (edits happen in place).
 *
 * <p>
 * Dirty state is content-based: {@link #isDirty()} returns {@code true} only when the
 * serialized JSON or the lang file would differ from what is currently on disk. The
 * {@link CommandStack} is kept solely for undo/redo; it is not authoritative for dirty state.
 *
 * <p>
 * No other class should access {@link ResourceManager} directly. All file I/O flow
 * through this facade.
 */
final class EditorSession {

    private final @NotNull ResourceManager resourceManager;
    final @NotNull CommandStack stack = new CommandStack();

    @Contract(pure = true)
    private EditorSession(@NotNull ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    /**
     * Opens a resource at {@code path} (directory or zip) and loads the credit JSON
     * and lang file.
     *
     * @param path filesystem path to the resource directory or zip file
     * @return a ready-to-use session
     * @throws Exception if the path cannot be opened or the files cannot be parsed
     */
    static @NotNull EditorSession open(@NotNull String path) throws Exception {
        ResourceManager rm = ResourceManager.open(path);
        try {
            rm.loadDocuments();
            return new EditorSession(rm);
        } catch (Exception ex) {
            try {
                rm.close();
            } catch (IOException suppressed) {
                ex.addSuppressed(suppressed);
            }
            throw ex;
        }
    }

    /** Convenience accessor for the credits document held by the resource manager. */
    @Contract(pure = true)
    @NotNull
    CreditsDocument creditsDoc() {
        return resourceManager.getCreditsDoc();
    }

    /** Convenience accessor for the lang document held by the resource manager. */
    @Contract(pure = true)
    @NotNull
    LangDocument langDoc() {
        return resourceManager.getLangDoc();
    }

    /** Returns the file name of the resource directory or zip, for use in the title bar. */
    @Contract(pure = true)
    @NotNull
    String displayPath() {
        return resourceManager.getDiskPath()
            .getFileName()
            .toString();
    }

    /**
     * Returns {@code true} if either the JSON or the lang file would differ from what is
     * currently on disk. Always returns {@code false} immediately after a successful
     * {@link #save()} or a fresh {@link #open(String)}.
     *
     * <p>
     * Delegates to {@link ResourceManager#isDirty()}, which is the authority for the
     * combined dirty state of all resources it manages.
     */
    @Contract(pure = true)
    boolean isDirty() {
        return resourceManager.isDirty();
    }

    /**
     * Writes modified resource files back to disk.
     *
     * <p>
     * Each file is written only when its content-based dirty flag is set. After a
     * successful writing the corresponding dirty flag is cleared.
     *
     * @throws Exception if a file cannot be serialized or written
     */
    void save() throws Exception {
        CreditsDocument creditsDoc = resourceManager.getCreditsDoc();
        LangDocument langDoc = resourceManager.getLangDoc();

        if (creditsDoc.isDirty()) {
            resourceManager.writeCredits();
            creditsDoc.markClean();
        }

        if (langDoc.isDirty()) {
            resourceManager.writeLang();
            langDoc.markClean();
        }
    }

    /** Closes the resource manager, silently swallowing any {@link IOException}. */
    void close() {
        try {
            resourceManager.close();
        } catch (IOException ignored) {}
    }
}
