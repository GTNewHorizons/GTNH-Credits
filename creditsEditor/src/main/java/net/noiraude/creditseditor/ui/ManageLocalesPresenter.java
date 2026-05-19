package net.noiraude.creditseditor.ui;

import java.awt.Frame;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.ui.dialog.ManageLocalesDialog;

import org.jetbrains.annotations.NotNull;

/** Opens the manage-locales window in response to bus requests. */
final class ManageLocalesPresenter {

    private final @NotNull DocumentBus bus;
    private final @NotNull Frame owner;
    private final @NotNull Supplier<Optional<EditorSession>> sessionSupplier;
    private final @NotNull CommandExecutor onCommand;

    ManageLocalesPresenter(@NotNull DocumentBus bus, @NotNull Frame owner,
        @NotNull Supplier<Optional<EditorSession>> sessionSupplier, @NotNull CommandExecutor onCommand) {
        this.bus = Objects.requireNonNull(bus);
        this.owner = Objects.requireNonNull(owner);
        this.sessionSupplier = Objects.requireNonNull(sessionSupplier);
        this.onCommand = Objects.requireNonNull(onCommand);
        bus.addListener(DocumentBus.TOPIC_REQUEST_MANAGE_LOCALES, e -> open());
    }

    private void open() {
        sessionSupplier.get()
            .ifPresent(s -> new ManageLocalesDialog(owner, bus, s, onCommand).setVisible(true));
    }
}
