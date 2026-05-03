package net.noiraude.creditseditor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import net.noiraude.creditseditor.ui.AppInfo;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.lang.LangParser;
import net.noiraude.libcredits.lang.LangSerializer;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.pack.PackMcmeta;
import net.noiraude.libcredits.parser.CreditsParseException;
import net.noiraude.libcredits.parser.CreditsParser;
import net.noiraude.libcredits.serializer.CreditsSerializer;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstracts access to GTNH Credits resource files ({@code credits.json} and lang files)
 * over either a plain directory tree or a Minecraft resource pack zip file.
 *
 * <p>
 * Get an instance via {@link #open(String)}, then call {@link #loadDocuments()} to parse the
 * resource files. The caller is responsible for closing the instance when done; use
 * try-with-resources.
 */
public final class ResourceManager implements Closeable {

    /** Relative path of the credits JSON inside the resource root. */
    public static final @NotNull String CREDITS_PATH = "assets/gtnhcredits/credits.json";

    /** Relative directory holding all per-locale lang files inside the resource root. */
    public static final @NotNull String LANG_DIR = "assets/gtnhcredits/lang";

    /** Filename suffix for Minecraft lang files. */
    public static final @NotNull String LANG_EXT = ".lang";

    /** Reference locale tag. English is always present and never removed. */
    public static final @NotNull String DEFAULT_LOCALE = "en_US";

    /** Relative path of the English lang file inside the resource root. */
    public static final @NotNull String LANG_PATH = LANG_DIR + "/" + DEFAULT_LOCALE + LANG_EXT;

    private final @NotNull Path diskPath;
    private final @Nullable FileSystem zipFs; // null for directory-mode instances

    /** Root path inside the active filesystem (directory path or zip root). */
    private final @NotNull Path resourceRoot;

    private @Nullable CreditsDocument creditsDoc;

    /**
     * All loaded lang documents keyed by locale tag (lang file basename without
     * {@value #LANG_EXT}). Always contains {@link #DEFAULT_LOCALE} after a successful
     * {@link #loadDocuments()} call, even when the on-disk lang directory is missing.
     */
    private final @NotNull Map<String, LangDocument> langDocs = new LinkedHashMap<>();

    private ResourceManager(@NotNull Path diskPath, @Nullable FileSystem zipFs) {
        this.diskPath = diskPath;
        this.zipFs = zipFs;
        this.resourceRoot = (zipFs != null) ? zipFs.getPath("/") : diskPath;
    }

    /**
     * Opens or creates the resource root described by {@code pathArg}.
     *
     * <ul>
     * <li>Existing directory: opened as-is in directory mode.</li>
     * <li>Existing {@code .zip} file: opened as a resource pack in zip mode.</li>
     * <li>Non-existent path without {@code .zip} suffix: directory is created and opened in
     * directory mode.</li>
     * <li>Non-existent path with {@code .zip} suffix: a new resource pack zip is created with
     * a {@code pack.mcmeta} for Minecraft 1.7.10 (pack_format 1), then opened in zip mode.</li>
     * </ul>
     *
     * @param pathArg the path argument supplied by the user
     * @return an open {@code ResourceManager}; must be closed when no longer needed
     * @throws IOException if the path exists but is neither a directory nor a {@code .zip} file,
     *                     or if any I/O error occurs during creation
     */
    @Contract("_ -> new")
    public static @NotNull ResourceManager open(@NotNull String pathArg) throws IOException {
        Path path = Paths.get(pathArg);
        boolean isZipSuffix = pathArg.endsWith(".zip");

        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                return new ResourceManager(path, null);
            } else if (isZipSuffix) {
                return new ResourceManager(path, openZipFilesystem(path, false));
            } else {
                throw new IOException("Path exists but is neither a directory nor a .zip file: " + path);
            }
        } else {
            if (isZipSuffix) {
                return new ResourceManager(path, createResourcePackZip(path));
            } else {
                Files.createDirectories(path);
                return new ResourceManager(path, null);
            }
        }
    }

    /**
     * Parses the credits JSON and every {@code *.lang} file under {@link #LANG_DIR} and
     * stores the resulting documents internally. Must be called once before
     * {@link #getCreditsDoc()}, {@link #getLangDoc()}, {@link #langDoc(String)},
     * {@link #availableLocales()}, or {@link #isDirty()} are meaningful.
     *
     * <p>
     * If no {@value #DEFAULT_LOCALE} lang file is present on disk, an empty in-memory
     * document is seeded for it so {@link #getLangDoc()} always succeeds; the file is
     * not created on disk until the next save.
     *
     * @throws IOException           if a file exists but cannot be read
     * @throws CreditsParseException if the credits JSON is structurally invalid
     */
    public void loadDocuments() throws IOException, CreditsParseException {
        if (notExists(CREDITS_PATH)) {
            creditsDoc = CreditsDocument.empty();
        } else {
            try (InputStream in = openRead(CREDITS_PATH)) {
                creditsDoc = CreditsParser.parse(in);
            }
        }

        langDocs.clear();
        Path langDir = resourceRoot.resolve(LANG_DIR);
        if (Files.isDirectory(langDir)) {
            List<Path> langFiles;
            try (Stream<Path> entries = Files.list(langDir)) {
                langFiles = entries.filter(Files::isRegularFile)
                    .filter(
                        p -> p.getFileName()
                            .toString()
                            .endsWith(LANG_EXT))
                    .sorted()
                    .toList();
            }
            for (Path langFile : langFiles) {
                String basename = langFile.getFileName()
                    .toString();
                String locale = basename.substring(0, basename.length() - LANG_EXT.length());
                try (InputStream in = Files.newInputStream(langFile)) {
                    langDocs.put(locale, LangParser.parse(in));
                }
            }
        }
        if (!langDocs.containsKey(DEFAULT_LOCALE)) {
            langDocs.put(DEFAULT_LOCALE, LangParser.empty());
        }
    }

    /**
     * Seeds this manager with already-loaded documents in lieu of {@link #loadDocuments()}.
     * Intended for the Save As flow, which writes existing in-memory documents to a freshly
     * opened destination without parsing whatever is already there.
     *
     * <p>
     * The supplied lang document is registered under {@link #DEFAULT_LOCALE}. Callers that
     * need to transfer additional locales should use {@link #addLocale(String)} after this
     * call (multi-locale Save As is wired in a later phase).
     */
    public void adoptDocuments(@NotNull CreditsDocument credits, @NotNull LangDocument lang) {
        this.creditsDoc = credits;
        this.langDocs.clear();
        this.langDocs.put(DEFAULT_LOCALE, lang);
    }

    /** Returns the mutable credits document loaded by {@link #loadDocuments()}. */
    @Contract(pure = true)
    public @NotNull CreditsDocument getCreditsDoc() {
        return Objects.requireNonNull(creditsDoc, "call loadDocuments() first");
    }

    /**
     * Returns the {@link #DEFAULT_LOCALE} lang document loaded by {@link #loadDocuments()}.
     * Equivalent to {@code langDoc(DEFAULT_LOCALE)} but never returns {@code null}.
     */
    @Contract(pure = true)
    public @NotNull LangDocument getLangDoc() {
        LangDocument doc = langDocs.get(DEFAULT_LOCALE);
        return Objects.requireNonNull(doc, "call loadDocuments() first");
    }

    /** Returns the lang document for {@code locale}, or {@code null} if not loaded. */
    @Contract(pure = true)
    public @Nullable LangDocument langDoc(@NotNull String locale) {
        return langDocs.get(locale);
    }

    /**
     * Returns the locale tags currently loaded in memory, in insertion order
     * ({@link #DEFAULT_LOCALE} first when it was seeded by {@link #loadDocuments()}).
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
        return langDocs.computeIfAbsent(locale, k -> LangParser.empty());
    }

    /**
     * Removes the in-memory lang document for {@code locale} and returns it, or
     * {@code null} if the locale was not loaded. The corresponding file is deleted from
     * disk by the next save.
     */
    public @Nullable LangDocument removeLocale(@NotNull String locale) {
        return langDocs.remove(locale);
    }

    /**
     * Returns {@code true} if any loaded document has unsaved changes.
     * Returns {@code false} if {@link #loadDocuments()} has not been called yet.
     */
    @Contract(pure = true)
    public boolean isDirty() {
        if (creditsDoc == null) return false;
        if (creditsDoc.isDirty()) return true;
        for (LangDocument doc : langDocs.values()) {
            if (doc.isDirty()) return true;
        }
        return false;
    }

    /** Returns the on-disk path of the directory or zip file. */
    @Contract(pure = true)
    public @NotNull Path getDiskPath() {
        return diskPath;
    }

    /** Returns {@code true} if {@code relativePath} does not exist inside the resource root. */
    @Contract(pure = true)
    public boolean notExists(@NotNull String relativePath) {
        return !Files.exists(resourceRoot.resolve(relativePath));
    }

    /**
     * Opens an {@link InputStream} for reading {@code relativePath} from the resource root.
     *
     * @throws IOException if the file does not exist or cannot be read
     */
    public @NotNull InputStream openRead(@NotNull String relativePath) throws IOException {
        return Files.newInputStream(resourceRoot.resolve(relativePath));
    }

    /**
     * Opens an {@link OutputStream} for writing {@code relativePath} inside the resource root.
     * Parent directories (or zip entries) are created automatically.
     *
     * @throws IOException if the file cannot be written
     */
    public @NotNull OutputStream openWrite(@NotNull String relativePath) throws IOException {
        Path target = resourceRoot.resolve(relativePath);
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        return Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Validates {@code data} via a round-trip parse and writes it to {@code credits.json}.
     * Throws before writing if validation fails.
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
        try (OutputStream out = openWrite(CREDITS_PATH)) {
            out.write(bytes);
        }
    }

    /**
     * Saves the {@link #DEFAULT_LOCALE} lang document to {@link #LANG_PATH}.
     *
     * @throws IOException if the file cannot be written
     */
    public void writeLang() throws IOException {
        LangDocument doc = Objects.requireNonNull(langDocs.get(DEFAULT_LOCALE), "call loadDocuments() first");
        try (OutputStream out = openWrite(LANG_PATH)) {
            LangSerializer.write(doc, out);
        }
    }

    /** Closes the underlying zip filesystem, if any. Has no effect in directory mode. */
    @Override
    public void close() throws IOException {
        if (zipFs != null) {
            zipFs.close();
        }
    }

    // -----------------------------------------------------------------------

    private static @NotNull FileSystem openZipFilesystem(@NotNull Path zip, boolean create) throws IOException {
        URI uri = URI.create("jar:" + zip.toUri());
        Map<String, String> env = new HashMap<>();
        if (create) {
            env.put("create", "true");
        }
        return FileSystems.newFileSystem(uri, env);
    }

    private static @NotNull FileSystem createResourcePackZip(@NotNull Path zip) throws IOException {
        Path parent = zip.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        // Write pack.mcmeta then close to flush the zip, then reopen for use.
        try (FileSystem fs = openZipFilesystem(zip, true)) {
            Files.writeString(fs.getPath(PackMcmeta.PATH), PackMcmeta.build(packDescription()), StandardCharsets.UTF_8);
        }
        return openZipFilesystem(zip, false);
    }

    @Contract(pure = true)
    private static @NotNull String packDescription() {
        return "GTNH Credits resource pack\nGenerated by " + AppInfo.name() + " " + AppInfo.version();
    }
}
