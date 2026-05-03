package net.noiraude.creditseditor.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.lang.LangParser;
import net.noiraude.libcredits.lang.LangSerializer;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.pack.CreditsLayout;
import net.noiraude.libcredits.parser.CreditsParseException;
import net.noiraude.libcredits.parser.CreditsParser;
import net.noiraude.libcredits.serializer.CreditsSerializer;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Typed document repository for GTNH Credits resources, layered over a
 * backend-agnostic {@link ResourceStorage}.
 *
 * <p>
 * Get an instance via {@link #open(String, String)}, then call {@link #loadDocuments()}
 * to parse the resource files. The instance owns its storage and must be closed when
 * done; use try-with-resources.
 */
public final class ResourceManager implements Closeable {

    private final @NotNull ResourceStorage storage;

    private @Nullable CreditsDocument creditsDoc;

    /**
     * All loaded lang documents keyed by locale tag (lang file basename without
     * {@link CreditsLayout#LANG_EXT}). Always contains
     * {@link CreditsLayout#DEFAULT_LOCALE} after a successful {@link #loadDocuments()}
     * call, even when the on-disk lang directory is missing.
     */
    private final @NotNull Map<String, LangDocument> langDocs = new LinkedHashMap<>();

    /** Locales removed since the last successful save; their files are deleted on save. */
    private final @NotNull Set<String> pendingDeletions = new HashSet<>();

    public ResourceManager(@NotNull ResourceStorage storage) {
        this.storage = storage;
    }

    /**
     * Returns {@code true} when a resource container exists at {@code pathArg} and is in
     * a state that {@link #open(String)} would accept. Cheap probe meant to be paired with
     * {@link #open(String)} or {@link #create(String, String)} to choose the next action.
     */
    @Contract(pure = true)
    public static boolean exists(@NotNull String pathArg) {
        return ResourceStorage.exists(pathArg);
    }

    /**
     * Opens an existing resource container at {@code pathArg} and wraps it in a manager.
     *
     * @throws IOException see {@link ResourceStorage#open(String)}
     */
    @Contract("_ -> new")
    public static @NotNull ResourceManager open(@NotNull String pathArg) throws IOException {
        return new ResourceManager(ResourceStorage.open(pathArg));
    }

    /**
     * Creates a new resource container at {@code pathArg}, seeded with {@code packDescription}
     * for {@code pack.mcmeta}, and wraps it in a manager.
     *
     * @throws IOException see {@link ResourceStorage#create(String, String)}
     */
    @Contract("_, _ -> new")
    public static @NotNull ResourceManager create(@NotNull String pathArg, @NotNull String packDescription)
        throws IOException {
        return new ResourceManager(ResourceStorage.create(pathArg, packDescription));
    }

    /**
     * Parses the credits JSON and every {@code *.lang} file under
     * {@link CreditsLayout#LANG_DIR} and stores the resulting documents internally.
     * Must be called once before {@link #getCreditsDoc()}, {@link #getLangDoc()},
     * {@link #langDoc(String)}, {@link #availableLocales()}, or {@link #isDirty()}
     * are meaningful.
     *
     * <p>
     * If no {@link CreditsLayout#DEFAULT_LOCALE} lang file is present on disk, an empty
     * in-memory document is seeded for it so {@link #getLangDoc()} always succeeds; the
     * file is not created on disk until the next save.
     *
     * @throws IOException           if a file exists but cannot be read
     * @throws CreditsParseException if the credits JSON is structurally invalid
     */
    public void loadDocuments() throws IOException, CreditsParseException {
        if (!storage.hasFile(CreditsLayout.CREDITS.get())) {
            creditsDoc = CreditsDocument.empty();
        } else {
            try (InputStream in = storage.openRead(CreditsLayout.CREDITS.get())) {
                creditsDoc = CreditsParser.parse(in);
            }
        }

        langDocs.clear();
        pendingDeletions.clear();
        List<String> langFiles;
        try (Stream<String> entries = storage.listFiles(CreditsLayout.LANG_DIR.get())) {
            langFiles = entries.filter(name -> name.endsWith(CreditsLayout.LANG_EXT))
                .collect(Collectors.toList());
        }
        for (String langFile : langFiles) {
            String locale = langFile.substring(0, langFile.length() - CreditsLayout.LANG_EXT.length());
            try (InputStream in = storage.openRead(CreditsLayout.getLangPath(locale))) {
                langDocs.put(locale, LangParser.parse(in));
            }
        }
        if (!langDocs.containsKey(CreditsLayout.DEFAULT_LOCALE)) {
            langDocs.put(CreditsLayout.DEFAULT_LOCALE, LangParser.empty());
        }
    }

    /**
     * Seeds this manager with already-loaded documents in lieu of {@link #loadDocuments()}.
     * Intended for the Save As flow, which writes existing in-memory documents to a freshly
     * opened destination without parsing whatever is already there.
     *
     * <p>
     * The supplied lang map replaces the in-memory state; iteration order is preserved when
     * {@code langs} is a {@link LinkedHashMap}. {@link CreditsLayout#DEFAULT_LOCALE} is
     * seeded with an empty document if absent from {@code langs}.
     */
    public void adoptDocuments(@NotNull CreditsDocument credits, @NotNull Map<String, LangDocument> langs) {
        this.creditsDoc = credits;
        this.langDocs.clear();
        this.langDocs.putAll(langs);
        if (!this.langDocs.containsKey(CreditsLayout.DEFAULT_LOCALE)) {
            this.langDocs.put(CreditsLayout.DEFAULT_LOCALE, LangParser.empty());
        }
        this.pendingDeletions.clear();
    }

    /** Returns the mutable credits document loaded by {@link #loadDocuments()}. */
    @Contract(pure = true)
    public @NotNull CreditsDocument getCreditsDoc() {
        return Objects.requireNonNull(creditsDoc, "call loadDocuments() first");
    }

    /**
     * Returns the {@link CreditsLayout#DEFAULT_LOCALE} lang document loaded by
     * {@link #loadDocuments()}. Equivalent to {@code langDoc(DEFAULT_LOCALE)} but
     * never returns {@code null}.
     */
    @Contract(pure = true)
    public @NotNull LangDocument getLangDoc() {
        LangDocument doc = langDocs.get(CreditsLayout.DEFAULT_LOCALE);
        return Objects.requireNonNull(doc, "call loadDocuments() first");
    }

    /** Returns the lang document for {@code locale}, or {@code null} if not loaded. */
    @Contract(pure = true)
    public @Nullable LangDocument langDoc(@NotNull String locale) {
        return langDocs.get(locale);
    }

    /**
     * Returns the locale tags currently loaded in memory, in insertion order
     * ({@link CreditsLayout#DEFAULT_LOCALE} first when it was seeded by
     * {@link #loadDocuments()}).
     */
    @Contract(pure = true)
    public @NotNull Set<String> availableLocales() {
        return Collections.unmodifiableSet(langDocs.keySet());
    }

    /**
     * Adds an empty lang document for {@code locale} if absent and returns the document
     * registered for it. The new locale's file is not created on disk until the next save.
     */
    public @NotNull LangDocument addLocale(@NotNull String locale) {
        pendingDeletions.remove(locale);
        return langDocs.computeIfAbsent(locale, k -> LangParser.empty());
    }

    /**
     * Removes the in-memory lang document for {@code locale} and returns it, or
     * {@code null} if the locale was not loaded. The corresponding file is deleted from
     * disk by the next save.
     */
    public @Nullable LangDocument removeLocale(@NotNull String locale) {
        LangDocument removed = langDocs.remove(locale);
        if (removed != null) {
            pendingDeletions.add(locale);
        }
        return removed;
    }

    /**
     * Returns {@code true} if any loaded document has unsaved changes or any locale is
     * pending on-disk deletion. Returns {@code false} if {@link #loadDocuments()} has not
     * been called yet.
     */
    @Contract(pure = true)
    public boolean isDirty() {
        if (creditsDoc == null) return false;
        if (creditsDoc.isDirty()) return true;
        if (!pendingDeletions.isEmpty()) return true;
        for (LangDocument doc : langDocs.values()) {
            if (doc.isDirty()) return true;
        }
        return false;
    }

    /** Returns the file name of the directory or zip backing this manager, for UI display. */
    @Contract(pure = true)
    public @NotNull String displayPath() {
        return storage.location()
            .getFileName()
            .toString();
    }

    /**
     * Returns the on-disk location of the directory or zip backing this manager, for path
     * equality checks (e.g. Save As same-target detection). Never use the returned path to
     * perform I/O; route reads and writes through the storage primitives instead.
     */
    @Contract(pure = true)
    public @NotNull java.nio.file.Path location() {
        return storage.location();
    }

    /**
     * Validates the credits document via a round-trip parse and writes it to
     * {@link CreditsLayout#CREDITS}. Throws before writing if validation fails.
     *
     * @throws IOException           if the file cannot be written
     * @throws CreditsParseException if the serialized data fails to reparse
     */
    public void writeCredits() throws IOException, CreditsParseException {
        CreditsDocument doc = Objects.requireNonNull(creditsDoc, "call loadDocuments() first");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        CreditsSerializer.write(doc, buf);
        byte[] bytes = buf.toByteArray();
        CreditsParser.parse(new ByteArrayInputStream(bytes)); // round-trip validation
        try (OutputStream out = storage.openWrite(CreditsLayout.CREDITS.get())) {
            out.write(bytes);
        }
    }

    /**
     * Saves every dirty lang document and deletes the on-disk file of every locale that
     * was removed via {@link #removeLocale(String)} since the last successful save.
     *
     * @throws IOException if any file cannot be written or deleted
     */
    public void writeLang() throws IOException {
        writeLang(false);
    }

    /**
     * Saves every loaded lang document unconditionally and deletes the on-disk file of
     * every locale that was removed via {@link #removeLocale(String)} since the last
     * successful save. Intended for the Save As flow, where the destination is fresh and
     * the dirty bits (relative to the previous storage) do not apply.
     *
     * @throws IOException if any file cannot be written or deleted
     */
    public void writeAllLang() throws IOException {
        writeLang(true);
    }

    private void writeLang(boolean force) throws IOException {
        Objects.requireNonNull(creditsDoc, "call loadDocuments() first");
        for (Map.Entry<String, LangDocument> entry : langDocs.entrySet()) {
            LangDocument doc = entry.getValue();
            if (force || doc.isDirty()) {
                try (OutputStream out = storage.openWrite(CreditsLayout.getLangPath(entry.getKey()))) {
                    LangSerializer.write(doc, out);
                }
            }
        }
        for (String locale : pendingDeletions) {
            storage.delete(CreditsLayout.getLangPath(locale));
        }
        pendingDeletions.clear();
    }

    /** Closes the underlying storage. */
    @Override
    public void close() throws IOException {
        storage.close();
    }
}
