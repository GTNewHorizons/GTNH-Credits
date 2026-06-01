package net.noiraude.creditseditor.ui;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * {@link JFileChooser} filter that accepts only directories.
 *
 * <p>
 * The dropdown description is supplied by the caller so this class stays I18n-agnostic and
 * reusable across any flow that wants a "directories only" entry in the filter list.
 */
public final class DirectoryFileFilter extends FileFilter {

    private final @NotNull String description;

    public DirectoryFileFilter(@NotNull String description) {
        this.description = description;
    }

    @Override
    public boolean accept(@NotNull File f) {
        return f.isDirectory();
    }

    @Contract(pure = true)
    @Override
    public @NotNull String getDescription() {
        return description;
    }
}
