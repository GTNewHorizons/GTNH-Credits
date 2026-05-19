package net.noiraude.creditseditor.ui;

import java.awt.Frame;
import java.util.Objects;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.ui.dialog.AboutDialog;
import net.noiraude.creditseditor.ui.dialog.ShortcutsDialog;

import org.jetbrains.annotations.NotNull;

/** Opens the editor's informational windows. */
final class HelpPresenter {

    private final @NotNull Frame owner;

    HelpPresenter(@NotNull DocumentBus bus, @NotNull Frame owner) {
        this.owner = Objects.requireNonNull(owner);
        bus.addListener(DocumentBus.TOPIC_REQUEST_SHORTCUTS, e -> openShortcuts());
        bus.addListener(DocumentBus.TOPIC_REQUEST_ABOUT, e -> openAbout());
    }

    private void openShortcuts() {
        new ShortcutsDialog(owner).setVisible(true);
    }

    private void openAbout() {
        new AboutDialog(owner).setVisible(true);
    }
}
