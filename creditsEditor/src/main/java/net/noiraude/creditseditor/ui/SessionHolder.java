package net.noiraude.creditseditor.ui;

import java.util.Optional;
import java.util.function.Consumer;

import net.noiraude.creditseditor.bus.DocumentSession;
import net.noiraude.creditseditor.bus.DocumentSessionSource;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Shared, mutable reference to the editor's current session. */
final class SessionHolder implements DocumentSessionSource {

    private @Nullable EditorSession session;

    @Contract(pure = true)
    @NotNull
    Optional<EditorSession> get() {
        return Optional.ofNullable(session);
    }

    @Contract(pure = true)
    boolean isPresent() {
        return session != null;
    }

    @Override
    @Contract(pure = true)
    public @NotNull Optional<DocumentSession> session() {
        return Optional.ofNullable(session);
    }

    void ifPresent(@NotNull Consumer<EditorSession> action) {
        if (isPresent()) action.accept(session);
    }

    /** Replaces the current session, closing the previous one when present. */
    void set(@NotNull EditorSession newSession) {
        if (session == newSession) return;
        if (isPresent()) session.close();
        session = newSession;
    }

    /** Closes the current session, if any, and clears the holder. */
    void closeIfPresent() {
        if (!isPresent()) return;
        session.close();
        session = null;
    }
}
