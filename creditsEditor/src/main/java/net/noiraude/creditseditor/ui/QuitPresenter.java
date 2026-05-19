package net.noiraude.creditseditor.ui;

import java.util.Objects;

import net.noiraude.creditseditor.bus.DocumentBus;

import org.jetbrains.annotations.NotNull;

/** Terminates the application after confirming any pending changes. */
final class QuitPresenter {

    private final @NotNull SessionHolder holder;
    private final @NotNull UnsavedChangesGate gate;

    QuitPresenter(@NotNull DocumentBus bus, @NotNull SessionHolder holder, @NotNull UnsavedChangesGate gate) {
        this.holder = Objects.requireNonNull(holder);
        this.gate = Objects.requireNonNull(gate);
        bus.addListener(DocumentBus.TOPIC_REQUEST_QUIT, e -> onQuit());
    }

    private void onQuit() {
        if (gate.shouldAbort()) return;
        holder.closeIfPresent();
        System.exit(0);
    }
}
