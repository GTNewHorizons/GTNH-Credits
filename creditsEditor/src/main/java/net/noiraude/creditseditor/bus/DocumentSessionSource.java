package net.noiraude.creditseditor.bus;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

/** Read-only access to the application's current document session. */
@FunctionalInterface
public interface DocumentSessionSource {

    @NotNull
    Optional<DocumentSession> session();
}
