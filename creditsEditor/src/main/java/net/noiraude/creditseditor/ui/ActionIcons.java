package net.noiraude.creditseditor.ui;

import java.awt.Image;
import java.awt.image.BaseMultiResolutionImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.jetbrains.annotations.NotNull;

/** Loader for the bundled toolbar action icons. */
public final class ActionIcons {

    private static final Logger LOG = System.getLogger(ActionIcons.class.getName());

    private static final int[] SIZES = { 16, 24, 32, 48 };

    private ActionIcons() {}

    public static @NotNull ImageIcon open() {
        return load("document-open");
    }

    public static @NotNull ImageIcon save() {
        return load("document-save");
    }

    public static @NotNull ImageIcon saveAs() {
        return load("document-save-as");
    }

    public static @NotNull ImageIcon undo() {
        return load("edit-undo");
    }

    public static @NotNull ImageIcon redo() {
        return load("edit-redo");
    }

    public static @NotNull ImageIcon manageLocales() {
        return load("preferences-desktop-locale");
    }

    private static @NotNull ImageIcon load(@NotNull String name) {
        List<Image> variants = new ArrayList<>(SIZES.length);
        for (int size : SIZES) {
            String path = "/icons/tango/" + size + "x" + size + "/" + name + ".png";
            try (InputStream in = ActionIcons.class.getResourceAsStream(path)) {
                if (in != null) variants.add(ImageIO.read(in));
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Failed to load icon resource: " + path, ex);
            }
        }
        if (variants.isEmpty()) return new ImageIcon();
        return new ImageIcon(new BaseMultiResolutionImage(variants.toArray(Image[]::new)));
    }
}
