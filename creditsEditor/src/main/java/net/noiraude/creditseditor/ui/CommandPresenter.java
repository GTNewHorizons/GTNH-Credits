package net.noiraude.creditseditor.ui;

import java.util.Objects;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.EditAbortedException;

import org.jetbrains.annotations.NotNull;

/** Executes commands against the active session. */
final class CommandPresenter implements CommandExecutor {

    private final @NotNull DocumentBus bus;
    private final @NotNull SessionHolder holder;

    CommandPresenter(@NotNull DocumentBus bus, @NotNull SessionHolder holder) {
        this.bus = Objects.requireNonNull(bus);
        this.holder = Objects.requireNonNull(holder);
    }

    @Override
    public void execute(@NotNull Command cmd) {
        holder.ifPresent(session -> runOn(session, cmd));
    }

    private void runOn(@NotNull EditorSession session, @NotNull Command cmd) {
        try {
            session.execute(cmd);
        } catch (EditAbortedException ex) {
            bus.fireEditAborted(ex);
        }
        afterCommand(session);
    }

    void afterCommand(@NotNull EditorSession session) {
        bus.fireCommandStackChanged(session.commandStackSnapshot());
        bus.fireDirtyChanged(session.isDirty());
    }
}
