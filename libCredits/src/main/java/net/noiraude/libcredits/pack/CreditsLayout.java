package net.noiraude.libcredits.pack;

/**
 * Filesystem layout of GTNH Credits resources inside a resource root (directory or
 * resource pack).
 *
 * <p>
 * Paths are kept as {@code String}s joined with {@code "/"}, so they address files
 * uniformly on the default filesystem and inside a jar/zip filesystem. Callers should
 * never wrap them in {@link java.nio.file.Paths#get} since that would bake the
 * platform-default separator into the value.
 */
public enum CreditsLayout {

    ROOT("assets/gtnhcredits"),
    CREDITS(ROOT, "credits.json"),
    LANG_DIR(ROOT, "lang");

    private final String path;

    CreditsLayout(String path) {
        this.path = path;
    }

    CreditsLayout(CreditsLayout parent, String child) {
        this.path = parent.path + "/" + child;
    }

    /** Returns the relative path as a forward-slash string usable in any NIO2 filesystem. */
    public String get() {
        return path;
    }

    @Override
    public String toString() {
        return path;
    }

    public static final String LANG_EXT = ".lang";

    /** Builds the relative path of the lang file for {@code locale}. */
    public static String getLangPath(String locale) {
        return LANG_DIR.path + "/" + locale + LANG_EXT;
    }
}
