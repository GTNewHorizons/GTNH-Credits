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
 * back this interface with either a directory tree on disk or a Minecraft resource pack zip
 * file; callers cannot tell them apart.
 *
 * <p>
 * Every {@code relPath} argument is a forward-slash separated path relative to the resource
 * root; values from {@link net.noiraude.libcredits.pack.CreditsLayout} are the canonical
 * source.
 *
 * <p>
 * Use {@link #exists(String)} to query whether a resource container is available at a path,
 * then {@link #open(String)} or {@link #create(String, String)} to get an instance.
 */
public interface ResourceStorage extends Closeable {

    /**
     * Returns {@code true} when {@code pathArg} points to an existing resource container
     * (a directory or a {@code .zip} file) that can be opened by {@link #open(String)}.
     * False on missing paths, unsupported file types, or anything that fails to be
     * recognized as a resource container.
     */
    @Contract(pure = true)
    static boolean exists(@NotNull String pathArg) {
        return ResourceStorageFactory.exists(pathArg);
    }

    /**
     * Opens an existing resource container at {@code pathArg}.
     *
     * @throws IOException if {@code pathArg} does not exist or is not a recognized resource
     *                     container
     */
    @Contract("_ -> new")
    static @NotNull ResourceStorage open(@NotNull String pathArg) throws IOException {
        return ResourceStorageFactory.open(pathArg);
    }

    /**
     * Creates a new resource container at {@code pathArg}, seeded with a {@code pack.mcmeta}
     * carrying {@code packDescription}. The backend is chosen from {@code pathArg}: paths
     * ending with {@code .zip} produce a Minecraft resource pack zip; everything else
     * produces a directory. Callers do not need to know which.
     *
     * @throws IOException if {@code pathArg} already exists, or any I/O error occurs
     */
    @Contract("_, _ -> new")
    static @NotNull ResourceStorage create(@NotNull String pathArg, @NotNull String packDescription)
        throws IOException {
        return ResourceStorageFactory.create(pathArg, packDescription);
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
