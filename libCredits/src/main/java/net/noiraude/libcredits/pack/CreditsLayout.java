package net.noiraude.libcredits.pack;

/**
 * Paths relative to a credits content directory. Storage adapters mount either a direct
 * directory or assets/gtnhcredits inside a standard resource pack.
 *
 * <p>
 * Paths are kept as {@code String}s joined with {@code "/"}, so they address files
 * uniformly on the default filesystem and inside a jar/zip filesystem. Callers should
 * never wrap them in {@link java.nio.file.Paths#get} since that would bake the
 * platform-default separator into the value.
 */
public enum CreditsLayout {

    ROOT(""),
    CREDITS(ROOT, "credits.json"),
    LANG_DIR(ROOT, "lang");

    private final String path;

    CreditsLayout(String path) {
        this.path = path;
    }

    CreditsLayout(CreditsLayout parent, String child) {
        this.path = parent.path.isEmpty() ? child : parent.path + "/" + child;
    }

    /** Returns the relative path as a forward-slash string usable in any NIO2 filesystem. */
    public String get() {
        return path;
    }

    @Override
    public String toString() {
        return path;
    }

    public static final String RESOURCE_DIRECTORY = "assets/gtnhcredits";
    public static final String MODPACK_DIRECTORY = "config/txloader/load/gtnhcredits";

    public enum Format {
        /** The selected directory directly contains {@code credits.json} and {@code lang/}; no pack metadata. */
        CONTENT_DIRECTORY,
        /** An unpacked resource pack, with credits under {@code assets/gtnhcredits} and root pack metadata. */
        RESOURCE_DIRECTORY,
        /** A standard resource pack: ZIP when the selected path ends in {@code .zip}, otherwise a directory. */
        RESOURCE_PACK
    }

    public static final String LANG_EXT = ".lang";

    /** Builds the relative path of the lang file for {@code locale}. */
    public static String getLangPath(String locale) {
        return LANG_DIR.path + "/" + locale + LANG_EXT;
    }
}
