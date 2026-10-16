package net.noiraude.creditseditor.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.noiraude.libcredits.pack.ResourcePackSelection;

/**
 * Read-only catalogue of language resources from a local Minecraft instance.
 * Includes GregTech's saved generated defaults without executing Minecraft or mod code.
 * Other runtime-generated translations without a saved resource remain unavailable.
 */
public final class TranslationCatalog {

    private static final Pattern GREGTECH_STRING_PROPERTY = Pattern.compile("S:(?:\"([^\"]+)\"|([^=]+))=(.*)");

    private final Map<String, Map<String, String>> languages = new LinkedHashMap<>();
    private boolean available;

    public static TranslationCatalog empty() {
        return new TranslationCatalog();
    }

    /**
     * Reads saved GregTech.lang defaults as English fallback, then sorted mod jars, TX Loader load
     * resources, enabled packs in options order, and TX Loader force resources. Later values replace
     * earlier values. Does not write to the instance or execute mod code. Generated defaults require
     * an instance that has already saved GregTech.lang.
     *
     * @throws IOException if the mods directory is missing or a resource cannot be read
     */
    public static TranslationCatalog load(Path instance) throws IOException {
        if (!Files.isDirectory(instance.resolve("mods"))) {
            throw new IOException("Not a Minecraft instance (missing mods directory): " + instance);
        }
        TranslationCatalog result = new TranslationCatalog();
        result.gregTechDefaults(instance.resolve("GregTech.lang"));
        try (Stream<Path> entries = Files.walk(instance.resolve("mods"))) {
            for (Path jar : entries.filter(
                p -> p.toString()
                    .endsWith(".jar"))
                .sorted()
                .toList()) result.archive(jar);
        }
        result.directory(instance.resolve("config/txloader/load"));
        Path options = instance.resolve("options.txt");
        for (String name : ResourcePackSelection.read(options)) {
            if (name.startsWith("file/")) name = name.substring(5);
            Path pack = instance.resolve("resourcepacks")
                .resolve(name);
            if (Files.isDirectory(pack)) result.directory(pack.resolve("assets"));
            else if (Files.isRegularFile(pack)) result.archive(pack);
        }
        result.directory(instance.resolve("config/txloader/forceload"));
        result.available = true;
        return result;
    }

    public boolean isAvailable() {
        return available;
    }

    /** Resolves the selected language first, then English. An unavailable catalogue has no values. */
    public Optional<String> resolve(String key, String locale) {
        String value = languages.getOrDefault(locale.toLowerCase(Locale.ROOT), Map.of())
            .get(key);
        if (value == null || value.isEmpty()) value = languages.getOrDefault("en_us", Map.of())
            .get(key);
        return Optional.ofNullable(value)
            .filter(v -> !v.isEmpty());
    }

    /** Returns an immutable, sorted snapshot of English keys for reference search. */
    public List<String> keys() {
        List<String> keys = new ArrayList<>(
            languages.getOrDefault("en_us", Map.of())
                .keySet());
        Collections.sort(keys);
        return List.copyOf(keys);
    }

    private void gregTechDefaults(Path path) throws IOException {
        if (!Files.isRegularFile(path)) return;
        Map<String, String> english = languages.computeIfAbsent("en_us", k -> new LinkedHashMap<>());
        try (BufferedReader input = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            boolean languageFile = false;
            String line;
            while ((line = input.readLine()) != null) {
                line = line.trim();
                if (line.equalsIgnoreCase("languagefile {")) {
                    languageFile = true;
                } else if (line.equals("}")) {
                    languageFile = false;
                } else if (languageFile) {
                    Matcher property = GREGTECH_STRING_PROPERTY.matcher(line);
                    if (property.matches()) {
                        String key = property.group(1) != null ? property.group(1) : property.group(2);
                        english.put(key, property.group(3));
                    }
                }
            }
        }
    }

    private void archive(Path path) throws IOException {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            List<? extends ZipEntry> entries = Collections.list(zip.entries());
            entries.sort(java.util.Comparator.comparing(ZipEntry::getName));
            for (ZipEntry entry : entries) {
                String[] parts = entry.getName()
                    .split("/");
                if (
                    parts.length == 4 && parts[0].equals("assets")
                        && parts[2].equals("lang")
                        && parts[3].endsWith(".lang")
                ) {
                    try (InputStream input = zip.getInputStream(entry)) {
                        language(parts[3], input);
                    }
                }
            }
        }
    }

    private void directory(Path root) throws IOException {
        if (!Files.isDirectory(root)) return;
        try (Stream<Path> entries = Files.walk(root)) {
            for (Path file : entries.filter(Files::isRegularFile)
                .filter(
                    p -> p.toString()
                        .endsWith(".lang"))
                .filter(
                    p -> p.getParent()
                        .getFileName()
                        .toString()
                        .equals("lang"))
                .sorted()
                .toList()) {
                try (InputStream input = Files.newInputStream(file)) {
                    language(
                        file.getFileName()
                            .toString(),
                        input);
                }
            }
        }
    }

    private void language(String fileName, InputStream input) throws IOException {
        String locale = fileName.substring(0, fileName.length() - 5)
            .toLowerCase(Locale.ROOT);
        Map<String, String> values = languages.computeIfAbsent(locale, k -> new LinkedHashMap<>());
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("\uFEFF")) line = line.substring(1);
            if (line.isEmpty() || line.startsWith("#")) continue;
            int separator = line.indexOf('=');
            if (separator > 0) values.put(line.substring(0, separator), line.substring(separator + 1));
        }
    }
}
