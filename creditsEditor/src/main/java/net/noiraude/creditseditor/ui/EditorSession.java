package net.noiraude.creditseditor.ui;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import net.noiraude.creditseditor.bus.DocumentSession;
import net.noiraude.creditseditor.bus.LocaleEditor;
import net.noiraude.creditseditor.bus.LocaleSnapshot;
import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.command.CommandStack;
import net.noiraude.creditseditor.command.CommandStackSnapshot;
import net.noiraude.creditseditor.command.EditAbortedException;
import net.noiraude.creditseditor.resource.ResourceManager;
import net.noiraude.creditseditor.service.LangResolver;
import net.noiraude.creditseditor.service.TranslationCatalog;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.lang.MinecraftLangBasename;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.pack.CreditsLayout;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Facade for one editing session: encapsulates the {@link ResourceManager}, loaded
 * documents, and the undo/redo stack.
 *
 * <p>
 * Documents are edited in place. Save As retargets the session only after the destination
 * has been written successfully; undo history remains attached to the edited documents.
 *
 * <p>
 * Dirty state is content-based: {@link #isDirty()} returns {@code true} only when the
 * serialized JSON or any owned lang content would differ from what is currently on disk. The
 * {@link CommandStack} is kept solely for undo/redo; it is not authoritative for dirty state.
 *
 * <p>
 * No other class should access {@link ResourceManager} directly. All file I/O flow
 * through this facade.
 */
final class EditorSession implements DocumentSession, LocaleEditor {

    private static final Logger LOG = System.getLogger(EditorSession.class.getName());

    private @NotNull ResourceManager resourceManager;
    private TranslationCatalog translations = TranslationCatalog.empty();
    private final @NotNull CommandStack stack = new CommandStack();

    @Contract(pure = true)
    private EditorSession(@NotNull ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    /**
     * Opens existing credits at a direct JSON path, content directory, modpack root,
     * resource directory or ZIP. Missing or ambiguous content is an error; this never creates files.
     *
     * @param path filesystem location containing existing credits
     * @return a ready-to-use session
     * @throws Exception if the path cannot be opened or the files cannot be parsed
     */
    @Contract("_ -> new")
    static @NotNull EditorSession open(@NotNull String path) throws Exception {
        ResourceManager rm = ResourceManager.open(path);
        try {
            rm.loadDocuments();
            EditorSession session = new EditorSession(rm);
            try {
                Path instance = findInstance(rm.location());
                if (instance != null) session.loadTranslations(instance);
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Could not load optional translation catalogue", ex);
            }
            return session;
        } catch (Exception ex) {
            try {
                rm.close();
            } catch (IOException suppressed) {
                ex.addSuppressed(suppressed);
            }
            throw ex;
        }
    }

    static @NotNull EditorSession create(String path, CreditsLayout.Format format) throws Exception {
        ResourceManager manager = ResourceManager.create(path, packDescription(), format);
        try {
            manager.loadDocuments();
            return new EditorSession(manager);
        } catch (Exception ex) {
            manager.close();
            throw ex;
        }
    }

    static Path findInstance(Path location) throws IOException {
        Path current = java.nio.file.Files.isDirectory(location) ? location : location.getParent();
        for (; current != null; current = current.getParent()) {
            Path mods = current.resolve("mods");
            if (java.nio.file.Files.isDirectory(mods)) {
                try (var entries = java.nio.file.Files.walk(mods)) {
                    if (
                        entries.anyMatch(
                            p -> p.toString()
                                .endsWith(".jar") && java.nio.file.Files.isRegularFile(p))
                    ) return current;
                }
            }
        }
        return null;
    }

    void loadTranslations(Path instance) throws IOException {
        translations = TranslationCatalog.load(instance);
    }

    @Override
    public Optional<String> externalTranslation(String key, String locale) {
        return translations.resolve(key, locale);
    }

    @Override
    public java.util.List<String> externalTranslationKeys() {
        return translations.keys();
    }

    @Override
    public boolean hasTranslationCatalog() {
        return translations.isAvailable();
    }

    private static @NotNull String packDescription() {
        return "GTNH Credits resource pack\nGenerated by " + AppInfo.name() + " " + AppInfo.version();
    }

    @Override
    @Contract(pure = true)
    public @NotNull CreditsDocument creditsDoc() {
        return resourceManager.getCreditsDoc();
    }

    /** Returns the default-locale lang document held by the resource manager. */
    @Contract(pure = true)
    @NotNull
    LangDocument langDoc() {
        return resourceManager.getLangDoc();
    }

    @Override
    @Contract(pure = true)
    public @NotNull Optional<LangDocument> langDoc(@NotNull String locale) {
        return resourceManager.langDoc(locale);
    }

    @Override
    @Contract(pure = true)
    public @NotNull Map<String, LangDocument> langDocs() {
        return resourceManager.langDocs();
    }

    /** Returns the locales currently loaded in the resource manager, in insertion order. */
    @Contract(pure = true)
    @NotNull
    java.util.@Unmodifiable Set<String> availableLocales() {
        return resourceManager.availableLocales();
    }

    @Override
    public void addLocale(@NotNull String locale) {
        resourceManager.addLocale(locale);
    }

    @Override
    public void removeLocale(@NotNull String locale) {
        resourceManager.removeLocale(locale);
    }

    @Override
    @Contract(pure = true)
    public @NotNull LocaleSnapshot snapshotLocale(@NotNull String locale) {
        return resourceManager.snapshotLocale(locale);
    }

    @Override
    public void applyLocaleSnapshot(@NotNull String locale, @NotNull LocaleSnapshot snapshot) {
        resourceManager.applyLocaleSnapshot(locale, snapshot);
    }

    java.util.List<String> validateContent() {
        return net.noiraude.libcredits.validation.CreditsValidator.validateContent(
            creditsDoc(),
            langDoc(),
            translations.isAvailable() ? key -> translations.resolve(key, "en_US")
                .isPresent() : null);
    }

    Path openPath() {
        return resourceManager.openPath();
    }

    /** Executes a command. */
    void execute(@NotNull Command cmd) throws EditAbortedException {
        stack.execute(cmd);
    }

    /** Reverts the most recently executed command. */
    void undo() throws EditAbortedException {
        stack.undo();
    }

    /** Re-applies the most recently undone command. */
    void redo() throws EditAbortedException {
        stack.redo();
    }

    /** Returns whether a command is available to undo. */
    @Contract(pure = true)
    boolean canUndo() {
        return stack.canUndo();
    }

    /** Returns whether a command is available to redo. */
    @Contract(pure = true)
    boolean canRedo() {
        return stack.canRedo();
    }

    /** Returns the current undo/redo snapshot. */
    @Contract(pure = true)
    @NotNull
    CommandStackSnapshot commandStackSnapshot() {
        return CommandStackSnapshot.of(stack);
    }

    /**
     * Returns the lang basename that best matches {@link Locale#getDefault()} among the
     * loaded locales, falling back to {@link LangResolver#DEFAULT_LOCALE} when nothing
     * matches. Used to seed the editing locale right after a session is opened.
     */
    @Contract(pure = true)
    @NotNull
    String defaultLocale() {
        if (
            resourceManager.location()
                .endsWith(Paths.get(CreditsLayout.MODPACK_DIRECTORY))
        ) return LangResolver.DEFAULT_LOCALE;
        return MinecraftLangBasename.forJavaLocale(Locale.getDefault(), availableLocales())
            .orElse(LangResolver.DEFAULT_LOCALE);
    }

    /** Returns the full credits JSON location, including the entry path inside an archive. */
    @Contract(pure = true)
    @NotNull
    String displayPath() {
        return resourceManager.displayPath();
    }

    /**
     * Returns {@code true} if the JSON or any owned lang content would differ from what is
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
        var errors = net.noiraude.libcredits.validation.CreditsValidator.validate(creditsDoc);
        if (!errors.isEmpty())
            throw new net.noiraude.libcredits.parser.CreditsParseException(String.join("; ", errors));

        if (creditsDoc.isDirty()) {
            resourceManager.writeCredits();
            creditsDoc.markClean();
        }

        resourceManager.writeLang();
        for (String locale : resourceManager.availableLocales()) {
            resourceManager.langDoc(locale)
                .ifPresent(LangDocument::markClean);
        }
    }

    /**
     * Saves as a standard resource pack: a ZIP for a {@code .zip} path, otherwise a directory.
     * See {@link #saveAs(String, CreditsLayout.Format)} for retargeting and failure behavior.
     */
    void saveAs(@NotNull String pathArg) throws Exception {
        saveAs(pathArg, CreditsLayout.Format.RESOURCE_PACK);
    }

    /**
     * Writes the documents in {@code format}, preserving unrelated destination content, then
     * retargets this session. If the resolved content directory and archive kind match the
     * current target, delegates to {@link #save()} without reopening the storage.
     *
     * @throws Exception if opening or writing fails; the original target stays active, but
     *                   partial destination writes are not rolled back
     */
    void saveAs(@NotNull String pathArg, CreditsLayout.Format format) throws Exception {
        Path newPath = Paths.get(pathArg)
            .toAbsolutePath()
            .normalize();
        Path curPath = resourceManager.location()
            .toAbsolutePath()
            .normalize();
        boolean archive = format == CreditsLayout.Format.RESOURCE_PACK && pathArg.toLowerCase(Locale.ROOT)
            .endsWith(".zip");
        Path destinationRoot = archive || format == CreditsLayout.Format.CONTENT_DIRECTORY ? newPath
            : newPath.resolve(CreditsLayout.RESOURCE_DIRECTORY);
        if (destinationRoot.equals(curPath) && archive == resourceManager.isArchive()) {
            save();
            return;
        }

        CreditsDocument credits = resourceManager.getCreditsDoc();
        Map<String, LangDocument> langs = langDocs();

        ResourceManager newRm = ResourceManager.destination(pathArg, packDescription(), format);
        try {
            newRm.adoptDocuments(credits, langs);
            newRm.writeCredits();
            newRm.writeAllLang();
        } catch (Exception ex) {
            try {
                newRm.close();
            } catch (IOException suppressed) {
                ex.addSuppressed(suppressed);
            }
            throw ex;
        }

        credits.markClean();
        for (LangDocument doc : langs.values()) {
            doc.markClean();
        }

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
