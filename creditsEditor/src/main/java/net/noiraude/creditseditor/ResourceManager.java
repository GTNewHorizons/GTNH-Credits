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
import java.util.HashMap;
import java.util.Map;

import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.lang.LangParser;
import net.noiraude.libcredits.lang.LangSerializer;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.parser.CreditsParseException;
import net.noiraude.libcredits.parser.CreditsParser;
import net.noiraude.libcredits.serializer.CreditsSerializer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

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
    public static final String CREDITS_PATH = "assets/gtnhcredits/credits.json";

    /** Relative path of the English lang file inside the resource root. */
    public static final String LANG_PATH = "assets/gtnhcredits/lang/en_US.lang";

    private final Path diskPath;
    private final FileSystem zipFs; // null for directory-mode instances

    /** Root path inside the active filesystem (directory path or zip root). */
    private final Path resourceRoot;

    private CreditsDocument creditsDoc;
    private LangDocument langDoc;

    private ResourceManager(Path diskPath, FileSystem zipFs) {
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
    public static ResourceManager open(String pathArg) throws IOException {
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
     * Parses the credits JSON and lang file from the resource root and stores the resulting
     * documents internally. Must be called once before {@link #getCreditsDoc()},
     * {@link #getLangDoc()}, or {@link #isDirty()} are meaningful.
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

        if (notExists(LANG_PATH)) {
            langDoc = LangParser.empty();
        } else {
            try (InputStream in = openRead(LANG_PATH)) {
                langDoc = LangParser.parse(in);
            }
        }
    }

    /** Returns the mutable credits document loaded by {@link #loadDocuments()}. */
    public CreditsDocument getCreditsDoc() {
        return creditsDoc;
    }

    /** Returns the mutable lang document loaded by {@link #loadDocuments()}. */
    public LangDocument getLangDoc() {
        return langDoc;
    }

    /**
     * Returns {@code true} if any loaded document has unsaved changes.
     * Returns {@code false} if {@link #loadDocuments()} has not been called yet.
     */
    public boolean isDirty() {
        if (creditsDoc == null || langDoc == null) return false;
        return creditsDoc.isDirty() || langDoc.isDirty();
    }

    /** Returns the on-disk path of the directory or zip file. */
    public Path getDiskPath() {
        return diskPath;
    }

    /** Returns {@code true} if {@code relativePath} does not exist inside the resource root. */
    public boolean notExists(String relativePath) {
        return !Files.exists(resourceRoot.resolve(relativePath));
    }

    /**
     * Opens an {@link InputStream} for reading {@code relativePath} from the resource root.
     *
     * @throws IOException if the file does not exist or cannot be read
     */
    public InputStream openRead(String relativePath) throws IOException {
        return Files.newInputStream(resourceRoot.resolve(relativePath));
    }

    /**
     * Opens an {@link OutputStream} for writing {@code relativePath} inside the resource root.
     * Parent directories (or zip entries) are created automatically.
     *
     * @throws IOException if the file cannot be written
     */
    public OutputStream openWrite(String relativePath) throws IOException {
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
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        CreditsSerializer.write(creditsDoc, buf);
        byte[] bytes = buf.toByteArray();
        CreditsParser.parse(new ByteArrayInputStream(bytes)); // round-trip validation
        try (OutputStream out = openWrite(CREDITS_PATH)) {
            out.write(bytes);
        }
    }

    /**
     * Saves the lang document to {@code lang/en_US.lang}.
     *
     * @throws IOException if the file cannot be written
     */
    public void writeLang() throws IOException {
        try (OutputStream out = openWrite(LANG_PATH)) {
            LangSerializer.write(langDoc, out);
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

    private static FileSystem openZipFilesystem(Path zip, boolean create) throws IOException {
        URI uri = URI.create("jar:" + zip.toUri());
        Map<String, String> env = new HashMap<>();
        if (create) {
            env.put("create", "true");
        }
        return FileSystems.newFileSystem(uri, env);
    }

    private static FileSystem createResourcePackZip(Path zip) throws IOException {
        Path parent = zip.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        // Write pack.mcmeta then close to flush the zip, then reopen for use.
        try (FileSystem fs = openZipFilesystem(zip, true)) {
            Files.writeString(fs.getPath("pack.mcmeta"), buildPackMcmeta(), StandardCharsets.UTF_8);
        }
        return openZipFilesystem(zip, false);
    }

    private static String buildPackMcmeta() {
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 1);
        pack.addProperty("description", "GTNH Credits resource pack");
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        return new GsonBuilder().setPrettyPrinting()
            .create()
            .toJson(root) + "\n";
    }
}
