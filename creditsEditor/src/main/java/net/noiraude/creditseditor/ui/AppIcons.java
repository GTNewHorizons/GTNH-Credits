package net.noiraude.creditseditor.ui;

import java.awt.Image;
import java.awt.Taskbar;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Loads the bundled application icons and applies them to the window and the OS taskbar. */
public final class AppIcons {

    private static final String[] RESOURCES = { "/icons/icon16.png", "/icons/icon32.png", "/icons/icon64.png",
        "/icons/icon128.png", };

    private static @NotNull List<Image> cached = List.of();

    @Contract(pure = true)
    private AppIcons() {}

    public static @NotNull List<Image> load() {
        if (!cached.isEmpty()) return cached;
        List<Image> images = new ArrayList<>(RESOURCES.length);
        for (String path : RESOURCES) {
            try (InputStream in = AppIcons.class.getResourceAsStream(path)) {
                if (in != null) images.add(ImageIO.read(in));
            } catch (IOException ignored) {}
        }
        cached = List.copyOf(images);
        return cached;
    }

    /** Applies the largest available icon to the OS taskbar/dock when the platform supports it. */
    public static void applyToTaskbar() {
        List<Image> images = load();
        if (images.isEmpty()) return;
        if (!Taskbar.isTaskbarSupported()) return;
        Taskbar taskbar = Taskbar.getTaskbar();
        if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) return;
        try {
            taskbar.setIconImage(images.getLast());
        } catch (UnsupportedOperationException | SecurityException ignored) {}
    }
}
