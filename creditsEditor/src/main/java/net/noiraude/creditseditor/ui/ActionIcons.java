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
final class ActionIcons {

    private static final Logger LOG = System.getLogger(ActionIcons.class.getName());

    private static final int[] SIZES = { 16, 24, 32, 48 };

    private ActionIcons() {}

    static @NotNull ImageIcon open() {
        return load("document-open");
    }

    static @NotNull ImageIcon newDoc() {
        return load("document-new");
    }

    static @NotNull ImageIcon save() {
        return load("document-save");
    }

    static @NotNull ImageIcon saveAs() {
        return load("document-save-as");
    }

    static @NotNull ImageIcon undo() {
        return load("edit-undo");
    }

    static @NotNull ImageIcon redo() {
        return load("edit-redo");
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
