package net.noiraude.creditseditor.ui;

import java.awt.Frame;
import java.util.Objects;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.ui.dialog.ManageLocalesDialog;

import org.jetbrains.annotations.NotNull;

/** Opens the manage-locales window in response to bus requests. */
final class ManageLocalesPresenter {

    private final @NotNull DocumentBus bus;
    private final @NotNull Frame owner;
    private final @NotNull SessionHolder holder;
    private final @NotNull CommandExecutor onCommand;

    ManageLocalesPresenter(@NotNull DocumentBus bus, @NotNull Frame owner, @NotNull SessionHolder holder,
        @NotNull CommandExecutor onCommand) {
        this.bus = Objects.requireNonNull(bus);
        this.owner = Objects.requireNonNull(owner);
        this.holder = Objects.requireNonNull(holder);
        this.onCommand = Objects.requireNonNull(onCommand);
        bus.addListener(DocumentBus.TOPIC_REQUEST_MANAGE_LOCALES, e -> open());
    }

    private void open() {
        holder.ifPresent(s -> new ManageLocalesDialog(owner, bus, s, onCommand).setVisible(true));
    }
}
