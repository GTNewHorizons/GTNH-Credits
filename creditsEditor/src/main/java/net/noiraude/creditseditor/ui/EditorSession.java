package net.noiraude.creditseditor.ui;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    private static final Logger LOG = System.getLogger(EditorSession.class.getName());

    private @NotNull ResourceManager resourceManager;
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

    /**
     * Writes the current documents to a fresh destination (directory or zip) and retargets
     * this session at it; subsequent {@link #save()} calls write to the new location.
     *
     * <p>
     * If {@code pathArg} resolves to the same on-disk location as the current target, this
     * method delegates to {@link #save()} to avoid reopening the same zip filesystem twice.
     *
     * @throws Exception if the destination cannot be opened, or if writing fails. On failure
     *                   the original target stays active so the user can retry.
     */
    void saveAs(@NotNull String pathArg) throws Exception {
        Path newPath = Paths.get(pathArg)
            .toAbsolutePath()
            .normalize();
        Path curPath = resourceManager.getDiskPath()
            .toAbsolutePath()
            .normalize();
        if (newPath.equals(curPath)) {
            save();
            return;
        }

        CreditsDocument credits = resourceManager.getCreditsDoc();
        LangDocument lang = resourceManager.getLangDoc();

        ResourceManager newRm = ResourceManager.open(pathArg);
        try {
            newRm.adoptDocuments(credits, lang);
            newRm.writeCredits();
            newRm.writeLang();
        } catch (Exception ex) {
            try {
                newRm.close();
            } catch (IOException suppressed) {
                ex.addSuppressed(suppressed);
            }
            throw ex;
        }

        credits.markClean();
        lang.markClean();

        ResourceManager old = resourceManager;
        resourceManager = newRm;
        try {
            old.close();
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Failed to close previous resource manager after Save As", ex);
        }
    }

    /**
     * Closes the resource manager. Any {@link IOException} is logged at
     * {@link Level#WARNING} and not rethrown; close-time failures are diagnostic noise
     * only and should never block shutdown paths such as window closing.
     */
    void close() {
        try {
            resourceManager.close();
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Failed to close resource manager", ex);
        }
    }
}
