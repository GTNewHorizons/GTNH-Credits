package net.noiraude.creditseditor.ui;

import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.noiraude.libcredits.pack.CreditsLayout.Format;

/** Open resolves existing content; creation explicitly selects a content directory or resource-pack layout. */
public final class CreditsResourceChooser {

    public record Creation(Path path, Format format) {}

    private CreditsResourceChooser() {}

    public static Optional<Path> chooseForOpen(Component parent) {
        JFileChooser chooser = chooser("filechooser.open.title");
        chooser.setFileFilter(new FileNameExtensionFilter(I18n.get("filechooser.filter.credits"), "json", "zip"));
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return Optional.empty();
        return Optional.of(
            chooser.getSelectedFile()
                .toPath());
    }

    public static Optional<Creation> chooseForNew(Component parent) {
        return create(parent, "filechooser.new.title");
    }

    public static Optional<Creation> chooseForSaveAs(Component parent) {
        return create(parent, "filechooser.save_as.title");
    }

    private static Optional<Creation> create(Component parent, String title) {
        JFileChooser chooser = chooser(title);
        FileFilter content = new DirectoryFileFilter(I18n.get("filechooser.filter.content"));
        FileFilter resource = new DirectoryFileFilter(I18n.get("filechooser.filter.resource"));
        FileFilter zip = new FileNameExtensionFilter(I18n.get("filechooser.filter.zip"), "zip");
        chooser.addChoosableFileFilter(content);
        chooser.addChoosableFileFilter(resource);
        chooser.addChoosableFileFilter(zip);
        chooser.setFileFilter(content);
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return Optional.empty();
        Path path = chooser.getSelectedFile()
            .toPath();
        if (
            chooser.getFileFilter() == zip && !path.toString()
                .toLowerCase(Locale.ROOT)
                .endsWith(".zip")
        ) path = Path.of(path + ".zip");
        return Optional.of(
            new Creation(
                path,
                chooser.getFileFilter() == content ? Format.CONTENT_DIRECTORY
                    : chooser.getFileFilter() == resource ? Format.RESOURCE_DIRECTORY : Format.RESOURCE_PACK));
    }

    private static JFileChooser chooser(String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(I18n.get(title));
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setAcceptAllFileFilterUsed(false);
        var recent = RecentLocations.user()
            .paths();
        if (!recent.isEmpty()) {
            File parent = Path.of(recent.getFirst())
                .toFile()
                .getParentFile();
            if (parent != null && parent.isDirectory()) chooser.setCurrentDirectory(parent);
        }
        return chooser;
    }
}
