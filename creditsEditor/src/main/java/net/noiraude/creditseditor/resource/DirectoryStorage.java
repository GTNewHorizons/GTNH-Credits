package net.noiraude.creditseditor.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * {@link ResourceStorage} backed by a directory tree on the default filesystem.
 * {@link #close()} is a no-op since no system resource is held.
 */
final class DirectoryStorage implements ResourceStorage {

    private final @NotNull Path root;

    @Contract(pure = true)
    DirectoryStorage(@NotNull Path root) {
        this.root = root;
    }

    @Override
    public boolean hasNoFile(@NotNull String relPath) {
        return !Files.exists(root.resolve(relPath));
    }

    @Override
    public @NotNull InputStream openRead(@NotNull String relPath) throws IOException {
        return Files.newInputStream(root.resolve(relPath));
    }

    @Override
    public @NotNull OutputStream openWrite(@NotNull String relPath) throws IOException {
        Path target = root.resolve(relPath);
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        return Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Override
    public void delete(@NotNull String relPath) throws IOException {
        Files.deleteIfExists(root.resolve(relPath));
    }

    @Override
    public @NotNull Stream<String> listFiles(@NotNull String relDir) throws IOException {
        Path dir = root.resolve(relDir);
        if (!Files.isDirectory(dir)) {
            return Stream.empty();
        }
        try (Stream<Path> entries = Files.list(dir)) {
            List<String> names = entries.filter(Files::isRegularFile)
                .map(
                    p -> p.getFileName()
                        .toString())
                .sorted(Comparator.naturalOrder())
                .toList();
            return names.stream();
        }
    }

    @Contract(pure = true)
    @Override
    public @NotNull Path location() {
        return root;
    }

    @Contract(pure = true)
    @Override
    public void close() {
        // Directory storage holds no system resource.
    }
}
