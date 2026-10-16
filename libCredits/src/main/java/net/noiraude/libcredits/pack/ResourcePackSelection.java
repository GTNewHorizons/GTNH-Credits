package net.noiraude.libcredits.pack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/** Reads the resource-pack selection from Minecraft 1.7.10 options without loading Minecraft classes. */
public final class ResourcePackSelection {

    private ResourcePackSelection() {}

    public static List<String> read(Path options) throws IOException {
        List<String> result = new ArrayList<>();
        if (!Files.isRegularFile(options)) return result;
        for (String line : Files.readAllLines(options, StandardCharsets.UTF_8)) {
            if (!line.startsWith("resourcePacks:")) continue;
            try {
                @SuppressWarnings("deprecation")
                JsonElement value = new JsonParser().parse(line.substring("resourcePacks:".length()));
                for (JsonElement entry : value.getAsJsonArray()) {
                    if (
                        !entry.isJsonPrimitive() || !entry.getAsJsonPrimitive()
                            .isString()
                    ) throw new IllegalArgumentException("pack name must be a string");
                    result.add(entry.getAsString());
                }
            } catch (RuntimeException ex) {
                throw new IOException("Invalid resourcePacks setting in " + options, ex);
            }
        }
        return result;
    }
}
