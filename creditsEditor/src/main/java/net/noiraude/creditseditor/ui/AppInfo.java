package net.noiraude.creditseditor.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Application metadata loaded from the bundled {@code /version.properties} resource.
 *
 * <p>
 * The properties file is populated at build time by the Gradle {@code processResources}
 * task for fields that depend on the build (e.g. {@code version}); static fields are
 * carried through verbatim. Missing values fall back to a safe empty or {@code "unknown"}
 * placeholder so the UI never renders {@code null}.
 */
public final class AppInfo {

    private static final @NotNull Properties PROPS = load();

    @Contract(pure = true)
    private AppInfo() {}

    public static @NotNull String name() {
        return get("name", "GTNH Credits Editor");
    }

    public static @NotNull String version() {
        return get("version", "unknown");
    }

    public static @NotNull String license() {
        return get("license", "");
    }

    public static @NotNull String author() {
        return get("author", "");
    }

    public static @NotNull String copyright() {
        return get("copyright", "");
    }

    public static @NotNull String url() {
        return get("url", "");
    }

    private static @NotNull String get(@NotNull String key, @NotNull String fallback) {
        String v = PROPS.getProperty(key);
        return v != null ? v : fallback;
    }

    private static @NotNull Properties load() {
        Properties p = new Properties();
        try (InputStream in = AppInfo.class.getResourceAsStream("/version.properties")) {
            if (in != null) {
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    p.load(reader);
                }
            }
        } catch (IOException ignored) {}
        return p;
    }
}
