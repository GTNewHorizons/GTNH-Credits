package net.noiraude.creditseditor.resource;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.noiraude.libcredits.pack.CreditsLayout;
import net.noiraude.libcredits.pack.CreditsLayout.Format;
import net.noiraude.libcredits.pack.PackMcmeta;

/** Resolves user-selected locations once; all storage paths are then relative to the credits content. */
final class ResourceStorageFactory {

    private ResourceStorageFactory() {}

    static boolean exists(String pathArg) {
        Path path = Paths.get(pathArg);
        return Files.isDirectory(path) || Files.isRegularFile(path) && (pathArg.toLowerCase(java.util.Locale.ROOT)
            .endsWith(".zip")
            || path.getFileName()
                .toString()
                .equals("credits.json"));
    }

    static ResourceStorage open(String pathArg) throws IOException {
        Path path = Paths.get(pathArg)
            .toAbsolutePath()
            .normalize();
        if (Files.isDirectory(path)) return new DirectoryStorage(resolveDirectory(path));
        if (
            Files.isRegularFile(path) && path.getFileName()
                .toString()
                .equals("credits.json")
        ) {
            return new DirectoryStorage(path.getParent());
        }
        if (
            Files.isRegularFile(path) && pathArg.toLowerCase(java.util.Locale.ROOT)
                .endsWith(".zip")
        ) {
            ResourceStorage storage = zip(path, false, "");
            if (storage.hasNoFile(CreditsLayout.CREDITS.get())) {
                storage.close();
                throw new IOException("No assets/gtnhcredits/credits.json in " + path);
            }
            return storage;
        }
        throw new IOException("No credits found at " + path + ". Use New to create credits.");
    }

    private static Path resolveDirectory(Path root) throws IOException {
        List<Path> candidates = new ArrayList<>();
        for (String relative : new String[] { "", CreditsLayout.RESOURCE_DIRECTORY, CreditsLayout.MODPACK_DIRECTORY }) {
            Path candidate = root.resolve(relative);
            if (Files.isRegularFile(candidate.resolve("credits.json"))) candidates.add(candidate);
        }
        if (candidates.size() == 1) return candidates.getFirst();
        if (
            candidates.isEmpty()
        ) throw new IOException(
            "No credits.json found at " + root
                + ". Select a credits file, content directory, modpack root or resource pack.");
        throw new IOException(
            "Multiple credits locations in " + root
                + ": "
                + candidates
                + ". Select the intended credits.json explicitly.");
    }

    static ResourceStorage create(String pathArg, String description) throws IOException {
        return create(pathArg, description, Format.RESOURCE_PACK);
    }

    static ResourceStorage create(String pathArg, String description, Format format) throws IOException {
        Path path = Paths.get(pathArg)
            .toAbsolutePath()
            .normalize();
        if (Files.exists(path)) throw new IOException("Refusing to overwrite existing path: " + path);
        return destination(pathArg, description, format);
    }

    /** Opens a destination in the explicitly chosen format, preserving unrelated existing files. */
    static ResourceStorage destination(String pathArg, String description, Format format) throws IOException {
        Path path = Paths.get(pathArg)
            .toAbsolutePath()
            .normalize();
        if (
            format == Format.RESOURCE_PACK && pathArg.toLowerCase(java.util.Locale.ROOT)
                .endsWith(".zip")
        ) return zip(path, !Files.exists(path), description);
        if (Files.exists(path) && !Files.isDirectory(path)) throw new IOException("Not a directory: " + path);
        Files.createDirectories(path);
        if (format != Format.CONTENT_DIRECTORY) {
            Path metadata = path.resolve(PackMcmeta.PATH);
            if (!Files.exists(metadata))
                Files.writeString(metadata, PackMcmeta.build(description), StandardCharsets.UTF_8);
            path = path.resolve(CreditsLayout.RESOURCE_DIRECTORY);
        }
        Files.createDirectories(path);
        return new DirectoryStorage(path);
    }

    private static ResourceStorage zip(Path path, boolean create, String description) throws IOException {
        if (create && path.getParent() != null) Files.createDirectories(path.getParent());
        FileSystem fs = FileSystems
            .newFileSystem(URI.create("jar:" + path.toUri()), create ? Map.of("create", "true") : Map.of());
        try {
            if (create) Files
                .writeString(fs.getPath("/", PackMcmeta.PATH), PackMcmeta.build(description), StandardCharsets.UTF_8);
            return new ZipPackStorage(path, fs);
        } catch (Exception ex) {
            fs.close();
            throw ex;
        }
    }
}
