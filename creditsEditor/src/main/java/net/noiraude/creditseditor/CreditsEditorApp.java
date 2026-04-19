package net.noiraude.creditseditor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.*;

import net.noiraude.creditseditor.ui.MainWindow;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.formdev.flatlaf.FlatLightLaf;

public class CreditsEditorApp {

    public static void main(@NotNull String[] args) {
        FlatLightLaf.setup();
        new CreditsEditorApp().start(args);
    }

    @Contract(pure = true)
    public CreditsEditorApp() {}

    public void start(@NotNull String[] argv) {
        Args args;
        try {
            args = Args.parse(argv);
        } catch (IllegalArgumentException e) {
            String cmd = cmd();
            System.err.println(cmd + ": " + e.getMessage());
            System.err.println("Try '" + cmd + " --help' for usage.");
            System.exit(1);
            return;
        }

        if (args.help()) {
            printHelp();
            System.exit(0);
        }

        if (args.version()) {
            System.out.println(cmd() + " " + version());
            System.exit(0);
        }

        final String resourcePath = args.resource();
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow(resourcePath);
            window.setVisible(true);
        });
    }

    @Contract(pure = true)
    private static @NotNull String cmd() {
        return System.getProperty("app.name", "gtnh-credits-editor");
    }

    @Contract(pure = true)
    private static @NotNull String version() {
        try (InputStream in = CreditsEditorApp.class.getResourceAsStream("/version.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                return p.getProperty("version", "unknown");
            }
        } catch (IOException ignored) {}
        return "unknown";
    }

    private static void printHelp() {
        String cmd = cmd();
        System.out.println("Usage: " + cmd + " [OPTIONS] [<path>]");
        System.out.println();
        System.out.println("  <path>               Resource directory or .zip resource pack to open on startup.");
        System.out.println("                       Without a path, use File > Open / New from the GUI.");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help           Show this help and exit.");
        System.out.println("  -v, --version        Show version and exit.");
        System.out.println("      --resource=<path>");
        System.out.println("                       Equivalent to the positional <path> argument.");
    }

    // -----------------------------------------------------------------------

    record Args(boolean help, boolean version, @Nullable String resource) {

        static @NotNull Args parse(String @NotNull [] argv) {
            Builder b = new Builder();
            for (String arg : argv) b.accept(arg);
            return b.build();
        }

        private static final class Builder {

            boolean help, version, endOfOptions;
            @Nullable
            String resource;

            void accept(@NotNull String arg) {
                if (endOfOptions || !arg.startsWith("-")) {
                    if (resource != null) throw new IllegalArgumentException("unexpected argument: " + arg);
                    resource = arg;
                    return;
                }
                applyOption(
                    arg.substring(1)
                        .split("=", 2));
            }

            private void applyOption(String @NotNull [] kv) {
                switch (kv[0]) {
                    case "h", "-help" -> help = true;
                    case "v", "-version" -> version = true;
                    case "-" -> endOfOptions = true;
                    case "-resource" -> {
                        if (kv.length < 2) throw new IllegalArgumentException("--resource requires a value");
                        if (resource != null) throw new IllegalArgumentException("duplicate option: --resource");
                        resource = kv[1];
                    }
                    default -> throw new IllegalArgumentException("unknown option: -" + kv[0]);
                }
            }

            @Contract(" -> new")
            @NotNull
            Args build() {
                return new Args(help, version, resource);
            }
        }
    }
}
