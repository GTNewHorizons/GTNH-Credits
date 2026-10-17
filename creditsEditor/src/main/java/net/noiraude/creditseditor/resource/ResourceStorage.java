package net.noiraude.creditseditor.resource;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Backend-agnostic byte-level access to a GTNH Credits resource container. Implementations
 * mount either a credits content directory on disk or {@code assets/gtnhcredits} inside a
 * Minecraft resource pack zip file.
 *
 * <p>
 * Every {@code relPath} argument is a forward-slash separated path relative to the mounted
 * content directory; values from {@link net.noiraude.libcredits.pack.CreditsLayout} are the canonical
 * source.
 *
 * <p>
 * Use {@link #open(String)} for existing credits and {@link #create(String, String)} for a new
 * container. The caller owns the returned storage and must close it.
 */
public interface ResourceStorage extends Closeable {

    /**
     * Tests for a candidate directory, {@code credits.json} file or ZIP. A positive result
     * does not establish that {@link #open(String)} can resolve existing credits there.
     * Returns {@code false} for missing paths and unsupported file types.
     */
    @Contract(pure = true)
    static boolean exists(@NotNull String pathArg) {
        return ResourceStorageFactory.exists(pathArg);
    }

    /**
     * Resolves existing credits from a direct JSON path, content directory, modpack root,
     * resource-pack directory or ZIP. No files are created. Directory inputs must resolve
     * to exactly one supported credits location; select the JSON file to disambiguate.
     *
     * @throws IOException if credits are missing, ambiguous or cannot be opened
     */
    @Contract("_ -> new")
    static @NotNull ResourceStorage open(@NotNull String pathArg) throws IOException {
        return ResourceStorageFactory.open(pathArg);
    }

    /**
     * Creates a new resource container at {@code pathArg}, seeded with a {@code pack.mcmeta}
     * carrying {@code packDescription}. The backend is chosen from {@code pathArg}: paths
     * ending with {@code .zip} produce a Minecraft resource pack zip; everything else
     * produces an unpacked resource-pack directory. Both mount {@code assets/gtnhcredits}
     * as the content directory.
     *
     * @throws IOException if {@code pathArg} already exists, or any I/O error occurs
     */
    @Contract("_, _ -> new")
    static @NotNull ResourceStorage create(@NotNull String pathArg, @NotNull String packDescription)
        throws IOException {
        return ResourceStorageFactory.create(pathArg, packDescription);
    }

    default boolean isArchive() {
        return false;
    }

    /** Returns {@code false} if {@code relPath} resolves to an existing file inside this storage. */
    boolean hasNoFile(@NotNull String relPath);

    /**
     * Opens an {@link InputStream} for reading {@code relPath}.
     *
     * @throws IOException if the file does not exist or cannot be read
     */
    @NotNull
    InputStream openRead(@NotNull String relPath) throws IOException;

    /**
     * Opens an {@link OutputStream} for writing {@code relPath}, truncating any prior content.
     * Parent directories (or zip entries) are created automatically.
     *
     * @throws IOException if the file cannot be written
     */
    @NotNull
    OutputStream openWrite(@NotNull String relPath) throws IOException;

    /**
     * Deletes {@code relPath} if it exists. No-op when the file is absent.
     *
     * @throws IOException if the file exists but cannot be deleted
     */
    void delete(@NotNull String relPath) throws IOException;

    /**
     * Returns the relative names of every regular file directly under {@code relDir},
     * sorted lexicographically. The returned stream is empty when the directory is missing.
     * The caller is responsible for closing the stream.
     *
     * @throws IOException if {@code relDir} exists but cannot be enumerated
     */
    @NotNull
    Stream<String> listFiles(@NotNull String relDir) throws IOException;

    /**
     * Returns the on-disk path of the directory or zip file. For display, equality, and
     * logging only; never use it to perform I/O. Use the {@code openRead} / {@code openWrite}
     * primitives instead so the same code works for both backends.
     */
    @NotNull
    Path location();
}
