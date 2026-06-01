package net.noiraude.creditseditor.ui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

import net.noiraude.creditseditor.bus.DocumentBus;

import org.jetbrains.annotations.NotNull;

/** Adapter from window-lifecycle events to application-lifecycle requests. */
public final class AppLifecycleController extends WindowAdapter {

    private final @NotNull DocumentBus bus;

    public AppLifecycleController(@NotNull DocumentBus bus) {
        this.bus = Objects.requireNonNull(bus);
    }

    @Override
    public void windowClosing(@NotNull WindowEvent e) {
        bus.fireQuitRequested();
    }
}
