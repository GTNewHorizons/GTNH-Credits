package net.noiraude.creditseditor.bus;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Test-only mutable source: lets a test rebind the session it exposes to a {@link DocumentBus}. */
public final class MutableSessionSource implements DocumentSessionSource {

    private @Nullable DocumentSession session;

    public void set(@NotNull DocumentSession session) {
        this.session = session;
    }

    @Override
    public @NotNull Optional<DocumentSession> session() {
        return Optional.ofNullable(session);
    }
}
