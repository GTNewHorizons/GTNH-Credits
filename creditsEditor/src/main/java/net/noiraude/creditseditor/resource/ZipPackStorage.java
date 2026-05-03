package net.noiraude.creditseditor.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

/**
 * {@link ResourceStorage} backed by a Minecraft resource pack zip, accessed through
 * the JDK's jar/zip filesystem provider. {@link #close()} closes the underlying
 * filesystem and flushes pending writes to the {@code .zip} file.
 */
final class ZipPackStorage implements ResourceStorage {

    private final @NotNull Path zipPath;
    private final @NotNull FileSystem fs;

    ZipPackStorage(@NotNull Path zipPath, @NotNull FileSystem fs) {
        this.zipPath = zipPath;
        this.fs = fs;
    }

    @Override
    public boolean hasFile(@NotNull String relPath) {
        return Files.exists(fs.getPath("/", relPath));
    }

    @Override
    public @NotNull InputStream openRead(@NotNull String relPath) throws IOException {
        return Files.newInputStream(fs.getPath("/", relPath));
    }

    @Override
    public @NotNull OutputStream openWrite(@NotNull String relPath) throws IOException {
        Path target = fs.getPath("/", relPath);
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        return Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Override
    public void delete(@NotNull String relPath) throws IOException {
        Files.deleteIfExists(fs.getPath("/", relPath));
    }

    @Override
    public @NotNull Stream<String> listFiles(@NotNull String relDir) throws IOException {
        Path dir = fs.getPath("/", relDir);
        if (!Files.isDirectory(dir)) {
            return Stream.empty();
        }
        try (Stream<Path> entries = Files.list(dir)) {
            List<String> names = entries.filter(Files::isRegularFile)
                .map(
                    p -> p.getFileName()
                        .toString())
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
            return names.stream();
        }
    }

    @Override
    public @NotNull Path location() {
        return zipPath;
    }

    @Override
    public void close() throws IOException {
        fs.close();
    }
}
