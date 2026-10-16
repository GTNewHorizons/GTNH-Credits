package net.noiraude.creditseditor;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import javax.swing.SwingUtilities;

import net.noiraude.creditseditor.resource.ResourceManager;
import net.noiraude.creditseditor.service.TranslationCatalog;
import net.noiraude.creditseditor.ui.AppIcons;
import net.noiraude.creditseditor.ui.AppInfo;
import net.noiraude.creditseditor.ui.EditorApplication;
import net.noiraude.libcredits.validation.CreditsValidator;

import com.formdev.flatlaf.FlatLightLaf;

/** GUI editor and read-only, headless content validation entry point. */
public class CreditsEditorApp {

    public static void main(String[] args) {
        new CreditsEditorApp().start(args);
    }

    public void start(String[] argv) {
        Args args;
        try {
            args = Args.parse(argv);
        } catch (IllegalArgumentException ex) {
            System.err.println(cmd() + ": " + ex.getMessage());
            System.err.println("Try '" + cmd() + " --help' for usage.");
            System.exit(2);
            return;
        }
        if (args.help()) {
            printHelp();
            return;
        }
        if (args.version()) {
            System.out.println(cmd() + " " + AppInfo.version());
            return;
        }
        if (args.schema()) {
            try (InputStream schema = CreditsEditorApp.class.getResourceAsStream("/credits.schema.json")) {
                if (schema == null) throw new IllegalStateException("Missing bundled credits schema");
                System.out.print(new String(schema.readAllBytes(), StandardCharsets.UTF_8));
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                System.exit(1);
            }
            return;
        }
        if (args.validate()) {
            System.exit(validate(args.resource(), args.instance(), System.out, System.err));
            return;
        }
        FlatLightLaf.setup();
        AppIcons.applyToTaskbar();
        SwingUtilities.invokeLater(() -> new EditorApplication().start(args.resource(), args.instance()));
    }

    /** Validates without creating or rewriting any files. External existence needs an explicit local instance. */
    public static int validate(String path, String instance, PrintStream output, PrintStream errors) {
        try (ResourceManager resources = ResourceManager.open(path)) {
            resources.loadDocuments();
            TranslationCatalog catalog = instance == null ? TranslationCatalog.empty()
                : TranslationCatalog.load(Path.of(instance));
            List<String> problems = CreditsValidator.validateContent(
                resources.getCreditsDoc(),
                resources.getLangDoc(),
                catalog.isAvailable() ? key -> catalog.resolve(key, "en_US")
                    .isPresent() : null);
            if (!problems.isEmpty()) {
                problems.forEach(errors::println);
                return 1;
            }
            output.println("Valid credits: " + resources.displayPath());
            if (
                !catalog.isAvailable() && resources.getCreditsDoc().persons.stream()
                    .flatMap(person -> person.memberships.stream())
                    .flatMap(membership -> membership.roles.stream())
                    .anyMatch(role -> role.isExternal())
            ) {
                output.println(
                    "External translation existence was not checked; "
                        + (instance == null ? "supply --instance=<path> to check instance language resources."
                            : "Minecraft 1.7.10 base language resources were not found in the selected instance."));
            }
            return 0;
        } catch (Exception ex) {
            errors.println("Credits validation failed: " + ex.getMessage());
            return 1;
        }
    }

    private static String cmd() {
        return System.getProperty("app.name", "gtnh-credits-editor");
    }

    private static void printHelp() {
        System.out.println("Usage: " + cmd() + " [OPTIONS] [<path>]");
        System.out.println(
            "  <path>                Modpack root, credits.json, content directory, resource directory or pack ZIP.");
        System.out.println("                        Without a path, use File > Open / Open Recent / New.");
        System.out.println("  -h, --help            Show this help.");
        System.out.println("  -v, --version         Show version.");
        System.out.println("      --resource=<path> Equivalent to positional <path>.");
        System.out.println(
            "      --instance=<path> Read translations from a local Minecraft instance for previews/validation.");
        System.out.println(
            "      --validate        Validate existing content without a GUI or file changes; requires <path>.");
        System.out.println("      --schema          Print the bundled versioned JSON schema.");
    }

    record Args(boolean help, boolean version, String resource, boolean validate, boolean schema, String instance) {

        static Args parse(String[] argv) {
            boolean help = false, version = false, validate = false, schema = false, end = false;
            String resource = null, instance = null;
            for (String arg : argv) {
                if (end || !arg.startsWith("-")) {
                    if (resource != null) throw new IllegalArgumentException("unexpected argument: " + arg);
                    resource = arg;
                } else if (arg.equals("--")) end = true;
                else if (arg.equals("-h") || arg.equals("--help")) help = true;
                else if (arg.equals("-v") || arg.equals("--version")) version = true;
                else if (arg.equals("--validate")) validate = true;
                else if (arg.equals("--schema")) schema = true;
                else if (arg.startsWith("--resource=")) {
                    if (resource != null) throw new IllegalArgumentException("duplicate resource path");
                    resource = arg.substring("--resource=".length());
                } else if (arg.startsWith("--instance=")) {
                    if (instance != null) throw new IllegalArgumentException("duplicate instance path");
                    instance = arg.substring("--instance=".length());
                } else throw new IllegalArgumentException("unknown option: " + arg);
            }
            if (resource != null && resource.isBlank() || instance != null && instance.isBlank())
                throw new IllegalArgumentException("paths must not be blank");
            if (validate && resource == null && !help)
                throw new IllegalArgumentException("--validate requires a credits path");
            if (validate && schema) throw new IllegalArgumentException("choose --validate or --schema");
            return new Args(help, version, resource, validate, schema, instance);
        }
    }
}
