package net.noiraude.creditseditor.ui;

import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * File chooser helper for credits resources.
 *
 * <p>
 * A credits resource is either a {@code .zip} archive or a directory. This helper centralizes
 * the dialog wiring (selection mode, filter list, default filter, ".zip" auto-append) for the
 * Open, New, and Save As flows so callers only need to ask for a {@link Path}.
 */
public final class CreditsResourceChooser {

    private CreditsResourceChooser() {}

    public static @NotNull Optional<Path> chooseForOpen(@Nullable Component parent) {
        Filters filters = Filters.create();
        JFileChooser chooser = configured(I18n.get("filechooser.open.title"), filters);
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return Optional.empty();
        return Optional.of(
            Paths.get(
                chooser.getSelectedFile()
                    .getAbsolutePath()));
    }

    public static @NotNull Optional<Path> chooseForNew(@Nullable Component parent) {
        return chooseForCreate(parent, "filechooser.new.title");
    }

    public static @NotNull Optional<Path> chooseForSaveAs(@Nullable Component parent) {
        return chooseForCreate(parent, "filechooser.save_as.title");
    }

    private static @NotNull Optional<Path> chooseForCreate(@Nullable Component parent, @NotNull String titleKey) {
        Filters filters = Filters.create();
        JFileChooser chooser = configured(I18n.get(titleKey), filters);
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return Optional.empty();

        File selected = chooser.getSelectedFile();
        if (
            chooser.getFileFilter() == filters.zip() && !selected.getName()
                .toLowerCase()
                .endsWith(".zip")
        ) {
            selected = new File(selected.getAbsolutePath() + ".zip");
        }
        return Optional.of(Paths.get(selected.getAbsolutePath()));
    }

    private static @NotNull JFileChooser configured(@NotNull String title, @NotNull Filters filters) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(filters.zip());
        chooser.addChoosableFileFilter(filters.dir());
        chooser.setFileFilter(filters.zip());
        return chooser;
    }

    private record Filters(@NotNull FileFilter zip, @NotNull FileFilter dir) {

        static @NotNull Filters create() {
            return new Filters(
                new FileNameExtensionFilter(I18n.get("filechooser.filter.zip"), "zip"),
                new DirectoryFileFilter(I18n.get("filechooser.filter.dir")));
        }
    }
}
