package net.noiraude.creditseditor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.SwingUtilities;

import net.noiraude.creditseditor.ui.MainWindow;

import com.formdev.flatlaf.FlatLightLaf;

public class CreditsEditorApp {

    private ResourceManager resourceManager;

    public static void main(String[] args) {
        FlatLightLaf.setup();
        new CreditsEditorApp().start(args);
    }

    public CreditsEditorApp() {}

    public void start(String[] argv) {
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

        if (args.resource() != null) {
            try {
                resourceManager = ResourceManager.open(args.resource());
            } catch (IOException e) {
                System.err.println(cmd() + ": " + e.getMessage());
                System.exit(1);
            }
        }

        final ResourceManager rm = resourceManager;
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow(rm);
            window.setVisible(true);
        });
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    private static String cmd() {
        return System.getProperty("app.name", "gtnh-credits-editor");
    }

    private static String version() {
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

    record Args(boolean help, boolean version, String resource) {

        static Args parse(String[] argv) {
            Builder b = new Builder();
            for (String arg : argv) b.accept(arg);
            return b.build();
        }

        private static final class Builder {

            boolean help, version, endOfOptions;
            String resource;

            void accept(String arg) {
                if (endOfOptions || !arg.startsWith("-")) {
                    if (resource != null) throw new IllegalArgumentException("unexpected argument: " + arg);
                    resource = arg;
                    return;
                }
                applyOption(
                    arg.substring(1)
                        .split("=", 2));
            }

            private void applyOption(String[] kv) {
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

            Args build() {
                return new Args(help, version, resource);
            }
        }
    }
}
