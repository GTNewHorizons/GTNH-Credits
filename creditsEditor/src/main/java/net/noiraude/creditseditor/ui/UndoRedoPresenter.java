package net.noiraude.creditseditor.ui;

import java.util.Objects;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.EditAbortedException;

import org.jetbrains.annotations.NotNull;

/** Dispatches undo and redo requests to the active session. */
final class UndoRedoPresenter {

    private final @NotNull DocumentBus bus;
    private final @NotNull SessionHolder holder;
    private final @NotNull CommandPresenter commandPresenter;

    UndoRedoPresenter(@NotNull DocumentBus bus, @NotNull SessionHolder holder,
        @NotNull CommandPresenter commandPresenter) {
        this.bus = Objects.requireNonNull(bus);
        this.holder = Objects.requireNonNull(holder);
        this.commandPresenter = Objects.requireNonNull(commandPresenter);
        bus.addListener(DocumentBus.TOPIC_REQUEST_UNDO, e -> onUndo());
        bus.addListener(DocumentBus.TOPIC_REQUEST_REDO, e -> onRedo());
    }

    private void onUndo() {
        holder.ifPresent(session -> {
            if (!session.canUndo()) return;
            try {
                session.undo();
            } catch (EditAbortedException ex) {
                bus.fireEditAborted(ex);
            }
            commandPresenter.afterCommand(session);
        });
    }

    private void onRedo() {
        holder.ifPresent(session -> {
            if (!session.canRedo()) return;
            try {
                session.redo();
            } catch (EditAbortedException ex) {
                bus.fireEditAborted(ex);
            }
            commandPresenter.afterCommand(session);
        });
    }
}
