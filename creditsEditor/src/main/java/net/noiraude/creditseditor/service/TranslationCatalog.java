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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Read-only catalogue of language resources from a local Minecraft instance.
 * Includes GregTech's saved generated defaults without executing Minecraft or mod code.
 * Other runtime-generated translations without a saved resource remain unavailable.
 */
public final class TranslationCatalog {

    private static final String MINECRAFT_VERSION = "1.7.10";
    private static final Pattern GREGTECH_STRING_PROPERTY = Pattern.compile("S:(?:\"([^\"]+)\"|([^=]+))=(.*)");

    private final Map<String, Map<String, String>> languages = new LinkedHashMap<>();
    private boolean available;

    public static TranslationCatalog empty() {
        return new TranslationCatalog();
    }

    /**
     * Reads Minecraft 1.7.10 base resources from standard launcher or Prism/MultiMC layouts,
     * saved GregTech.lang defaults as English fallback, then sorted active mod archives, TX Loader load
     * resources, enabled packs in options order, and TX Loader force resources. Later resource values
     * replace earlier values; saved defaults only fill missing English keys. Does not write to the
     * instance or execute mod code. Generated defaults require an instance that has already saved GregTech.lang.
     *
     * @throws IOException if the mods directory is missing or a resource cannot be read
     */
    public static TranslationCatalog load(Path instance) throws IOException {
        if (!Files.isDirectory(instance.resolve("mods"))) {
            throw new IOException("Not a Minecraft instance (missing mods directory): " + instance);
        }
        TranslationCatalog result = new TranslationCatalog();
        try {
            result.available = result.minecraftLanguages(
                instance.toAbsolutePath()
                    .normalize());
        } catch (RuntimeException ex) {
            throw new IOException("Invalid Minecraft resource metadata for " + instance, ex);
        }
        result.gregTechDefaults(instance.resolve("GregTech.lang"));
        result.modArchives(instance.resolve("mods"));
        result.modArchives(
            instance.resolve("mods")
                .resolve(MINECRAFT_VERSION));
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
        return result;
    }

    /**
     * Whether the catalogue includes base-game English resources, so absent static references can
     * be reported as missing. Resolved mod translations remain usable when these resources are absent.
     */
    public boolean isAvailable() {
        return available;
    }

    /** Resolves the selected language first, then English, including any partially loaded catalogue. */
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

    private boolean minecraftLanguages(Path instance) throws IOException {
        List<Path> clients = new ArrayList<>();
        Path assets = instance.resolve("assets");
        Path parent = instance.getParent();
        if (parent != null && Files.isRegularFile(parent.resolve("mmc-pack.json"))) {
            boolean supportedVersion = false;
            JsonObject manifest = readJson(parent.resolve("mmc-pack.json"));
            if (
                !manifest.has("components") || !manifest.get("components")
                    .isJsonArray()
            ) return false;
            for (var component : manifest.getAsJsonArray("components")) {
                if (!component.isJsonObject()) continue;
                JsonObject value = component.getAsJsonObject();
                if (!value.has("uid") || !value.has("version")) continue;
                if (
                    "net.minecraft".equals(
                        value.get("uid")
                            .getAsString())
                ) {
                    supportedVersion = MINECRAFT_VERSION.equals(
                        value.get("version")
                            .getAsString());
                    break;
                }
            }
            if (!supportedVersion) return false;
            String library = "libraries/com/mojang/minecraft/" + MINECRAFT_VERSION
                + "/minecraft-"
                + MINECRAFT_VERSION
                + "-client.jar";
            clients.add(parent.resolve(library));
            Path instances = parent.getParent();
            if (instances != null && instances.getParent() != null) {
                Path launcher = instances.getParent();
                clients.add(launcher.resolve(library));
                assets = launcher.resolve("assets");
            }
        } else {
            clients.add(instance.resolve("versions/" + MINECRAFT_VERSION + "/" + MINECRAFT_VERSION + ".jar"));
        }
        boolean english = false;
        for (Path client : clients) {
            if (!Files.isRegularFile(client)) continue;
            english = archive(client);
            break;
        }
        return assetLanguages(assets) || english;
    }

    private boolean assetLanguages(Path assets) throws IOException {
        Path index = assets.resolve("indexes/" + MINECRAFT_VERSION + ".json");
        if (!Files.isRegularFile(index)) return false;
        boolean english = false;
        for (var entry : readJson(index).getAsJsonObject("objects")
            .entrySet()) {
            String[] parts = entry.getKey()
                .split("/");
            if (
                parts.length != 3 || !parts[0].equals("minecraft")
                    || !parts[1].equals("lang")
                    || !parts[2].endsWith(".lang")
            ) continue;
            String hash = entry.getValue()
                .getAsJsonObject()
                .get("hash")
                .getAsString();
            if (!hash.matches("[0-9a-f]{40}")) throw new IOException("Invalid language asset hash in " + index);
            Path file = assets.resolve("objects/" + hash.substring(0, 2) + "/" + hash);
            if (!Files.isRegularFile(file)) continue;
            try (InputStream input = Files.newInputStream(file)) {
                language(parts[2], input);
            }
            if (parts[2].equalsIgnoreCase("en_US.lang")) english = true;
        }
        return english;
    }

    private static JsonObject readJson(Path path) throws IOException {
        try (var input = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            @SuppressWarnings("deprecation")
            JsonObject value = new JsonParser().parse(input)
                .getAsJsonObject();
            return value;
        } catch (RuntimeException ex) {
            throw new IOException("Invalid Minecraft resource metadata: " + path, ex);
        }
    }

    private void modArchives(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return;
        try (Stream<Path> entries = Files.list(directory)) {
            for (Path file : entries.filter(Files::isRegularFile)
                .filter(path -> {
                    String name = path.getFileName()
                        .toString();
                    return name.endsWith(".jar") || name.endsWith(".zip");
                })
                .sorted()
                .toList()) archive(file);
        }
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
                        english.putIfAbsent(key, property.group(3));
                    }
                }
            }
        }
    }

    private boolean archive(Path path) throws IOException {
        boolean minecraftEnglish = false;
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
                    if (parts[1].equals("minecraft") && parts[3].equalsIgnoreCase("en_US.lang")) {
                        minecraftEnglish = true;
                    }
                }
            }
        }
        return minecraftEnglish;
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
