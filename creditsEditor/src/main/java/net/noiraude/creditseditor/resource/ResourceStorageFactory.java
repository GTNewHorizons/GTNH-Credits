package net.noiraude.creditseditor.resource;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import net.noiraude.libcredits.pack.PackMcmeta;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Internal helpers that decide which {@link ResourceStorage} implementation backs a given
 * user-supplied path. The only place in the codebase that knows about directories vs jar
 * URIs vs {@link PackMcmeta} seeding.
 */
final class ResourceStorageFactory {

    private static final String ZIP_SUFFIX = ".zip";

    @Contract(pure = true)
    private ResourceStorageFactory() {}

    static boolean exists(@NotNull String pathArg) {
        Path path = Paths.get(pathArg);
        if (!Files.exists(path) || !Files.isReadable(path)) {
            return false;
        }
        return Files.isDirectory(path) || (pathArg.endsWith(ZIP_SUFFIX) && Files.isRegularFile(path));
    }

    @Contract("_ -> new")
    static @NotNull ResourceStorage open(@NotNull String pathArg) throws IOException {
        Path path = Paths.get(pathArg);
        if (!Files.exists(path)) {
            throw new IOException("No resource container at " + path);
        }
        if (Files.isDirectory(path)) {
            return new DirectoryStorage(path);
        }
        if (pathArg.endsWith(ZIP_SUFFIX) && Files.isRegularFile(path)) {
            return new ZipPackStorage(path, openZipFilesystem(path, false));
        }
        throw new IOException("Path is not a recognized resource container: " + path);
    }

    @Contract("_, _ -> new")
    static @NotNull ResourceStorage create(@NotNull String pathArg, @NotNull String packDescription)
        throws IOException {
        Path path = Paths.get(pathArg);
        if (Files.exists(path)) {
            throw new IOException("Refusing to overwrite existing path: " + path);
        }
        if (pathArg.endsWith(ZIP_SUFFIX)) {
            return new ZipPackStorage(path, createResourcePackZip(path, packDescription));
        }
        Files.createDirectories(path);
        Path mcmeta = path.resolve(PackMcmeta.PATH);
        Files.writeString(mcmeta, PackMcmeta.build(packDescription), StandardCharsets.UTF_8);
        return new DirectoryStorage(path);
    }

    private static @NotNull FileSystem openZipFilesystem(@NotNull Path zip, boolean create) throws IOException {
        URI uri = URI.create("jar:" + zip.toUri());
        Map<String, String> env = new HashMap<>();
        if (create) {
            env.put("create", "true");
        }
        return FileSystems.newFileSystem(uri, env);
    }

    private static @NotNull FileSystem createResourcePackZip(@NotNull Path zip, @NotNull String packDescription)
        throws IOException {
        Path parent = zip.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        // Write pack.mcmeta then close to flush the zip, then reopen for use.
        try (FileSystem fs = openZipFilesystem(zip, true)) {
            Files.writeString(fs.getPath(PackMcmeta.PATH), PackMcmeta.build(packDescription), StandardCharsets.UTF_8);
        }
        return openZipFilesystem(zip, false);
    }
}
