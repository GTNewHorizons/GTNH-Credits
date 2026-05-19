package net.noiraude.creditseditor.ui;

import java.awt.Frame;
import java.util.Objects;

import net.noiraude.creditseditor.bus.DocumentBus;

import org.jetbrains.annotations.NotNull;

/** Persists the active session to its current location. */
final class SavePresenter implements SaveService {

    private final @NotNull DocumentBus bus;
    private final @NotNull Frame owner;
    private final @NotNull SessionHolder holder;

    SavePresenter(@NotNull DocumentBus bus, @NotNull Frame owner, @NotNull SessionHolder holder) {
        this.bus = Objects.requireNonNull(bus);
        this.owner = Objects.requireNonNull(owner);
        this.holder = Objects.requireNonNull(holder);
        bus.addListener(DocumentBus.TOPIC_REQUEST_SAVE, e -> trySave());
    }

    @Override
    public boolean trySave() {
        return holder.get()
            .map(this::writeAndFire)
            .orElse(false);
    }

    private boolean writeAndFire(@NotNull EditorSession session) {
        try {
            session.save();
        } catch (Exception ex) {
            ErrorPresenter.show(owner, I18n.get("dialog.save.error.title"), ex);
            return false;
        }
        bus.fireDirtyChanged(session.isDirty());
        return true;
    }
}
