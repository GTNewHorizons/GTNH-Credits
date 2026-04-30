package net.noiraude.libcredits.pack;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Builds the {@code pack.mcmeta} text for a Minecraft 1.7.10 resource pack carrying
 * GTNH Credits resources.
 *
 * <p>
 * The metadata is intentionally minimal: only {@code pack_format} and {@code description}.
 * Callers may supply the description as a plain Java string; this class is responsible
 * for proper JSON encoding (quotes, backslashes, control characters, non-ASCII).
 */
public final class PackMcmeta {

    /** Relative path of {@code pack.mcmeta} inside a resource pack root. */
    public static final String PATH = "pack.mcmeta";

    /** Pack format used by Minecraft 1.7.10. */
    public static final int PACK_FORMAT = 1;

    private static final String DEFAULT_DESCRIPTION = "GTNH Credits resource pack";

    private PackMcmeta() {}

    /** Builds the {@code pack.mcmeta} JSON with a generic default description. */
    public static String build() {
        return build(DEFAULT_DESCRIPTION);
    }

    /**
     * Builds the {@code pack.mcmeta} JSON using the given description. The description
     * is a plain Java string; JSON escaping is handled by this method.
     */
    public static String build(String description) {
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", PACK_FORMAT);
        pack.addProperty("description", description);
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        return new GsonBuilder().setPrettyPrinting()
            .create()
            .toJson(root) + "\n";
    }
}
