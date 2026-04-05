package net.noiraude.creditseditor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CreditsEditorApp {

    private ResourceManager resourceManager;

    public static void main(String[] args) {
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

        // TODO: launch GUI (resourceManager may be null; File menu sets it)
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    // -----------------------------------------------------------------------

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
            boolean help = false, version = false;
            String resource = null;
            boolean endOfOptions = false;

            for (String arg : argv) {
                if (endOfOptions || !arg.startsWith("-")) {
                    if (resource != null) throw new IllegalArgumentException("unexpected argument: " + arg);
                    resource = arg;
                    continue;
                }
                String[] parts = arg.substring(1).split("=", 2);
                String name  = parts[0];
                String value = parts.length > 1 ? parts[1] : null;
                switch (name) {
                    case "h", "-help"    -> help = true;
                    case "v", "-version" -> version = true;
                    case "-"             -> endOfOptions = true;
                    case "-resource"     -> {
                        if (value == null) throw new IllegalArgumentException("--resource requires a value");
                        if (resource != null) throw new IllegalArgumentException("unexpected argument: " + arg);
                        resource = value;
                    }
                    default -> throw new IllegalArgumentException("unknown option: " + arg);
                }
            }
            return new Args(help, version, resource);
        }
    }
}
